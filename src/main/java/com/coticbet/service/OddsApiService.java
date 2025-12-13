package com.coticbet.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.coticbet.domain.entity.Event;
import com.coticbet.domain.entity.EventOption;
import com.coticbet.domain.enums.EventCategory;
import com.coticbet.domain.enums.EventStatus;
import com.coticbet.domain.enums.PricingModel;
import com.coticbet.dto.external.OddsApiEventResponse;
import com.coticbet.repository.EventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OddsApiService {

    private final RestTemplate restTemplate;
    private final EventRepository eventRepository;
    private final WebSocketService webSocketService;
    private final EventService eventService;

    @Value("${app.odds-api.key:74b0d97c2323c279b2e4306b3f7fd045}")
    private String apiKey;

    @Value("${app.odds-api.base-url:https://api.the-odds-api.com/v4}")
    private String baseUrl;

    @Value("${app.odds-api.sports:soccer_brazil_campeonato}")
    private String sports;

    @Value("${app.odds-api.regions:us}")
    private String regions;

    @Value("${app.odds-api.markets:h2h}")
    private String markets;

    /**
     * Scheduled job to fetch odds from The Odds API every 4 hours
     */
    @Scheduled(fixedRateString = "${app.odds-api.fetch-interval:14400000}") // 4 hours default
    public void fetchOddsFromApi() {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("⚠️ Odds API key not configured. Skipping fetch.");
            return;
        }

        log.info("🔄 Fetching odds from The Odds API...");

        try {
            String url = String.format(
                    "%s/sports/%s/odds?apiKey=%s&regions=%s&markets=%s",
                    baseUrl, sports, apiKey, regions, markets);

            ResponseEntity<List<OddsApiEventResponse>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<OddsApiEventResponse>>() {
                    });

            List<OddsApiEventResponse> events = response.getBody();
            if (events == null || events.isEmpty()) {
                log.info("No events found from API");
                return;
            }

            log.info("📊 Received {} events from The Odds API", events.size());

            for (OddsApiEventResponse apiEvent : events) {
                processApiEvent(apiEvent);
            }

            log.info("✅ Odds sync completed");

        } catch (Exception e) {
            log.error("❌ Error fetching odds from API: {}", e.getMessage(), e);
        }
    }

    private void processApiEvent(OddsApiEventResponse apiEvent) {
        Optional<Event> existingEvent = eventRepository.findByExternalId(apiEvent.getId());

        if (existingEvent.isPresent()) {
            Event event = existingEvent.get();

            // Don't update if already LOCKED or SETTLED
            if (event.getStatus() == EventStatus.LOCKED ||
                    event.getStatus() == EventStatus.SETTLED ||
                    event.getStatus() == EventStatus.CANCELED) {
                log.debug("Skipping update for locked/settled event: {}", event.getTitle());
                return;
            }

            // Update odds
            updateEventOdds(event, apiEvent);
            event.setUpdatedAt(LocalDateTime.now());
            eventRepository.save(event);

            // Broadcast update
            webSocketService.broadcastEventUpdate(eventService.toResponse(event));
            log.debug("Updated odds for event: {}", event.getTitle());

        } else {
            // Create new event
            Event newEvent = createEventFromApi(apiEvent);
            eventRepository.save(newEvent);
            log.info("Created new event: {}", newEvent.getTitle());
        }
    }

    private Event createEventFromApi(OddsApiEventResponse apiEvent) {
        List<EventOption> options = extractOptionsFromApi(apiEvent);

        LocalDateTime commenceTime = parseCommenceTime(apiEvent.getCommenceTime());

        return Event.builder()
                .title(String.format("%s vs %s", apiEvent.getHomeTeam(), apiEvent.getAwayTeam()))
                .category(EventCategory.SPORTS)
                .status(EventStatus.OPEN)
                .pricingModel(PricingModel.FIXED_ODDS)
                .externalId(apiEvent.getId())
                .commenceTime(commenceTime)
                .options(options)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private void updateEventOdds(Event event, OddsApiEventResponse apiEvent) {
        List<EventOption> newOptions = extractOptionsFromApi(apiEvent);

        for (EventOption newOpt : newOptions) {
            event.getOptions().stream()
                    .filter(opt -> opt.getName().equals(newOpt.getName()))
                    .findFirst()
                    .ifPresent(opt -> opt.setCurrentOdd(newOpt.getCurrentOdd()));
        }
    }

    private List<EventOption> extractOptionsFromApi(OddsApiEventResponse apiEvent) {
        List<EventOption> options = new ArrayList<>();

        if (apiEvent.getBookmakers() == null || apiEvent.getBookmakers().isEmpty()) {
            // Default options if no bookmakers
            options.add(createOption(apiEvent.getHomeTeam(), new BigDecimal("2.00")));
            options.add(createOption("Draw", new BigDecimal("3.00")));
            options.add(createOption(apiEvent.getAwayTeam(), new BigDecimal("2.50")));
            return options;
        }

        // Get first bookmaker's h2h market
        OddsApiEventResponse.Bookmaker bookmaker = apiEvent.getBookmakers().get(0);
        if (bookmaker.getMarkets() == null || bookmaker.getMarkets().isEmpty()) {
            return options;
        }

        OddsApiEventResponse.Market market = bookmaker.getMarkets().stream()
                .filter(m -> "h2h".equals(m.getKey()))
                .findFirst()
                .orElse(bookmaker.getMarkets().get(0));

        for (OddsApiEventResponse.Outcome outcome : market.getOutcomes()) {
            BigDecimal odd = BigDecimal.valueOf(outcome.getPrice())
                    .setScale(2, RoundingMode.HALF_UP);
            options.add(createOption(outcome.getName(), odd));
        }

        // Add Draw option if not present (for soccer)
        boolean hasDraw = options.stream()
                .anyMatch(opt -> opt.getName().equalsIgnoreCase("Draw"));
        if (!hasDraw && apiEvent.getSportKey().contains("soccer")) {
            options.add(createOption("Draw", new BigDecimal("3.50")));
        }

        return options;
    }

    private EventOption createOption(String name, BigDecimal odd) {
        return EventOption.builder()
                .id(UUID.randomUUID().toString())
                .name(name)
                .currentOdd(odd)
                .seedOdd(odd)
                .totalStaked(BigDecimal.ZERO)
                .build();
    }

    private LocalDateTime parseCommenceTime(String commenceTime) {
        try {
            OffsetDateTime odt = OffsetDateTime.parse(commenceTime);
            return odt.toLocalDateTime();
        } catch (Exception e) {
            log.warn("Could not parse commence time: {}, using now", commenceTime);
            return LocalDateTime.now().plusHours(24);
        }
    }

    /**
     * Manual trigger for testing
     */
    public void triggerFetch() {
        fetchOddsFromApi();
    }
}

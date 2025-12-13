package com.coticbet.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.coticbet.domain.entity.Bet;
import com.coticbet.domain.entity.Event;
import com.coticbet.domain.entity.EventOption;
import com.coticbet.domain.enums.BetStatus;
import com.coticbet.domain.enums.EventCategory;
import com.coticbet.domain.enums.EventStatus;
import com.coticbet.dto.request.CreateEventRequest;
import com.coticbet.dto.response.EventOptionResponse;
import com.coticbet.dto.response.EventResponse;
import com.coticbet.exception.BusinessException;
import com.coticbet.exception.ResourceNotFoundException;
import com.coticbet.repository.BetRepository;
import com.coticbet.repository.EventRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final BetRepository betRepository;
    private final WebSocketService webSocketService;

    public EventResponse createInternalEvent(CreateEventRequest request) {
        List<EventOption> options = request.getOptions().stream()
                .map(opt -> EventOption.builder()
                        .id(UUID.randomUUID().toString())
                        .name(opt.getName())
                        .currentOdd(opt.getInitialOdd())
                        .seedOdd(opt.getInitialOdd())
                        .totalStaked(BigDecimal.ZERO)
                        .build())
                .collect(Collectors.toList());

        Event event = Event.builder()
                .title(request.getTitle())
                .category(EventCategory.INTERNAL)
                .status(EventStatus.PENDING)
                .pricingModel(request.getPricingModel())
                .commenceTime(request.getCommenceTime())
                .options(options)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        event = eventRepository.save(event);
        return toResponse(event);
    }

    public EventResponse updateEventStatus(String eventId, EventStatus newStatus) {
        Event event = findEventById(eventId);

        validateStatusTransition(event.getStatus(), newStatus);

        event.setStatus(newStatus);
        event.setUpdatedAt(LocalDateTime.now());
        event = eventRepository.save(event);

        // Notify users with bets on this event when locked
        if (newStatus == EventStatus.LOCKED) {
            notifyUsersOfLockedEvent(event);
        }

        // Broadcast event update via WebSocket
        webSocketService.broadcastEventUpdate(toResponse(event));

        return toResponse(event);
    }

    private void notifyUsersOfLockedEvent(Event event) {
        List<Bet> pendingBets = betRepository.findByEventIdAndStatus(event.getId(), BetStatus.PENDING);

        // Get unique user IDs
        pendingBets.stream()
                .map(Bet::getUserId)
                .distinct()
                .forEach(userId -> webSocketService.notifyUser(
                        userId,
                        "EVENT_LOCKED",
                        String.format("O evento '%s' foi travado. Aguarde o resultado!", event.getTitle())));
    }

    public List<EventResponse> getOpenEvents() {
        return eventRepository.findByStatusIn(List.of(EventStatus.OPEN))
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<EventResponse> getAllEvents() {
        return eventRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public EventResponse getEventById(String eventId) {
        return toResponse(findEventById(eventId));
    }

    public Event findEventById(String eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));
    }

    public Event saveEvent(Event event) {
        return eventRepository.save(event);
    }

    private void validateStatusTransition(EventStatus current, EventStatus target) {
        boolean valid = switch (current) {
            case PENDING -> target == EventStatus.OPEN || target == EventStatus.CANCELED;
            case OPEN -> target == EventStatus.LOCKED || target == EventStatus.CANCELED;
            case LOCKED -> target == EventStatus.SETTLED || target == EventStatus.CANCELED;
            case SETTLED, CANCELED -> false;
        };

        if (!valid) {
            throw new BusinessException(
                    String.format("Cannot transition from %s to %s", current, target));
        }
    }

    public EventResponse toResponse(Event event) {
        List<EventOptionResponse> options = event.getOptions().stream()
                .map(opt -> EventOptionResponse.builder()
                        .id(opt.getId())
                        .name(opt.getName())
                        .currentOdd(opt.getCurrentOdd())
                        .totalStaked(opt.getTotalStaked())
                        .build())
                .collect(Collectors.toList());

        return EventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .category(event.getCategory())
                .status(event.getStatus())
                .pricingModel(event.getPricingModel())
                .commenceTime(event.getCommenceTime())
                .options(options)
                .winnerOptionId(event.getWinnerOptionId())
                .build();
    }
}

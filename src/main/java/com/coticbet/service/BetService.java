package com.coticbet.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coticbet.domain.entity.Bet;
import com.coticbet.domain.entity.BetLeg;
import com.coticbet.domain.entity.Event;
import com.coticbet.domain.entity.EventOption;
import com.coticbet.domain.entity.User;
import com.coticbet.domain.enums.BetStatus;
import com.coticbet.domain.enums.BetType;
import com.coticbet.domain.enums.EventStatus;
import com.coticbet.domain.enums.LegStatus;
import com.coticbet.domain.enums.PricingModel;
import com.coticbet.domain.enums.Role;
import com.coticbet.domain.enums.TransactionOrigin;
import com.coticbet.dto.request.PlaceBetRequest;
import com.coticbet.dto.request.PlaceBetRequest.BetSelection;
import com.coticbet.dto.response.BetResponse;
import com.coticbet.dto.response.BetResponse.LegResponse;
import com.coticbet.exception.BusinessException;
import com.coticbet.exception.ResourceNotFoundException;
import com.coticbet.repository.BetRepository;
import com.coticbet.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BetService {

    private final BetRepository betRepository;
    private final UserRepository userRepository;
    private final EventService eventService;
    private final WalletService walletService;
    private final WebSocketService webSocketService;

    @Transactional
    public BetResponse placeBet(String userId, PlaceBetRequest request) {
        log.info("[BET] Iniciando aposta - userId={}, tipo={}, valor={}, seleções={}",
                userId, request.getSelections().size() == 1 ? "SINGLE" : "MULTIPLE",
                request.getAmount(), request.getSelections().size());

        // Check if user is admin - admins cannot bet
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (user.getRole() == Role.ADMIN) {
            log.warn("[BET] Administrador tentou fazer aposta - userId={}", userId);
            throw new BusinessException("Administrators cannot place bets");
        }

        List<BetSelection> selections = request.getSelections();

        // Validate: at least 1 selection
        if (selections == null || selections.isEmpty()) {
            throw new BusinessException("At least one selection is required");
        }

        // Validate: no duplicate events in multiple bet
        Set<String> eventIds = new HashSet<>();
        for (BetSelection sel : selections) {
            if (!eventIds.add(sel.getEventId())) {
                throw new BusinessException("Cannot select multiple options from the same event");
            }
        }

        // Determine bet type
        BetType betType = selections.size() == 1 ? BetType.SINGLE : BetType.MULTIPLE;

        // Build legs and calculate total odd
        List<BetLeg> legs = new ArrayList<>();
        BigDecimal totalOdd = BigDecimal.ONE;
        List<Event> eventsToUpdate = new ArrayList<>();

        for (BetSelection selection : selections) {
            Event event = eventService.findEventById(selection.getEventId());

            // Validate event is open for betting
            if (event.getStatus() != EventStatus.OPEN) {
                throw new BusinessException("Event '" + event.getTitle() + "' is not open for betting");
            }

            // Find the selected option
            EventOption selectedOption = event.getOptions().stream()
                    .filter(opt -> opt.getId().equals(selection.getOptionId()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Option", selection.getOptionId()));

            // Snapshot the locked odd
            BigDecimal lockedOdd = selectedOption.getCurrentOdd();
            totalOdd = totalOdd.multiply(lockedOdd);

            // Create leg
            BetLeg leg = BetLeg.builder()
                    .eventId(event.getId())
                    .eventTitle(event.getTitle())
                    .chosenOptionId(selection.getOptionId())
                    .chosenOptionLabel(selectedOption.getName())
                    .lockedOdd(lockedOdd)
                    .status(LegStatus.PENDING)
                    .build();

            legs.add(leg);

            // Update total staked on option
            selectedOption.setTotalStaked(
                    selectedOption.getTotalStaked().add(request.getAmount()));

            // Recalculate odds if dynamic parimutuel
            if (event.getPricingModel() == PricingModel.DYNAMIC_PARIMUTUEL) {
                recalculateDynamicOdds(event);
            }

            eventsToUpdate.add(event);
        }

        // Round total odd
        totalOdd = totalOdd.setScale(2, RoundingMode.HALF_UP);

        // Calculate potential payout
        BigDecimal potentialPayout = request.getAmount().multiply(totalOdd)
                .setScale(2, RoundingMode.HALF_UP);

        // Validate user has sufficient balance
        if (!walletService.hasBalance(userId, request.getAmount())) {
            log.warn("[BET] Saldo insuficiente - userId={}, valor={}", userId, request.getAmount());
            throw new BusinessException("Insufficient balance");
        }

        log.debug("[BET] Validações OK - odd total={}, payout potencial={}", totalOdd, potentialPayout);

        // Debit wallet
        walletService.debit(userId, request.getAmount(), TransactionOrigin.BET_ENTRY, null);

        // Create bet with legs
        Bet bet = Bet.builder()
                .userId(userId)
                .type(betType)
                .legs(legs)
                .totalOdd(totalOdd)
                .amount(request.getAmount())
                .potentialPayout(potentialPayout)
                .status(BetStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        // For backward compatibility, also set legacy fields for single bets
        if (betType == BetType.SINGLE) {
            BetLeg firstLeg = legs.get(0);
            bet.setEventId(firstLeg.getEventId());
            bet.setChosenOptionId(firstLeg.getChosenOptionId());
            bet.setLockedOdd(firstLeg.getLockedOdd());
        }

        bet = betRepository.save(bet);
        log.info("[BET] Aposta criada com sucesso - betId={}, userId={}, tipo={}, valor={}, payout potencial={}",
                bet.getId(), userId, betType, request.getAmount(), potentialPayout);

        // Save all updated events and broadcast
        for (Event event : eventsToUpdate) {
            eventService.saveEvent(event);
            webSocketService.broadcastEventUpdate(eventService.toResponse(event));
        }

        return toBetResponse(bet);
    }

    private void recalculateDynamicOdds(Event event) {
        BigDecimal totalStakedOnEvent = event.getOptions().stream()
                .map(EventOption::getTotalStaked)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        for (EventOption option : event.getOptions()) {
            if (option.getTotalStaked().compareTo(BigDecimal.ZERO) > 0) {
                // Odd = TotalOnEvent / TotalOnOption
                BigDecimal newOdd = totalStakedOnEvent.divide(
                        option.getTotalStaked(), 2, RoundingMode.HALF_UP);
                option.setCurrentOdd(newOdd);
            } else {
                // Use seed odd when no stakes on this option
                option.setCurrentOdd(option.getSeedOdd());
            }
        }
    }

    public List<BetResponse> getUserBets(String userId) {
        return betRepository.findByUserId(userId).stream()
                .map(this::toBetResponse)
                .collect(Collectors.toList());
    }

    public List<Bet> getPendingBetsForEvent(String eventId) {
        return betRepository.findByEventIdAndStatus(eventId, BetStatus.PENDING);
    }

    /**
     * Find all pending bets that have a leg for the given event
     */
    public List<Bet> findPendingBetsWithEventLeg(String eventId) {
        return betRepository.findByLegsEventIdAndStatus(eventId, BetStatus.PENDING);
    }

    public Bet saveBet(Bet bet) {
        return betRepository.save(bet);
    }

    /**
     * Convert Bet entity to BetResponse DTO
     */
    public BetResponse toBetResponse(Bet bet) {
        // Handle legacy bets (no legs array)
        if (bet.isLegacyBet()) {
            return convertLegacyBet(bet);
        }

        List<LegResponse> legResponses = bet.getLegs().stream()
                .map(leg -> LegResponse.builder()
                        .eventId(leg.getEventId())
                        .eventTitle(leg.getEventTitle())
                        .chosenOptionId(leg.getChosenOptionId())
                        .chosenOptionName(leg.getChosenOptionLabel())
                        .lockedOdd(leg.getLockedOdd())
                        .status(leg.getStatus())
                        .build())
                .collect(Collectors.toList());

        BetResponse.BetResponseBuilder builder = BetResponse.builder()
                .id(bet.getId())
                .type(bet.getType() != null ? bet.getType() : BetType.SINGLE)
                .totalOdd(bet.getTotalOdd())
                .amount(bet.getAmount())
                .potentialPayout(bet.getPotentialPayout())
                .status(bet.getStatus())
                .createdAt(bet.getCreatedAt())
                .legs(legResponses);

        // For backward compatibility, populate legacy fields from first leg
        if (!bet.getLegs().isEmpty()) {
            BetLeg firstLeg = bet.getLegs().get(0);
            builder.eventId(firstLeg.getEventId())
                    .eventTitle(firstLeg.getEventTitle())
                    .chosenOptionId(firstLeg.getChosenOptionId())
                    .chosenOptionName(firstLeg.getChosenOptionLabel())
                    .lockedOdd(firstLeg.getLockedOdd());
        }

        return builder.build();
    }

    /**
     * Convert legacy bet (without legs) to response
     */
    private BetResponse convertLegacyBet(Bet bet) {
        Event event = null;
        EventOption option = null;

        try {
            event = eventService.findEventById(bet.getEventId());
            option = event.getOptions().stream()
                    .filter(opt -> opt.getId().equals(bet.getChosenOptionId()))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            // Event may have been deleted
        }

        // Create a single leg response for legacy bet
        LegResponse legResponse = LegResponse.builder()
                .eventId(bet.getEventId())
                .eventTitle(event != null ? event.getTitle() : "Unknown Event")
                .chosenOptionId(bet.getChosenOptionId())
                .chosenOptionName(option != null ? option.getName() : "Unknown Option")
                .lockedOdd(bet.getLockedOdd())
                .status(bet.getStatus() == BetStatus.WON ? LegStatus.WON
                        : bet.getStatus() == BetStatus.LOST ? LegStatus.LOST : LegStatus.PENDING)
                .build();

        return BetResponse.builder()
                .id(bet.getId())
                .type(BetType.SINGLE)
                .totalOdd(bet.getLockedOdd())
                .amount(bet.getAmount())
                .potentialPayout(bet.getPotentialPayout())
                .status(bet.getStatus())
                .createdAt(bet.getCreatedAt())
                .legs(List.of(legResponse))
                // Legacy fields
                .eventId(bet.getEventId())
                .eventTitle(event != null ? event.getTitle() : "Unknown Event")
                .chosenOptionId(bet.getChosenOptionId())
                .chosenOptionName(option != null ? option.getName() : "Unknown Option")
                .lockedOdd(bet.getLockedOdd())
                .build();
    }
}

package com.coticbet.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coticbet.domain.entity.Bet;
import com.coticbet.domain.entity.Event;
import com.coticbet.domain.entity.EventOption;
import com.coticbet.domain.entity.User;
import com.coticbet.domain.enums.BetStatus;
import com.coticbet.domain.enums.EventStatus;
import com.coticbet.domain.enums.PricingModel;
import com.coticbet.domain.enums.Role;
import com.coticbet.domain.enums.TransactionOrigin;
import com.coticbet.dto.request.PlaceBetRequest;
import com.coticbet.dto.response.BetResponse;
import com.coticbet.exception.BusinessException;
import com.coticbet.exception.ResourceNotFoundException;
import com.coticbet.repository.BetRepository;
import com.coticbet.repository.UserRepository;

import lombok.RequiredArgsConstructor;

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
        // Check if user is admin - admins cannot bet
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (user.getRole() == Role.ADMIN) {
            throw new BusinessException("Administrators cannot place bets");
        }

        Event event = eventService.findEventById(request.getEventId());

        // Validate event is open for betting
        if (event.getStatus() != EventStatus.OPEN) {
            throw new BusinessException("Event is not open for betting");
        }

        // Find the selected option
        EventOption selectedOption = event.getOptions().stream()
                .filter(opt -> opt.getId().equals(request.getOptionId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Option", request.getOptionId()));

        // Validate user has sufficient balance
        if (!walletService.hasBalance(userId, request.getAmount())) {
            throw new BusinessException("Insufficient balance");
        }

        // Snapshot the locked odd
        BigDecimal lockedOdd = selectedOption.getCurrentOdd();
        BigDecimal potentialPayout = request.getAmount().multiply(lockedOdd)
                .setScale(2, RoundingMode.HALF_UP);

        // Debit wallet
        walletService.debit(userId, request.getAmount(), TransactionOrigin.BET_ENTRY, null);

        // Create bet
        Bet bet = Bet.builder()
                .userId(userId)
                .eventId(event.getId())
                .chosenOptionId(request.getOptionId())
                .lockedOdd(lockedOdd)
                .amount(request.getAmount())
                .potentialPayout(potentialPayout)
                .status(BetStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        bet = betRepository.save(bet);

        // Update total staked on option
        selectedOption.setTotalStaked(
                selectedOption.getTotalStaked().add(request.getAmount()));

        // Recalculate odds if dynamic parimutuel
        if (event.getPricingModel() == PricingModel.DYNAMIC_PARIMUTUEL) {
            recalculateDynamicOdds(event);
        }

        eventService.saveEvent(event);

        // Broadcast event update via WebSocket
        webSocketService.broadcastEventUpdate(eventService.toResponse(event));

        return toBetResponse(bet, event, selectedOption);
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
                .map(bet -> {
                    Event event = eventService.findEventById(bet.getEventId());
                    EventOption option = event.getOptions().stream()
                            .filter(opt -> opt.getId().equals(bet.getChosenOptionId()))
                            .findFirst()
                            .orElse(null);
                    return toBetResponse(bet, event, option);
                })
                .collect(Collectors.toList());
    }

    public List<Bet> getPendingBetsForEvent(String eventId) {
        return betRepository.findByEventIdAndStatus(eventId, BetStatus.PENDING);
    }

    public Bet saveBet(Bet bet) {
        return betRepository.save(bet);
    }

    private BetResponse toBetResponse(Bet bet, Event event, EventOption option) {
        return BetResponse.builder()
                .id(bet.getId())
                .eventId(bet.getEventId())
                .eventTitle(event != null ? event.getTitle() : null)
                .chosenOptionId(bet.getChosenOptionId())
                .chosenOptionName(option != null ? option.getName() : null)
                .lockedOdd(bet.getLockedOdd())
                .amount(bet.getAmount())
                .potentialPayout(bet.getPotentialPayout())
                .status(bet.getStatus())
                .createdAt(bet.getCreatedAt())
                .build();
    }
}

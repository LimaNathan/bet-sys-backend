package com.coticbet.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coticbet.domain.entity.Bet;
import com.coticbet.domain.entity.BetLeg;
import com.coticbet.domain.entity.Event;
import com.coticbet.domain.enums.BetStatus;
import com.coticbet.domain.enums.EventStatus;
import com.coticbet.domain.enums.LegStatus;
import com.coticbet.domain.enums.TransactionOrigin;
import com.coticbet.event.BetSettledEvent;
import com.coticbet.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementService {

    private final EventService eventService;
    private final BetService betService;
    private final WalletService walletService;
    private final WebSocketService webSocketService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void settleEvent(String eventId, String winnerOptionId) {
        Event event = eventService.findEventById(eventId);

        // Validate event status
        if (event.getStatus() != EventStatus.LOCKED) {
            throw new BusinessException("Event must be LOCKED before settlement");
        }

        // Validate winner option exists
        boolean optionExists = event.getOptions().stream()
                .anyMatch(opt -> opt.getId().equals(winnerOptionId));
        if (!optionExists) {
            throw new BusinessException("Invalid winner option ID");
        }

        // Update event
        event.setWinnerOptionId(winnerOptionId);
        event.setStatus(EventStatus.SETTLED);
        event.setUpdatedAt(LocalDateTime.now());
        eventService.saveEvent(event);

        // Process legacy single bets (using old eventId field)
        processLegacyBets(eventId, winnerOptionId, event);

        // Process multi-leg bets (using legs array)
        processMultiLegBets(eventId, winnerOptionId, event);

        // Broadcast event update
        webSocketService.broadcastEventUpdate(eventService.toResponse(event));
    }

    /**
     * Process legacy bets that use the old eventId field (backward compatibility)
     */
    @SuppressWarnings("deprecation")
    private void processLegacyBets(String eventId, String winnerOptionId, Event event) {
        List<Bet> legacyBets = betService.getPendingBetsForEvent(eventId);

        for (Bet bet : legacyBets) {
            // Skip if this bet has legs (it's a new format bet)
            if (bet.getLegs() != null && !bet.getLegs().isEmpty()) {
                continue;
            }

            bet.setSettledAt(LocalDateTime.now());

            if (bet.getChosenOptionId().equals(winnerOptionId)) {
                // Winner!
                bet.setStatus(BetStatus.WON);
                walletService.credit(
                        bet.getUserId(),
                        bet.getPotentialPayout(),
                        TransactionOrigin.BET_WIN,
                        bet.getId());

                // Notify user
                webSocketService.notifyUser(
                        bet.getUserId(),
                        "BET_WON",
                        String.format("Parabéns! Você ganhou R$ %.2f no evento '%s'!",
                                bet.getPotentialPayout(), event.getTitle()));
            } else {
                // Loser
                bet.setStatus(BetStatus.LOST);

                // Notify loser too
                webSocketService.notifyUser(
                        bet.getUserId(),
                        "BET_LOST",
                        String.format("Que pena! Você perdeu sua aposta no evento '%s'.", event.getTitle()));
            }
            betService.saveBet(bet);

            // Publish event for badge checking (async)
            eventPublisher.publishEvent(new BetSettledEvent(this, bet, event.getTitle()));
        }
    }

    /**
     * Process multi-leg bets that have this event as one of their legs
     */
    private void processMultiLegBets(String eventId, String winnerOptionId, Event event) {
        List<Bet> multiLegBets = betService.findPendingBetsWithEventLeg(eventId);

        for (Bet bet : multiLegBets) {
            if (bet.getLegs() == null || bet.getLegs().isEmpty()) {
                continue; // Skip legacy bets (already processed)
            }

            // Find and update the specific leg for this event
            boolean legFound = false;
            for (BetLeg leg : bet.getLegs()) {
                if (leg.getEventId().equals(eventId)) {
                    legFound = true;

                    if (leg.getChosenOptionId().equals(winnerOptionId)) {
                        leg.setStatus(LegStatus.WON);
                        log.info("Leg WON for bet {} on event {}", bet.getId(), eventId);
                    } else {
                        leg.setStatus(LegStatus.LOST);
                        log.info("Leg LOST for bet {} on event {}", bet.getId(), eventId);
                    }
                    break;
                }
            }

            if (!legFound) {
                log.warn("Leg not found for event {} in bet {}", eventId, bet.getId());
                continue;
            }

            // Re-evaluate the overall bet status
            evaluateMultiLegBetStatus(bet, event);
        }
    }

    /**
     * Evaluate the overall status of a multi-leg bet after a leg is settled.
     * - If ANY leg is LOST → bet is LOST
     * - If ALL legs are WON/VOID → bet is WON (pay user)
     * - If some legs are still PENDING → bet stays PENDING
     */
    private void evaluateMultiLegBetStatus(Bet bet, Event settledEvent) {
        // Check if any leg has LOST
        if (bet.hasLostLeg()) {
            // Bet is lost immediately
            bet.setStatus(BetStatus.LOST);
            bet.setSettledAt(LocalDateTime.now());
            betService.saveBet(bet);

            webSocketService.notifyUser(
                    bet.getUserId(),
                    "BET_LOST",
                    String.format("Que pena! Sua aposta múltipla perdeu no evento '%s'.", settledEvent.getTitle()));

            // Publish for badge checking
            eventPublisher.publishEvent(new BetSettledEvent(this, bet, "Aposta Múltipla"));

            log.info("Multi-leg bet {} LOST due to leg on event {}", bet.getId(), settledEvent.getId());
            return;
        }

        // Check if all legs are settled (WON or VOID)
        if (bet.allLegsWon()) {
            // Recalculate payout (in case of VOID legs, odd becomes 1.00)
            BigDecimal effectiveTotalOdd = bet.getLegs().stream()
                    .map(leg -> leg.getStatus() == LegStatus.VOID ? BigDecimal.ONE : leg.getLockedOdd())
                    .reduce(BigDecimal.ONE, BigDecimal::multiply)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal effectivePayout = bet.getAmount().multiply(effectiveTotalOdd)
                    .setScale(2, RoundingMode.HALF_UP);

            bet.setStatus(BetStatus.WON);
            bet.setSettledAt(LocalDateTime.now());
            bet.setTotalOdd(effectiveTotalOdd);
            bet.setPotentialPayout(effectivePayout);
            betService.saveBet(bet);

            // Credit wallet
            walletService.credit(
                    bet.getUserId(),
                    effectivePayout,
                    TransactionOrigin.BET_WIN,
                    bet.getId());

            webSocketService.notifyUser(
                    bet.getUserId(),
                    "BET_WON",
                    String.format("🎉 Parabéns! Você acertou TODAS as pernas! Ganhou R$ %.2f!", effectivePayout));

            // Publish for badge checking
            eventPublisher.publishEvent(new BetSettledEvent(this, bet, "Aposta Múltipla"));

            log.info("Multi-leg bet {} WON! Payout: {}", bet.getId(), effectivePayout);
            return;
        }

        // Still have pending legs, just save the updated leg status
        betService.saveBet(bet);
        log.info("Multi-leg bet {} still has pending legs", bet.getId());
    }

    /**
     * Cancel an event and void all related bets
     */
    @Transactional
    public void cancelEvent(String eventId) {
        Event event = eventService.findEventById(eventId);

        // Update event status
        event.setStatus(EventStatus.CANCELED);
        event.setUpdatedAt(LocalDateTime.now());
        eventService.saveEvent(event);

        // Process multi-leg bets: mark the leg as VOID
        List<Bet> multiLegBets = betService.findPendingBetsWithEventLeg(eventId);

        for (Bet bet : multiLegBets) {
            if (bet.getLegs() == null)
                continue;

            for (BetLeg leg : bet.getLegs()) {
                if (leg.getEventId().equals(eventId)) {
                    leg.setStatus(LegStatus.VOID);
                    log.info("Leg VOIDED for bet {} on canceled event {}", bet.getId(), eventId);
                }
            }

            // Re-evaluate the bet
            evaluateMultiLegBetStatus(bet, event);
        }

        // Refund legacy single bets on this event
        List<Bet> legacyBets = betService.getPendingBetsForEvent(eventId);
        for (Bet bet : legacyBets) {
            if (bet.getLegs() != null && !bet.getLegs().isEmpty()) {
                continue; // Already processed above
            }

            // Refund the bet amount
            walletService.credit(
                    bet.getUserId(),
                    bet.getAmount(),
                    TransactionOrigin.BET_WIN, // Using BET_WIN for refund
                    bet.getId());

            bet.setStatus(BetStatus.WON); // Treat as won (refund)
            bet.setSettledAt(LocalDateTime.now());
            betService.saveBet(bet);

            webSocketService.notifyUser(
                    bet.getUserId(),
                    "EVENT_CANCELED",
                    String.format("O evento '%s' foi cancelado. Seu valor de R$ %.2f foi devolvido.",
                            event.getTitle(), bet.getAmount()));
        }

        // Broadcast event update
        webSocketService.broadcastEventUpdate(eventService.toResponse(event));
    }
}

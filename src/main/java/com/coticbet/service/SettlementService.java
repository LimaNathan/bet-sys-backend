package com.coticbet.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coticbet.domain.entity.Bet;
import com.coticbet.domain.entity.Event;
import com.coticbet.domain.enums.BetStatus;
import com.coticbet.domain.enums.EventStatus;
import com.coticbet.domain.enums.TransactionOrigin;
import com.coticbet.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private final EventService eventService;
    private final BetService betService;
    private final WalletService walletService;
    private final WebSocketService webSocketService;

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

        // Process all pending bets
        List<Bet> pendingBets = betService.getPendingBetsForEvent(eventId);

        for (Bet bet : pendingBets) {
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
        }

        // Broadcast event update
        webSocketService.broadcastEventUpdate(eventService.toResponse(event));
    }
}

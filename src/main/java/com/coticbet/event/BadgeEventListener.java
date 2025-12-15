package com.coticbet.event;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.coticbet.service.BadgeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Async listener for badge-related events.
 * Runs in separate thread to not block main operations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BadgeEventListener {

    private final BadgeService badgeService;

    @Async
    @EventListener
    public void handleBetSettled(BetSettledEvent event) {
        log.debug("Processing badge check for bet: {}", event.getBet().getId());

        try {
            badgeService.checkBadgesAfterBetSettled(event.getBet(), event.getEventTitle());
        } catch (Exception e) {
            log.error("Error checking badges for bet {}: {}", event.getBet().getId(), e.getMessage(), e);
        }
    }
}

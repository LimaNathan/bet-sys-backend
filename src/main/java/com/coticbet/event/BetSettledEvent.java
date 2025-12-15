package com.coticbet.event;

import org.springframework.context.ApplicationEvent;

import com.coticbet.domain.entity.Bet;

import lombok.Getter;

/**
 * Event published when a bet is settled (WON or LOST).
 */
@Getter
public class BetSettledEvent extends ApplicationEvent {

    private final Bet bet;
    private final String eventTitle;

    public BetSettledEvent(Object source, Bet bet, String eventTitle) {
        super(source);
        this.bet = bet;
        this.eventTitle = eventTitle;
    }
}

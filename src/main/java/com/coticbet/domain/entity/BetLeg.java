package com.coticbet.domain.entity;

import java.math.BigDecimal;

import com.coticbet.domain.enums.LegStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single leg (selection) in a bet.
 * Embedded document within Bet.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BetLeg {

    private String eventId;

    // Cached for display without additional queries
    private String eventTitle;

    private String chosenOptionId;

    // Cached for display
    private String chosenOptionLabel;

    private BigDecimal lockedOdd;

    @Builder.Default
    private LegStatus status = LegStatus.PENDING;
}

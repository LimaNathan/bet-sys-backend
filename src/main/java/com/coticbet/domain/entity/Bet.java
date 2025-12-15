package com.coticbet.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.coticbet.domain.enums.BetStatus;
import com.coticbet.domain.enums.BetType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "bets")
public class Bet {

    @Id
    private String id;

    private String userId;

    /**
     * Type of bet: SINGLE or MULTIPLE (parlay)
     */
    @Builder.Default
    private BetType type = BetType.SINGLE;

    /**
     * List of legs (selections) in this bet.
     * For SINGLE bets, this list has exactly 1 element.
     * For MULTIPLE bets, this list has 2+ elements.
     */
    @Builder.Default
    private List<BetLeg> legs = new ArrayList<>();

    /**
     * Combined odd (product of all leg odds).
     * For SINGLE bets, this equals the single leg's odd.
     */
    private BigDecimal totalOdd;

    /**
     * Amount wagered on this bet
     */
    private BigDecimal amount;

    /**
     * Potential payout if all legs win (amount * totalOdd)
     */
    private BigDecimal potentialPayout;

    @Builder.Default
    private BetStatus status = BetStatus.PENDING;

    private LocalDateTime createdAt;

    private LocalDateTime settledAt;

    // ==================== LEGACY FIELDS (for backward compatibility)
    // ====================
    // These fields are deprecated and will be removed in future versions.
    // Use legs array instead.

    /**
     * @deprecated Use legs[0].eventId instead
     */
    @Deprecated
    private String eventId;

    /**
     * @deprecated Use legs[0].chosenOptionId instead
     */
    @Deprecated
    private String chosenOptionId;

    /**
     * @deprecated Use legs[0].lockedOdd instead
     */
    @Deprecated
    private BigDecimal lockedOdd;

    // ==================== HELPER METHODS ====================

    /**
     * Check if this bet is a legacy bet (no legs array populated)
     */
    public boolean isLegacyBet() {
        return (legs == null || legs.isEmpty()) && eventId != null;
    }

    /**
     * Get first leg (for SINGLE bets or backward compatibility)
     */
    public BetLeg getFirstLeg() {
        if (legs != null && !legs.isEmpty()) {
            return legs.get(0);
        }
        return null;
    }

    /**
     * Check if all legs have a final status (WON, LOST, or VOID)
     */
    public boolean allLegsSettled() {
        if (legs == null || legs.isEmpty()) {
            return false;
        }
        return legs.stream().allMatch(leg -> leg.getStatus() != com.coticbet.domain.enums.LegStatus.PENDING);
    }

    /**
     * Check if any leg has LOST status
     */
    public boolean hasLostLeg() {
        if (legs == null)
            return false;
        return legs.stream().anyMatch(leg -> leg.getStatus() == com.coticbet.domain.enums.LegStatus.LOST);
    }

    /**
     * Check if all legs have WON or VOID status (no LOST, no PENDING)
     */
    public boolean allLegsWon() {
        if (legs == null || legs.isEmpty()) {
            return false;
        }
        return legs.stream().allMatch(leg -> leg.getStatus() == com.coticbet.domain.enums.LegStatus.WON ||
                leg.getStatus() == com.coticbet.domain.enums.LegStatus.VOID);
    }
}

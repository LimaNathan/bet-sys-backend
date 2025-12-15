package com.coticbet.dto.request;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for placing bets.
 * Supports both single bets (1 selection) and multiple/parlay bets (2+
 * selections).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceBetRequest {

    /**
     * List of selections for this bet.
     * - Single bet: 1 selection
     * - Multiple/Parlay: 2+ selections (from different events)
     */
    @NotEmpty(message = "At least one selection is required")
    @Valid
    private List<BetSelection> selections;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    /**
     * Inner class representing a single selection (event + option)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BetSelection {

        @NotNull(message = "Event ID is required")
        private String eventId;

        @NotNull(message = "Option ID is required")
        private String optionId;
    }
}

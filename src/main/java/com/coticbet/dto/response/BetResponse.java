package com.coticbet.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.coticbet.domain.enums.BetStatus;
import com.coticbet.domain.enums.BetType;
import com.coticbet.domain.enums.LegStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BetResponse {

    private String id;
    private BetType type;
    private BigDecimal totalOdd;
    private BigDecimal amount;
    private BigDecimal potentialPayout;
    private BetStatus status;
    private LocalDateTime createdAt;

    // Legs for multiple bets
    private List<LegResponse> legs;

    // Legacy fields for backward compatibility with frontend
    private String eventId;
    private String eventTitle;
    private String chosenOptionId;
    private String chosenOptionName;
    private BigDecimal lockedOdd;

    /**
     * Response DTO for individual bet legs
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LegResponse {
        private String eventId;
        private String eventTitle;
        private String chosenOptionId;
        private String chosenOptionName;
        private BigDecimal lockedOdd;
        private LegStatus status;
    }
}

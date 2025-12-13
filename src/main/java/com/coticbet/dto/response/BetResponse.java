package com.coticbet.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.coticbet.domain.enums.BetStatus;

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
    private String eventId;
    private String eventTitle;
    private String chosenOptionId;
    private String chosenOptionName;
    private BigDecimal lockedOdd;
    private BigDecimal amount;
    private BigDecimal potentialPayout;
    private BetStatus status;
    private LocalDateTime createdAt;
}

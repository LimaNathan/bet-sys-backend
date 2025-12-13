package com.coticbet.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.coticbet.domain.enums.RequestStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoneyRequestResponse {

    private String id;
    private String userId;
    private String userEmail;
    private BigDecimal amountRequested;
    private String reason;
    private RequestStatus status;
    private String reviewedBy;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
}

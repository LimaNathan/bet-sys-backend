package com.coticbet.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.coticbet.domain.enums.RequestStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "money_requests")
public class MoneyRequest {

    @Id
    private String id;

    private String userId;

    private BigDecimal amountRequested;

    private String reason;

    @Builder.Default
    private RequestStatus status = RequestStatus.PENDING;

    private String reviewedBy;

    private LocalDateTime createdAt;

    private LocalDateTime reviewedAt;
}

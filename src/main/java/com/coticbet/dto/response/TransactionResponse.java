package com.coticbet.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.coticbet.domain.enums.TransactionOrigin;
import com.coticbet.domain.enums.TransactionType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {

    private String id;
    private TransactionType type;
    private TransactionOrigin origin;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private LocalDateTime createdAt;
}

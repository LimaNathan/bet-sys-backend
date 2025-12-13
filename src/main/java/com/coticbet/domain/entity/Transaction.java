package com.coticbet.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

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
@Document(collection = "transactions")
public class Transaction {

    @Id
    private String id;

    private String userId;

    private TransactionType type;

    private TransactionOrigin origin;

    private BigDecimal amount;

    private BigDecimal balanceAfter;

    private String referenceId;

    private LocalDateTime createdAt;
}

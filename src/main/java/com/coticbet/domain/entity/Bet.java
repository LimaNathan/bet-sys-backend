package com.coticbet.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.coticbet.domain.enums.BetStatus;

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

    private String eventId;

    private String chosenOptionId;

    private BigDecimal lockedOdd;

    private BigDecimal amount;

    private BigDecimal potentialPayout;

    @Builder.Default
    private BetStatus status = BetStatus.PENDING;

    private LocalDateTime createdAt;
}

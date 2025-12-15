package com.coticbet.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.coticbet.domain.enums.BadgeType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Embedded document representing a badge earned by a user.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Badge {

    private BadgeType code;
    private LocalDateTime earnedAt;
    private BigDecimal rewardAmount;
}

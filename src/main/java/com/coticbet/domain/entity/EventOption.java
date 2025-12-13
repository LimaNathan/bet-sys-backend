package com.coticbet.domain.entity;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventOption {

    private String id;

    private String name;

    private BigDecimal currentOdd;

    private BigDecimal seedOdd;

    @Builder.Default
    private BigDecimal totalStaked = BigDecimal.ZERO;
}

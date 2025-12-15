package com.coticbet.dto.response;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardEntry {

    private int rank;
    private String userId;
    private String name;
    private BigDecimal value;
    private String valueLabel;
}

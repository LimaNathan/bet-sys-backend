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
public class EventOptionResponse {

    private String id;
    private String name;
    private BigDecimal currentOdd;
    private BigDecimal totalStaked;
}

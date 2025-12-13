package com.coticbet.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventOptionRequest {

    @NotBlank(message = "Option name is required")
    private String name;

    @NotNull(message = "Initial odd is required")
    @Positive(message = "Odd must be positive")
    private BigDecimal initialOdd;
}

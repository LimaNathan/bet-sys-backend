package com.coticbet.dto.request;

import java.time.LocalDateTime;
import java.util.List;

import com.coticbet.domain.enums.PricingModel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateEventRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotNull(message = "Pricing model is required")
    private PricingModel pricingModel;

    @NotNull(message = "Commence time is required")
    private LocalDateTime commenceTime;

    @NotNull(message = "Options are required")
    private List<EventOptionRequest> options;
}

package com.coticbet.dto.request;

import com.coticbet.domain.enums.EventStatus;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEventStatusRequest {

    @NotNull(message = "Status is required")
    private EventStatus status;
}

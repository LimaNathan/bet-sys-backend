package com.coticbet.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.coticbet.domain.enums.EventCategory;
import com.coticbet.domain.enums.EventStatus;
import com.coticbet.domain.enums.PricingModel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventResponse {

    private String id;
    private String title;
    private EventCategory category;
    private EventStatus status;
    private PricingModel pricingModel;
    private LocalDateTime commenceTime;
    private List<EventOptionResponse> options;
    private String winnerOptionId;
}

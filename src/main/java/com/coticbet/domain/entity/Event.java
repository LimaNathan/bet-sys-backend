package com.coticbet.domain.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

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
@Document(collection = "events")
public class Event {

    @Id
    private String id;

    private String title;

    private EventCategory category;

    @Builder.Default
    private EventStatus status = EventStatus.PENDING;

    private PricingModel pricingModel;

    @Indexed
    private String externalId;

    private LocalDateTime commenceTime;

    @Builder.Default
    private List<EventOption> options = new ArrayList<>();

    private String winnerOptionId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

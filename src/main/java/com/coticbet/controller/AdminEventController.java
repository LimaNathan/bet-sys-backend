package com.coticbet.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coticbet.dto.request.CreateEventRequest;
import com.coticbet.dto.request.SettleEventRequest;
import com.coticbet.dto.request.UpdateEventStatusRequest;
import com.coticbet.dto.response.EventResponse;
import com.coticbet.service.EventService;
import com.coticbet.service.SettlementService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/events")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminEventController {

    private final EventService eventService;
    private final SettlementService settlementService;

    @PostMapping
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody CreateEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createInternalEvent(request));
    }

    @GetMapping
    public ResponseEntity<List<EventResponse>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEvent(@PathVariable String id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<EventResponse> updateEventStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateEventStatusRequest request) {
        return ResponseEntity.ok(eventService.updateEventStatus(id, request.getStatus()));
    }

    @PostMapping("/{id}/settle")
    public ResponseEntity<Map<String, String>> settleEvent(
            @PathVariable String id,
            @Valid @RequestBody SettleEventRequest request) {
        settlementService.settleEvent(id, request.getWinnerOptionId());
        return ResponseEntity.ok(Map.of("message", "Event settled successfully"));
    }
}

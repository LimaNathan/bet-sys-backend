package com.coticbet.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coticbet.domain.entity.User;
import com.coticbet.dto.response.MoneyRequestResponse;
import com.coticbet.security.CustomUserDetailsService;
import com.coticbet.service.MoneyRequestService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/money-requests")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminMoneyRequestController {

    private final MoneyRequestService moneyRequestService;
    private final CustomUserDetailsService userDetailsService;

    @GetMapping
    public ResponseEntity<List<MoneyRequestResponse>> getPendingRequests() {
        return ResponseEntity.ok(moneyRequestService.getPendingRequests());
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Map<String, String>> approveRequest(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User admin = userDetailsService.getUserByEmail(userDetails.getUsername());
        moneyRequestService.approveRequest(id, admin.getId());
        return ResponseEntity.ok(Map.of("message", "Request approved successfully"));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Map<String, String>> rejectRequest(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User admin = userDetailsService.getUserByEmail(userDetails.getUsername());
        moneyRequestService.rejectRequest(id, admin.getId());
        return ResponseEntity.ok(Map.of("message", "Request rejected"));
    }
}

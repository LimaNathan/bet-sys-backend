package com.coticbet.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coticbet.domain.entity.User;
import com.coticbet.dto.request.CreateMoneyRequestRequest;
import com.coticbet.dto.response.MoneyRequestResponse;
import com.coticbet.security.CustomUserDetailsService;
import com.coticbet.service.MoneyRequestService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/money-requests")
@RequiredArgsConstructor
public class MoneyRequestController {

    private final MoneyRequestService moneyRequestService;
    private final CustomUserDetailsService userDetailsService;

    @PostMapping
    public ResponseEntity<MoneyRequestResponse> createRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateMoneyRequestRequest request) {
        User user = userDetailsService.getUserByEmail(userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(moneyRequestService.createRequest(user.getId(), request));
    }

    @GetMapping
    public ResponseEntity<List<MoneyRequestResponse>> getMyRequests(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userDetailsService.getUserByEmail(userDetails.getUsername());
        return ResponseEntity.ok(moneyRequestService.getUserRequests(user.getId()));
    }
}

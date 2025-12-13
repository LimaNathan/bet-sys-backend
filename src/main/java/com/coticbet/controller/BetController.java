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
import com.coticbet.dto.request.PlaceBetRequest;
import com.coticbet.dto.response.BetResponse;
import com.coticbet.security.CustomUserDetailsService;
import com.coticbet.service.BetService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/bets")
@RequiredArgsConstructor
public class BetController {

    private final BetService betService;
    private final CustomUserDetailsService userDetailsService;

    @PostMapping
    public ResponseEntity<BetResponse> placeBet(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PlaceBetRequest request) {
        User user = userDetailsService.getUserByEmail(userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(betService.placeBet(user.getId(), request));
    }

    @GetMapping
    public ResponseEntity<List<BetResponse>> getMyBets(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userDetailsService.getUserByEmail(userDetails.getUsername());
        return ResponseEntity.ok(betService.getUserBets(user.getId()));
    }
}

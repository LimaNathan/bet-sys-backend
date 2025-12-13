package com.coticbet.controller;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.coticbet.domain.entity.Transaction;
import com.coticbet.domain.entity.User;
import com.coticbet.dto.response.TransactionResponse;
import com.coticbet.dto.response.WalletResponse;
import com.coticbet.security.CustomUserDetailsService;
import com.coticbet.service.TransactionService;
import com.coticbet.service.WalletService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final TransactionService transactionService;
    private final CustomUserDetailsService userDetailsService;

    @GetMapping
    public ResponseEntity<WalletResponse> getBalance(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userDetailsService.getUserByEmail(userDetails.getUsername());
        BigDecimal balance = walletService.getBalance(user.getId());

        return ResponseEntity.ok(WalletResponse.builder()
                .balance(balance)
                .build());
    }

    @PostMapping("/daily-bonus")
    public ResponseEntity<WalletResponse> claimDailyBonus(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userDetailsService.getUserByEmail(userDetails.getUsername());
        BigDecimal newBalance = walletService.claimDailyBonus(user.getId());

        return ResponseEntity.ok(WalletResponse.builder()
                .balance(newBalance)
                .message("Daily bonus claimed successfully!")
                .build());
    }

    @GetMapping("/transactions")
    public ResponseEntity<Page<TransactionResponse>> getTransactions(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        User user = userDetailsService.getUserByEmail(userDetails.getUsername());
        Pageable pageable = PageRequest.of(page, size);

        Page<Transaction> transactions = transactionService.getUserTransactions(user.getId(), pageable);

        Page<TransactionResponse> response = transactions.map(t -> TransactionResponse.builder()
                .id(t.getId())
                .type(t.getType())
                .origin(t.getOrigin())
                .amount(t.getAmount())
                .balanceAfter(t.getBalanceAfter())
                .createdAt(t.getCreatedAt())
                .build());

        return ResponseEntity.ok(response);
    }
}

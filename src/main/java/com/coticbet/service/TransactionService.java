package com.coticbet.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.coticbet.domain.entity.Transaction;
import com.coticbet.domain.enums.TransactionOrigin;
import com.coticbet.domain.enums.TransactionType;
import com.coticbet.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public Transaction createTransaction(
            String userId,
            TransactionType type,
            TransactionOrigin origin,
            BigDecimal amount,
            BigDecimal balanceAfter,
            String referenceId) {
        Transaction transaction = Transaction.builder()
                .userId(userId)
                .type(type)
                .origin(origin)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .referenceId(referenceId)
                .createdAt(LocalDateTime.now())
                .build();

        return transactionRepository.save(transaction);
    }

    public Page<Transaction> getUserTransactions(String userId, Pageable pageable) {
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
}

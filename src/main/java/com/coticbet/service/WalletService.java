package com.coticbet.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coticbet.domain.entity.User;
import com.coticbet.domain.enums.TransactionOrigin;
import com.coticbet.domain.enums.TransactionType;
import com.coticbet.exception.BusinessException;
import com.coticbet.exception.InsufficientBalanceException;
import com.coticbet.exception.ResourceNotFoundException;
import com.coticbet.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final UserRepository userRepository;
    private final TransactionService transactionService;

    @Value("${app.daily-bonus-amount:100}")
    private BigDecimal dailyBonusAmount;

    public BigDecimal getBalance(String userId) {
        User user = findUserById(userId);
        return user.getWalletBalance();
    }

    @Transactional
    public BigDecimal claimDailyBonus(String userId) {
        User user = findUserById(userId);

        LocalDate today = LocalDate.now();
        if (user.getLastDailyBonus() != null) {
            LocalDate lastBonusDate = user.getLastDailyBonus().toLocalDate();
            if (!lastBonusDate.isBefore(today)) {
                throw new BusinessException("Daily bonus already claimed today");
            }
        }

        BigDecimal newBalance = user.getWalletBalance().add(dailyBonusAmount);
        user.setWalletBalance(newBalance);
        user.setLastDailyBonus(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        transactionService.createTransaction(
                userId,
                TransactionType.DEPOSIT,
                TransactionOrigin.DAILY_BONUS,
                dailyBonusAmount,
                newBalance,
                null);

        return newBalance;
    }

    @Transactional
    public BigDecimal credit(String userId, BigDecimal amount, TransactionOrigin origin, String referenceId) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Amount must be positive");
        }

        User user = findUserById(userId);
        BigDecimal newBalance = user.getWalletBalance().add(amount);
        user.setWalletBalance(newBalance);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        transactionService.createTransaction(
                userId,
                TransactionType.DEPOSIT,
                origin,
                amount,
                newBalance,
                referenceId);

        return newBalance;
    }

    @Transactional
    public BigDecimal debit(String userId, BigDecimal amount, TransactionOrigin origin, String referenceId) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Amount must be positive");
        }

        User user = findUserById(userId);

        if (user.getWalletBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException();
        }

        BigDecimal newBalance = user.getWalletBalance().subtract(amount);
        user.setWalletBalance(newBalance);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        transactionService.createTransaction(
                userId,
                TransactionType.WITHDRAW,
                origin,
                amount,
                newBalance,
                referenceId);

        return newBalance;
    }

    public boolean hasBalance(String userId, BigDecimal amount) {
        User user = findUserById(userId);
        return user.getWalletBalance().compareTo(amount) >= 0;
    }

    private User findUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }
}

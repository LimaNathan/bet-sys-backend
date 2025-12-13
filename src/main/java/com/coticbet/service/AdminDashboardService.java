package com.coticbet.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.coticbet.domain.entity.User;
import com.coticbet.domain.enums.BetStatus;
import com.coticbet.domain.enums.EventStatus;
import com.coticbet.domain.enums.Role;
import com.coticbet.domain.enums.TransactionOrigin;
import com.coticbet.dto.response.HouseStatisticsResponse;
import com.coticbet.repository.BetRepository;
import com.coticbet.repository.EventRepository;
import com.coticbet.repository.TransactionRepository;
import com.coticbet.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final BetRepository betRepository;
    private final TransactionRepository transactionRepository;

    public HouseStatisticsResponse getHouseStatistics() {
        // Calculate total bets received (sum of all BET_ENTRY transactions)
        BigDecimal totalBetsReceived = transactionRepository.findAll().stream()
                .filter(t -> t.getOrigin() == TransactionOrigin.BET_ENTRY)
                .map(t -> t.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate total payouts (sum of all BET_WIN transactions)
        BigDecimal totalPayouts = transactionRepository.findAll().stream()
                .filter(t -> t.getOrigin() == TransactionOrigin.BET_WIN)
                .map(t -> t.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // House profit = bets received - payouts
        BigDecimal houseProfit = totalBetsReceived.subtract(totalPayouts);

        // Total in user wallets (exclude admin)
        BigDecimal totalInUserWallets = userRepository.findAll().stream()
                .filter(u -> u.getRole() != Role.ADMIN)
                .map(User::getWalletBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Counts
        long totalBetsCount = betRepository.count();
        long pendingBetsCount = betRepository.findAll().stream()
                .filter(b -> b.getStatus() == BetStatus.PENDING)
                .count();

        long totalUsers = userRepository.findAll().stream()
                .filter(u -> u.getRole() != Role.ADMIN)
                .count();

        long totalEvents = eventRepository.count();
        long openEvents = eventRepository.findByStatus(EventStatus.OPEN).size();
        long settledEvents = eventRepository.findByStatus(EventStatus.SETTLED).size();

        return HouseStatisticsResponse.builder()
                .totalBetsReceived(totalBetsReceived)
                .totalPayouts(totalPayouts)
                .houseProfit(houseProfit)
                .totalInUserWallets(totalInUserWallets)
                .totalBetsCount(totalBetsCount)
                .pendingBetsCount(pendingBetsCount)
                .totalUsers(totalUsers)
                .totalEvents(totalEvents)
                .openEvents(openEvents)
                .settledEvents(settledEvents)
                .build();
    }
}

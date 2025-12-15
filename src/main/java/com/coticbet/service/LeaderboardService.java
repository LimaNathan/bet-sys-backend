package com.coticbet.service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.coticbet.domain.entity.Bet;
import com.coticbet.domain.entity.User;
import com.coticbet.domain.enums.BetStatus;
import com.coticbet.domain.enums.Role;
import com.coticbet.dto.response.LeaderboardEntry;
import com.coticbet.repository.BetRepository;
import com.coticbet.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final UserRepository userRepository;
    private final BetRepository betRepository;

    /**
     * Ranking by total wallet balance (O Magnata)
     * No caching since wallet balances change frequently
     */
    public List<LeaderboardEntry> getWealthRanking() {
        List<User> users = userRepository.findAll(Sort.by(Sort.Direction.DESC, "walletBalance"));

        List<LeaderboardEntry> entries = new ArrayList<>();
        int rank = 1;

        for (User user : users) {
            // Skip admins
            if (user.getRole() == Role.ADMIN)
                continue;

            // Skip users with negative balance in wealth ranking
            if (user.getWalletBalance().compareTo(BigDecimal.ZERO) < 0)
                continue;

            entries.add(LeaderboardEntry.builder()
                    .rank(rank++)
                    .userId(user.getId())
                    .name(user.getName() != null ? user.getName() : user.getEmail().split("@")[0])
                    .value(user.getWalletBalance())
                    .valueLabel("R$ " + user.getWalletBalance().setScale(2).toString())
                    .build());

            if (rank > 10)
                break; // Top 10
        }

        return entries;
    }

    /**
     * Ranking by net profit - monthly (O Trader)
     */
    @Cacheable(value = "leaderboard", key = "'profit_monthly'")
    public List<LeaderboardEntry> getProfitRanking() {
        LocalDateTime startOfMonth = LocalDateTime.now()
                .withDayOfMonth(1)
                .withHour(0).withMinute(0).withSecond(0).withNano(0);

        return calculateNetProfitRanking(startOfMonth, true);
    }

    /**
     * Ranking by net loss - weekly (Mão de Alface)
     */
    @Cacheable(value = "leaderboard", key = "'loss_weekly'")
    public List<LeaderboardEntry> getLossRanking() {
        LocalDateTime startOfWeek = LocalDateTime.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .withHour(0).withMinute(0).withSecond(0).withNano(0);

        return calculateNetProfitRanking(startOfWeek, false);
    }

    private List<LeaderboardEntry> calculateNetProfitRanking(LocalDateTime since, boolean ascending) {
        // Get all settled bets since the date
        List<Bet> bets = betRepository.findAll().stream()
                .filter(bet -> bet.getSettledAt() != null && bet.getSettledAt().isAfter(since))
                .filter(bet -> bet.getStatus() == BetStatus.WON || bet.getStatus() == BetStatus.LOST)
                .toList();

        // Calculate net profit per user
        Map<String, BigDecimal> userProfits = new HashMap<>();

        for (Bet bet : bets) {
            BigDecimal profit;

            if (bet.getStatus() == BetStatus.WON) {
                // Profit = payout - amount bet
                profit = bet.getPotentialPayout().subtract(bet.getAmount());
            } else {
                // Loss = negative amount bet
                profit = bet.getAmount().negate();
            }

            userProfits.merge(bet.getUserId(), profit, BigDecimal::add);
        }

        // Sort by profit (ascending for top profit, descending for top loss)
        Comparator<Map.Entry<String, BigDecimal>> comparator = ascending
                ? Map.Entry.<String, BigDecimal>comparingByValue().reversed()
                : Map.Entry.comparingByValue();

        // Filter based on ranking type:
        // - For profit ranking (ascending=true): show only positive profits
        // - For loss ranking (ascending=false): show only negative profits (losses)
        List<Map.Entry<String, BigDecimal>> filteredEntries = userProfits.entrySet().stream()
                .filter(entry -> ascending
                        ? entry.getValue().compareTo(BigDecimal.ZERO) > 0 // Profit ranking: only positive
                        : entry.getValue().compareTo(BigDecimal.ZERO) < 0) // Loss ranking: only negative
                .sorted(comparator)
                .limit(10)
                .toList();

        // Build response
        List<LeaderboardEntry> entries = new ArrayList<>();
        int rank = 1;

        for (Map.Entry<String, BigDecimal> entry : filteredEntries) {
            User user = userRepository.findById(entry.getKey()).orElse(null);
            if (user == null || user.getRole() == Role.ADMIN)
                continue;

            String name = user.getName() != null ? user.getName() : user.getEmail().split("@")[0];
            String prefix = entry.getValue().compareTo(BigDecimal.ZERO) >= 0 ? "+ R$ " : "- R$ ";
            String valueLabel = prefix + entry.getValue().abs().setScale(2).toString();

            entries.add(LeaderboardEntry.builder()
                    .rank(rank++)
                    .userId(user.getId())
                    .name(name)
                    .value(entry.getValue())
                    .valueLabel(valueLabel)
                    .build());
        }

        return entries;
    }
}

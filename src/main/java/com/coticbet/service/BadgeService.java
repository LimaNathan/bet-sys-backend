package com.coticbet.service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.coticbet.domain.entity.Badge;
import com.coticbet.domain.entity.Bet;
import com.coticbet.domain.entity.User;
import com.coticbet.domain.enums.BadgeType;
import com.coticbet.domain.enums.BetStatus;
import com.coticbet.domain.enums.TransactionOrigin;
import com.coticbet.repository.BetRepository;
import com.coticbet.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BadgeService {

    private final UserRepository userRepository;
    private final BetRepository betRepository;
    private final WalletService walletService;
    private final WebSocketService webSocketService;

    private static final int PLATINUM_THRESHOLD = 10;

    /**
     * Check and award badges after a bet is settled.
     */
    public void checkBadgesAfterBetSettled(Bet bet, String eventTitle) {
        User user = userRepository.findById(bet.getUserId()).orElse(null);
        if (user == null)
            return;

        List<Bet> userBets = betRepository.findByUserId(user.getId(), Sort.by(Sort.Direction.DESC, "createdAt"));

        // Check each badge rule
        if (bet.getStatus() == BetStatus.LOST) {
            checkMickJagger(user, userBets);
            checkRobinHoodReverso(user, bet);
            checkIludido(user, bet);
        }

        if (bet.getStatus() == BetStatus.WON) {
            checkMaeDinah(user, bet);
            checkPuxaSaco(user, bet, eventTitle);
        }

        // Always check
        checkJulius(user, userBets);
        checkInimigoDoFim(user, bet);
        checkReuniaoEmail(user, userBets);

        // Check platinum
        checkPlatinum(user);
    }

    /**
     * MICK_JAGGER: 5 consecutive LOST bets
     */
    private void checkMickJagger(User user, List<Bet> bets) {
        if (user.hasBadge(BadgeType.MICK_JAGGER))
            return;

        int consecutiveLosses = 0;
        for (Bet bet : bets) {
            if (bet.getStatus() == BetStatus.LOST) {
                consecutiveLosses++;
                if (consecutiveLosses >= 5) {
                    awardBadge(user, BadgeType.MICK_JAGGER);
                    return;
                }
            } else if (bet.getStatus() == BetStatus.WON) {
                break; // Reset on win
            }
        }
    }

    /**
     * ROBIN_HOOD_REVERSO: Lose bet > R$500 with Odd < 1.20
     */
    private void checkRobinHoodReverso(User user, Bet bet) {
        if (user.hasBadge(BadgeType.ROBIN_HOOD_REVERSO))
            return;

        BigDecimal oddToCheck = bet.getTotalOdd() != null ? bet.getTotalOdd() : bet.getLockedOdd();
        if (bet.getAmount().compareTo(new BigDecimal("500")) > 0 &&
                oddToCheck != null && oddToCheck.compareTo(new BigDecimal("1.20")) < 0) {
            awardBadge(user, BadgeType.ROBIN_HOOD_REVERSO);
        }
    }

    /**
     * ILUDIDO: Lose bet with Odd > 5.0
     */
    private void checkIludido(User user, Bet bet) {
        if (user.hasBadge(BadgeType.ILUDIDO))
            return;

        BigDecimal oddToCheck = bet.getTotalOdd() != null ? bet.getTotalOdd() : bet.getLockedOdd();
        if (oddToCheck != null && oddToCheck.compareTo(new BigDecimal("5.0")) > 0) {
            awardBadge(user, BadgeType.ILUDIDO);
        }
    }

    /**
     * MAE_DINAH: Win bet with Odd >= 10.0
     */
    private void checkMaeDinah(User user, Bet bet) {
        if (user.hasBadge(BadgeType.MAE_DINAH))
            return;

        BigDecimal oddToCheck = bet.getTotalOdd() != null ? bet.getTotalOdd() : bet.getLockedOdd();
        if (oddToCheck != null && oddToCheck.compareTo(new BigDecimal("10.0")) >= 0) {
            awardBadge(user, BadgeType.MAE_DINAH);
        }
    }

    /**
     * JULIUS: 10 bets with amount <= R$1.00
     */
    private void checkJulius(User user, List<Bet> bets) {
        if (user.hasBadge(BadgeType.JULIUS))
            return;

        long count = bets.stream()
                .filter(b -> b.getAmount().compareTo(BigDecimal.ONE) <= 0)
                .count();

        if (count >= 10) {
            awardBadge(user, BadgeType.JULIUS);
        }
    }

    /**
     * INIMIGO_DO_FIM: Bet created < 2 min before event start
     * (Would need event commenceTime - simplified check)
     */
    private void checkInimigoDoFim(User user, Bet bet) {
        // This would require event data - simplified: just mark as checked
        // In real implementation, would check bet.createdAt vs event.commenceTime
    }

    /**
     * REUNIAO_EMAIL: 5 bets between 09:00-18:00 on weekdays in same day
     */
    private void checkReuniaoEmail(User user, List<Bet> bets) {
        if (user.hasBadge(BadgeType.REUNIAO_EMAIL))
            return;

        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        DayOfWeek dayOfWeek = today.getDayOfWeek();

        // Only check on weekdays
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY)
            return;

        long workHoursBets = bets.stream()
                .filter(b -> b.getCreatedAt() != null)
                .filter(b -> b.getCreatedAt().toLocalDate().equals(today.toLocalDate()))
                .filter(b -> {
                    LocalTime time = b.getCreatedAt().toLocalTime();
                    return time.isAfter(LocalTime.of(9, 0)) && time.isBefore(LocalTime.of(18, 0));
                })
                .count();

        if (workHoursBets >= 5) {
            awardBadge(user, BadgeType.REUNIAO_EMAIL);
        }
    }

    /**
     * PUXA_SACO: Win bet on event with title containing "Chefe", "Gerente", or
     * "Reunião"
     */
    private void checkPuxaSaco(User user, Bet bet, String eventTitle) {
        if (user.hasBadge(BadgeType.PUXA_SACO))
            return;

        if (eventTitle != null) {
            String title = eventTitle.toLowerCase();
            if (title.contains("chefe") || title.contains("gerente") || title.contains("reunião")
                    || title.contains("reuniao")) {
                awardBadge(user, BadgeType.PUXA_SACO);
            }
        }
    }

    /**
     * DONO_DA_BANCA (Platinum): 10+ unique badges
     */
    private void checkPlatinum(User user) {
        if (user.hasBadge(BadgeType.DONO_DA_BANCA))
            return;

        // Count badges excluding platinum itself
        long badgeCount = user.getBadges().stream()
                .filter(b -> b.getCode() != BadgeType.DONO_DA_BANCA)
                .count();

        if (badgeCount >= PLATINUM_THRESHOLD) {
            awardBadge(user, BadgeType.DONO_DA_BANCA);
        }
    }

    /**
     * Award a badge to user with wallet credit and notification.
     */
    private void awardBadge(User user, BadgeType badgeType) {
        log.info("Awarding badge {} to user {}", badgeType, user.getEmail());

        // Create badge
        Badge badge = Badge.builder()
                .code(badgeType)
                .earnedAt(LocalDateTime.now())
                .rewardAmount(badgeType.getRewardAmount())
                .build();

        // Add to user
        user.getBadges().add(badge);
        userRepository.save(user);

        // Credit wallet
        walletService.credit(
                user.getId(),
                badgeType.getRewardAmount(),
                TransactionOrigin.ACHIEVEMENT_REWARD,
                "BADGE_" + badgeType.name());

        // Notify user
        webSocketService.notifyUser(
                user.getId(),
                "BADGE_UNLOCKED",
                String.format("🏆 Conquista Desbloqueada: %s (+ R$ %.2f)",
                        badgeType.getTitle(), badgeType.getRewardAmount()));
    }

    /**
     * Get all badges for catalog display.
     */
    public List<BadgeType> getAllBadgeTypes() {
        return List.of(BadgeType.values());
    }

    /**
     * Get user's earned badges.
     */
    public List<Badge> getUserBadges(String userId) {
        User user = userRepository.findById(userId).orElse(null);
        return user != null ? user.getBadges() : List.of();
    }
}

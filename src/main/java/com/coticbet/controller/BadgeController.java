package com.coticbet.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coticbet.domain.entity.User;
import com.coticbet.repository.UserRepository;
import com.coticbet.service.BadgeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/badges")
@RequiredArgsConstructor
public class BadgeController {

    private final BadgeService badgeService;
    private final UserRepository userRepository;

    /**
     * Get all available badges (catalog).
     */
    @GetMapping("/catalog")
    public ResponseEntity<List<BadgeCatalogItem>> getCatalog() {
        List<BadgeCatalogItem> catalog = badgeService.getAllBadgeTypes().stream()
                .map(b -> new BadgeCatalogItem(
                        b.name(),
                        b.getTitle(),
                        b.getDescription(),
                        b.getCategory(),
                        b.getRewardAmount().doubleValue()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(catalog);
    }

    /**
     * Get current user's badges.
     */
    @GetMapping("/my")
    public ResponseEntity<List<UserBadgeItem>> getMyBadges(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
        if (user == null) {
            return ResponseEntity.ok(List.of());
        }

        List<UserBadgeItem> badges = user.getBadges().stream()
                .map(b -> new UserBadgeItem(
                        b.getCode().name(),
                        b.getCode().getTitle(),
                        b.getCode().getDescription(),
                        b.getCode().getCategory(),
                        b.getRewardAmount().doubleValue(),
                        b.getEarnedAt().toString()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(badges);
    }

    // DTOs
    record BadgeCatalogItem(String code, String title, String description, String category, double rewardAmount) {
    }

    record UserBadgeItem(String code, String title, String description, String category, double rewardAmount,
            String earnedAt) {
    }
}

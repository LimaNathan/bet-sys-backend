package com.coticbet.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.coticbet.domain.enums.Role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String name;

    private String password;

    @Builder.Default
    private Role role = Role.USER;

    @Builder.Default
    private BigDecimal walletBalance = BigDecimal.ZERO;

    @Builder.Default
    private List<Badge> badges = new ArrayList<>();

    private LocalDateTime lastDailyBonus;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public boolean hasBadge(com.coticbet.domain.enums.BadgeType badgeType) {
        return badges != null && badges.stream()
                .anyMatch(b -> b.getCode() == badgeType);
    }
}

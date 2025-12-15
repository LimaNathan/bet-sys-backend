package com.coticbet.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.coticbet.domain.entity.Bet;
import com.coticbet.domain.entity.BetLeg;
import com.coticbet.domain.entity.Event;
import com.coticbet.domain.entity.EventOption;
import com.coticbet.domain.enums.BetType;
import com.coticbet.domain.enums.LegStatus;
import com.coticbet.repository.BetRepository;
import com.coticbet.repository.EventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Migration script to convert legacy bets (with eventId field)
 * to the new format (with legs array).
 * Runs on application startup.
 */
@Slf4j
@Component
@Order(10) // Run after DataLoader
@RequiredArgsConstructor
public class LegacyBetMigration implements CommandLineRunner {

    private final BetRepository betRepository;
    private final EventRepository eventRepository;

    @Override
    @SuppressWarnings("deprecation")
    public void run(String... args) {
        log.info("🔄 Checking for legacy bets to migrate...");

        List<Bet> allBets = betRepository.findAll();
        int migratedCount = 0;

        for (Bet bet : allBets) {
            // Check if this is a legacy bet (has eventId but no legs)
            if (bet.getEventId() != null && (bet.getLegs() == null || bet.getLegs().isEmpty())) {
                migrateLegacyBet(bet);
                migratedCount++;
            }
        }

        if (migratedCount > 0) {
            log.info("✅ Migrated {} legacy bets to new format", migratedCount);
        } else {
            log.info("✅ No legacy bets found to migrate");
        }
    }

    @SuppressWarnings("deprecation")
    private void migrateLegacyBet(Bet bet) {
        try {
            // Get event and option info for leg label
            String eventTitle = "Unknown Event";
            String optionLabel = "Unknown Option";

            Event event = eventRepository.findById(bet.getEventId()).orElse(null);
            if (event != null) {
                eventTitle = event.getTitle();

                EventOption option = event.getOptions().stream()
                        .filter(opt -> opt.getId().equals(bet.getChosenOptionId()))
                        .findFirst()
                        .orElse(null);

                if (option != null) {
                    optionLabel = option.getName();
                }
            }

            // Determine leg status from bet status
            LegStatus legStatus = switch (bet.getStatus()) {
                case WON -> LegStatus.WON;
                case LOST -> LegStatus.LOST;
                default -> LegStatus.PENDING;
            };

            // Create single leg from legacy fields
            BetLeg leg = BetLeg.builder()
                    .eventId(bet.getEventId())
                    .eventTitle(eventTitle)
                    .chosenOptionId(bet.getChosenOptionId())
                    .chosenOptionLabel(optionLabel)
                    .lockedOdd(bet.getLockedOdd())
                    .status(legStatus)
                    .build();

            // Set new fields
            bet.setType(BetType.SINGLE);
            bet.setLegs(new ArrayList<>(List.of(leg)));
            bet.setTotalOdd(bet.getLockedOdd());

            betRepository.save(bet);

            log.debug("Migrated bet {} for event '{}'", bet.getId(), eventTitle);

        } catch (Exception e) {
            log.error("Failed to migrate bet {}: {}", bet.getId(), e.getMessage());
        }
    }
}

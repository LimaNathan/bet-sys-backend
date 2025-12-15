package com.coticbet.config;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.coticbet.domain.entity.User;
import com.coticbet.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Data migration runner that executes on application startup.
 * Migrates existing users without a 'name' field.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataMigration implements ApplicationRunner {

    private final UserRepository userRepository;

    @Override
    public void run(ApplicationArguments args) {
        migrateUserNames();
    }

    private void migrateUserNames() {
        List<User> usersWithoutName = userRepository.findAll().stream()
                .filter(user -> user.getName() == null || user.getName().isBlank())
                .toList();

        if (usersWithoutName.isEmpty()) {
            log.info("No users need name migration.");
            return;
        }

        log.info("Migrating {} users without display name...", usersWithoutName.size());

        for (User user : usersWithoutName) {
            String emailPrefix = user.getEmail().split("@")[0];
            user.setName(emailPrefix);
            userRepository.save(user);
            log.debug("Migrated user {} -> name: {}", user.getEmail(), emailPrefix);
        }

        log.info("User name migration completed.");
    }
}

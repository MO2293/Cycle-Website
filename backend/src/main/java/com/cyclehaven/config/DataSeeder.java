package com.cyclehaven.config;

import com.cyclehaven.entity.Role;
import com.cyclehaven.entity.User;
import com.cyclehaven.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Creates the initial admin account on first startup.
 *
 * <p>This is how the admin role is bootstrapped now that the hardcoded
 * {@code EECS4413@gmail.com} / {@code 4413} check is gone. The difference that
 * matters: those credentials came from configuration, not source code, so the
 * deployed password is set by an environment variable and never appears in the
 * repository. The account is created only if it is missing, so restarting the app
 * never resets a password an operator has since changed.
 */
@Configuration
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Bean
    CommandLineRunner seedAdmin(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${cyclehaven.seed.enabled}") boolean seedEnabled,
            @Value("${cyclehaven.seed.admin-email}") String adminEmail,
            @Value("${cyclehaven.seed.admin-password}") String adminPassword) {

        return args -> {
            if (!seedEnabled) {
                return;
            }
            if (userRepository.existsByEmailIgnoreCase(adminEmail)) {
                return;
            }

            User admin = new User();
            admin.setName("Store Administrator");
            admin.setEmail(adminEmail.toLowerCase());
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            admin.setRole(Role.ADMIN);
            admin.setCountry("Canada");
            userRepository.save(admin);

            // The password itself is never logged.
            log.info("Seeded admin account: {}", adminEmail);
        };
    }
}

package com.smartoffice.config;

import com.smartoffice.entity.User;
import com.smartoffice.entity.enums.Role;
import com.smartoffice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminAccountInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${INITIAL_ADMIN_USERNAME:}")
    private String adminUsername;

    @Value("${INITIAL_ADMIN_EMAIL:}")
    private String adminEmail;

    @Value("${INITIAL_ADMIN_PASSWORD:}")
    private String adminPassword;

    public AdminAccountInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (adminUsername == null || adminUsername.trim().isEmpty() ||
                adminPassword == null || adminPassword.trim().isEmpty()) {
            log.debug("No initial admin credentials provided via environment variables. Skipping initialization.");
            return;
        }

        String username = adminUsername.trim();
        String email = (adminEmail != null && !adminEmail.trim().isEmpty())
                ? adminEmail.trim().toLowerCase()
                : "admin@smartoffice.com";

        if (userRepository.existsByUsername(username)) {
            log.info("Initial admin account with username '{}' already exists. Skipping initialization.", username);
            return;
        }

        if (userRepository.existsByEmail(email)) {
            log.info("Initial admin account with email '{}' already exists. Skipping initialization.", email);
            return;
        }

        User admin = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(adminPassword.trim()))
                .role(Role.ADMIN)
                .enabled(true)
                .build();

        userRepository.save(admin);
        log.info("Initial admin account '{}' created successfully with role ADMIN.", username);
    }
}

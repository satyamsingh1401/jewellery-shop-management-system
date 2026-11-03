package com.myhourly.jewellery.security.config;

import com.myhourly.jewellery.security.entity.User;
import com.myhourly.jewellery.security.enums.Role;
import com.myhourly.jewellery.security.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Component

public class InitialAdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminName;
    private final String adminEmail;
    private final String adminPassword;

    public InitialAdminInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${INITIAL_ADMIN_NAME:}") String adminName,
            @Value("${INITIAL_ADMIN_EMAIL:}") String adminEmail,
            @Value("${INITIAL_ADMIN_PASSWORD:}") String adminPassword) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminName = adminName;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        // Do not create or reset an Admin if one already exists.
        if (userRepository.existsByRole(Role.ADMIN)) {
            return;
        }

        // Initial credentials are required if no Admin exists.
        if (adminName.isBlank()
                || adminEmail.isBlank()
                || adminPassword.isBlank()) {

            throw new IllegalStateException(
                    "No Admin account exists. Configure "
                            + "INITIAL_ADMIN_NAME, INITIAL_ADMIN_EMAIL, "
                            + "and INITIAL_ADMIN_PASSWORD environment variables."
            );
        }

        String normalizedEmail =
                adminEmail.trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalStateException(
                    "The configured initial Admin email already belongs "
                            + "to another account. Resolve this conflict before startup."
            );
        }

        if (adminPassword.length() < 8
                || adminPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException(
                    "The initial Admin password must contain at least "
                            + "8 characters and no more than 72 UTF-8 bytes."
            );
        }

        User admin = new User(
                adminName.trim(),
                normalizedEmail,
                passwordEncoder.encode(adminPassword),
                Role.ADMIN
        );

        userRepository.save(admin);
    }
}

package com.myhourly.jewellery.inventory.e2e;

import com.myhourly.jewellery.security.entity.User;
import com.myhourly.jewellery.security.enums.Role;
import com.myhourly.jewellery.security.repository.UserRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@TestConfiguration
public class E2eSecurityConfig {

    static final String ADMIN = "admin@e2e.test";
    static final String ADMIN_PASS = "admin-pass";
    static final String STAFF = "staff@e2e.test";
    static final String STAFF_PASS = "staff-pass";

    @Bean
    ApplicationRunner e2eStaffSeeder(UserRepository users, PasswordEncoder encoder) {
        return args -> {
            if (!users.existsByEmailIgnoreCase(STAFF)) {
                users.save(new User("E2E Staff", STAFF, encoder.encode(STAFF_PASS), Role.SALES_STAFF));
            }
        };
    }
}

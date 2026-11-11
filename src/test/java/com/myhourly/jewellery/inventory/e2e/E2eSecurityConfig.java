package com.myhourly.jewellery.inventory.e2e;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@TestConfiguration
public class E2eSecurityConfig {

    static final String ADMIN = "admin";
    static final String ADMIN_PASS = "admin-pass";
    static final String STAFF = "staff";
    static final String STAFF_PASS = "staff-pass";

    @Bean
    UserDetailsService e2eUsers() {
        return new InMemoryUserDetailsManager(
                User.withUsername(ADMIN).password("{noop}" + ADMIN_PASS).roles("ADMIN").build(),
                User.withUsername(STAFF).password("{noop}" + STAFF_PASS).roles("STAFF").build());
    }
}

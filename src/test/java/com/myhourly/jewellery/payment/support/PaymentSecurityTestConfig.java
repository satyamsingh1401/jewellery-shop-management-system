package com.myhourly.jewellery.payment.support;

import com.myhourly.jewellery.security.config.JacksonConfig;
import com.myhourly.jewellery.security.config.SecurityConfig;
import com.myhourly.jewellery.security.exception.CustomAccessDeniedHandler;
import com.myhourly.jewellery.security.exception.CustomAuthenticationEntryPoint;
import com.myhourly.jewellery.security.jwt.JwtAuthenticationFilter;
import com.myhourly.jewellery.security.jwt.JwtService;
import com.myhourly.jewellery.security.service.CustomUserDetailsService;
import com.myhourly.jewellery.security.service.RevokedTokenService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

// Exercise the production security chain, token validation and method rules.
// Only persistence is mocked in MVC slice tests.
@TestConfiguration(proxyBeanMethods = false)
@Import({SecurityConfig.class, JacksonConfig.class,
        CustomAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class,
        JwtAuthenticationFilter.class, JwtService.class,
        CustomUserDetailsService.class, RevokedTokenService.class})
public class PaymentSecurityTestConfig {
}

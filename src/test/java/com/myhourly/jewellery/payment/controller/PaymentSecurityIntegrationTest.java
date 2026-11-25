package com.myhourly.jewellery.payment.controller;

import com.myhourly.jewellery.payment.dto.PaymentRequestDto;
import com.myhourly.jewellery.payment.service.PaymentService;
import com.myhourly.jewellery.payment.support.PaymentSecurityTestConfig;
import com.myhourly.jewellery.security.entity.User;
import com.myhourly.jewellery.security.enums.Role;
import com.myhourly.jewellery.security.jwt.JwtService;
import com.myhourly.jewellery.security.repository.RevokedTokenRepository;
import com.myhourly.jewellery.security.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@MockitoBean(types = {com.myhourly.jewellery.security.repository.UserRepository.class,
        com.myhourly.jewellery.security.repository.RevokedTokenRepository.class})
@WebMvcTest(PaymentController.class)
@ActiveProfiles("test")
@Import(PaymentSecurityTestConfig.class)
class PaymentSecurityIntegrationTest {
    private static final String CREATE = """
            {"orderId":1,"amount":100,"paymentMethod":"UPI"}
            """;
    private static final String REFUND = """
            {"amount":10,"reason":"Customer return"}
            """;

    @Autowired MockMvc mvc;
    @Autowired JwtService jwtService;
    @Autowired UserRepository userRepository;
    @Autowired RevokedTokenRepository revokedTokenRepository;
    @MockitoBean PaymentService paymentService;

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "SALES_STAFF"})
    void bearerTokenAllowsNormalPaymentActions(String role) throws Exception {
        String authorization = bearer(Role.valueOf(role), "staff@example.test", true);
        for (String path : new String[]{"", "/pending", "/1", "/invoice/1"}) {
            mvc.perform(get("/api/payments" + path).header("Authorization", authorization))
                    .andExpect(status().isOk());
        }
        mvc.perform(post("/api/payments").header("Authorization", authorization)
                        .contentType(APPLICATION_JSON).content(CREATE))
                .andExpect(status().isCreated());
        for (String action : new String[]{"confirm", "fail", "cancel"}) {
            mvc.perform(post("/api/payments/1/" + action).header("Authorization", authorization))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void adminCanRefundWithBearerToken() throws Exception {
        mvc.perform(post("/api/payments/1/refund")
                        .header("Authorization", bearer(Role.ADMIN, "admin@example.test", true))
                        .contentType(APPLICATION_JSON).content(REFUND))
                .andExpect(status().isOk());
        verify(paymentService).recordRefund(1L, new java.math.BigDecimal("10"), "Customer return");
    }

    @Test
    void salesStaffCannotRefund() throws Exception {
        mvc.perform(post("/api/payments/1/refund")
                        .header("Authorization", bearer(Role.SALES_STAFF, "staff@example.test", true))
                        .contentType(APPLICATION_JSON).content(REFUND))
                .andExpect(status().isForbidden());
        verifyNoInteractions(paymentService);
    }

    @Test
    void paymentRecorderUsesAuthenticatedEmailIncludingLongEmails() throws Exception {
        String email = "a".repeat(64) + "@" + "b".repeat(63) + "." + "c".repeat(16) + ".test";
        mvc.perform(post("/api/payments")
                        .header("Authorization", bearer(Role.SALES_STAFF, email, true))
                        .contentType(APPLICATION_JSON)
                        .content(CREATE.replace("\"orderId\":1", "\"createdBy\":\"forged\",\"orderId\":1")))
                .andExpect(status().isCreated());
        verify(paymentService).createPayment(any(PaymentRequestDto.class), eq(email));
    }

    @Test
    void everyPaymentEndpointRequiresAuthentication() throws Exception {
        for (String path : new String[]{"", "/pending", "/1", "/invoice/1"}) {
            mvc.perform(get("/api/payments" + path)).andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/payments").contentType(APPLICATION_JSON).content(CREATE))
                .andExpect(status().isUnauthorized());
        for (String action : new String[]{"confirm", "fail", "cancel", "refund"}) {
            mvc.perform(post("/api/payments/1/" + action)
                            .contentType(APPLICATION_JSON).content(REFUND))
                    .andExpect(status().isUnauthorized());
        }
        verifyNoInteractions(paymentService);
    }

    @Test
    void unrelatedRoleCannotAccessAnyPaymentEndpoint() throws Exception {
        for (String path : new String[]{"", "/pending", "/1", "/invoice/1"}) {
            mvc.perform(get("/api/payments" + path).with(user("viewer").roles("USER")))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(post("/api/payments").with(user("viewer").roles("USER"))
                        .contentType(APPLICATION_JSON).content(CREATE))
                .andExpect(status().isForbidden());
        for (String action : new String[]{"confirm", "fail", "cancel", "refund"}) {
            mvc.perform(post("/api/payments/1/" + action).with(user("viewer").roles("USER"))
                            .contentType(APPLICATION_JSON).content(REFUND))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(paymentService);
    }

    @Test
    void invalidBearerTokenIsRejected() throws Exception {
        mvc.perform(get("/api/payments").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(paymentService);
    }

    @Test
    void revokedBearerTokenIsRejected() throws Exception {
        String authorization = bearer(Role.ADMIN, "admin@example.test", true);
        when(revokedTokenRepository.existsByJti(anyString())).thenReturn(true);
        mvc.perform(get("/api/payments").header("Authorization", authorization))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(paymentService);
    }

    @Test
    void inactiveUserCannotUseExistingToken() throws Exception {
        mvc.perform(get("/api/payments")
                        .header("Authorization", bearer(Role.ADMIN, "admin@example.test", false)))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(paymentService);
    }

    private String bearer(Role role, String email, boolean active) {
        User account = new User("Test Staff", email, "unused-password-hash", role);
        account.setActive(active);
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(account));
        return "Bearer " + jwtService.generateToken(account);
    }
}

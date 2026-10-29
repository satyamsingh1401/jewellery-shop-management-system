package com.myhourly.jewellery.payment.controller;

import com.myhourly.jewellery.payment.dto.PaymentRequestDto;
import com.myhourly.jewellery.payment.dto.PaymentResponseDto;
import com.myhourly.jewellery.payment.enums.PaymentStatus;
import com.myhourly.jewellery.payment.service.PaymentService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void getAllPaymentsShouldReturnOk() throws Exception {
        when(paymentService.getAllPayments()).thenReturn(List.of());

        mockMvc.perform(get("/api/payments")
                .with(httpBasic("user", "PaymentTest@123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getPaymentByIdShouldReturnPayment() throws Exception {
        PaymentResponseDto response = new PaymentResponseDto();
        response.setId(1L);
        response.setAmount(new BigDecimal("2500.00"));
        response.setStatus(PaymentStatus.COMPLETED);

        when(paymentService.getPaymentById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/payments/1")
                .with(httpBasic("user", "PaymentTest@123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.amount").value(2500.00));
    }

    @Test
    void getPaymentsWithoutAuthenticationShouldReturnUnauthorized()
            throws Exception {
        mockMvc.perform(get("/api/payments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createPaymentWithInvalidAmountShouldReturnBadRequest()
            throws Exception {
        String requestJson = """
                {
                    "amount": 0,
                    "paymentMethod": "UPI"
                }
                """;

        mockMvc.perform(post("/api/payments")
                .with(httpBasic("user", "PaymentTest@123"))
                .with(csrf())
                .contentType(APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPaymentWithMissingAmountShouldReturnBadRequest()
            throws Exception {
        String requestJson = """
                {
                    "paymentMethod": "UPI"
                }
                """;

        mockMvc.perform(post("/api/payments")
                .with(httpBasic("user", "PaymentTest@123"))
                .with(csrf())
                .contentType(APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPaymentWithValidRequestShouldReturnCreated()
            throws Exception {
        PaymentResponseDto response = new PaymentResponseDto();
        response.setId(10L);
        response.setAmount(new BigDecimal("1500.00"));
        response.setStatus(PaymentStatus.PENDING);

        when(paymentService.createPayment(any(PaymentRequestDto.class)))
                .thenReturn(response);

        String requestJson = """
                {
                    "amount": 1500,
                    "paymentMethod": "UPI"
                }
                """;

        mockMvc.perform(post("/api/payments")
                .with(httpBasic("user", "PaymentTest@123"))
                .with(csrf())
                .contentType(APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void createPaymentWithInvalidMethodShouldReturnBadRequest()
            throws Exception {
        String requestJson = """
                {
                    "amount": 1500,
                    "paymentMethod": "INVALID"
                }
                """;

        mockMvc.perform(post("/api/payments")
                .with(httpBasic("user", "PaymentTest@123"))
                .with(csrf())
                .contentType(APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPaymentByMissingIdShouldReturnNotFound() throws Exception {
        when(paymentService.getPaymentById(999L))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Payment not found: 999"));

        mockMvc.perform(get("/api/payments/999")
                .with(httpBasic("user", "PaymentTest@123")))
                .andExpect(status().isNotFound());
    }
}
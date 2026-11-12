package com.myhourly.jewellery.payment.controller;

import com.myhourly.jewellery.common.exception.BusinessValidationException;
import com.myhourly.jewellery.common.exception.ResourceNotFoundException;
import com.myhourly.jewellery.payment.dto.PaymentRequestDto;
import com.myhourly.jewellery.payment.dto.PaymentResponseDto;
import com.myhourly.jewellery.payment.enums.PaymentStatus;
import com.myhourly.jewellery.payment.service.PaymentService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
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
                .with(user("user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getPendingPaymentsShouldReturnOk() throws Exception {
        PaymentResponseDto pending = new PaymentResponseDto();
        pending.setId(5L);
        pending.setStatus(PaymentStatus.PENDING);

        when(paymentService.getPendingPayments()).thenReturn(List.of(pending));

        mockMvc.perform(get("/api/payments/pending")
                .with(user("user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void getPaymentByIdShouldReturnPayment() throws Exception {
        PaymentResponseDto response = new PaymentResponseDto();
        response.setId(1L);
        response.setAmount(new BigDecimal("2500.00"));
        response.setStatus(PaymentStatus.COMPLETED);

        when(paymentService.getPaymentById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/payments/1")
                .with(user("user")))
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
                    "orderId": 1,
                    "amount": 0,
                    "paymentMethod": "UPI"
                }
                """;

        mockMvc.perform(post("/api/payments")
                .with(user("user"))
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
                    "orderId": 1,
                    "paymentMethod": "UPI"
                }
                """;

        mockMvc.perform(post("/api/payments")
                .with(user("user"))
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

        when(paymentService.createPayment(
                any(PaymentRequestDto.class), any(String.class)))
                .thenReturn(response);

        String requestJson = """
                {
                    "orderId": 1,
                    "amount": 1500,
                    "paymentMethod": "UPI"
                }
                """;

        mockMvc.perform(post("/api/payments")
                .with(user("user"))
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
                    "orderId": 1,
                    "amount": 1500,
                    "paymentMethod": "INVALID"
                }
                """;

        mockMvc.perform(post("/api/payments")
                .with(user("user"))
                .with(csrf())
                .contentType(APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPaymentByMissingIdShouldReturnNotFound() throws Exception {
        when(paymentService.getPaymentById(999L))
                .thenThrow(new ResourceNotFoundException("Payment not found: 999"));

        mockMvc.perform(get("/api/payments/999")
                .with(user("user")))
                .andExpect(status().isNotFound());
    }

    @Test
    void createPaymentWithMalformedJsonShouldReturnStandardError()
            throws Exception {
        String requestJson = """
                {
                    "orderId": 1,
                    "amount": 500,
                    "paymentMethod":
                }
                """;

        mockMvc.perform(post("/api/payments")
                .with(user("user"))
                .with(csrf())
                .contentType(APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message")
                        .value("Request body contains invalid JSON or enum values"))
                .andExpect(jsonPath("$.path").value("/api/payments"));
    }

    @Test
    void createPaymentWithMissingPaymentMethodShouldReturnValidationError()
            throws Exception {
        String requestJson = """
                {
                    "orderId": 1,
                    "amount": 500
                }
                """;

        mockMvc.perform(post("/api/payments")
                .with(user("user"))
                .with(csrf())
                .contentType(APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Request validation failed"))
                .andExpect(jsonPath("$.fieldErrors.paymentMethod")
                        .value("Payment method is required"))
                .andExpect(jsonPath("$.path").value("/api/payments"));
    }

    @Test
    void createPaymentWithMissingOrderIdShouldReturnValidationError()
            throws Exception {
        String requestJson = """
                {
                    "amount": 500,
                    "paymentMethod": "UPI"
                }
                """;

        mockMvc.perform(post("/api/payments")
                .with(user("user"))
                .with(csrf())
                .contentType(APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Request validation failed"))
                .andExpect(jsonPath("$.fieldErrors.orderId")
                        .value("Order ID is required"));
    }

    @Test
    void createPaymentWithInvalidOrderIdShouldReturnBadRequest()
            throws Exception {
        String requestJson = """
                {
                    "orderId": 0,
                    "amount": 500,
                    "paymentMethod": "UPI"
                }
                """;

        mockMvc.perform(post("/api/payments")
                .with(user("user"))
                .with(csrf())
                .contentType(APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMissingPaymentShouldReturnStandardErrorResponse()
            throws Exception {
        when(paymentService.getPaymentById(999L))
                .thenThrow(new ResourceNotFoundException("Payment not found: 999"));

        mockMvc.perform(get("/api/payments/999")
                .with(user("user")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message")
                        .value("Payment not found: 999"))
                .andExpect(jsonPath("$.path").value("/api/payments/999"));
    }

    // ---------------------------------------------------------------
    // payments for an invoice
    // ---------------------------------------------------------------

    @Test
    void getPaymentsForInvoiceShouldReturnPayments() throws Exception {
        PaymentResponseDto response = new PaymentResponseDto();
        response.setId(3L);
        response.setInvoiceId(7L);
        response.setStatus(PaymentStatus.COMPLETED);

        when(paymentService.getPaymentsForInvoice(7L))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/payments/invoice/7")
                .with(user("user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(3))
                .andExpect(jsonPath("$[0].invoiceId").value(7));
    }

    @Test
    void getPaymentsForInvoiceWithoutAuthenticationShouldReturnUnauthorized()
            throws Exception {
        mockMvc.perform(get("/api/payments/invoice/7"))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------
    // confirm / fail / cancel
    // ---------------------------------------------------------------

    @Test
    void confirmPaymentShouldReturnCompletedPayment() throws Exception {
        PaymentResponseDto response = new PaymentResponseDto();
        response.setId(5L);
        response.setStatus(PaymentStatus.COMPLETED);

        when(paymentService.confirmPayment(5L)).thenReturn(response);

        mockMvc.perform(post("/api/payments/5/confirm")
                .with(user("user"))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void confirmPaymentWithoutAuthenticationShouldReturnUnauthorized()
            throws Exception {
        mockMvc.perform(post("/api/payments/5/confirm").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void confirmMissingPaymentShouldReturnNotFound() throws Exception {
        when(paymentService.confirmPayment(999L))
                .thenThrow(new ResourceNotFoundException("Payment not found: 999"));

        mockMvc.perform(post("/api/payments/999/confirm")
                .with(user("user"))
                .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void confirmNonPendingPaymentShouldReturnBadRequest() throws Exception {
        when(paymentService.confirmPayment(6L))
                .thenThrow(new BusinessValidationException("Only pending payments can be moved to COMPLETED"));

        mockMvc.perform(post("/api/payments/6/confirm")
                .with(user("user"))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void failPaymentShouldReturnFailedPayment() throws Exception {
        PaymentResponseDto response = new PaymentResponseDto();
        response.setId(5L);
        response.setStatus(PaymentStatus.FAILED);

        when(paymentService.failPayment(5L)).thenReturn(response);

        mockMvc.perform(post("/api/payments/5/fail")
                .with(user("user"))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    void failNonPendingPaymentShouldReturnBadRequest() throws Exception {
        when(paymentService.failPayment(6L))
                .thenThrow(new BusinessValidationException("Only pending payments can be moved to FAILED"));

        mockMvc.perform(post("/api/payments/6/fail")
                .with(user("user"))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancelPaymentShouldReturnCancelledPayment() throws Exception {
        PaymentResponseDto response = new PaymentResponseDto();
        response.setId(5L);
        response.setStatus(PaymentStatus.CANCELLED);

        when(paymentService.cancelPayment(5L)).thenReturn(response);

        mockMvc.perform(post("/api/payments/5/cancel")
                .with(user("user"))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void cancelMissingPaymentShouldReturnNotFound() throws Exception {
        when(paymentService.cancelPayment(999L))
                .thenThrow(new ResourceNotFoundException("Payment not found: 999"));

        mockMvc.perform(post("/api/payments/999/cancel")
                .with(user("user"))
                .with(csrf()))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------
    // refund
    // ---------------------------------------------------------------

    @Test
    void refundWithZeroAmountShouldReturnBadRequest()
            throws Exception {
        String requestJson = """
                {
                    "amount": 0,
                    "reason": "Test refund"
                }
                """;

        mockMvc.perform(post("/api/payments/1/refund")
                .with(user("user"))
                .with(csrf())
                .contentType(APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refundWithMissingAmountShouldReturnBadRequest()
            throws Exception {
        String requestJson = """
                {
                    "reason": "Test refund"
                }
                """;

        mockMvc.perform(post("/api/payments/1/refund")
                .with(user("user"))
                .with(csrf())
                .contentType(APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refundWithBlankReasonShouldReturnBadRequest()
            throws Exception {
        String requestJson = """
                {
                    "amount": 10,
                    "reason": "   "
                }
                """;

        mockMvc.perform(post("/api/payments/1/refund")
                .with(user("user"))
                .with(csrf())
                .contentType(APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refundWithNegativeAmountShouldReturnBadRequest()
            throws Exception {
        String requestJson = """
                {
                    "amount": -10,
                    "reason": "Test refund"
                }
                """;

        mockMvc.perform(post("/api/payments/1/refund")
                .with(user("user"))
                .with(csrf())
                .contentType(APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refundWithMissingReasonShouldReturnBadRequest()
            throws Exception {
        String requestJson = """
                {
                    "amount": 10
                }
                """;

        mockMvc.perform(post("/api/payments/1/refund")
                .with(user("user"))
                .with(csrf())
                .contentType(APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refundWithValidRequestShouldReturnOk()
            throws Exception {
        PaymentResponseDto response = new PaymentResponseDto();
        response.setId(1L);
        response.setAmount(new BigDecimal("100.00"));
        response.setRefundedAmount(new BigDecimal("10.00"));
        response.setStatus(PaymentStatus.PARTIALLY_REFUNDED);

        when(paymentService.recordRefund(
                1L, new BigDecimal("10"), "Test refund"))
                .thenReturn(response);

        String requestJson = """
                {
                    "amount": 10,
                    "reason": "Test refund"
                }
                """;

        mockMvc.perform(post("/api/payments/1/refund")
                .with(user("user"))
                .with(csrf())
                .contentType(APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.refundedAmount").value(10.00))
                .andExpect(jsonPath("$.status")
                        .value("PARTIALLY_REFUNDED"));
    }
}
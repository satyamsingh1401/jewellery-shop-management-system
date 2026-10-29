
package com.myhourly.jewellery.payment.service;

import com.myhourly.jewellery.payment.Payment;
import com.myhourly.jewellery.payment.dto.PaymentRequestDto;
import com.myhourly.jewellery.payment.enums.PaymentMethod;
import com.myhourly.jewellery.payment.enums.PaymentStatus;
import com.myhourly.jewellery.payment.repository.PaymentRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void createPaymentShouldSavePendingPayment() {
        PaymentRequestDto request = new PaymentRequestDto();
        request.setAmount(new BigDecimal("1500.00"));
        request.setPaymentMethod(PaymentMethod.UPI);

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = paymentService.createPayment(request);

        assertEquals(new BigDecimal("1500.00"), response.getAmount());
        assertEquals(PaymentStatus.PENDING, response.getStatus());
        assertEquals(BigDecimal.ZERO, response.getRefundedAmount());
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void getPaymentByIdShouldThrowWhenPaymentDoesNotExist() {
        when(paymentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> paymentService.getPaymentById(999L));
    }

    @Test
    void refundShouldFailWhenPaymentDoesNotExist() {
        when(paymentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> paymentService.recordRefund(
                        999L, new BigDecimal("500"), "Test refund"));
    }

    @Test
    void refundShouldFailForPendingPayment() {
        Payment payment = createPayment(PaymentStatus.PENDING);

        when(paymentRepository.findById(10L)).thenReturn(Optional.of(payment));

        assertThrows(ResponseStatusException.class,
                () -> paymentService.recordRefund(
                        10L, new BigDecimal("500"), "Test refund"));

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void refundShouldFailWhenAmountIsZero() {
        Payment payment = createPayment(PaymentStatus.COMPLETED);

        when(paymentRepository.findById(10L)).thenReturn(Optional.of(payment));

        assertThrows(ResponseStatusException.class,
                () -> paymentService.recordRefund(
                        10L, BigDecimal.ZERO, "Test refund"));

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void refundShouldFailWhenAmountExceedsPaymentAmount() {
        Payment payment = createPayment(PaymentStatus.COMPLETED);

        when(paymentRepository.findById(10L)).thenReturn(Optional.of(payment));

        assertThrows(ResponseStatusException.class,
                () -> paymentService.recordRefund(
                        10L, new BigDecimal("2000"), "Excess refund"));

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void partialRefundShouldSetPartiallyRefundedStatus() {
        Payment payment = createPayment(PaymentStatus.COMPLETED);

        when(paymentRepository.findById(10L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = paymentService.recordRefund(
                10L, new BigDecimal("500"), "Partial refund");

        assertEquals(PaymentStatus.PARTIALLY_REFUNDED, response.getStatus());
        assertEquals(new BigDecimal("500"), response.getRefundedAmount());
        verify(paymentRepository).save(payment);
    }

    @Test
    void fullRefundShouldSetRefundedStatus() {
        Payment payment = createPayment(PaymentStatus.COMPLETED);

        when(paymentRepository.findById(10L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = paymentService.recordRefund(
                10L, new BigDecimal("1500"), "Full refund");

        assertEquals(PaymentStatus.REFUNDED, response.getStatus());
        assertEquals(new BigDecimal("1500"), response.getRefundedAmount());
        verify(paymentRepository).save(payment);
    }

    private Payment createPayment(PaymentStatus status) {
        Payment payment = new Payment();
        payment.setAmount(new BigDecimal("1500"));
        payment.setStatus(status);
        payment.setRefundedAmount(BigDecimal.ZERO);
        return payment;
    }
}

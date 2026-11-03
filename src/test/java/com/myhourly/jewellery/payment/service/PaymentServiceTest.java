package com.myhourly.jewellery.payment.service;

import com.myhourly.jewellery.payment.entity.Payment;
import com.myhourly.jewellery.common.exception.BusinessValidationException;
import com.myhourly.jewellery.common.exception.ResourceNotFoundException;
import com.myhourly.jewellery.payment.dto.PaymentRequestDto;
import com.myhourly.jewellery.payment.enums.PaymentMethod;
import com.myhourly.jewellery.payment.enums.PaymentStatus;
import com.myhourly.jewellery.payment.repository.PaymentRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final String STAFF = "staff1";

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    // ---------------------------------------------------------------
    // createPayment
    // ---------------------------------------------------------------

    @Test
    void createPaymentShouldSavePendingPayment() {
        PaymentRequestDto request = validRequest();

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = paymentService.createPayment(request, STAFF);

        assertEquals(new BigDecimal("1500.00"), response.getAmount());
        assertEquals(PaymentMethod.UPI, response.getPaymentMethod());
        assertEquals(PaymentStatus.PENDING, response.getStatus());
        assertEquals(BigDecimal.ZERO, response.getRefundedAmount());

        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void createPaymentShouldStoreCreatedByAndBillId() {
        PaymentRequestDto request = validRequest();

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = paymentService.createPayment(request, "  staff1  ");

        ArgumentCaptor<Payment> captor =
                ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());

        assertEquals("staff1", captor.getValue().getCreatedBy());
        assertEquals(7L, captor.getValue().getBillId());
        assertEquals("staff1", response.getCreatedBy());
        assertEquals(7L, response.getBillId());
    }

    @Test
    void createPaymentShouldRejectNullCreatedBy() {
        assertThrows(
                BusinessValidationException.class,
                () -> paymentService.createPayment(validRequest(), null));

        verifyNoInteractions(paymentRepository);
    }

    @Test
    void createPaymentShouldRejectBlankCreatedBy() {
        assertThrows(
                BusinessValidationException.class,
                () -> paymentService.createPayment(validRequest(), "   "));

        verifyNoInteractions(paymentRepository);
    }

    @Test
    void createPaymentShouldRejectNullRequest() {
        assertThrows(BusinessValidationException.class,
                () -> paymentService.createPayment(null, STAFF));

        verifyNoInteractions(paymentRepository);
    }

    @Test
    void createPaymentShouldRejectNullAmount() {
        PaymentRequestDto request = validRequest();
        request.setAmount(null);

        assertThrows(BusinessValidationException.class,
                () -> paymentService.createPayment(request, STAFF));

        verifyNoInteractions(paymentRepository);
    }

    @Test
    void createPaymentShouldRejectAmountBelowMinimum() {
        PaymentRequestDto request = validRequest();
        request.setAmount(new BigDecimal("0.009"));

        assertThrows(BusinessValidationException.class,
                () -> paymentService.createPayment(request, STAFF));

        verifyNoInteractions(paymentRepository);
    }

    @Test
    void createPaymentShouldRejectMoreThanTwoDecimalPlaces() {
        PaymentRequestDto request = validRequest();
        request.setAmount(new BigDecimal("10.005"));

        assertThrows(
                BusinessValidationException.class,
                () -> paymentService.createPayment(request, STAFF));

        verifyNoInteractions(paymentRepository);
    }

    @Test
    void createPaymentShouldAcceptMinimumAmount() {
        PaymentRequestDto request = validRequest();
        request.setAmount(new BigDecimal("0.01"));

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = paymentService.createPayment(request, STAFF);

        assertEquals(new BigDecimal("0.01"), response.getAmount());
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void createPaymentShouldRejectNullPaymentMethod() {
        PaymentRequestDto request = validRequest();
        request.setPaymentMethod(null);

        assertThrows(BusinessValidationException.class,
                () -> paymentService.createPayment(request, STAFF));

        verifyNoInteractions(paymentRepository);
    }

    // ---------------------------------------------------------------
    // getPaymentById
    // ---------------------------------------------------------------

    @Test
    void getPaymentByIdShouldThrowWhenPaymentDoesNotExist() {
        when(paymentRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> paymentService.getPaymentById(999L));
    }

    @Test
    void getPaymentByIdShouldRejectInvalidId() {
        assertThrows(BusinessValidationException.class,
                () -> paymentService.getPaymentById(0L));

        assertThrows(BusinessValidationException.class,
                () -> paymentService.getPaymentById(-1L));

        verifyNoInteractions(paymentRepository);
    }

    // ---------------------------------------------------------------
    // getPaymentsForBill
    // ---------------------------------------------------------------

    @Test
    void getPaymentsForBillShouldReturnMatchingPayments() {
        Payment first = createPayment(PaymentStatus.COMPLETED);
        first.setBillId(7L);
        Payment second = createPayment(PaymentStatus.PENDING);
        second.setBillId(7L);

        when(paymentRepository.findByBillIdOrderByCreatedAtDesc(7L))
                .thenReturn(List.of(first, second));

        var results = paymentService.getPaymentsForBill(7L);

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(r -> r.getBillId() == 7L));
        assertEquals(PaymentStatus.COMPLETED, results.get(0).getStatus());
    }

    @Test
    void getPaymentsForBillShouldReturnEmptyListWhenNoPayments() {
        when(paymentRepository.findByBillIdOrderByCreatedAtDesc(8L))
                .thenReturn(List.of());

        var results = paymentService.getPaymentsForBill(8L);

        assertTrue(results.isEmpty());
    }

    @Test
    void getPaymentsForBillShouldRejectInvalidBillId() {
        assertThrows(BusinessValidationException.class,
                () -> paymentService.getPaymentsForBill(null));

        assertThrows(BusinessValidationException.class,
                () -> paymentService.getPaymentsForBill(0L));

        assertThrows(BusinessValidationException.class,
                () -> paymentService.getPaymentsForBill(-1L));

        verifyNoInteractions(paymentRepository);
    }

    // ---------------------------------------------------------------
    // confirmPayment
    // ---------------------------------------------------------------

    @Test
    void confirmPaymentShouldMovePendingToCompletedAndSetPaymentDate() {
        Payment payment = createPayment(PaymentStatus.PENDING);

        when(paymentRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = paymentService.confirmPayment(10L);

        assertEquals(PaymentStatus.COMPLETED, response.getStatus());
        assertNotNull(response.getPaymentDate());
        verify(paymentRepository).save(payment);
    }

    @Test
    void confirmPaymentShouldFailWhenPaymentDoesNotExist() {
        when(paymentRepository.findByIdForUpdate(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> paymentService.confirmPayment(999L));

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void confirmPaymentShouldRejectInvalidId() {
        assertThrows(BusinessValidationException.class,
                () -> paymentService.confirmPayment(0L));

        verifyNoInteractions(paymentRepository);
    }

    @Test
    void confirmPaymentShouldRejectNonPendingPayments() {
        for (PaymentStatus status : new PaymentStatus[] {
                PaymentStatus.COMPLETED,
                PaymentStatus.FAILED,
                PaymentStatus.CANCELLED,
                PaymentStatus.PARTIALLY_REFUNDED,
                PaymentStatus.REFUNDED }) {

            Payment payment = createPayment(status);

            when(paymentRepository.findByIdForUpdate(10L))
                    .thenReturn(Optional.of(payment));

            assertThrows(
                    BusinessValidationException.class,
                    () -> paymentService.confirmPayment(10L));

            assertEquals(status, payment.getStatus());
        }

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    // ---------------------------------------------------------------
    // failPayment
    // ---------------------------------------------------------------

    @Test
    void failPaymentShouldMovePendingToFailed() {
        Payment payment = createPayment(PaymentStatus.PENDING);

        when(paymentRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = paymentService.failPayment(10L);

        assertEquals(PaymentStatus.FAILED, response.getStatus());
        verify(paymentRepository).save(payment);
    }

    @Test
    void failPaymentShouldRejectCompletedPayment() {
        Payment payment = createPayment(PaymentStatus.COMPLETED);

        when(paymentRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(payment));

        assertThrows(BusinessValidationException.class,
                () -> paymentService.failPayment(10L));

        assertEquals(PaymentStatus.COMPLETED, payment.getStatus());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void failPaymentShouldFailWhenPaymentDoesNotExist() {
        when(paymentRepository.findByIdForUpdate(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> paymentService.failPayment(999L));
    }

    // ---------------------------------------------------------------
    // cancelPayment
    // ---------------------------------------------------------------

    @Test
    void cancelPaymentShouldMovePendingToCancelled() {
        Payment payment = createPayment(PaymentStatus.PENDING);

        when(paymentRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = paymentService.cancelPayment(10L);

        assertEquals(PaymentStatus.CANCELLED, response.getStatus());
        verify(paymentRepository).save(payment);
    }

    @Test
    void cancelPaymentShouldRejectAlreadyFailedPayment() {
        Payment payment = createPayment(PaymentStatus.FAILED);

        when(paymentRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(payment));

        assertThrows(BusinessValidationException.class,
                () -> paymentService.cancelPayment(10L));

        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void cancelPaymentShouldRejectInvalidId() {
        assertThrows(BusinessValidationException.class,
                () -> paymentService.cancelPayment(-5L));

        verifyNoInteractions(paymentRepository);
    }

    // ---------------------------------------------------------------
    // recordRefund
    // ---------------------------------------------------------------

    @Test
    void refundShouldFailWhenPaymentDoesNotExist() {
        when(paymentRepository.findByIdForUpdate(999L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> paymentService.recordRefund(
                        999L, new BigDecimal("500"), "Test refund"));
    }

    @Test
    void refundShouldRejectInvalidPaymentId() {
        assertThrows(BusinessValidationException.class,
                () -> paymentService.recordRefund(
                        0L, new BigDecimal("500"), "Test refund"));

        verifyNoInteractions(paymentRepository);
    }

    @Test
    void refundShouldFailForPendingPayment() {
        Payment payment = createPayment(PaymentStatus.PENDING);

        when(paymentRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(payment));

        assertThrows(BusinessValidationException.class,
                () -> paymentService.recordRefund(
                        10L, new BigDecimal("500"), "Test refund"));

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void refundShouldFailForFailedPayment() {
        assertRefundRejectedForStatus(PaymentStatus.FAILED);
    }

    @Test
    void refundShouldFailForCancelledPayment() {
        assertRefundRejectedForStatus(PaymentStatus.CANCELLED);
    }

    @Test
    void refundShouldFailWhenAmountIsNull() {
        assertRefundRejected(null, "Test refund");
    }

    @Test
    void refundShouldFailWhenAmountIsZero() {
        assertRefundRejected(BigDecimal.ZERO, "Test refund");
    }

    @Test
    void refundShouldFailWhenAmountIsNegative() {
        assertRefundRejected(new BigDecimal("-10"), "Test refund");
    }

    @Test
    void refundShouldFailWhenAmountIsBelowMinimum() {
        assertRefundRejected(new BigDecimal("0.009"), "Test refund");
    }

    @Test
    void refundShouldFailWhenAmountHasMoreThanTwoDecimalPlaces() {
        assertRefundRejected(new BigDecimal("10.005"), "Test refund");
    }

    @Test
    void refundShouldFailWhenReasonIsNull() {
        assertRefundRejected(new BigDecimal("100"), null);
    }

    @Test
    void refundShouldFailWhenReasonIsBlank() {
        assertRefundRejected(new BigDecimal("100"), "   ");
    }

    @Test
    void refundShouldFailWhenAmountExceedsPaymentAmount() {
        assertRefundRejected(new BigDecimal("2000"), "Excess refund");
    }

    @Test
    void partialRefundShouldSetPartiallyRefundedStatus() {
        Payment payment = createPayment(PaymentStatus.COMPLETED);

        when(paymentRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = paymentService.recordRefund(
                10L, new BigDecimal("500"), "Partial refund");

        assertEquals(PaymentStatus.PARTIALLY_REFUNDED, response.getStatus());
        assertEquals(new BigDecimal("500"), response.getRefundedAmount());
        assertEquals("Partial refund", response.getRefundReason());

        verify(paymentRepository).save(payment);
    }

    @Test
    void fullRefundShouldSetRefundedStatus() {
        Payment payment = createPayment(PaymentStatus.COMPLETED);

        when(paymentRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = paymentService.recordRefund(
                10L, new BigDecimal("1500"), "Full refund");

        assertEquals(PaymentStatus.REFUNDED, response.getStatus());
        assertEquals(new BigDecimal("1500"), response.getRefundedAmount());

        verify(paymentRepository).save(payment);
    }

    @Test
    void repeatedRefundShouldAllowRemainingRefundableAmount() {
        Payment payment = createPayment(PaymentStatus.PARTIALLY_REFUNDED);
        payment.setRefundedAmount(new BigDecimal("500"));

        when(paymentRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = paymentService.recordRefund(
                10L, new BigDecimal("1000"), "Remaining refund");

        assertEquals(PaymentStatus.REFUNDED, response.getStatus());
        assertEquals(new BigDecimal("1500"), response.getRefundedAmount());
    }

    @Test
    void repeatedPartialRefundShouldStayPartiallyRefunded() {
        Payment payment = createPayment(PaymentStatus.PARTIALLY_REFUNDED);
        payment.setRefundedAmount(new BigDecimal("500"));

        when(paymentRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = paymentService.recordRefund(
                10L, new BigDecimal("300"), "Second partial refund");

        assertEquals(PaymentStatus.PARTIALLY_REFUNDED, response.getStatus());
        assertEquals(new BigDecimal("800"), response.getRefundedAmount());
    }

    @Test
    void repeatedRefundShouldKeepEarlierReasons() {
        Payment payment = createPayment(PaymentStatus.PARTIALLY_REFUNDED);
        payment.setRefundedAmount(new BigDecimal("500"));
        payment.setRefundReason("First reason");

        when(paymentRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = paymentService.recordRefund(
                10L, new BigDecimal("100"), "  Second reason  ");

        assertEquals("First reason; Second reason", response.getRefundReason());
    }

    @Test
    void refundShouldRejectWhenReasonHistoryExceedsLimit() {
        Payment payment = createPayment(PaymentStatus.PARTIALLY_REFUNDED);
        payment.setRefundedAmount(new BigDecimal("100"));
        payment.setRefundReason("a".repeat(495));

        when(paymentRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(payment));

        assertThrows(
                BusinessValidationException.class,
                () -> paymentService.recordRefund(
                        10L, new BigDecimal("100"), "abcdefghij"));

        assertEquals(new BigDecimal("100"), payment.getRefundedAmount());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void repeatedRefundShouldRejectAmountAboveRemainingBalance() {
        Payment payment = createPayment(PaymentStatus.PARTIALLY_REFUNDED);
        payment.setRefundedAmount(new BigDecimal("1000"));

        when(paymentRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(payment));

        assertThrows(BusinessValidationException.class,
                () -> paymentService.recordRefund(
                        10L, new BigDecimal("501"), "Excess refund"));

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void refundShouldFailWhenAlreadyFullyRefunded() {
        assertRefundRejectedForStatus(PaymentStatus.REFUNDED);
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private void assertRefundRejectedForStatus(PaymentStatus status) {
        Payment payment = createPayment(status);

        when(paymentRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(payment));

        assertThrows(BusinessValidationException.class,
                () -> paymentService.recordRefund(
                        10L, new BigDecimal("100"), "Test refund"));

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    private void assertRefundRejected(BigDecimal amount, String reason) {
        Payment payment = createPayment(PaymentStatus.COMPLETED);

        when(paymentRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(payment));

        assertThrows(BusinessValidationException.class,
                () -> paymentService.recordRefund(10L, amount, reason));

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    private PaymentRequestDto validRequest() {
        PaymentRequestDto request = new PaymentRequestDto();
        request.setBillId(7L);
        request.setAmount(new BigDecimal("1500.00"));
        request.setPaymentMethod(PaymentMethod.UPI);
        return request;
    }

    private Payment createPayment(PaymentStatus status) {
        Payment payment = new Payment();
        payment.setAmount(new BigDecimal("1500"));
        payment.setStatus(status);
        payment.setRefundedAmount(BigDecimal.ZERO);
        return payment;
    }
}
package com.myhourly.jewellery.payment.service;

import com.myhourly.jewellery.billing.dto.InvoiceResponse;
import com.myhourly.jewellery.billing.enums.InvoiceStatus;
import com.myhourly.jewellery.billing.service.InvoiceService;
import com.myhourly.jewellery.payment.entity.Payment;
import com.myhourly.jewellery.common.exception.BusinessValidationException;
import com.myhourly.jewellery.common.exception.ResourceNotFoundException;
import com.myhourly.jewellery.payment.dto.PaymentRequestDto;
import com.myhourly.jewellery.payment.enums.OrderPaymentStatus;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final String STAFF = "staff1";
    private static final Long ORDER_ID = 7L;
    private static final Long INVOICE_ID = 70L;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private OrderPaymentStatusUpdater orderPaymentStatusUpdater;

    @InjectMocks
    private PaymentService paymentService;

    // ---------------------------------------------------------------
    // createPayment
    // ---------------------------------------------------------------

    @Test
    void createPaymentShouldSavePendingPayment() {
        PaymentRequestDto request = validRequest();
        stubInvoice(InvoiceStatus.GENERATED, "5000.00", "0", "0");

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
    void createPaymentShouldStoreCreatedByAndInvoiceId() {
        PaymentRequestDto request = validRequest();
        stubInvoice(InvoiceStatus.GENERATED, "5000.00", "0", "0");

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = paymentService.createPayment(request, "  staff1  ");

        ArgumentCaptor<Payment> captor =
                ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());

        assertEquals("staff1", captor.getValue().getCreatedBy());
        assertEquals(INVOICE_ID, captor.getValue().getInvoiceId());
        assertEquals("staff1", response.getCreatedBy());
        assertEquals(INVOICE_ID, response.getInvoiceId());
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
        stubInvoice(InvoiceStatus.GENERATED, "5000.00", "0", "0");

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

    @Test
    void createPaymentShouldFailWhenOrderHasNoActiveInvoice() {
        when(invoiceService.getActiveByOrderId(ORDER_ID))
                .thenThrow(new ResourceNotFoundException(
                        "No active invoice found for order " + ORDER_ID));

        assertThrows(ResourceNotFoundException.class,
                () -> paymentService.createPayment(validRequest(), STAFF));

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void createPaymentShouldRejectInvoiceThatIsNotGenerated() {
        for (InvoiceStatus status : new InvoiceStatus[] {
                InvoiceStatus.PAID, InvoiceStatus.CANCELLED }) {

            stubInvoice(status, "5000.00", null, null);

            assertThrows(BusinessValidationException.class,
                    () -> paymentService.createPayment(
                            validRequest(), STAFF));
        }

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void createPaymentShouldRejectAmountAboveOutstandingBalance() {
        // 5000 total, 4000 already paid or pending, 500 of it refunded
        // -> only 1500 is still outstanding.
        stubInvoice(InvoiceStatus.GENERATED, "5000.00", "4000", "500");

        PaymentRequestDto request = validRequest();
        request.setAmount(new BigDecimal("1500.01"));

        assertThrows(BusinessValidationException.class,
                () -> paymentService.createPayment(request, STAFF));

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void createPaymentShouldAcceptAmountEqualToOutstandingBalance() {
        stubInvoice(InvoiceStatus.GENERATED, "5000.00", "4000", "500");

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = paymentService.createPayment(validRequest(), STAFF);

        assertEquals(new BigDecimal("1500.00"), response.getAmount());
        verify(paymentRepository).save(any(Payment.class));
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
    // getPaymentsForInvoice
    // ---------------------------------------------------------------

    @Test
    void getPaymentsForInvoiceShouldReturnMatchingPayments() {
        Payment first = createPayment(PaymentStatus.COMPLETED);
        first.setInvoiceId(7L);
        Payment second = createPayment(PaymentStatus.PENDING);
        second.setInvoiceId(7L);

        when(paymentRepository.findByInvoiceIdOrderByCreatedAtDesc(7L))
                .thenReturn(List.of(first, second));

        var results = paymentService.getPaymentsForInvoice(7L);

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(r -> r.getInvoiceId() == 7L));
        assertEquals(PaymentStatus.COMPLETED, results.get(0).getStatus());
    }

    @Test
    void getPaymentsForInvoiceShouldReturnEmptyListWhenNoPayments() {
        when(paymentRepository.findByInvoiceIdOrderByCreatedAtDesc(8L))
                .thenReturn(List.of());

        var results = paymentService.getPaymentsForInvoice(8L);

        assertTrue(results.isEmpty());
    }

    @Test
    void getPaymentsForInvoiceShouldRejectInvalidInvoiceId() {
        assertThrows(BusinessValidationException.class,
                () -> paymentService.getPaymentsForInvoice(null));

        assertThrows(BusinessValidationException.class,
                () -> paymentService.getPaymentsForInvoice(0L));

        assertThrows(BusinessValidationException.class,
                () -> paymentService.getPaymentsForInvoice(-1L));

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
        // 1500 received so far, invoice total 5000: stays GENERATED.
        stubLockedInvoice(InvoiceStatus.GENERATED, "5000.00", "1500", "0");

        var response = paymentService.confirmPayment(10L);

        assertEquals(PaymentStatus.COMPLETED, response.getStatus());
        assertNotNull(response.getPaymentDate());
        verify(paymentRepository).save(payment);
        verify(invoiceService, never())
                .updateStatus(any(Long.class), any(InvoiceStatus.class));
    }

    @Test
    void confirmPaymentShouldMarkInvoicePaidWhenTotalIsReached() {
        Payment payment = createPayment(PaymentStatus.PENDING);

        when(paymentRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        stubLockedInvoice(InvoiceStatus.GENERATED, "1500.00", "1500", "0");

        paymentService.confirmPayment(10L);

        verify(invoiceService).updateStatus(INVOICE_ID, InvoiceStatus.PAID);
    }

    @Test
    void confirmPaymentShouldRejectWhenInvoiceIsNoLongerGenerated() {
        Payment payment = createPayment(PaymentStatus.PENDING);

        when(paymentRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(payment));
        stubLockedInvoice(InvoiceStatus.CANCELLED, "1500.00", null, null);

        assertThrows(BusinessValidationException.class,
                () -> paymentService.confirmPayment(10L));

        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(invoiceService, never())
                .updateStatus(any(Long.class), any(InvoiceStatus.class));
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
        stubLockedInvoice(InvoiceStatus.GENERATED, "5000.00", "1500", "500");
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
        stubLockedInvoice(InvoiceStatus.GENERATED, "5000.00", "1500", "1500");
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
    void refundShouldMoveInvoiceBackToGeneratedWhenItDropsBelowTotal() {
        Payment payment = createPayment(PaymentStatus.COMPLETED);

        when(paymentRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        // Invoice is PAID (total 1500); after the refund only 1000 is left.
        stubLockedInvoice(InvoiceStatus.PAID, "1500.00", "1500", "500");

        paymentService.recordRefund(
                10L, new BigDecimal("500"), "Partial refund");

        verify(invoiceService).markUnpaid(INVOICE_ID);
    }

    @Test
    void refundShouldKeepInvoicePaidWhenTotalIsStillCovered() {
        Payment payment = createPayment(PaymentStatus.COMPLETED);

        when(paymentRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        // Invoice total is 1000 and 1000 is still received after the refund.
        stubLockedInvoice(InvoiceStatus.PAID, "1000.00", "1500", "500");

        paymentService.recordRefund(
                10L, new BigDecimal("500"), "Partial refund");

        verify(invoiceService, never()).markUnpaid(any(Long.class));
    }

    @Test
    void repeatedRefundShouldAllowRemainingRefundableAmount() {
        stubLockedInvoice(InvoiceStatus.GENERATED, "5000.00", "1500", "500");
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
        stubLockedInvoice(InvoiceStatus.GENERATED, "5000.00", "1500", "500");
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
        stubLockedInvoice(InvoiceStatus.GENERATED, "5000.00", "1500", "500");
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

    // ---------------------------------------------------------------
    // Order payment status sync
    // ---------------------------------------------------------------

    @Test
    void confirmPaymentShouldTellOrderPartiallyPaidWhenTotalNotReached() {
        Payment payment = createPayment(PaymentStatus.PENDING);

        when(paymentRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        stubLockedInvoice(InvoiceStatus.GENERATED, "5000.00", "1500", "0");

        paymentService.confirmPayment(10L);

        verify(orderPaymentStatusUpdater).updatePaymentStatus(
                ORDER_ID, OrderPaymentStatus.PARTIALLY_PAID);
    }

    @Test
    void confirmPaymentShouldTellOrderPaidWhenTotalIsReached() {
        Payment payment = createPayment(PaymentStatus.PENDING);

        when(paymentRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        stubLockedInvoice(InvoiceStatus.GENERATED, "1500.00", "1500", "0");

        paymentService.confirmPayment(10L);

        verify(orderPaymentStatusUpdater).updatePaymentStatus(
                ORDER_ID, OrderPaymentStatus.PAID);
    }

    @Test
    void partialRefundShouldTellOrderPartiallyPaid() {
        Payment payment = createPayment(PaymentStatus.COMPLETED);

        when(paymentRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        // Invoice total 1500, 1500 paid, 500 refunded: 1000 is left.
        stubLockedInvoice(InvoiceStatus.PAID, "1500.00", "1500", "500");

        paymentService.recordRefund(
                10L, new BigDecimal("500"), "Partial refund");

        verify(orderPaymentStatusUpdater).updatePaymentStatus(
                ORDER_ID, OrderPaymentStatus.PARTIALLY_PAID);
    }

    @Test
    void fullRefundShouldTellOrderRefunded() {
        Payment payment = createPayment(PaymentStatus.COMPLETED);

        when(paymentRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        // Everything that was paid has been refunded.
        stubLockedInvoice(InvoiceStatus.PAID, "1500.00", "1500", "1500");

        paymentService.recordRefund(
                10L, new BigDecimal("1500"), "Full refund");

        verify(orderPaymentStatusUpdater).updatePaymentStatus(
                ORDER_ID, OrderPaymentStatus.REFUNDED);
    }

    @Test
    void orderShouldNotBeToldWhenConfirmIsRejected() {
        Payment payment = createPayment(PaymentStatus.PENDING);

        when(paymentRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(payment));
        stubLockedInvoice(InvoiceStatus.CANCELLED, "1500.00", null, null);

        assertThrows(BusinessValidationException.class,
                () -> paymentService.confirmPayment(10L));

        verifyNoInteractions(orderPaymentStatusUpdater);
    }

    @Test
    void orderShouldNotBeToldWhenPaymentFailsOrIsCancelled() {
        Payment failing = createPayment(PaymentStatus.PENDING);
        Payment cancelling = createPayment(PaymentStatus.PENDING);

        when(paymentRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(failing));
        when(paymentRepository.findByIdForUpdate(11L))
                .thenReturn(Optional.of(cancelling));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.failPayment(10L);
        paymentService.cancelPayment(11L);

        verifyNoInteractions(orderPaymentStatusUpdater);
    }

    @Test
    void confirmPaymentShouldFailWhenOrderModuleFails() {
        Payment payment = createPayment(PaymentStatus.PENDING);

        when(paymentRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        stubLockedInvoice(InvoiceStatus.GENERATED, "5000.00", "1500", "0");
        doThrow(new ResourceNotFoundException("Order not found: " + ORDER_ID))
                .when(orderPaymentStatusUpdater)
                .updatePaymentStatus(any(Long.class),
                        any(OrderPaymentStatus.class));

        // The exception goes out of the @Transactional method, so the
        // transaction rolls back and the payment is not confirmed.
        assertThrows(ResourceNotFoundException.class,
                () -> paymentService.confirmPayment(10L));
    }

    // Invoice of order 7, found and locked by the service. The two repository
    // numbers are what the service sums up for that invoice.
    private void stubInvoice(InvoiceStatus status, String grandTotal,
                             String paid, String refunded) {
        InvoiceResponse invoice = invoice(status, grandTotal);

        when(invoiceService.getActiveByOrderId(ORDER_ID)).thenReturn(invoice);
        when(invoiceService.getByIdForUpdate(INVOICE_ID)).thenReturn(invoice);
        stubSums(paid, refunded);
    }

    // For confirm and refund: the invoice is only locked by id.
    private void stubLockedInvoice(InvoiceStatus status, String grandTotal,
                                   String paid, String refunded) {
        when(invoiceService.getByIdForUpdate(INVOICE_ID))
                .thenReturn(invoice(status, grandTotal));
        stubSums(paid, refunded);
    }

    private void stubSums(String paid, String refunded) {
        if (paid == null) {
            return;
        }
        when(paymentRepository.sumAmountsByInvoiceIdAndStatusIn(
                eq(INVOICE_ID), anyList()))
                .thenReturn(new BigDecimal(paid));
        when(paymentRepository.sumRefundedAmountsByInvoiceIdAndStatusIn(
                eq(INVOICE_ID), anyList()))
                .thenReturn(new BigDecimal(refunded));
    }

    private InvoiceResponse invoice(InvoiceStatus status, String grandTotal) {
        return new InvoiceResponse(INVOICE_ID, "INV-000070", 1L, ORDER_ID,
                status, null, List.of(), null, null, null, null,
                new BigDecimal(grandTotal));
    }

    private PaymentRequestDto validRequest() {
        PaymentRequestDto request = new PaymentRequestDto();
        request.setOrderId(ORDER_ID);
        request.setAmount(new BigDecimal("1500.00"));
        request.setPaymentMethod(PaymentMethod.UPI);
        return request;
    }

    private Payment createPayment(PaymentStatus status) {
        Payment payment = new Payment();
        payment.setInvoiceId(INVOICE_ID);
        payment.setAmount(new BigDecimal("1500"));
        payment.setStatus(status);
        payment.setRefundedAmount(BigDecimal.ZERO);
        return payment;
    }
}
package com.myhourly.jewellery.payment.repository;

import com.myhourly.jewellery.payment.entity.Payment;
import com.myhourly.jewellery.payment.enums.PaymentMethod;
import com.myhourly.jewellery.payment.enums.PaymentStatus;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class PaymentRepositoryTest {

    private static final Long TEST_INVOICE_ID = 900000001L;
    private static final Long OTHER_INVOICE_ID = 900000002L;
    private static final Long DEFAULT_INVOICE_ID = 900000099L;

    private static final List<PaymentStatus> SUCCESSFUL_STATUSES = List.of(
            PaymentStatus.COMPLETED,
            PaymentStatus.PARTIALLY_REFUNDED,
            PaymentStatus.REFUNDED);

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void findByStatusOrderByCreatedAtDescShouldReturnMatchingPayments() {
        int baseline = paymentRepository
                .findByStatusOrderByCreatedAtDesc(PaymentStatus.PENDING)
                .size();

        paymentRepository.save(
                createPayment("1000", PaymentStatus.PENDING));
        paymentRepository.save(
                createPayment("2000", PaymentStatus.COMPLETED));
        paymentRepository.flush();

        var results = paymentRepository
                .findByStatusOrderByCreatedAtDesc(PaymentStatus.PENDING);

        assertEquals(baseline + 1, results.size());
        assertTrue(results.stream()
                .allMatch(p -> p.getStatus() == PaymentStatus.PENDING));
    }

    @Test
    void findByStatusInShouldReturnMatchingPayments() {
        int baseline = paymentRepository.findByStatusIn(
                List.of(PaymentStatus.PENDING, PaymentStatus.COMPLETED))
                .size();

        paymentRepository.save(
                createPayment("1000", PaymentStatus.PENDING));
        paymentRepository.save(
                createPayment("2000", PaymentStatus.COMPLETED));
        paymentRepository.save(
                createPayment("3000", PaymentStatus.REFUNDED));
        paymentRepository.flush();

        var results = paymentRepository.findByStatusIn(
                List.of(PaymentStatus.PENDING, PaymentStatus.COMPLETED));

        assertEquals(baseline + 2, results.size());
        assertTrue(results.stream().allMatch(p ->
                p.getStatus() == PaymentStatus.PENDING
                        || p.getStatus() == PaymentStatus.COMPLETED));
    }

    @Test
    void sumAmountsByStatusShouldReturnCorrectTotal() {
        BigDecimal baseline = paymentRepository
                .sumAmountsByStatus(PaymentStatus.COMPLETED);

        paymentRepository.save(
                createPayment("1000", PaymentStatus.COMPLETED));
        paymentRepository.save(
                createPayment("2500", PaymentStatus.COMPLETED));
        paymentRepository.save(
                createPayment("500", PaymentStatus.PENDING));
        paymentRepository.flush();

        BigDecimal total = paymentRepository
                .sumAmountsByStatus(PaymentStatus.COMPLETED);

        assertEquals(
                0,
                baseline.add(new BigDecimal("3500")).compareTo(total));
    }

    @Test
    void sumRefundedAmountsByStatusShouldReturnCorrectTotal() {
        BigDecimal baseline = paymentRepository
                .sumRefundedAmountsByStatus(
                        PaymentStatus.PARTIALLY_REFUNDED);

        Payment first = createPayment(
                "1000", PaymentStatus.PARTIALLY_REFUNDED);
        first.setRefundedAmount(new BigDecimal("200"));

        Payment second = createPayment(
                "1500", PaymentStatus.REFUNDED);
        second.setRefundedAmount(new BigDecimal("1500"));

        paymentRepository.save(first);
        paymentRepository.save(second);
        paymentRepository.flush();

        BigDecimal total = paymentRepository
                .sumRefundedAmountsByStatus(
                        PaymentStatus.PARTIALLY_REFUNDED);

        assertEquals(
                0,
                baseline.add(new BigDecimal("200")).compareTo(total));
    }

    @Test
    void findByIdForUpdateShouldReturnPayment() {
        Payment saved = paymentRepository.save(
                createPayment("1200", PaymentStatus.PENDING));
        paymentRepository.flush();

        var result = paymentRepository.findByIdForUpdate(saved.getId());

        assertTrue(result.isPresent());
        assertEquals(saved.getId(), result.get().getId());
    }

    @Test
    void findByIdForUpdateShouldReturnEmptyForMissingId() {
        var result = paymentRepository.findByIdForUpdate(Long.MAX_VALUE);

        assertTrue(result.isEmpty());
    }

    // ---------------------------------------------------------------
    // Invoice-wise queries
    // ---------------------------------------------------------------

    @Test
    void findByInvoiceIdShouldReturnOnlyThatInvoicesPayments() {
        int baseline = paymentRepository
                .findByInvoiceIdOrderByCreatedAtDesc(TEST_INVOICE_ID).size();

        paymentRepository.save(createPayment(
                "1000", PaymentStatus.COMPLETED, TEST_INVOICE_ID));
        paymentRepository.save(createPayment(
                "2000", PaymentStatus.PENDING, TEST_INVOICE_ID));
        paymentRepository.save(createPayment(
                "3000", PaymentStatus.COMPLETED, OTHER_INVOICE_ID));
        paymentRepository.flush();

        var results = paymentRepository
                .findByInvoiceIdOrderByCreatedAtDesc(TEST_INVOICE_ID);

        assertEquals(baseline + 2, results.size());
        assertTrue(results.stream()
                .allMatch(p -> TEST_INVOICE_ID.equals(p.getInvoiceId())));
    }

    @Test
    void findByInvoiceIdShouldReturnEmptyForInvoiceWithoutPayments() {
        var results = paymentRepository
                .findByInvoiceIdOrderByCreatedAtDesc(Long.MAX_VALUE);

        assertTrue(results.isEmpty());
    }

    @Test
    void sumAmountsByInvoiceIdShouldCountOnlySuccessfulStatusesOfThatInvoice() {
        BigDecimal baseline = paymentRepository
                .sumAmountsByInvoiceIdAndStatusIn(
                        TEST_INVOICE_ID, SUCCESSFUL_STATUSES);

        paymentRepository.save(createPayment(
                "1000", PaymentStatus.COMPLETED, TEST_INVOICE_ID));
        paymentRepository.save(createPayment(
                "500", PaymentStatus.PARTIALLY_REFUNDED, TEST_INVOICE_ID));
        paymentRepository.save(createPayment(
                "700", PaymentStatus.PENDING, TEST_INVOICE_ID));
        paymentRepository.save(createPayment(
                "800", PaymentStatus.FAILED, TEST_INVOICE_ID));
        paymentRepository.save(createPayment(
                "9000", PaymentStatus.COMPLETED, OTHER_INVOICE_ID));
        paymentRepository.flush();

        BigDecimal total = paymentRepository
                .sumAmountsByInvoiceIdAndStatusIn(
                        TEST_INVOICE_ID, SUCCESSFUL_STATUSES);

        assertEquals(
                0,
                baseline.add(new BigDecimal("1500")).compareTo(total));
    }

    @Test
    void sumAmountsByInvoiceIdShouldReturnZeroForInvoiceWithoutPayments() {
        BigDecimal total = paymentRepository
                .sumAmountsByInvoiceIdAndStatusIn(
                        Long.MAX_VALUE, SUCCESSFUL_STATUSES);

        assertEquals(0, BigDecimal.ZERO.compareTo(total));
    }

    @Test
    void sumRefundedAmountsByInvoiceIdShouldCountOnlyThatInvoicesRefunds() {
        BigDecimal baseline = paymentRepository
                .sumRefundedAmountsByInvoiceIdAndStatusIn(
                        TEST_INVOICE_ID, SUCCESSFUL_STATUSES);

        Payment partial = createPayment(
                "1000", PaymentStatus.PARTIALLY_REFUNDED, TEST_INVOICE_ID);
        partial.setRefundedAmount(new BigDecimal("300"));

        Payment full = createPayment(
                "400", PaymentStatus.REFUNDED, TEST_INVOICE_ID);
        full.setRefundedAmount(new BigDecimal("400"));

        Payment otherInvoice = createPayment(
                "600", PaymentStatus.REFUNDED, OTHER_INVOICE_ID);
        otherInvoice.setRefundedAmount(new BigDecimal("600"));

        paymentRepository.save(partial);
        paymentRepository.save(full);
        paymentRepository.save(otherInvoice);
        paymentRepository.flush();

        BigDecimal total = paymentRepository
                .sumRefundedAmountsByInvoiceIdAndStatusIn(
                        TEST_INVOICE_ID, SUCCESSFUL_STATUSES);

        assertEquals(
                0,
                baseline.add(new BigDecimal("700")).compareTo(total));
    }

    @Test
    void existsByInvoiceIdAndStatusInShouldFindOnlyMatchingStatuses() {
        paymentRepository.save(createPayment(
                "1000", PaymentStatus.FAILED, TEST_INVOICE_ID));
        paymentRepository.flush();

        assertFalse(paymentRepository.existsByInvoiceIdAndStatusIn(
                TEST_INVOICE_ID,
                List.of(PaymentStatus.PENDING, PaymentStatus.COMPLETED)));

        paymentRepository.save(createPayment(
                "1000", PaymentStatus.COMPLETED, TEST_INVOICE_ID));
        paymentRepository.flush();

        assertTrue(paymentRepository.existsByInvoiceIdAndStatusIn(
                TEST_INVOICE_ID,
                List.of(PaymentStatus.PENDING, PaymentStatus.COMPLETED)));
    }

    @Test
    void existsByInvoiceIdAndStatusInShouldIgnoreOtherInvoices() {
        paymentRepository.save(createPayment(
                "1000", PaymentStatus.COMPLETED, OTHER_INVOICE_ID));
        paymentRepository.flush();

        assertFalse(paymentRepository.existsByInvoiceIdAndStatusIn(
                TEST_INVOICE_ID, List.of(PaymentStatus.COMPLETED)));
    }

    private Payment createPayment(String amount, PaymentStatus status) {
        Payment payment = new Payment();
        payment.setAmount(new BigDecimal(amount));
        payment.setPaymentMethod(PaymentMethod.UPI);
        payment.setStatus(status);
        payment.setRefundedAmount(BigDecimal.ZERO);
        payment.setInvoiceId(DEFAULT_INVOICE_ID);
        return payment;
    }

    private Payment createPayment(
            String amount, PaymentStatus status, Long invoiceId) {

        Payment payment = createPayment(amount, status);
        payment.setInvoiceId(invoiceId);
        return payment;
    }
}
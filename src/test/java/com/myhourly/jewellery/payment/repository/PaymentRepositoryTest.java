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
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class PaymentRepositoryTest {

    private static final Long TEST_BILL_ID = 900000001L;
    private static final Long OTHER_BILL_ID = 900000002L;

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
    // Bill-wise queries
    // ---------------------------------------------------------------

    @Test
    void findByBillIdShouldReturnOnlyThatBillsPayments() {
        int baseline = paymentRepository
                .findByBillIdOrderByCreatedAtDesc(TEST_BILL_ID).size();

        paymentRepository.save(createPayment(
                "1000", PaymentStatus.COMPLETED, TEST_BILL_ID));
        paymentRepository.save(createPayment(
                "2000", PaymentStatus.PENDING, TEST_BILL_ID));
        paymentRepository.save(createPayment(
                "3000", PaymentStatus.COMPLETED, OTHER_BILL_ID));
        paymentRepository.flush();

        var results = paymentRepository
                .findByBillIdOrderByCreatedAtDesc(TEST_BILL_ID);

        assertEquals(baseline + 2, results.size());
        assertTrue(results.stream()
                .allMatch(p -> TEST_BILL_ID.equals(p.getBillId())));
    }

    @Test
    void findByBillIdShouldReturnEmptyForBillWithoutPayments() {
        var results = paymentRepository
                .findByBillIdOrderByCreatedAtDesc(Long.MAX_VALUE);

        assertTrue(results.isEmpty());
    }

    @Test
    void sumAmountsByBillIdShouldCountOnlySuccessfulStatusesOfThatBill() {
        BigDecimal baseline = paymentRepository
                .sumAmountsByBillIdAndStatusIn(
                        TEST_BILL_ID, SUCCESSFUL_STATUSES);

        paymentRepository.save(createPayment(
                "1000", PaymentStatus.COMPLETED, TEST_BILL_ID));
        paymentRepository.save(createPayment(
                "500", PaymentStatus.PARTIALLY_REFUNDED, TEST_BILL_ID));
        paymentRepository.save(createPayment(
                "700", PaymentStatus.PENDING, TEST_BILL_ID));
        paymentRepository.save(createPayment(
                "800", PaymentStatus.FAILED, TEST_BILL_ID));
        paymentRepository.save(createPayment(
                "9000", PaymentStatus.COMPLETED, OTHER_BILL_ID));
        paymentRepository.flush();

        BigDecimal total = paymentRepository
                .sumAmountsByBillIdAndStatusIn(
                        TEST_BILL_ID, SUCCESSFUL_STATUSES);

        assertEquals(
                0,
                baseline.add(new BigDecimal("1500")).compareTo(total));
    }

    @Test
    void sumAmountsByBillIdShouldReturnZeroForBillWithoutPayments() {
        BigDecimal total = paymentRepository
                .sumAmountsByBillIdAndStatusIn(
                        Long.MAX_VALUE, SUCCESSFUL_STATUSES);

        assertEquals(0, BigDecimal.ZERO.compareTo(total));
    }

    @Test
    void sumRefundedAmountsByBillIdShouldCountOnlyThatBillsRefunds() {
        BigDecimal baseline = paymentRepository
                .sumRefundedAmountsByBillIdAndStatusIn(
                        TEST_BILL_ID, SUCCESSFUL_STATUSES);

        Payment partial = createPayment(
                "1000", PaymentStatus.PARTIALLY_REFUNDED, TEST_BILL_ID);
        partial.setRefundedAmount(new BigDecimal("300"));

        Payment full = createPayment(
                "400", PaymentStatus.REFUNDED, TEST_BILL_ID);
        full.setRefundedAmount(new BigDecimal("400"));

        Payment otherBill = createPayment(
                "600", PaymentStatus.REFUNDED, OTHER_BILL_ID);
        otherBill.setRefundedAmount(new BigDecimal("600"));

        paymentRepository.save(partial);
        paymentRepository.save(full);
        paymentRepository.save(otherBill);
        paymentRepository.flush();

        BigDecimal total = paymentRepository
                .sumRefundedAmountsByBillIdAndStatusIn(
                        TEST_BILL_ID, SUCCESSFUL_STATUSES);

        assertEquals(
                0,
                baseline.add(new BigDecimal("700")).compareTo(total));
    }

    private Payment createPayment(String amount, PaymentStatus status) {
        Payment payment = new Payment();
        payment.setAmount(new BigDecimal(amount));
        payment.setPaymentMethod(PaymentMethod.UPI);
        payment.setStatus(status);
        payment.setRefundedAmount(BigDecimal.ZERO);
        return payment;
    }

    private Payment createPayment(
            String amount, PaymentStatus status, Long billId) {

        Payment payment = createPayment(amount, status);
        payment.setBillId(billId);
        return payment;
    }
}
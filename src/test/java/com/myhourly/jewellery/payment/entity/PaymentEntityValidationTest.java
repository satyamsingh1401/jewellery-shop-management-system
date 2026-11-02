package com.myhourly.jewellery.payment.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.myhourly.jewellery.payment.enums.PaymentStatus;

import jakarta.persistence.Version;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class PaymentEntityValidationTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    private Payment createValidPayment() {
        Payment payment = new Payment();

        payment.setAmount(new BigDecimal("100.00"));
        payment.setRefundedAmount(BigDecimal.ZERO);
        payment.setCurrency("INR");

        // Initialize createdAt, updatedAt and paymentDate
        payment.onCreate();

        return payment;
    }

    @Test
    void zeroPaymentAmountShouldBeRejected() {
        Payment payment = createValidPayment();
        payment.setAmount(BigDecimal.ZERO);

        Set<ConstraintViolation<Payment>> violations =
                validator.validate(payment);

        assertTrue(violations.stream().anyMatch(
                violation -> violation.getPropertyPath()
                        .toString().equals("amount")));
    }

    @Test
    void nullPaymentAmountShouldBeRejected() {
        Payment payment = createValidPayment();
        payment.setAmount(null);

        Set<ConstraintViolation<Payment>> violations =
                validator.validate(payment);

        assertTrue(violations.stream().anyMatch(
                violation -> violation.getPropertyPath()
                        .toString().equals("amount")));
    }

    @Test
    void negativeRefundedAmountShouldBeRejected() {
        Payment payment = createValidPayment();
        payment.setRefundedAmount(new BigDecimal("-0.01"));

        Set<ConstraintViolation<Payment>> violations =
                validator.validate(payment);

        assertTrue(violations.stream().anyMatch(
                violation -> violation.getPropertyPath()
                        .toString().equals("refundedAmount")));
    }

    @Test
    void invalidCurrencyLengthShouldBeRejected() {
        Payment payment = createValidPayment();
        payment.setCurrency("USDD");

        Set<ConstraintViolation<Payment>> violations =
                validator.validate(payment);

        assertTrue(violations.stream().anyMatch(
                violation -> violation.getPropertyPath()
                        .toString().equals("currency")));
    }

    @Test
    void blankCurrencyShouldBeRejected() {
        Payment payment = createValidPayment();
        payment.setCurrency(" ");

        Set<ConstraintViolation<Payment>> violations =
                validator.validate(payment);

        assertTrue(violations.stream().anyMatch(
                violation -> violation.getPropertyPath()
                        .toString().equals("currency")));
    }

    @Test
    void pendingPaymentCanBeCompleted() {
        Payment payment = createValidPayment();

        payment.transitionTo(PaymentStatus.COMPLETED);

        assertEquals(PaymentStatus.COMPLETED, payment.getStatus());
    }

    @Test
    void completedPaymentCanBePartiallyRefunded() {
        Payment payment = createValidPayment();

        payment.transitionTo(PaymentStatus.COMPLETED);
        payment.transitionTo(PaymentStatus.PARTIALLY_REFUNDED);

        assertEquals(
                PaymentStatus.PARTIALLY_REFUNDED,
                payment.getStatus());
    }

    @Test
    void refundedPaymentCannotBeCompletedAgain() {
        Payment payment = createValidPayment();

        payment.transitionTo(PaymentStatus.COMPLETED);
        payment.transitionTo(PaymentStatus.REFUNDED);

        assertThrows(
                IllegalStateException.class,
                () -> payment.transitionTo(PaymentStatus.COMPLETED));
    }

    @Test
    void pendingPaymentCannotBeRefundedDirectly() {
        Payment payment = createValidPayment();

        assertThrows(
                IllegalStateException.class,
                () -> payment.transitionTo(PaymentStatus.REFUNDED));
    }

    @Test
    void paymentEntityHasOptimisticLockingVersion() throws Exception {
        java.lang.reflect.Field versionField =
                Payment.class.getDeclaredField("version");

        assertTrue(versionField.isAnnotationPresent(Version.class));
    }
}
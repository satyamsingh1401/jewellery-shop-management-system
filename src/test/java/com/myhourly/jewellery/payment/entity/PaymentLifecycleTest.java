package com.myhourly.jewellery.payment.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class PaymentLifecycleTest {

    @Test
    void newPaymentHasNoPaymentDateUntilItIsConfirmed() {
        Payment payment = new Payment();

        payment.onCreate();

        assertNotNull(payment.getCreatedAt());
        assertNull(payment.getPaymentDate());
    }

    @Test
    void externalReferenceIsOptional() {
        Payment payment = new Payment();

        assertNull(payment.getExternalReference());
    }
}
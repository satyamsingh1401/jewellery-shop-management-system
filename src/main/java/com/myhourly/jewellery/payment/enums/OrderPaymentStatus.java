package com.myhourly.jewellery.payment.enums;

/**
 * Payment state that Payment reports to the Order module.
 * The names match the Order module's own payment status, so the Order side
 * can convert with a simple valueOf(name()).
 */
public enum OrderPaymentStatus {
    PARTIALLY_PAID,
    PAID,
    REFUNDED
}
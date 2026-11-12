package com.myhourly.jewellery.payment.service;

import com.myhourly.jewellery.payment.enums.OrderPaymentStatus;

/**
 * Called by Payment when money for an order is confirmed or refunded.
 * The Order module implements this interface as a Spring bean and updates the
 * payment status of that order. Payment never depends on the Order module.
 */
public interface OrderPaymentStatusUpdater {

    void updatePaymentStatus(Long orderId, OrderPaymentStatus status);
}
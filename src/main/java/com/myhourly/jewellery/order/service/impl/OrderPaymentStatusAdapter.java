package com.myhourly.jewellery.order.service.impl;

import com.myhourly.jewellery.common.exception.ResourceNotFoundException;
import com.myhourly.jewellery.order.enums.PaymentStatus;
import com.myhourly.jewellery.order.repository.OrderRepository;
import com.myhourly.jewellery.payment.enums.OrderPaymentStatus;
import com.myhourly.jewellery.payment.service.OrderPaymentStatusUpdater;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderPaymentStatusAdapter implements OrderPaymentStatusUpdater {
    private final OrderRepository orderRepository;

    public OrderPaymentStatusAdapter(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void updatePaymentStatus(Long orderId, OrderPaymentStatus status) {
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        order.setPaymentStatus(PaymentStatus.valueOf(status.name()));
        orderRepository.save(order);
    }
}

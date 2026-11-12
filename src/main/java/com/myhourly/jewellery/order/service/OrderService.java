package com.myhourly.jewellery.order.service;

import java.util.List;

import com.myhourly.jewellery.order.dto.CreateOrderRequest;
import com.myhourly.jewellery.order.dto.OrderResponse;
import com.myhourly.jewellery.order.enums.OrderStatus;
import com.myhourly.jewellery.order.enums.PaymentStatus;

public interface OrderService {

    OrderResponse createOrder(CreateOrderRequest request);

    OrderResponse getOrderById(Long id);
    
    OrderResponse getOrderByOrderNumber(String orderNumber);

    List<OrderResponse> getOrdersByCustomerId(Long customerId);

    List<OrderResponse> getAllOrders();

    OrderResponse updateOrderStatus(String orderNumber, OrderStatus status);

    OrderResponse updatePaymentStatus(String orderNumber, PaymentStatus paymentStatus);
}
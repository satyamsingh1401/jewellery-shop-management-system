package com.myhourly.jewellery.order.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.myhourly.jewellery.order.dto.CreateOrderRequest;
import com.myhourly.jewellery.order.dto.OrderItemRequest;
import com.myhourly.jewellery.order.dto.OrderResponse;
import com.myhourly.jewellery.order.entity.Order;
import com.myhourly.jewellery.order.entity.OrderItem;
import com.myhourly.jewellery.order.enums.OrderStatus;
import com.myhourly.jewellery.order.enums.PaymentStatus;
import com.myhourly.jewellery.order.repository.OrderRepository;



@Service
public class OrderServiceImpl implements OrderService {

	private final OrderRepository orderRepository;

	public OrderServiceImpl(OrderRepository orderRepository) {
		this.orderRepository = orderRepository;
	}
	
	
	@Override
	@Transactional
	public OrderResponse createOrder(CreateOrderRequest request) {
		Order order = new Order();
		order.setOrderNumber(generateOrderNumber());
		order.setCustomerId(request.getCustomerId());
		order.setCreatedBy(request.getCreatedBy());
		
		BigDecimal discount = (request.getDiscountAmount() != null) ? request.getDiscountAmount(): BigDecimal.ZERO;
		order.setDiscountAmount(discount);
		
		BigDecimal subtotal = BigDecimal.ZERO;
		BigDecimal totalTax = BigDecimal.ZERO;
		
		if(request.getItems() != null) {
			for (OrderItemRequest itemReq : request.getItems()) {
                OrderItem item = new OrderItem();
                item.setProductId(itemReq.getProductId());
                item.setQuantity(itemReq.getQuantity());
                item.setNetWeight(itemReq.getNetWeight());
                item.setRatePerGramSnapshot(itemReq.getRatePerGramSnapshot());
                item.setMakingChargeSnapshot(itemReq.getMakingChargeSnapshot());
                item.setTaxSnapshot(itemReq.getTaxSnapshot());

                BigDecimal goldCost = itemReq.getNetWeight().multiply(itemReq.getRatePerGramSnapshot());
                BigDecimal itemBaseUnit = goldCost.add(itemReq.getMakingChargeSnapshot());
                BigDecimal itemBaseTotal = itemBaseUnit.multiply(BigDecimal.valueOf(itemReq.getQuantity()));

                BigDecimal itemTax = (itemReq.getTaxSnapshot() != null) 
                        ? itemReq.getTaxSnapshot() 
                        : BigDecimal.ZERO;

                BigDecimal lineTotal = itemBaseTotal.add(itemTax);
                item.setLineTotal(lineTotal);

                order.addItem(item);

                subtotal = subtotal.add(itemBaseTotal);
                totalTax = totalTax.add(itemTax);
            }
		}
		
		order.setSubtotalAmount(subtotal);
		order.setTaxAmount(totalTax);
		order.setTotalAmount(subtotal.add(totalTax).subtract(discount));
		
		Order savedOrder = orderRepository.save(order);
        return mapToResponse(savedOrder);
	}
	
	
	@Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByOrderNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Order not found with order number: " + orderNumber));
        return mapToResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCustomerId(Long customerId) {
        List<Order> orders = orderRepository.findByCustomerId(customerId);
        List<OrderResponse> responses = new ArrayList<>();
        for (Order order : orders) {
            responses.add(mapToResponse(order));
        }
        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        List<Order> orders = orderRepository.findAll();
        List<OrderResponse> responses = new ArrayList<>();
        for (Order order : orders) {
            responses.add(mapToResponse(order));
        }
        return responses;
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(String orderNumber, OrderStatus status) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Order not found with order number: " + orderNumber));
        order.setOrderStatus(status);
        return mapToResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderResponse updatePaymentStatus(String orderNumber, PaymentStatus paymentStatus) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Order not found with order number: " + orderNumber));
        order.setPaymentStatus(paymentStatus);
        return mapToResponse(orderRepository.save(order));
    }

    private String generateOrderNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String suffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "ORD-" + timestamp + "-" + suffix;
    }

    private OrderResponse mapToResponse(Order order) {
        List<OrderItemRequest> itemDtos = new ArrayList<>();
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                OrderItemRequest dto = new OrderItemRequest(
                        item.getProductId(),
                        item.getQuantity(),
                        item.getNetWeight(),
                        item.getRatePerGramSnapshot(),
                        item.getMakingChargeSnapshot(),
                        item.getTaxSnapshot(),
                        item.getLineTotal()
                );
                itemDtos.add(dto);
            }
        }

        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getCustomerId(),
                order.getOrderStatus(),
                order.getPaymentStatus(),
                order.getSubtotalAmount(),
                order.getDiscountAmount(),
                order.getTaxAmount(),
                order.getTotalAmount(),
                order.getCreatedBy(),
                order.getCreatedAt(),
                itemDtos
        );
    }
}

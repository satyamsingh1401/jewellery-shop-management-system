package com.myhourly.jewellery.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.myhourly.jewellery.order.enums.OrderStatus;
import com.myhourly.jewellery.order.enums.PaymentStatus;

public class OrderResponse {

	private Long id;
	private String orderNumber;
	private Long customerId;
	private OrderStatus orderStatus;
	private PaymentStatus paymentStatus;
	private BigDecimal subtotalAmount;
	private BigDecimal discountAmount;
	private BigDecimal taxAmount;
	private BigDecimal totalAmount;
	private String createdBy;
	private LocalDateTime createdAt;
	private List<OrderItemRequest> items = new ArrayList<>();
	
	
	public OrderResponse() {
		super();
		// TODO Auto-generated constructor stub
	}


	public OrderResponse(Long id, String orderNumber, Long customerId, OrderStatus orderStatus,
			PaymentStatus paymentStatus, BigDecimal subtotalAmount, BigDecimal discountAmount, BigDecimal taxAmount,
			BigDecimal totalAmount, String createdBy, LocalDateTime createdAt, List<OrderItemRequest> items) {
		super();
		this.id = id;
		this.orderNumber = orderNumber;
		this.customerId = customerId;
		this.orderStatus = orderStatus;
		this.paymentStatus = paymentStatus;
		this.subtotalAmount = subtotalAmount;
		this.discountAmount = discountAmount;
		this.taxAmount = taxAmount;
		this.totalAmount = totalAmount;
		this.createdBy = createdBy;
		this.createdAt = createdAt;
		this.items = items;
	}


	public Long getId() {
		return id;
	}


	public void setId(Long id) {
		this.id = id;
	}


	public String getOrderNumber() {
		return orderNumber;
	}


	public void setOrderNumber(String orderNumber) {
		this.orderNumber = orderNumber;
	}


	public Long getCustomerId() {
		return customerId;
	}


	public void setCustomerId(Long customerId) {
		this.customerId = customerId;
	}


	public OrderStatus getOrderStatus() {
		return orderStatus;
	}


	public void setOrderStatus(OrderStatus orderStatus) {
		this.orderStatus = orderStatus;
	}


	public PaymentStatus getPaymentStatus() {
		return paymentStatus;
	}


	public void setPaymentStatus(PaymentStatus paymentStatus) {
		this.paymentStatus = paymentStatus;
	}


	public BigDecimal getSubtotalAmount() {
		return subtotalAmount;
	}


	public void setSubtotalAmount(BigDecimal subtotalAmount) {
		this.subtotalAmount = subtotalAmount;
	}


	public BigDecimal getDiscountAmount() {
		return discountAmount;
	}


	public void setDiscountAmount(BigDecimal discountAmount) {
		this.discountAmount = discountAmount;
	}


	public BigDecimal getTaxAmount() {
		return taxAmount;
	}


	public void setTaxAmount(BigDecimal taxAmount) {
		this.taxAmount = taxAmount;
	}


	public BigDecimal getTotalAmount() {
		return totalAmount;
	}


	public void setTotalAmount(BigDecimal totalAmount) {
		this.totalAmount = totalAmount;
	}


	public String getCreatedBy() {
		return createdBy;
	}


	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}


	public LocalDateTime getCreatedAt() {
		return createdAt;
	}


	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}


	public List<OrderItemRequest> getItems() {
		return items;
	}


	public void setItems(List<OrderItemRequest> items) {
		this.items = items;
	}
	
	
	
	
}

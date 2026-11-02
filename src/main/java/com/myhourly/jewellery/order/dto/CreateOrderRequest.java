package com.myhourly.jewellery.order.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CreateOrderRequest {

	private Long customerId;
	private BigDecimal discountAmount;
	private String createdBy;
	private List<OrderItemRequest> items = new ArrayList<>();
	
	
	public CreateOrderRequest() {
		super();
		// TODO Auto-generated constructor stub
	}


	public CreateOrderRequest(Long customerId, BigDecimal discountAmount, String createdBy,
			List<OrderItemRequest> items) {
		super();
		this.customerId = customerId;
		this.discountAmount = discountAmount;
		this.createdBy = createdBy;
		this.items = items;
	}


	public Long getCustomerId() {
		return customerId;
	}


	public void setCustomerId(Long customerId) {
		this.customerId = customerId;
	}


	public BigDecimal getDiscountAmount() {
		return discountAmount;
	}


	public void setDiscountAmount(BigDecimal discountAmount) {
		this.discountAmount = discountAmount;
	}


	public String getCreatedBy() {
		return createdBy;
	}


	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}


	public List<OrderItemRequest> getItems() {
		return items;
	}


	public void setItems(List<OrderItemRequest> items) {
		this.items = items;
	}
	
	
	
	
}

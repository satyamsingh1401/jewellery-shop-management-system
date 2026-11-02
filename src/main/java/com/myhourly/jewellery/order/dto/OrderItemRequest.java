package com.myhourly.jewellery.order.dto;

import java.math.BigDecimal;

public class OrderItemRequest {
 
	private Long productId;
	private Integer quantity;
	private BigDecimal netWeight;
	private BigDecimal ratePerGramSnapshot;
	private BigDecimal makingChargeSnapshot;
	private BigDecimal taxSnapshot;
	private BigDecimal lineTotal;
	
	
	public OrderItemRequest() {
		
	}


	public OrderItemRequest(Long productId, Integer quantity, BigDecimal netWeight, BigDecimal ratePerGramSnapshot,
			BigDecimal makingChargeSnapshot, BigDecimal taxSnapshot, BigDecimal lineTotal) {
		super();
		this.productId = productId;
		this.quantity = quantity;
		this.netWeight = netWeight;
		this.ratePerGramSnapshot = ratePerGramSnapshot;
		this.makingChargeSnapshot = makingChargeSnapshot;
		this.taxSnapshot = taxSnapshot;
		this.lineTotal = lineTotal;
		
	}


	public Long getProductId() {
		return productId;
	}


	public void setProductId(Long productId) {
		this.productId = productId;
	}


	public Integer getQuantity() {
		return quantity;
	}


	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}


	public BigDecimal getNetWeight() {
		return netWeight;
	}


	public void setNetWeight(BigDecimal netWeight) {
		this.netWeight = netWeight;
	}


	public BigDecimal getRatePerGramSnapshot() {
		return ratePerGramSnapshot;
	}


	public void setRatePerGramSnapshot(BigDecimal ratePerGramSnapshot) {
		this.ratePerGramSnapshot = ratePerGramSnapshot;
	}


	public BigDecimal getMakingChargeSnapshot() {
		return makingChargeSnapshot;
	}


	public void setMakingChargeSnapshot(BigDecimal makingChargeSnapshot) {
		this.makingChargeSnapshot = makingChargeSnapshot;
	}


	public BigDecimal getTaxSnapshot() {
		return taxSnapshot;
	}


	public void setTaxSnapshot(BigDecimal taxSnapshot) {
		this.taxSnapshot = taxSnapshot;
	}
	
	public BigDecimal getLineTotal() {
		return lineTotal;
	}
	
	public void setLineTotal(BigDecimal lineTotal) {
		this.lineTotal = lineTotal;
	}
	
	
}

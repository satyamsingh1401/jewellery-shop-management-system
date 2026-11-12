package com.myhourly.jewellery.order.entity;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnore
    private Order order;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(precision = 10, scale = 3)
    private BigDecimal netWeight;

    @Column(precision = 14, scale = 2)
    private BigDecimal ratePerGramSnapshot;

    @Column(precision = 14, scale = 2)
    private BigDecimal makingChargeSnapshot;

    @Column(precision = 14, scale = 2)
    private BigDecimal taxSnapshot;

    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal lineTotal;

    
    public OrderItem(Long productId, Integer quantity, BigDecimal netWeight, 
            BigDecimal ratePerGramSnapshot, BigDecimal makingChargeSnapshot, 
            BigDecimal taxSnapshot, BigDecimal lineTotal) {
this.productId = productId;
this.quantity = quantity;
this.netWeight = netWeight;
this.ratePerGramSnapshot = ratePerGramSnapshot;
this.makingChargeSnapshot = makingChargeSnapshot;
this.taxSnapshot = taxSnapshot;
this.lineTotal = lineTotal;
}
    
    
    public OrderItem() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
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
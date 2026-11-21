package com.myhourly.jewellery.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "cancelled_order")
public class CancelledOrder {

    @Id
    @Column(name = "order_id", length = 64)
    private String orderId;

    @Column(name = "cancelled_at", nullable = false)
    private LocalDateTime cancelledAt = LocalDateTime.now();

    protected CancelledOrder() { }

    public CancelledOrder(String orderId) {
        this.orderId = orderId;
    }

    public String getOrderId() { return orderId; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
}

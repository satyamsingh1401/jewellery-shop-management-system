package com.myhourly.jewellery.inventory.dto;

import com.myhourly.jewellery.inventory.entity.MovementType;
import com.myhourly.jewellery.inventory.entity.StockMovement;

import java.time.LocalDateTime;

public class StockMovementResponse {

    private final Long id;
    private final Long productId;
    private final MovementType movementType;
    private final int quantityChange;
    private final int balanceAfter;
    private final String referenceType;
    private final String referenceId;
    private final String reason;
    private final String createdBy;
    private final LocalDateTime createdAt;

    public StockMovementResponse(Long id, Long productId, MovementType movementType, int quantityChange,
                                 int balanceAfter, String referenceType, String referenceId, String reason,
                                 String createdBy, LocalDateTime createdAt) {
        this.id = id;
        this.productId = productId;
        this.movementType = movementType;
        this.quantityChange = quantityChange;
        this.balanceAfter = balanceAfter;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.reason = reason;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public static StockMovementResponse from(StockMovement m) {
        return new StockMovementResponse(m.getId(), m.getProductId(), m.getMovementType(), m.getQuantityChange(),
                m.getBalanceAfter(), m.getReferenceType(), m.getReferenceId(), m.getReason(),
                m.getCreatedBy(), m.getCreatedAt());
    }

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public MovementType getMovementType() { return movementType; }
    public int getQuantityChange() { return quantityChange; }
    public int getBalanceAfter() { return balanceAfter; }
    public String getReferenceType() { return referenceType; }
    public String getReferenceId() { return referenceId; }
    public String getReason() { return reason; }
    public String getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

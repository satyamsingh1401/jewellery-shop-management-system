package com.myhourly.jewellery.inventory.dto;

import com.myhourly.jewellery.inventory.entity.Inventory;

import java.time.LocalDateTime;

public class InventoryResponse {

    private final Long productId;
    private final int availableQuantity;
    private final int reorderLevel;
    private final boolean lowStock;
    private final LocalDateTime updatedAt;

    public InventoryResponse(Long productId, int availableQuantity, int reorderLevel,
                             boolean lowStock, LocalDateTime updatedAt) {
        this.productId = productId;
        this.availableQuantity = availableQuantity;
        this.reorderLevel = reorderLevel;
        this.lowStock = lowStock;
        this.updatedAt = updatedAt;
    }

    public static InventoryResponse from(Inventory i) {
        return new InventoryResponse(i.getProductId(), i.getAvailableQuantity(), i.getReorderLevel(),
                i.getAvailableQuantity() <= i.getReorderLevel(), i.getUpdatedAt());
    }

    public Long getProductId() { return productId; }
    public int getAvailableQuantity() { return availableQuantity; }
    public int getReorderLevel() { return reorderLevel; }
    public boolean isLowStock() { return lowStock; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}

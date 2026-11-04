package com.myhourly.jewellery.reporting.model;

public record StockLevel(
        Long productId,
        String sku,
        String name,
        String category,
        int availableQuantity,
        int reorderLevel
) {

    public boolean isLowStock() {
        return availableQuantity <= reorderLevel;
    }
}

package com.myhourly.jewellery.reporting.dto;

public record StockLevelResponse(
        Long productId,
        String sku,
        String name,
        String category,
        int availableQuantity,
        int reorderLevel,
        boolean lowStock
) {
}

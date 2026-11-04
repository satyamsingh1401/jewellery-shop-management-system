package com.myhourly.jewellery.reporting.dto;

import java.util.List;

public record InventorySummaryResponse(
        int totalProducts,
        long totalUnits,
        int lowStockCount,
        List<StockLevelResponse> items
) {
}

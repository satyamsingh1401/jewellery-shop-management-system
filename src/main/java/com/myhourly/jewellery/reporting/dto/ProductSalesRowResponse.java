package com.myhourly.jewellery.reporting.dto;

import java.math.BigDecimal;

public record ProductSalesRowResponse(
        int rank,
        Long productId,
        String sku,
        String name,
        String category,
        long quantitySold,
        BigDecimal weightSold,
        BigDecimal revenue
) {
}

package com.myhourly.jewellery.reporting.model;

import java.math.BigDecimal;

public record ProductSalesFigures(
        Long productId,
        String sku,
        String name,
        String category,
        long quantitySold,
        BigDecimal weightSold,
        BigDecimal revenue
) {

    public ProductSalesFigures {
        weightSold = weightSold == null ? BigDecimal.ZERO : weightSold;
        revenue = revenue == null ? BigDecimal.ZERO : revenue;
    }
}

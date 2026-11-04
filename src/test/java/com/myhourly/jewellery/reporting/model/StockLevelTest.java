package com.myhourly.jewellery.reporting.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class StockLevelTest {

    private static StockLevel stock(int available, int reorder) {
        return new StockLevel(1L, "SKU-1", "Ring", "RING", available, reorder);
    }

    @Test
    void lowStockWhenBelowReorderLevel() {
        assertThat(stock(2, 5).isLowStock()).isTrue();
    }

    @Test
    void lowStockWhenEqualToReorderLevel() {
        assertThat(stock(5, 5).isLowStock()).isTrue();
    }

    @Test
    void notLowStockWhenAboveReorderLevel() {
        assertThat(stock(6, 5).isLowStock()).isFalse();
    }

    @Test
    void zeroStockWithZeroReorderIsLow() {
        assertThat(stock(0, 0).isLowStock()).isTrue();
    }

    @Test
    void productSalesFiguresNullWeightAndRevenueBecomeZero() {
        ProductSalesFigures figures = new ProductSalesFigures(1L, "SKU", "Ring", "RING", 3, null, null);

        assertThat(figures.weightSold()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(figures.revenue()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}

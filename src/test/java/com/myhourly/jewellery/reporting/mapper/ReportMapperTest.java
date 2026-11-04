package com.myhourly.jewellery.reporting.mapper;

import com.myhourly.jewellery.reporting.dto.DailySalesReportResponse;
import com.myhourly.jewellery.reporting.dto.ProductSalesRowResponse;
import com.myhourly.jewellery.reporting.dto.SalesSummaryResponse;
import com.myhourly.jewellery.reporting.dto.StockLevelResponse;
import com.myhourly.jewellery.reporting.model.DailySalesFigures;
import com.myhourly.jewellery.reporting.model.ProductSalesFigures;
import com.myhourly.jewellery.reporting.model.SalesFigures;
import com.myhourly.jewellery.reporting.model.StockLevel;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ReportMapperTest {

    private final ReportMapper mapper = new ReportMapper();

    @Test
    void summaryScalesMoneyToTwoDecimalsHalfUp() {
        SalesFigures figures = new SalesFigures(
                4, new BigDecimal("100.005"), new BigDecimal("1.004"), new BigDecimal("3"),
                new BigDecimal("102.0050"), 1, new BigDecimal("2.125"), 2);

        SalesSummaryResponse summary = mapper.toSummary(figures);

        assertThat(summary.orderCount()).isEqualTo(4);
        assertThat(summary.subtotalAmount()).isEqualTo(new BigDecimal("100.01"));
        assertThat(summary.discountAmount()).isEqualTo(new BigDecimal("1.00"));
        assertThat(summary.taxAmount()).isEqualTo(new BigDecimal("3.00"));
        assertThat(summary.totalAmount()).isEqualTo(new BigDecimal("102.01"));
        assertThat(summary.refundedCount()).isEqualTo(1);
        assertThat(summary.refundedAmount()).isEqualTo(new BigDecimal("2.13"));
        assertThat(summary.netAmount()).isEqualTo(new BigDecimal("99.88"));
        assertThat(summary.cancelledCount()).isEqualTo(2);
    }

    @Test
    void summaryOfZeroHasScaleTwo() {
        SalesSummaryResponse summary = mapper.toSummary(SalesFigures.ZERO);

        assertThat(summary.totalAmount()).isEqualTo(new BigDecimal("0.00"));
        assertThat(summary.subtotalAmount().scale()).isEqualTo(2);
        assertThat(summary.netAmount().scale()).isEqualTo(2);
    }

    @Test
    void dailyCarriesDateAndSummary() {
        LocalDate day = LocalDate.of(2026, 5, 1);
        SalesFigures figures = new SalesFigures(1, BigDecimal.TEN, null, null, BigDecimal.TEN, 0, null, 0);

        DailySalesReportResponse daily = mapper.toDaily(new DailySalesFigures(day, figures));

        assertThat(daily.date()).isEqualTo(day);
        assertThat(daily.summary().orderCount()).isEqualTo(1);
        assertThat(daily.summary().totalAmount()).isEqualTo(new BigDecimal("10.00"));
    }

    @Test
    void productRowScalesWeightAndRevenueAndKeepsRank() {
        ProductSalesFigures figures = new ProductSalesFigures(
                7L, "SKU-7", "Necklace", "NECKLACE", 3, new BigDecimal("12.3455"), new BigDecimal("4999.995"));

        ProductSalesRowResponse row = mapper.toProductRow(5, figures);

        assertThat(row.rank()).isEqualTo(5);
        assertThat(row.productId()).isEqualTo(7L);
        assertThat(row.sku()).isEqualTo("SKU-7");
        assertThat(row.name()).isEqualTo("Necklace");
        assertThat(row.category()).isEqualTo("NECKLACE");
        assertThat(row.quantitySold()).isEqualTo(3);
        assertThat(row.weightSold()).isEqualTo(new BigDecimal("12.346"));
        assertThat(row.revenue()).isEqualTo(new BigDecimal("5000.00"));
    }

    @Test
    void productRowPadsScale() {
        ProductSalesFigures figures = new ProductSalesFigures(1L, "S", "N", "C", 1, BigDecimal.ONE, null);

        ProductSalesRowResponse row = mapper.toProductRow(1, figures);

        assertThat(row.weightSold()).isEqualTo(new BigDecimal("1.000"));
        assertThat(row.revenue()).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void stockLevelCopiesFieldsAndLowStockFlag() {
        StockLevelResponse low = mapper.toStockLevel(new StockLevel(2L, "SKU-2", "Bangle", "BANGLE", 1, 3));
        StockLevelResponse ok = mapper.toStockLevel(new StockLevel(3L, "SKU-3", "Chain", "CHAIN", 9, 3));

        assertThat(low.productId()).isEqualTo(2L);
        assertThat(low.sku()).isEqualTo("SKU-2");
        assertThat(low.name()).isEqualTo("Bangle");
        assertThat(low.category()).isEqualTo("BANGLE");
        assertThat(low.availableQuantity()).isEqualTo(1);
        assertThat(low.reorderLevel()).isEqualTo(3);
        assertThat(low.lowStock()).isTrue();
        assertThat(ok.lowStock()).isFalse();
    }
}

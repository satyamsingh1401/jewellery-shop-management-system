package com.myhourly.jewellery.reporting.mapper;

import com.myhourly.jewellery.reporting.dto.DailySalesReportResponse;
import com.myhourly.jewellery.reporting.dto.ProductSalesRowResponse;
import com.myhourly.jewellery.reporting.dto.SalesSummaryResponse;
import com.myhourly.jewellery.reporting.dto.StockLevelResponse;
import com.myhourly.jewellery.reporting.model.DailySalesFigures;
import com.myhourly.jewellery.reporting.model.ProductSalesFigures;
import com.myhourly.jewellery.reporting.model.SalesFigures;
import com.myhourly.jewellery.reporting.model.StockLevel;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class ReportMapper {

    private static final int MONEY_SCALE = 2;
    private static final int WEIGHT_SCALE = 3;

    public SalesSummaryResponse toSummary(SalesFigures figures) {
        return new SalesSummaryResponse(
                figures.orderCount(),
                money(figures.subtotalAmount()),
                money(figures.discountAmount()),
                money(figures.taxAmount()),
                money(figures.totalAmount()),
                figures.refundedCount(),
                money(figures.refundedAmount()),
                money(figures.netAmount()),
                figures.cancelledCount()
        );
    }

    public DailySalesReportResponse toDaily(DailySalesFigures daily) {
        return new DailySalesReportResponse(daily.date(), toSummary(daily.figures()));
    }

    public ProductSalesRowResponse toProductRow(int rank, ProductSalesFigures figures) {
        return new ProductSalesRowResponse(
                rank,
                figures.productId(),
                figures.sku(),
                figures.name(),
                figures.category(),
                figures.quantitySold(),
                weight(figures.weightSold()),
                money(figures.revenue())
        );
    }

    public StockLevelResponse toStockLevel(StockLevel stock) {
        return new StockLevelResponse(
                stock.productId(),
                stock.sku(),
                stock.name(),
                stock.category(),
                stock.availableQuantity(),
                stock.reorderLevel(),
                stock.isLowStock()
        );
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal weight(BigDecimal value) {
        return value.setScale(WEIGHT_SCALE, RoundingMode.HALF_UP);
    }
}

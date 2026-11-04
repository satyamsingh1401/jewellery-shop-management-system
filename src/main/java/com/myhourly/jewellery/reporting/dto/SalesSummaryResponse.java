package com.myhourly.jewellery.reporting.dto;

import java.math.BigDecimal;

public record SalesSummaryResponse(
        long orderCount,
        BigDecimal subtotalAmount,
        BigDecimal discountAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        long refundedCount,
        BigDecimal refundedAmount,
        BigDecimal netAmount,
        long cancelledCount
) {
}

package com.myhourly.jewellery.reporting.model;

import java.math.BigDecimal;

public record SalesFigures(
        long orderCount,
        BigDecimal subtotalAmount,
        BigDecimal discountAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        long refundedCount,
        BigDecimal refundedAmount,
        long cancelledCount
) {

    public static final SalesFigures ZERO = new SalesFigures(
            0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0, BigDecimal.ZERO, 0);

    public SalesFigures {
        subtotalAmount = orZero(subtotalAmount);
        discountAmount = orZero(discountAmount);
        taxAmount = orZero(taxAmount);
        totalAmount = orZero(totalAmount);
        refundedAmount = orZero(refundedAmount);
    }

    public BigDecimal netAmount() {
        return totalAmount.subtract(refundedAmount);
    }

    public SalesFigures plus(SalesFigures other) {
        return new SalesFigures(
                orderCount + other.orderCount,
                subtotalAmount.add(other.subtotalAmount),
                discountAmount.add(other.discountAmount),
                taxAmount.add(other.taxAmount),
                totalAmount.add(other.totalAmount),
                refundedCount + other.refundedCount,
                refundedAmount.add(other.refundedAmount),
                cancelledCount + other.cancelledCount
        );
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}

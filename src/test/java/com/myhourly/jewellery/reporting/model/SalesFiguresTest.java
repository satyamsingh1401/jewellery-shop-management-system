package com.myhourly.jewellery.reporting.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SalesFiguresTest {

    @Test
    void nullAmountsBecomeZero() {
        SalesFigures figures = new SalesFigures(0, null, null, null, null, 0, null, 0);

        assertThat(figures.subtotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(figures.discountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(figures.taxAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(figures.totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(figures.refundedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(figures.netAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void zeroConstantHasNoActivity() {
        assertThat(SalesFigures.ZERO.orderCount()).isZero();
        assertThat(SalesFigures.ZERO.refundedCount()).isZero();
        assertThat(SalesFigures.ZERO.cancelledCount()).isZero();
        assertThat(SalesFigures.ZERO.totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(SalesFigures.ZERO.netAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void netAmountSubtractsRefunds() {
        SalesFigures figures = new SalesFigures(
                3, new BigDecimal("1000"), new BigDecimal("50"), new BigDecimal("28.5"),
                new BigDecimal("978.50"), 1, new BigDecimal("200.25"), 0);

        assertThat(figures.netAmount()).isEqualByComparingTo("778.25");
    }

    @Test
    void netAmountCanBeNegative() {
        SalesFigures figures = new SalesFigures(
                0, null, null, null, BigDecimal.ZERO, 1, new BigDecimal("150"), 0);

        assertThat(figures.netAmount()).isEqualByComparingTo("-150");
    }

    @Test
    void plusAddsEveryField() {
        SalesFigures a = new SalesFigures(
                2, new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("5"),
                new BigDecimal("95"), 1, new BigDecimal("20"), 1);
        SalesFigures b = new SalesFigures(
                3, new BigDecimal("200.50"), new BigDecimal("0.50"), new BigDecimal("6"),
                new BigDecimal("206"), 2, new BigDecimal("30"), 4);

        SalesFigures sum = a.plus(b);

        assertThat(sum.orderCount()).isEqualTo(5);
        assertThat(sum.subtotalAmount()).isEqualByComparingTo("300.50");
        assertThat(sum.discountAmount()).isEqualByComparingTo("10.50");
        assertThat(sum.taxAmount()).isEqualByComparingTo("11");
        assertThat(sum.totalAmount()).isEqualByComparingTo("301");
        assertThat(sum.refundedCount()).isEqualTo(3);
        assertThat(sum.refundedAmount()).isEqualByComparingTo("50");
        assertThat(sum.cancelledCount()).isEqualTo(5);
        assertThat(sum.netAmount()).isEqualByComparingTo("251");
    }

    @Test
    void plusZeroIsIdentity() {
        SalesFigures figures = new SalesFigures(
                1, new BigDecimal("10"), BigDecimal.ONE, BigDecimal.ONE, new BigDecimal("10"), 0, null, 2);

        SalesFigures sum = SalesFigures.ZERO.plus(figures);

        assertThat(sum.orderCount()).isEqualTo(1);
        assertThat(sum.subtotalAmount()).isEqualByComparingTo("10");
        assertThat(sum.totalAmount()).isEqualByComparingTo("10");
        assertThat(sum.refundedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(sum.cancelledCount()).isEqualTo(2);
    }
}

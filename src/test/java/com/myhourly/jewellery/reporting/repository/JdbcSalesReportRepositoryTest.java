package com.myhourly.jewellery.reporting.repository;

import com.myhourly.jewellery.reporting.config.ReportingConfiguration;
import com.myhourly.jewellery.reporting.model.DailySalesFigures;
import com.myhourly.jewellery.reporting.model.ProductSalesFigures;
import com.myhourly.jewellery.reporting.model.ReportPeriod;
import com.myhourly.jewellery.reporting.model.SalesFigures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("reporting-test")
@Import({JdbcSalesReportRepository.class, ReportingConfiguration.class})
@Sql({"/reporting/schema.sql", "/reporting/products-data.sql", "/reporting/sales-data.sql"})
class JdbcSalesReportRepositoryTest {

    private static final LocalDate MARCH_9 = LocalDate.of(2026, 3, 9);
    private static final LocalDate MARCH_10 = LocalDate.of(2026, 3, 10);
    private static final LocalDate MARCH_11 = LocalDate.of(2026, 3, 11);
    private static final LocalDate MARCH_12 = LocalDate.of(2026, 3, 12);
    private static final LocalDate MARCH_13 = LocalDate.of(2026, 3, 13);

    @Autowired
    private JdbcSalesReportRepository repository;

    @Test
    void dailyFiguresCountSalesRefundsAndCancellationsWithinTheDay() {
        List<DailySalesFigures> rows = repository.findDailySalesFigures(ReportPeriod.ofDay(MARCH_10));

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().date()).isEqualTo(MARCH_10);
        SalesFigures figures = rows.getFirst().figures();
        assertThat(figures.orderCount()).isEqualTo(3);
        assertThat(figures.subtotalAmount()).isEqualByComparingTo("3500.00");
        assertThat(figures.discountAmount()).isEqualByComparingTo("50.00");
        assertThat(figures.taxAmount()).isEqualByComparingTo("105.00");
        assertThat(figures.totalAmount()).isEqualByComparingTo("3555.00");
        assertThat(figures.refundedCount()).isEqualTo(1);
        assertThat(figures.refundedAmount()).isEqualByComparingTo("515.00");
        assertThat(figures.cancelledCount()).isEqualTo(2);
        assertThat(figures.netAmount()).isEqualByComparingTo("3040.00");
    }

    @Test
    void dayBoundaryIncludesMidnightAndExcludesNextMidnight() {
        SalesFigures march9 = repository.findDailySalesFigures(ReportPeriod.ofDay(MARCH_9)).getFirst().figures();
        SalesFigures march11 = repository.findDailySalesFigures(ReportPeriod.ofDay(MARCH_11)).getFirst().figures();

        assertThat(march9.orderCount()).isEqualTo(1);
        assertThat(march9.totalAmount()).isEqualByComparingTo("402.00");
        assertThat(march11.orderCount()).isEqualTo(1);
        assertThat(march11.totalAmount()).isEqualByComparingTo("309.00");
    }

    @Test
    void multiDayRangeIsGroupedPerDayInDateOrder() {
        List<DailySalesFigures> rows = repository.findDailySalesFigures(new ReportPeriod(MARCH_9, MARCH_12));

        assertThat(rows).extracting(DailySalesFigures::date).containsExactly(MARCH_9, MARCH_10, MARCH_11, MARCH_12);
        assertThat(rows).extracting(row -> row.figures().orderCount()).containsExactly(1L, 3L, 1L, 0L);
        assertThat(rows).extracting(row -> row.figures().cancelledCount()).containsExactly(0L, 2L, 0L, 1L);
    }

    @Test
    void cancelledOnlyDayHasZeroSalesAmounts() {
        SalesFigures figures = repository.findDailySalesFigures(ReportPeriod.ofDay(MARCH_12)).getFirst().figures();

        assertThat(figures.orderCount()).isZero();
        assertThat(figures.totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(figures.refundedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(figures.cancelledCount()).isEqualTo(1);
    }

    @Test
    void pendingOnlyDayProducesNoRow() {
        assertThat(repository.findDailySalesFigures(ReportPeriod.ofDay(MARCH_13))).isEmpty();
    }

    @Test
    void dayWithoutOrdersProducesNoRow() {
        assertThat(repository.findDailySalesFigures(ReportPeriod.ofDay(LocalDate.of(2026, 4, 1)))).isEmpty();
    }

    @Test
    void topProductsExcludeRefundedAndNonSaleOrdersAndBreakTiesByProductId() {
        List<ProductSalesFigures> rows = repository.findTopProductSales(ReportPeriod.ofDay(MARCH_10), 10);

        assertThat(rows).extracting(ProductSalesFigures::productId).containsExactly(101L, 999L, 102L, 103L);
        assertThat(rows).extracting(ProductSalesFigures::revenue)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("1000"), new BigDecimal("1000"), new BigDecimal("500"), new BigDecimal("500"));

        ProductSalesFigures ring = rows.getFirst();
        assertThat(ring.sku()).isEqualTo("R-101");
        assertThat(ring.name()).isEqualTo("Gold Ring");
        assertThat(ring.category()).isEqualTo("RING");
        assertThat(ring.quantitySold()).isEqualTo(3);
        assertThat(ring.weightSold()).isEqualByComparingTo("10.500");
        assertThat(rows.get(2).weightSold()).isEqualByComparingTo("10.000");
        assertThat(rows.get(3).weightSold()).isEqualByComparingTo("5.000");
    }

    @Test
    void productMissingFromCatalogueStillAppearsWithNullDetails() {
        ProductSalesFigures missing = repository.findTopProductSales(ReportPeriod.ofDay(MARCH_10), 10).get(1);

        assertThat(missing.productId()).isEqualTo(999L);
        assertThat(missing.sku()).isNull();
        assertThat(missing.name()).isNull();
        assertThat(missing.category()).isNull();
        assertThat(missing.quantitySold()).isEqualTo(1);
        assertThat(missing.weightSold()).isEqualByComparingTo("2.000");
    }

    @Test
    void topProductsRespectLimit() {
        assertThat(repository.findTopProductSales(ReportPeriod.ofDay(MARCH_10), 3))
                .extracting(ProductSalesFigures::productId).containsExactly(101L, 999L, 102L);
        assertThat(repository.findTopProductSales(ReportPeriod.ofDay(MARCH_10), 1))
                .extracting(ProductSalesFigures::productId).containsExactly(101L);
    }

    @Test
    void topProductsAcrossRangeHonourDayBoundaries() {
        List<ProductSalesFigures> rows = repository.findTopProductSales(new ReportPeriod(MARCH_9, MARCH_11), 10);

        assertThat(rows).extracting(ProductSalesFigures::productId).containsExactly(104L, 101L, 999L, 102L, 103L);
        assertThat(rows.getFirst().revenue()).isEqualByComparingTo("12221.00");
        assertThat(rows.getFirst().quantitySold()).isEqualTo(2);
    }

    @Test
    void topProductsForEmptyPeriodAreEmpty() {
        assertThat(repository.findTopProductSales(ReportPeriod.ofDay(LocalDate.of(2026, 4, 1)), 10)).isEmpty();
    }
}

package com.myhourly.jewellery.reporting.service;

import com.myhourly.jewellery.common.exception.BusinessValidationException;
import com.myhourly.jewellery.reporting.config.ReportingClock;
import com.myhourly.jewellery.reporting.config.ReportingProperties;
import com.myhourly.jewellery.reporting.dto.ProductSalesReportResponse;
import com.myhourly.jewellery.reporting.dto.ProductSalesRowResponse;
import com.myhourly.jewellery.reporting.mapper.ReportMapper;
import com.myhourly.jewellery.reporting.model.ProductSalesFigures;
import com.myhourly.jewellery.reporting.model.ReportPeriod;
import com.myhourly.jewellery.reporting.repository.SalesReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductSalesReportServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 30);
    private static final LocalDate FROM = TODAY.minusDays(29);

    @Mock
    private SalesReportRepository repository;

    @Mock
    private ReportingClock clock;

    private ProductSalesReportService service;

    @BeforeEach
    void setUp() {
        lenient().when(clock.today()).thenReturn(TODAY);
        ReportingProperties properties = new ReportingProperties(
                List.of("CONFIRMED", "COMPLETED"), "CANCELLED", "REFUNDED", 366, 10, 100, ZoneId.of("Asia/Kolkata"));
        service = new ProductSalesReportService(
                repository, new ReportRequestValidator(properties, clock), new ReportMapper());
    }

    private static ProductSalesFigures product(long id, long quantity, String revenue) {
        return new ProductSalesFigures(id, "SKU-" + id, "Product " + id, "RING", quantity,
                new BigDecimal("1.5"), new BigDecimal(revenue));
    }

    @Test
    void ranksRowsInRepositoryOrder() {
        when(repository.findTopProductSales(new ReportPeriod(FROM, TODAY), 3)).thenReturn(List.of(
                product(9, 5, "9000"), product(4, 8, "5000.5"), product(7, 1, "100")));

        ProductSalesReportResponse response = service.topProducts(FROM, TODAY, 3);

        assertThat(response.products()).extracting(ProductSalesRowResponse::rank).containsExactly(1, 2, 3);
        assertThat(response.products()).extracting(ProductSalesRowResponse::productId).containsExactly(9L, 4L, 7L);
        assertThat(response.products().get(1).revenue()).isEqualTo(new BigDecimal("5000.50"));
        assertThat(response.products().get(1).weightSold()).isEqualTo(new BigDecimal("1.500"));
        assertThat(response.products().get(1).sku()).isEqualTo("SKU-4");
        assertThat(response.products().get(1).quantitySold()).isEqualTo(8);
    }

    @Test
    void returnsPeriodAndResolvedLimit() {
        when(repository.findTopProductSales(any(), anyInt())).thenReturn(List.of());

        ProductSalesReportResponse response = service.topProducts(FROM, TODAY, 25);

        assertThat(response.from()).isEqualTo(FROM);
        assertThat(response.to()).isEqualTo(TODAY);
        assertThat(response.limit()).isEqualTo(25);
        assertThat(response.products()).isEmpty();
        verify(repository).findTopProductSales(new ReportPeriod(FROM, TODAY), 25);
    }

    @Test
    void usesDefaultLimitWhenNull() {
        when(repository.findTopProductSales(any(), eq(10))).thenReturn(List.of(product(1, 2, "10")));

        ProductSalesReportResponse response = service.topProducts(FROM, TODAY, null);

        assertThat(response.limit()).isEqualTo(10);
        assertThat(response.products()).hasSize(1);
        verify(repository).findTopProductSales(new ReportPeriod(FROM, TODAY), 10);
    }

    @Test
    void rejectsLimitAboveMaxWithoutQuerying() {
        assertThatThrownBy(() -> service.topProducts(FROM, TODAY, 101))
                .isInstanceOf(BusinessValidationException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsInvalidRangeWithoutQuerying() {
        assertThatThrownBy(() -> service.topProducts(TODAY, FROM, 5))
                .isInstanceOf(BusinessValidationException.class);
        verifyNoInteractions(repository);
    }
}

package com.myhourly.jewellery.reporting.service;

import com.myhourly.jewellery.common.exception.BusinessValidationException;
import com.myhourly.jewellery.reporting.config.ReportingClock;
import com.myhourly.jewellery.reporting.config.ReportingProperties;
import com.myhourly.jewellery.reporting.dto.DailySalesReportResponse;
import com.myhourly.jewellery.reporting.dto.SalesRangeReportResponse;
import com.myhourly.jewellery.reporting.mapper.ReportMapper;
import com.myhourly.jewellery.reporting.model.DailySalesFigures;
import com.myhourly.jewellery.reporting.model.ReportPeriod;
import com.myhourly.jewellery.reporting.model.SalesFigures;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalesReportServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 30);

    @Mock
    private SalesReportRepository repository;

    @Mock
    private ReportingClock clock;

    private SalesReportService service;

    @BeforeEach
    void setUp() {
        lenient().when(clock.today()).thenReturn(TODAY);
        ReportingProperties properties = new ReportingProperties(
                List.of("CONFIRMED", "COMPLETED"), "CANCELLED", "REFUNDED", 366, 10, 100, ZoneId.of("Asia/Kolkata"));
        service = new SalesReportService(repository, new ReportRequestValidator(properties, clock), new ReportMapper());
    }

    private static SalesFigures figures(long orders, String total, long refunds, String refunded, long cancelled) {
        BigDecimal amount = new BigDecimal(total);
        return new SalesFigures(orders, amount, BigDecimal.ZERO, BigDecimal.ZERO, amount,
                refunds, new BigDecimal(refunded), cancelled);
    }

    @Test
    void dailySalesReturnsFiguresForTheDay() {
        LocalDate day = TODAY.minusDays(1);
        when(repository.findDailySalesFigures(ReportPeriod.ofDay(day)))
                .thenReturn(List.of(new DailySalesFigures(day, figures(3, "1500.5", 1, "100", 2))));

        DailySalesReportResponse response = service.dailySales(day);

        assertThat(response.date()).isEqualTo(day);
        assertThat(response.summary().orderCount()).isEqualTo(3);
        assertThat(response.summary().totalAmount()).isEqualTo(new BigDecimal("1500.50"));
        assertThat(response.summary().netAmount()).isEqualTo(new BigDecimal("1400.50"));
        assertThat(response.summary().cancelledCount()).isEqualTo(2);
        verify(repository).findDailySalesFigures(ReportPeriod.ofDay(day));
    }

    @Test
    void dailySalesZeroFillsWhenNoRows() {
        when(repository.findDailySalesFigures(any())).thenReturn(List.of());

        DailySalesReportResponse response = service.dailySales(TODAY);

        assertThat(response.date()).isEqualTo(TODAY);
        assertThat(response.summary().orderCount()).isZero();
        assertThat(response.summary().totalAmount()).isEqualTo(new BigDecimal("0.00"));
        assertThat(response.summary().netAmount()).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void dailySalesRejectsFutureDateWithoutQuerying() {
        assertThatThrownBy(() -> service.dailySales(TODAY.plusDays(1)))
                .isInstanceOf(BusinessValidationException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void salesRangeZeroFillsMissingDays() {
        LocalDate from = TODAY.minusDays(3);
        when(repository.findDailySalesFigures(new ReportPeriod(from, TODAY))).thenReturn(List.of(
                new DailySalesFigures(from.plusDays(1), figures(2, "200", 0, "0", 0)),
                new DailySalesFigures(TODAY, figures(1, "50", 0, "0", 1))));

        SalesRangeReportResponse response = service.salesRange(from, TODAY);

        assertThat(response.from()).isEqualTo(from);
        assertThat(response.to()).isEqualTo(TODAY);
        assertThat(response.days()).extracting(DailySalesReportResponse::date)
                .containsExactly(from, from.plusDays(1), from.plusDays(2), TODAY);
        assertThat(response.days()).extracting(day -> day.summary().orderCount())
                .containsExactly(0L, 2L, 0L, 1L);
        assertThat(response.days().get(0).summary().totalAmount()).isEqualTo(new BigDecimal("0.00"));
        assertThat(response.days().get(2).summary().totalAmount()).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void salesRangeMergesDuplicateDayRows() {
        LocalDate day = TODAY.minusDays(1);
        when(repository.findDailySalesFigures(any())).thenReturn(List.of(
                new DailySalesFigures(day, figures(2, "100.10", 0, "0", 0)),
                new DailySalesFigures(day, figures(1, "50.20", 1, "20", 1))));

        SalesRangeReportResponse response = service.salesRange(day, day);

        assertThat(response.days()).hasSize(1);
        DailySalesReportResponse merged = response.days().getFirst();
        assertThat(merged.summary().orderCount()).isEqualTo(3);
        assertThat(merged.summary().totalAmount()).isEqualByComparingTo("150.30");
        assertThat(merged.summary().refundedCount()).isEqualTo(1);
        assertThat(merged.summary().refundedAmount()).isEqualByComparingTo("20");
        assertThat(merged.summary().netAmount()).isEqualByComparingTo("130.30");
        assertThat(merged.summary().cancelledCount()).isEqualTo(1);
    }

    @Test
    void salesRangeTotalsAreSumOfDays() {
        LocalDate from = TODAY.minusDays(2);
        when(repository.findDailySalesFigures(any())).thenReturn(List.of(
                new DailySalesFigures(from, figures(2, "1000", 1, "250", 0)),
                new DailySalesFigures(from.plusDays(1), figures(4, "2000.005", 0, "0", 3)),
                new DailySalesFigures(TODAY, figures(1, "10", 1, "10", 1))));

        SalesRangeReportResponse response = service.salesRange(from, TODAY);

        assertThat(response.totals().orderCount()).isEqualTo(7);
        assertThat(response.totals().subtotalAmount()).isEqualTo(new BigDecimal("3010.01"));
        assertThat(response.totals().totalAmount()).isEqualTo(new BigDecimal("3010.01"));
        assertThat(response.totals().refundedCount()).isEqualTo(2);
        assertThat(response.totals().refundedAmount()).isEqualTo(new BigDecimal("260.00"));
        assertThat(response.totals().netAmount()).isEqualTo(new BigDecimal("2750.01"));
        assertThat(response.totals().cancelledCount()).isEqualTo(4);
    }

    @Test
    void salesRangeWithNoRowsHasZeroTotals() {
        when(repository.findDailySalesFigures(any())).thenReturn(List.of());

        SalesRangeReportResponse response = service.salesRange(TODAY.minusDays(1), TODAY);

        assertThat(response.days()).hasSize(2);
        assertThat(response.totals().orderCount()).isZero();
        assertThat(response.totals().totalAmount()).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void salesRangeRejectsInvalidRangeWithoutQuerying() {
        assertThatThrownBy(() -> service.salesRange(TODAY, TODAY.minusDays(1)))
                .isInstanceOf(BusinessValidationException.class);
        verifyNoInteractions(repository);
    }
}

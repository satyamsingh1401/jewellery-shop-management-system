package com.myhourly.jewellery.reporting.controller;

import com.myhourly.jewellery.common.exception.BusinessValidationException;
import com.myhourly.jewellery.reporting.dto.DailySalesReportResponse;
import com.myhourly.jewellery.reporting.dto.ProductSalesReportResponse;
import com.myhourly.jewellery.reporting.dto.ProductSalesRowResponse;
import com.myhourly.jewellery.reporting.dto.SalesRangeReportResponse;
import com.myhourly.jewellery.reporting.dto.SalesSummaryResponse;
import com.myhourly.jewellery.reporting.service.ProductSalesReportService;
import com.myhourly.jewellery.reporting.service.SalesReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SalesReportController.class)
@WithMockUser
class SalesReportControllerTest {

    private static final LocalDate FROM = LocalDate.of(2026, 10, 1);
    private static final LocalDate TO = LocalDate.of(2026, 10, 2);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SalesReportService salesReportService;

    @MockitoBean
    private ProductSalesReportService productSalesReportService;

    private static SalesSummaryResponse summary(long orders, String total) {
        return new SalesSummaryResponse(orders, new BigDecimal(total), new BigDecimal("0.00"),
                new BigDecimal("0.00"), new BigDecimal(total), 0, new BigDecimal("0.00"),
                new BigDecimal(total), 1);
    }

    @Test
    void dailySalesReturnsReport() throws Exception {
        when(salesReportService.dailySales(FROM))
                .thenReturn(new DailySalesReportResponse(FROM, summary(3, "1500.50")));

        mockMvc.perform(get("/api/reports/daily-sales").param("date", "2026-10-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-10-01"))
                .andExpect(jsonPath("$.summary.orderCount").value(3))
                .andExpect(jsonPath("$.summary.totalAmount").value(1500.50))
                .andExpect(jsonPath("$.summary.netAmount").value(1500.50))
                .andExpect(jsonPath("$.summary.cancelledCount").value(1));

        verify(salesReportService).dailySales(FROM);
    }

    @Test
    void dailySalesWithoutDateReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/reports/daily-sales"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Required parameter 'date' is missing"))
                .andExpect(jsonPath("$.path").value("/api/reports/daily-sales"))
                .andExpect(jsonPath("$.fieldErrors.date").value("Required parameter 'date' is missing"))
                .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(salesReportService);
    }

    @Test
    void dailySalesWithMalformedDateReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/reports/daily-sales").param("date", "2026-13-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Parameter 'date' must be a valid date in yyyy-MM-dd format"))
                .andExpect(jsonPath("$.path").value("/api/reports/daily-sales"))
                .andExpect(jsonPath("$.fieldErrors.date").exists());

        verifyNoInteractions(salesReportService);
    }

    @Test
    void dailySalesBusinessValidationFailureReturnsBadRequest() throws Exception {
        when(salesReportService.dailySales(any()))
                .thenThrow(new BusinessValidationException("'date' must not be in the future"));

        mockMvc.perform(get("/api/reports/daily-sales").param("date", "2099-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("'date' must not be in the future"))
                .andExpect(jsonPath("$.path").value("/api/reports/daily-sales"));
    }

    @Test
    void salesRangeReturnsReport() throws Exception {
        SalesRangeReportResponse response = new SalesRangeReportResponse(FROM, TO, summary(5, "2500.00"),
                List.of(new DailySalesReportResponse(FROM, summary(3, "1500.00")),
                        new DailySalesReportResponse(TO, summary(2, "1000.00"))));
        when(salesReportService.salesRange(FROM, TO)).thenReturn(response);

        mockMvc.perform(get("/api/reports/sales").param("from", "2026-10-01").param("to", "2026-10-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2026-10-01"))
                .andExpect(jsonPath("$.to").value("2026-10-02"))
                .andExpect(jsonPath("$.totals.orderCount").value(5))
                .andExpect(jsonPath("$.totals.totalAmount").value(2500.00))
                .andExpect(jsonPath("$.days.length()").value(2))
                .andExpect(jsonPath("$.days[1].date").value("2026-10-02"))
                .andExpect(jsonPath("$.days[1].summary.orderCount").value(2));

        verify(salesReportService).salesRange(FROM, TO);
    }

    @Test
    void salesRangeWithoutToReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/reports/sales").param("from", "2026-10-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Required parameter 'to' is missing"))
                .andExpect(jsonPath("$.path").value("/api/reports/sales"))
                .andExpect(jsonPath("$.fieldErrors.to").exists());

        verifyNoInteractions(salesReportService);
    }

    @Test
    void salesRangeWithMalformedFromReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/reports/sales").param("from", "2026-13-01").param("to", "2026-10-02"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parameter 'from' must be a valid date in yyyy-MM-dd format"))
                .andExpect(jsonPath("$.fieldErrors.from").exists());

        verifyNoInteractions(salesReportService);
    }

    @Test
    void salesRangeBusinessValidationFailureReturnsBadRequest() throws Exception {
        when(salesReportService.salesRange(any(), any()))
                .thenThrow(new BusinessValidationException("'from' must be on or before 'to'"));

        mockMvc.perform(get("/api/reports/sales").param("from", "2026-10-02").param("to", "2026-10-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("'from' must be on or before 'to'"))
                .andExpect(jsonPath("$.path").value("/api/reports/sales"));
    }

    @Test
    void productSalesReturnsReportWithLimit() throws Exception {
        ProductSalesRowResponse row = new ProductSalesRowResponse(1, 42L, "RING-001", "Gold Ring", "RING",
                4, new BigDecimal("12.500"), new BigDecimal("80000.00"));
        when(productSalesReportService.topProducts(FROM, TO, 5))
                .thenReturn(new ProductSalesReportResponse(FROM, TO, 5, List.of(row)));

        mockMvc.perform(get("/api/reports/product-sales")
                        .param("from", "2026-10-01").param("to", "2026-10-02").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2026-10-01"))
                .andExpect(jsonPath("$.limit").value(5))
                .andExpect(jsonPath("$.products.length()").value(1))
                .andExpect(jsonPath("$.products[0].rank").value(1))
                .andExpect(jsonPath("$.products[0].productId").value(42))
                .andExpect(jsonPath("$.products[0].sku").value("RING-001"))
                .andExpect(jsonPath("$.products[0].quantitySold").value(4))
                .andExpect(jsonPath("$.products[0].revenue").value(80000.00));

        verify(productSalesReportService).topProducts(FROM, TO, 5);
    }

    @Test
    void productSalesPassesNullLimitWhenOmitted() throws Exception {
        when(productSalesReportService.topProducts(FROM, TO, null))
                .thenReturn(new ProductSalesReportResponse(FROM, TO, 10, List.of()));

        mockMvc.perform(get("/api/reports/product-sales").param("from", "2026-10-01").param("to", "2026-10-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limit").value(10))
                .andExpect(jsonPath("$.products.length()").value(0));

        verify(productSalesReportService).topProducts(any(), any(), isNull());
    }

    @Test
    void productSalesWithoutFromReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/reports/product-sales").param("to", "2026-10-02"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Required parameter 'from' is missing"))
                .andExpect(jsonPath("$.path").value("/api/reports/product-sales"))
                .andExpect(jsonPath("$.fieldErrors.from").exists());

        verifyNoInteractions(productSalesReportService);
    }

    @Test
    void productSalesWithNonNumericLimitReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/reports/product-sales")
                        .param("from", "2026-10-01").param("to", "2026-10-02").param("limit", "ten"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parameter 'limit' must be a whole number"))
                .andExpect(jsonPath("$.fieldErrors.limit").exists());

        verifyNoInteractions(productSalesReportService);
    }

    @Test
    void productSalesBusinessValidationFailureReturnsBadRequest() throws Exception {
        when(productSalesReportService.topProducts(any(), any(), any()))
                .thenThrow(new BusinessValidationException("'limit' must be between 1 and 100"));

        mockMvc.perform(get("/api/reports/product-sales")
                        .param("from", "2026-10-01").param("to", "2026-10-02").param("limit", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("'limit' must be between 1 and 100"))
                .andExpect(jsonPath("$.path").value("/api/reports/product-sales"));
    }
}

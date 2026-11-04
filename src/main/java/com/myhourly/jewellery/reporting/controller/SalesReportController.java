package com.myhourly.jewellery.reporting.controller;

import com.myhourly.jewellery.reporting.dto.DailySalesReportResponse;
import com.myhourly.jewellery.reporting.dto.ProductSalesReportResponse;
import com.myhourly.jewellery.reporting.dto.SalesRangeReportResponse;
import com.myhourly.jewellery.reporting.service.ProductSalesReportService;
import com.myhourly.jewellery.reporting.service.SalesReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
public class SalesReportController {

    private final SalesReportService salesReportService;
    private final ProductSalesReportService productSalesReportService;

    public SalesReportController(SalesReportService salesReportService,
                                 ProductSalesReportService productSalesReportService) {
        this.salesReportService = salesReportService;
        this.productSalesReportService = productSalesReportService;
    }

    @GetMapping("/daily-sales")
    public ResponseEntity<DailySalesReportResponse> dailySales(
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(salesReportService.dailySales(date));
    }

    @GetMapping("/sales")
    public ResponseEntity<SalesRangeReportResponse> salesRange(
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(salesReportService.salesRange(from, to));
    }

    @GetMapping("/product-sales")
    public ResponseEntity<ProductSalesReportResponse> productSales(
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate to,
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(productSalesReportService.topProducts(from, to, limit));
    }
}

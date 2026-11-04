package com.myhourly.jewellery.reporting.controller;

import com.myhourly.jewellery.reporting.dto.InventorySummaryResponse;
import com.myhourly.jewellery.reporting.service.InventoryReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class InventoryReportController {

    private final InventoryReportService inventoryReportService;

    public InventoryReportController(InventoryReportService inventoryReportService) {
        this.inventoryReportService = inventoryReportService;
    }

    @GetMapping("/inventory")
    public ResponseEntity<InventorySummaryResponse> inventory(
            @RequestParam(defaultValue = "false") boolean lowStockOnly) {
        return ResponseEntity.ok(inventoryReportService.summary(lowStockOnly));
    }
}

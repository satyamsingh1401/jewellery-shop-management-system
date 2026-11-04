package com.myhourly.jewellery.reporting.service;

import com.myhourly.jewellery.reporting.dto.InventorySummaryResponse;
import com.myhourly.jewellery.reporting.dto.StockLevelResponse;
import com.myhourly.jewellery.reporting.mapper.ReportMapper;
import com.myhourly.jewellery.reporting.model.StockLevel;
import com.myhourly.jewellery.reporting.repository.InventoryReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class InventoryReportService {

    private final InventoryReportRepository repository;
    private final ReportMapper mapper;

    public InventoryReportService(InventoryReportRepository repository, ReportMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public InventorySummaryResponse summary(boolean lowStockOnly) {
        List<StockLevel> all = repository.findAllStockLevels();
        long totalUnits = all.stream().mapToLong(StockLevel::availableQuantity).sum();
        int lowStockCount = (int) all.stream().filter(StockLevel::isLowStock).count();
        List<StockLevelResponse> items = all.stream()
                .filter(stock -> !lowStockOnly || stock.isLowStock())
                .map(mapper::toStockLevel)
                .toList();
        return new InventorySummaryResponse(all.size(), totalUnits, lowStockCount, items);
    }
}

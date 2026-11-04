package com.myhourly.jewellery.reporting.repository;

import com.myhourly.jewellery.reporting.model.StockLevel;

import java.util.List;

public interface InventoryReportRepository {

    List<StockLevel> findAllStockLevels();
}

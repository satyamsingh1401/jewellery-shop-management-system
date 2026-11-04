package com.myhourly.jewellery.reporting.repository;

import com.myhourly.jewellery.reporting.model.DailySalesFigures;
import com.myhourly.jewellery.reporting.model.ProductSalesFigures;
import com.myhourly.jewellery.reporting.model.ReportPeriod;

import java.util.List;

public interface SalesReportRepository {

    List<DailySalesFigures> findDailySalesFigures(ReportPeriod period);

    List<ProductSalesFigures> findTopProductSales(ReportPeriod period, int limit);
}

package com.myhourly.jewellery.reporting.dto;

import java.time.LocalDate;
import java.util.List;

public record SalesRangeReportResponse(
        LocalDate from,
        LocalDate to,
        SalesSummaryResponse totals,
        List<DailySalesReportResponse> days
) {
}

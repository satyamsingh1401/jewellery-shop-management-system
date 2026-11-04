package com.myhourly.jewellery.reporting.dto;

import java.time.LocalDate;

public record DailySalesReportResponse(LocalDate date, SalesSummaryResponse summary) {
}

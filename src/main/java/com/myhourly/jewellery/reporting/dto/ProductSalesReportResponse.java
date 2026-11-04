package com.myhourly.jewellery.reporting.dto;

import java.time.LocalDate;
import java.util.List;

public record ProductSalesReportResponse(
        LocalDate from,
        LocalDate to,
        int limit,
        List<ProductSalesRowResponse> products
) {
}

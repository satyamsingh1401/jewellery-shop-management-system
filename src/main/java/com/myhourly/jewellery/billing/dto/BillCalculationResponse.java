package com.myhourly.jewellery.billing.dto;

import java.math.BigDecimal;
import java.util.List;

public record BillCalculationResponse(
        List<BillItemResponse> items,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal taxableAmount,
        BigDecimal tax,
        BigDecimal grandTotal) {
}
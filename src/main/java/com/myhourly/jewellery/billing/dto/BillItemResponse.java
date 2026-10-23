package com.myhourly.jewellery.billing.dto;

import java.math.BigDecimal;

public record BillItemResponse(
        Long productId,
        BigDecimal metalValue,
        BigDecimal makingCharge,
        BigDecimal lineTotal) {
}
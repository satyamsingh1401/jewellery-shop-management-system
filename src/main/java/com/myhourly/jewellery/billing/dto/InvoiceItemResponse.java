package com.myhourly.jewellery.billing.dto;

import java.math.BigDecimal;

public record InvoiceItemResponse(
        Long productId,
        BigDecimal netWeight,
        BigDecimal ratePerGram,
        BigDecimal metalValue,
        BigDecimal makingCharge,
        BigDecimal lineTotal) {
}

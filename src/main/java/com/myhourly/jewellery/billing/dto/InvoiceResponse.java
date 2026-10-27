package com.myhourly.jewellery.billing.dto;

import com.myhourly.jewellery.billing.enums.InvoiceStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record InvoiceResponse(
        Long id,
        String invoiceNumber,
        Long customerId,
        Long orderId,
        InvoiceStatus status,
        LocalDateTime createdAt,
        List<InvoiceItemResponse> items,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal taxableAmount,
        BigDecimal tax,
        BigDecimal grandTotal) {
}
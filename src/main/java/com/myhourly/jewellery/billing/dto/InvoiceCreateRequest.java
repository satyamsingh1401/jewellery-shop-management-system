package com.myhourly.jewellery.billing.dto;

import com.myhourly.jewellery.billing.enums.DiscountType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.List;

public record InvoiceCreateRequest(
        @NotNull Long customerId,
        Long orderId,
        @NotEmpty @Valid List<BillItemRequest> items,
        DiscountType discountType,
        @PositiveOrZero BigDecimal discountValue) {
}
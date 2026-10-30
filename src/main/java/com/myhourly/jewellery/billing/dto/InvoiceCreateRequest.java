package com.myhourly.jewellery.billing.dto;

import com.myhourly.jewellery.billing.enums.DiscountType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.List;

public record InvoiceCreateRequest(
        @NotNull(message = "customerId is required") Long customerId,
        Long orderId,
        @NotEmpty(message = "at least one item is required") @Valid List<BillItemRequest> items,
        DiscountType discountType,
        @PositiveOrZero(message = "discountValue must not be negative")
        @Digits(integer = 12, fraction = 2, message = "discountValue allows up to 12 digits and 2 decimal places")
        BigDecimal discountValue) {
}
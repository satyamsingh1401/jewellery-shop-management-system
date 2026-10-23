package com.myhourly.jewellery.billing.dto;

import com.myhourly.jewellery.billing.enums.MakingChargeType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record BillItemRequest(
        Long productId,
        @NotNull @Positive BigDecimal netWeight,
        @NotNull @Positive BigDecimal ratePerGram,
        @NotNull MakingChargeType makingChargeType,
        @NotNull @PositiveOrZero BigDecimal makingChargeValue) {
}
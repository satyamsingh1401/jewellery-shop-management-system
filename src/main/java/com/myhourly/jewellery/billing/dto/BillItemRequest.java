package com.myhourly.jewellery.billing.dto;

import com.myhourly.jewellery.billing.enums.MakingChargeType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record BillItemRequest(
        Long productId,
        @NotNull(message = "netWeight is required")
        @Positive(message = "netWeight must be greater than zero")
        @Digits(integer = 9, fraction = 3, message = "netWeight allows up to 9 digits and 3 decimal places")
        BigDecimal netWeight,
        @NotNull(message = "ratePerGram is required")
        @Positive(message = "ratePerGram must be greater than zero")
        @Digits(integer = 10, fraction = 2, message = "ratePerGram allows up to 10 digits and 2 decimal places")
        BigDecimal ratePerGram,
        @NotNull(message = "makingChargeType is required")
        MakingChargeType makingChargeType,
        @NotNull(message = "makingChargeValue is required")
        @PositiveOrZero(message = "makingChargeValue must not be negative")
        @Digits(integer = 10, fraction = 2, message = "makingChargeValue allows up to 10 digits and 2 decimal places")
        BigDecimal makingChargeValue) {
}
package com.myhourly.jewellery.metalrate.dto;

import com.myhourly.jewellery.metalrate.enums.MetalType;
import com.myhourly.jewellery.metalrate.enums.Purity;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MetalValueRequest(

        @NotNull(message = "Metal type is required")
        MetalType metalType,

        @NotNull(message = "Purity is required")
        Purity purity,

        @NotNull(message = "Net weight is required")
        @DecimalMin(value = "0.000", inclusive = false, message = "Net weight must be greater than zero")
        @Digits(integer = 7, fraction = 3, message = "Net weight allows up to 7 digits and 3 decimals")
        BigDecimal netWeight,

        LocalDateTime at
) {
}

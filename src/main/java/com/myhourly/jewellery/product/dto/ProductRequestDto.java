package com.myhourly.jewellery.product.dto;

import com.myhourly.jewellery.product.enums.MakingChargeType;
import com.myhourly.jewellery.product.enums.MetalType;
import com.myhourly.jewellery.product.enums.Purity;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductRequestDto(

        @NotBlank(message = "SKU is required")
        @Size(max = 50)
        @Pattern(
                regexp ="^[A-Za-z0-9_-]+$" ,
                message = "SKU can contain letters, numbers, _ and -only"
        )
        String sku,

        @NotBlank(message = "Product name is required")
        @Size(max = 150)
        String name,

        @NotBlank(message = "Category is required")
        @Size(max = 80)
        String category,

        @NotNull(message = "Metal type is required")
        MetalType metalType,

        @NotNull(message = "Purity is required")
        Purity purity,

        @NotNull(message = "Gross weight is required")
        @DecimalMin(
                value = "0.000",
                inclusive = false,
                message = "Gross weight must be greater than zero"
        )
        @Digits(integer = 9, fraction = 3)
                BigDecimal grossWeight,


        @NotNull(message = "stone weight is required")
        @DecimalMin(
                value="0.000",
                message = "stone weight cannot be negative"
        )
        @Digits(integer = 9, fraction = 3)
        BigDecimal stoneWeight,

        @NotNull(message = "Making charge type is required")
        MakingChargeType makingChargeType,

        @NotNull(message = "Making charge value is required")
        @DecimalMin(
                value = "0.00",
                message = "Making charge cannot be negative"
        )
        @Digits(integer = 10, fraction = 2)
        BigDecimal makingChargeValue


) {



}

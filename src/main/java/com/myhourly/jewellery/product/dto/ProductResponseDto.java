package com.myhourly.jewellery.product.dto;

import com.myhourly.jewellery.product.enums.MakingChargeType;
import com.myhourly.jewellery.product.enums.MetalType;
import com.myhourly.jewellery.product.enums.Purity;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponseDto (

        Long id,
        String sku,
        String name,
        String category,
        MetalType metalType,
        Purity purity,
        BigDecimal grossWeight,
        BigDecimal stoneWeight,
        BigDecimal netWeight,
        MakingChargeType makingChargeType,
        BigDecimal makingChargeValue,
        boolean active,
        Instant createdAt,
        Instant updatedAt
){
}

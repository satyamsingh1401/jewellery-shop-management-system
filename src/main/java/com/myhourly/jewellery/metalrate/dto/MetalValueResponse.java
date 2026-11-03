package com.myhourly.jewellery.metalrate.dto;

import com.myhourly.jewellery.metalrate.enums.MetalType;
import com.myhourly.jewellery.metalrate.enums.Purity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MetalValueResponse(
        MetalType metalType,
        Purity purity,
        BigDecimal netWeight,
        Long rateId,
        BigDecimal ratePerGram,
        String currency,
        LocalDateTime rateEffectiveFrom,
        BigDecimal metalValue
) {
}

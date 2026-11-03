package com.myhourly.jewellery.metalrate.dto;

import com.myhourly.jewellery.metalrate.enums.MetalType;
import com.myhourly.jewellery.metalrate.enums.Purity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MetalRateResponse(
        Long id,
        MetalType metalType,
        Purity purity,
        BigDecimal ratePerGram,
        String currency,
        LocalDateTime effectiveFrom,
        String remarks,
        String createdBy,
        LocalDateTime createdAt
) {
}

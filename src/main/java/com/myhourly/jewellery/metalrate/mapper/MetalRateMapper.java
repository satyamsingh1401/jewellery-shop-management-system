package com.myhourly.jewellery.metalrate.mapper;

import com.myhourly.jewellery.metalrate.dto.MetalRateRequest;
import com.myhourly.jewellery.metalrate.dto.MetalRateResponse;
import com.myhourly.jewellery.metalrate.entity.MetalRate;
import org.springframework.stereotype.Component;

@Component
public class MetalRateMapper {

    public MetalRate toEntity(MetalRateRequest request) {
        MetalRate rate = new MetalRate();
        rate.setMetalType(request.metalType());
        rate.setPurity(request.purity());
        rate.setRatePerGram(request.ratePerGram());
        rate.setEffectiveFrom(request.effectiveFrom());
        rate.setRemarks(normalize(request.remarks()));
        return rate;
    }

    public MetalRateResponse toResponse(MetalRate rate) {
        return new MetalRateResponse(
                rate.getId(),
                rate.getMetalType(),
                rate.getPurity(),
                rate.getRatePerGram(),
                rate.getCurrency(),
                rate.getEffectiveFrom(),
                rate.getRemarks(),
                rate.getCreatedBy(),
                rate.getCreatedAt());
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}

package com.myhourly.jewellery.metalrate.dto;

import java.util.List;

public record MetalRatePageResponse(
        List<MetalRateResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}

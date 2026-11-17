package com.myhourly.jewellery.security.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.Map;

@Builder
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {
}
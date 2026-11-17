package com.myhourly.jewellery.common.response;

import lombok.Builder;

import java.time.Instant;
import java.util.Map;

@Builder
public record ApiError(

        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {


}

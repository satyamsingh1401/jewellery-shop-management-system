package com.myhourly.jewellery.security.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateSalesStaffStatusRequest(

        @NotNull(message = "Active status is required")
        Boolean active

) {
}
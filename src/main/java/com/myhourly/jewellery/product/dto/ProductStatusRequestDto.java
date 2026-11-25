package com.myhourly.jewellery.product.dto;

import jakarta.validation.constraints.NotNull;

public record ProductStatusRequestDto(

        @NotNull(message = "Active status is required")
        Boolean active

) {
}

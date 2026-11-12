
package com.myhourly.jewellery.metalrate.dto;

import com.myhourly.jewellery.metalrate.enums.MetalType;
import com.myhourly.jewellery.metalrate.enums.Purity;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MetalRateRequest(

		@NotNull(message = "Metal type is required") MetalType metalType,

		@NotNull(message = "Purity is required") Purity purity,

		@NotNull(message = "Rate per gram is required") @DecimalMin(value = "0.00", inclusive = false, message = "Rate per gram must be greater than zero") @Digits(integer = 10, fraction = 2, message = "Rate per gram allows up to 10 digits and 2 decimals") BigDecimal ratePerGram,

		LocalDateTime effectiveFrom,

		@Size(max = 255, message = "Remarks must not exceed 255 characters") String remarks) {
}

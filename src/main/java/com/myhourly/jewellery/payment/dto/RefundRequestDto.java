package com.myhourly.jewellery.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class RefundRequestDto {

    @NotNull(message = "Refund amount is required")
    @DecimalMin(
            value = "0.01",
            message = "Refund amount must be at least 0.01"
    )
    @Digits(integer = 12, fraction = 2,
            message = "Refund amount allows at most 12 digits and 2 decimal places")
    private BigDecimal amount;

    @NotBlank(message = "Refund reason is required")
    private String reason;

    public RefundRequestDto() {
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
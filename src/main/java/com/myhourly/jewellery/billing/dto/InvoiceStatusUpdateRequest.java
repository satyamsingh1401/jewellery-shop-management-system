package com.myhourly.jewellery.billing.dto;

import com.myhourly.jewellery.billing.enums.InvoiceStatus;
import jakarta.validation.constraints.NotNull;

public record InvoiceStatusUpdateRequest(@NotNull InvoiceStatus status) {
}
package com.myhourly.jewellery.billing.dto;

public record BillingError(int status, String error, String message) {
}
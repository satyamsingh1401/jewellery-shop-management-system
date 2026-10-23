package com.myhourly.jewellery.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class StockInRequest {

    @Min(1)
    private int quantity;

    @NotBlank
    @Size(max = 255)
    private String reason;

    public StockInRequest() { }

    public StockInRequest(int quantity, String reason) {
        this.quantity = quantity;
        this.reason = reason;
    }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}

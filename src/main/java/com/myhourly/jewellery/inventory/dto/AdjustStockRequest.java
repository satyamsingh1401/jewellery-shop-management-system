package com.myhourly.jewellery.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** quantityChange: positive adds stock, negative removes stock. Zero is rejected by the service. */
public class AdjustStockRequest {

    private int quantityChange;

    @NotBlank
    @Size(max = 255)
    private String reason;

    public AdjustStockRequest() { }

    public AdjustStockRequest(int quantityChange, String reason) {
        this.quantityChange = quantityChange;
        this.reason = reason;
    }

    public int getQuantityChange() { return quantityChange; }
    public void setQuantityChange(int quantityChange) { this.quantityChange = quantityChange; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}

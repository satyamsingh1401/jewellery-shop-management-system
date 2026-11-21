package com.myhourly.jewellery.inventory.dto;

import com.myhourly.jewellery.inventory.InventoryLimits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class StockInRequest {

    @NotNull
    @Min(1)
    @Max(InventoryLimits.MAX_QUANTITY_PER_MOVEMENT)
    private Integer quantity;

    @NotBlank
    @Size(max = 255)
    private String reason;

    public StockInRequest() { }

    public StockInRequest(Integer quantity, String reason) {
        this.quantity = quantity;
        this.reason = reason;
    }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}

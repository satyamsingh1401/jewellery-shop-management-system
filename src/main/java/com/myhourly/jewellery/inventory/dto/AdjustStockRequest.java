package com.myhourly.jewellery.inventory.dto;

import com.myhourly.jewellery.inventory.InventoryLimits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** quantityChange: positive adds stock, negative removes stock. Zero is rejected by the service. */
public class AdjustStockRequest {

    @NotNull
    @Min(-InventoryLimits.MAX_QUANTITY_PER_MOVEMENT)
    @Max(InventoryLimits.MAX_QUANTITY_PER_MOVEMENT)
    private Integer quantityChange;

    @NotBlank
    @Size(max = 255)
    private String reason;

    public AdjustStockRequest() { }

    public AdjustStockRequest(Integer quantityChange, String reason) {
        this.quantityChange = quantityChange;
        this.reason = reason;
    }

    public Integer getQuantityChange() { return quantityChange; }
    public void setQuantityChange(Integer quantityChange) { this.quantityChange = quantityChange; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}

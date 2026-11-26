package com.myhourly.jewellery.inventory.dto;

import com.myhourly.jewellery.inventory.InventoryLimits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class CreateInventoryRequest {

    @NotNull
    @Positive
    private Long productId;

    @NotNull
    @Min(0)
    @Max(InventoryLimits.MAX_QUANTITY_PER_MOVEMENT)
    private Integer initialQuantity;

    @NotNull
    @Min(0)
    private Integer reorderLevel;

    public CreateInventoryRequest() { }

    public CreateInventoryRequest(Long productId, Integer initialQuantity, Integer reorderLevel) {
        this.productId = productId;
        this.initialQuantity = initialQuantity;
        this.reorderLevel = reorderLevel;
    }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Integer getInitialQuantity() { return initialQuantity; }
    public void setInitialQuantity(Integer initialQuantity) { this.initialQuantity = initialQuantity; }
    public Integer getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(Integer reorderLevel) { this.reorderLevel = reorderLevel; }
}

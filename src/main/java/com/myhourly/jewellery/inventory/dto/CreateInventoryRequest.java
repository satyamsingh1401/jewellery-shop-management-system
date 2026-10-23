package com.myhourly.jewellery.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class CreateInventoryRequest {

    @NotNull
    @Positive
    private Long productId;

    @Min(0)
    private int initialQuantity;

    @Min(0)
    private int reorderLevel;

    public CreateInventoryRequest() { }

    public CreateInventoryRequest(Long productId, int initialQuantity, int reorderLevel) {
        this.productId = productId;
        this.initialQuantity = initialQuantity;
        this.reorderLevel = reorderLevel;
    }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public int getInitialQuantity() { return initialQuantity; }
    public void setInitialQuantity(int initialQuantity) { this.initialQuantity = initialQuantity; }
    public int getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(int reorderLevel) { this.reorderLevel = reorderLevel; }
}

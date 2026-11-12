package com.myhourly.jewellery.inventory.dto;

public class AvailabilityResponse {

    private final Long productId;
    private final int requestedQuantity;
    private final boolean available;

    public AvailabilityResponse(Long productId, int requestedQuantity, boolean available) {
        this.productId = productId;
        this.requestedQuantity = requestedQuantity;
        this.available = available;
    }

    public Long getProductId() { return productId; }
    public int getRequestedQuantity() { return requestedQuantity; }
    public boolean isAvailable() { return available; }
}

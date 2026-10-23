package com.myhourly.jewellery.inventory.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(Long productId, int requested) {
        super("Insufficient stock for product " + productId + " (requested " + requested + ")");
    }
}

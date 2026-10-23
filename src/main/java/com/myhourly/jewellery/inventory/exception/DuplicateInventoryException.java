package com.myhourly.jewellery.inventory.exception;

public class DuplicateInventoryException extends RuntimeException {
    public DuplicateInventoryException(Long productId) {
        super("Inventory already exists for product " + productId);
    }
}

package com.myhourly.jewellery.common.exception;

public class InsufficientStockException extends BusinessValidationException {

    public InsufficientStockException(String message) {
        super(message);
    }
}

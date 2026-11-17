package com.myhourly.jewellery.common.exception;

public class AccountInactiveException extends RuntimeException {

    public AccountInactiveException(String message) {
        super(message);
    }
}
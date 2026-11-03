package com.myhourly.jewellery.security.exception;

public class AccountInactiveException extends RuntimeException {

    public AccountInactiveException(String message) {
        super(message);
    }
}
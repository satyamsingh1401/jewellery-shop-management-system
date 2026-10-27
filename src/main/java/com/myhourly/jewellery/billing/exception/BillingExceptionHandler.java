package com.myhourly.jewellery.billing.exception;

import com.myhourly.jewellery.billing.dto.BillingError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Limited to the billing package so it doesn't interfere with other modules.
// Replace with the shared common/exception handling once the team adds it.
@RestControllerAdvice(basePackages = "com.myhourly.jewellery.billing")
public class BillingExceptionHandler {

    @ExceptionHandler(InvoiceNotFoundException.class)
    public ResponseEntity<BillingError> handleNotFound(InvoiceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<BillingError> handleBadRequest(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    private ResponseEntity<BillingError> build(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(new BillingError(status.value(), status.getReasonPhrase(), message));
    }
}
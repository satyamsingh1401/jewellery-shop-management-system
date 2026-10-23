package com.myhourly.jewellery.inventory.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns this module's exceptions into clean HTTP responses (no stack traces).
 * Scoped to this module's package so it cannot clash with teammates' handlers.
 * When the team has one global handler, move these methods into it.
 */
@RestControllerAdvice(basePackages = "com.myhourly.jewellery.inventory")
public class InventoryExceptionHandler {

    @ExceptionHandler(InventoryNotFoundException.class)
    public ProblemDetail notFound(InventoryNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({InsufficientStockException.class, DuplicateInventoryException.class})
    public ProblemDetail conflict(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail badRequest(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
}

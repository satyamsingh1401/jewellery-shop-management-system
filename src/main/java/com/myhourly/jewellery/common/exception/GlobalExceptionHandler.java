package com.myhourly.jewellery.common.exception;

import com.myhourly.jewellery.common.response.ApiError;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, WebRequest request){

        return build(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request,
                Map.of()
        );
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiError> handleDuplicate(DuplicateResourceException ex, WebRequest request){
        return build(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                request,
                Map.of()
        );
    }

    @ExceptionHandler(BusinessValidationException.class)
    public ResponseEntity<ApiError> handleBusinessValidation(BusinessValidationException ex, WebRequest request){
        return build(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request,
                Map.of()
        );
    }

    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex , WebRequest request ){

        Map<String, String> errors = new LinkedHashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.putIfAbsent(
                        error.getField(),
                        error.getDefaultMessage()
                )
        );

        return build(
                HttpStatus.BAD_REQUEST,
                "Request validation failed",
                request,
                errors
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(
            DataIntegrityViolationException ex,
            WebRequest request) {

        return build(
                HttpStatus.CONFLICT,
                "The operation conflicts with existing data",
                request,
                Map.of()
        );
    }

    private ResponseEntity<ApiError> build(
            HttpStatus status, String message, WebRequest request,
            Map<String,String> fieldErrors){
        String path = ((ServletWebRequest) request)
                .getRequest().getRequestURI();

        ApiError body = new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                fieldErrors
        );
        return ResponseEntity.status(status).body(body);

    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableRequest(
            HttpMessageNotReadableException ex,
            WebRequest request) {

        return build(
                HttpStatus.BAD_REQUEST,
                "Request body contains invalid JSON or enum values",
                request,
                Map.of()
        );
    }



}

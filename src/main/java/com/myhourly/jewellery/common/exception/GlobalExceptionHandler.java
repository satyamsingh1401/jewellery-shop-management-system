package com.myhourly.jewellery.common.exception;

import com.myhourly.jewellery.common.response.ApiError;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, WebRequest request) {

        return build(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request,
                Map.of()
        );
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiError> handleDuplicate(DuplicateResourceException ex, WebRequest request) {
        return build(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                request,
                Map.of()
        );
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ApiError> handleInsufficientStock(InsufficientStockException ex, WebRequest request) {
        return build(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                request,
                Map.of()
        );
    }

    @ExceptionHandler(BusinessValidationException.class)
    public ResponseEntity<ApiError> handleBusinessValidation(BusinessValidationException ex, WebRequest request) {
        return build(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request,
                Map.of()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, WebRequest request) {

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

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParameter(
            MissingServletRequestParameterException ex,
            WebRequest request) {

        String name = ex.getParameterName();
        return build(
                HttpStatus.BAD_REQUEST,
                "Request validation failed",
                request,
                Map.of(name, name + " is required")
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            WebRequest request) {

        String name = ex.getName();
        Class<?> type = ex.getRequiredType();
        String expected = type == null ? "valid value" : "valid " + type.getSimpleName();
        return build(
                HttpStatus.BAD_REQUEST,
                "Request validation failed",
                request,
                Map.of(name, name + " must be a " + expected)
        );
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ApiError> handleUnknownSortProperty(
            PropertyReferenceException ex,
            WebRequest request) {

        return build(
                HttpStatus.BAD_REQUEST,
                "Request validation failed",
                request,
                Map.of("sort", "sort must reference a supported property")
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            WebRequest request) {

        return build(
                HttpStatus.METHOD_NOT_ALLOWED,
                "HTTP method " + ex.getMethod() + " is not supported for this endpoint",
                request,
                Map.of(),
                ex.getHeaders()
        );
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex,
            WebRequest request) {

        return build(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Content-Type is not supported; supported types: "
                        + MediaType.toString(ex.getSupportedMediaTypes()),
                request,
                Map.of(),
                ex.getHeaders()
        );
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ApiError> handleMediaTypeNotAcceptable(
            HttpMediaTypeNotAcceptableException ex,
            WebRequest request) {

        return build(
                HttpStatus.NOT_ACCEPTABLE,
                "Requested response type is not available; supported types: "
                        + MediaType.toString(ex.getSupportedMediaTypes()),
                request,
                Map.of(),
                ex.getHeaders()
        );
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ApiError> handleNoEndpoint(
            Exception ex,
            WebRequest request) {

        return build(
                HttpStatus.NOT_FOUND,
                "No endpoint matches the requested path",
                request,
                Map.of()
        );
    }

    private ResponseEntity<ApiError> build(
            HttpStatus status, String message, WebRequest request,
            Map<String, String> fieldErrors) {
        return build(status, message, request, fieldErrors, HttpHeaders.EMPTY);
    }

    private ResponseEntity<ApiError> build(
            HttpStatus status, String message, WebRequest request,
            Map<String, String> fieldErrors, HttpHeaders headers) {
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
        return ResponseEntity.status(status).headers(headers).body(body);

    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(
            ConstraintViolationException ex,
            WebRequest request) {

        Map<String, String> errors = new LinkedHashMap<>();

        ex.getConstraintViolations().forEach(violation -> {

            String field = violation.getPropertyPath().toString();

            errors.putIfAbsent(
                    field,
                    violation.getMessage()
            );
        });

        return build(
                HttpStatus.BAD_REQUEST,
                "Request validation failed",
                request,
                errors
        );
    }


}

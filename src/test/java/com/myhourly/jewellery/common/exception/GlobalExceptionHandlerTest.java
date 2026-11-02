package com.myhourly.jewellery.common.exception;

import com.myhourly.jewellery.common.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler =
            new GlobalExceptionHandler();

    @Test
    void shouldReturn400ForMalformedRequestBody() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/payments");

        ResponseEntity<ApiErrorResponse> response =
                handler.handleInvalidRequestBody(null, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals(
                "Invalid or malformed request body",
                response.getBody().getMessage());
        assertEquals("/api/payments", response.getBody().getPath());
    }

    @Test
    void shouldReturn500ForUnexpectedException() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/payments");

        ResponseEntity<ApiErrorResponse> response =
                handler.handleUnexpectedException(
                        new RuntimeException("Internal detail"),
                        request);

        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR,
                response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getStatus());
        assertEquals(
                "An unexpected error occurred",
                response.getBody().getMessage());
        assertEquals("/api/payments", response.getBody().getPath());
        assertFalse(
                response.getBody().getMessage().contains("Internal detail"));
    }

    @Test
    void shouldKeep405ForUnsupportedHttpMethod() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/payments");

        ResponseEntity<ApiErrorResponse> response =
                handler.handleUnexpectedException(
                        new HttpRequestMethodNotSupportedException("DELETE"),
                        request);

        assertEquals(
                HttpStatus.METHOD_NOT_ALLOWED,
                response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(405, response.getBody().getStatus());
    }

    @Test
    void shouldReturn409ForOptimisticLockingFailure() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/payments/1/refund");

        ResponseEntity<ApiErrorResponse> response =
                handler.handleOptimisticLockingFailure(
                        new OptimisticLockingFailureException("stale"),
                        request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().getStatus());
    }

    @Test
    void shouldReturn409ForDataIntegrityViolation() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/payments");

        ResponseEntity<ApiErrorResponse> response =
                handler.handleDataIntegrityViolation(
                        new DataIntegrityViolationException("duplicate"),
                        request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().getMessage().contains("duplicate"));
    }
}
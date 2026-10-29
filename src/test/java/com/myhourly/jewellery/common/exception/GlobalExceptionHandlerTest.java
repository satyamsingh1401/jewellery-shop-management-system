
package com.myhourly.jewellery.common.exception;

import com.myhourly.jewellery.common.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

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
}

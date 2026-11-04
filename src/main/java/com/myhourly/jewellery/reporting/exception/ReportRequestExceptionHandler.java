package com.myhourly.jewellery.reporting.exception;

import com.myhourly.jewellery.common.response.ApiError;
import com.myhourly.jewellery.reporting.controller.SalesReportController;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackageClasses = SalesReportController.class)
public class ReportRequestExceptionHandler {

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParameter(MissingServletRequestParameterException ex,
                                                           HttpServletRequest request) {
        String name = ex.getParameterName();
        return badRequest(name, "Required parameter '" + name + "' is missing", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                       HttpServletRequest request) {
        String name = ex.getName();
        return badRequest(name, describeMismatch(name, ex.getRequiredType()), request);
    }

    private String describeMismatch(String name, Class<?> requiredType) {
        if (requiredType == LocalDate.class) {
            return "Parameter '" + name + "' must be a valid date in yyyy-MM-dd format";
        }
        if (requiredType == Integer.class || requiredType == int.class) {
            return "Parameter '" + name + "' must be a whole number";
        }
        if (requiredType == Boolean.class || requiredType == boolean.class) {
            return "Parameter '" + name + "' must be true or false";
        }
        return "Parameter '" + name + "' has an invalid value";
    }

    private ResponseEntity<ApiError> badRequest(String name, String message, HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ApiError body = new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                Map.of(name, message)
        );
        return ResponseEntity.status(status).body(body);
    }
}

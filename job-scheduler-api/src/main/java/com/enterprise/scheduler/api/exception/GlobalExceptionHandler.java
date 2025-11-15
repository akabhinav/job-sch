package com.enterprise.scheduler.api.exception;

import com.enterprise.scheduler.api.dto.ApiResponse;
import com.enterprise.scheduler.common.exception.JobExecutionException;
import com.enterprise.scheduler.common.exception.JobNotFoundException;
import com.enterprise.scheduler.common.exception.SchedulerException;
import com.enterprise.scheduler.security.exception.InvalidApiKeyException;
import com.enterprise.scheduler.security.exception.TenantAccessDeniedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Global exception handler for REST API
 * Feature #43: REST API Design
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(JobNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleJobNotFoundException(
            JobNotFoundException ex, WebRequest request) {
        log.error("Job not found: {}", ex.getMessage());

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .status("error")
                .message(ex.getMessage())
                .requestId(generateRequestId())
                .build();

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    @ExceptionHandler(JobExecutionException.class)
    public ResponseEntity<ApiResponse<Void>> handleJobExecutionException(
            JobExecutionException ex, WebRequest request) {
        log.error("Job execution error: {}", ex.getMessage(), ex);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .status("error")
                .message("Job execution failed: " + ex.getMessage())
                .requestId(generateRequestId())
                .build();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    @ExceptionHandler(SchedulerException.class)
    public ResponseEntity<ApiResponse<Void>> handleSchedulerException(
            SchedulerException ex, WebRequest request) {
        log.error("Scheduler error: {}", ex.getMessage(), ex);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .status("error")
                .message("Scheduler error: " + ex.getMessage())
                .requestId(generateRequestId())
                .build();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    @ExceptionHandler(InvalidApiKeyException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidApiKeyException(
            InvalidApiKeyException ex, WebRequest request) {
        log.warn("Invalid API key: {}", ex.getMessage());

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .status("error")
                .message("Invalid or expired API key")
                .requestId(generateRequestId())
                .build();

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(response);
    }

    @ExceptionHandler(TenantAccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleTenantAccessDeniedException(
            TenantAccessDeniedException ex, WebRequest request) {
        log.warn("Tenant access denied: {}", ex.getMessage());

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .status("error")
                .message("Access denied to tenant resources")
                .requestId(generateRequestId())
                .build();

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        log.warn("Access denied: {}", ex.getMessage());

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .status("error")
                .message("Access denied: " + ex.getMessage())
                .requestId(generateRequestId())
                .build();

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {
        log.warn("Validation error: {}", ex.getMessage());

        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>builder()
                .status("error")
                .message("Validation failed")
                .data(errors)
                .requestId(generateRequestId())
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        log.warn("Invalid argument: {}", ex.getMessage());

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .status("error")
                .message(ex.getMessage())
                .requestId(generateRequestId())
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(
            Exception ex, WebRequest request) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .status("error")
                .message("An unexpected error occurred")
                .requestId(generateRequestId())
                .build();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    private String generateRequestId() {
        return UUID.randomUUID().toString();
    }
}

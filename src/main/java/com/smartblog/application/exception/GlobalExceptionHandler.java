package com.smartblog.application.exception;

import com.smartblog.core.dto.ApiResponse;
import com.smartblog.core.exceptions.DuplicateException;
import com.smartblog.core.exceptions.NotAuthorizedException;
import com.smartblog.core.exceptions.NotFoundException;
import com.smartblog.core.exceptions.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for all REST controllers.
 * <p>
 * Centralizes exception handling to provide consistent error responses
 * across all API endpoints. Converts exceptions into appropriate HTTP
 * status codes and user-friendly error messages.
 * </p>
 *
 * <h3>Exception Mapping:</h3>
 * <ul>
 *   <li>{@link ValidationException} → 400 BAD_REQUEST</li>
 *   <li>{@link NotFoundException} → 404 NOT_FOUND</li>
 *   <li>{@link DuplicateException} → 409 CONFLICT</li>
 *   <li>{@link NotAuthorizedException} → 403 FORBIDDEN</li>
 *   <li>{@link MethodArgumentNotValidException} → 400 BAD_REQUEST (Bean Validation)</li>
 *   <li>{@link Exception} → 500 INTERNAL_SERVER_ERROR</li>
 * </ul>
 *
 * @since Epic 3
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handles validation exceptions from business logic.
     *
     * @param ex ValidationException thrown from service methods
     * @return 400 BAD_REQUEST with error message
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(ValidationException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handles Bean Validation (@Valid) failures.
     *
     * @param ex MethodArgumentNotValidException from Spring Validation
     * @return 400 BAD_REQUEST with field-specific error messages
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Validation failed for {} fields", errors.size());

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error("Validation failed", errors));
    }

    /**
     * Handles resource not found exceptions.
     *
     * @param ex NotFoundException thrown from service/controller
     * @return 404 NOT_FOUND with error message
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFoundException(NotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.notFound(ex.getMessage()));
    }

    /**
     * Handles duplicate resource exceptions.
     *
     * @param ex DuplicateException thrown from service layer
     * @return 409 CONFLICT with error message
     */
    @ExceptionHandler(DuplicateException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateException(DuplicateException ex) {
        log.warn("Duplicate resource: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(HttpStatus.CONFLICT, ex.getMessage()));
    }

    /**
     * Handles authorization failures.
     *
     * @param ex NotAuthorizedException thrown from security checks
     * @return 403 FORBIDDEN with error message
     */
    @ExceptionHandler(NotAuthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotAuthorizedException(NotAuthorizedException ex) {
        log.warn("Authorization failed: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(HttpStatus.FORBIDDEN, ex.getMessage()));
    }

    /**
     * Handles type mismatch exceptions.
     *
     * @param ex MethodArgumentTypeMismatchException from Spring
     * @return 400 BAD_REQUEST with error message
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String error = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s",
                ex.getValue(), ex.getName(), ex.getRequiredType().getSimpleName());

        log.warn("Type mismatch: {}", error);

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(error));
    }

    /**
     * Handles all uncaught exceptions.
     *
     * @param ex Any uncaught exception
     * @return 500 INTERNAL_SERVER_ERROR with generic error message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR,
                        "An unexpected error occurred. Please contact support."));
    }
}


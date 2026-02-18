package com.smartblog.core.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {


    private ResponseStatus status;


    private int statusCode;

    private String message;


    private T data;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();


    private Object errors;

    private PaginationMetadata pagination;

    /**
     * Create success response with data
     * HTTP 200 OK
     */

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .status(ResponseStatus.SUCCESS)
                .statusCode(HttpStatus.OK.value())
                .message(message)
                .data(data)
                .build();
    }
    /**
     * Create success response without data
     * HTTP 200 OK
     */

    public static <T> ApiResponse<T> success(String message) {
        return success(message, null);
    }


     /** Create success response with data (201 Created)
     * Used for POST requests
     */
    public static <T> ApiResponse<T> created(String message, T data) {
        return ApiResponse.<T>builder()
                .status(ResponseStatus.SUCCESS)
                .statusCode(HttpStatus.CREATED.value())
                .message(message)
                .data(data)
                .build();
    }


    /**
     * Create success response with pagination
     * HTTP 200 OK with pagination metadata
     */
    public static <T> ApiResponse<T> success(String message, T data, PaginationMetadata pagination) {
        return ApiResponse.<T>builder()
                .status(ResponseStatus.SUCCESS)
                .statusCode(HttpStatus.OK.value())
                .message(message)
                .data(data)
                .pagination(pagination)
                .build();
    }

    /**
     * Create error response with message only
     * HTTP 400 Bad Request
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .status(ResponseStatus.ERROR)
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .message(message)
                .build();
    }

    /**
     * Create error response with message and error details
     * HTTP 400 Bad Request
     */
    public static <T> ApiResponse<T> error(String message, Object errors) {
        return ApiResponse.<T>builder()
                .status(ResponseStatus.ERROR)
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .message(message)
                .errors(errors)
                .build();
    }

    /**
     * Create error response with custom HTTP status
     */
    public static <T> ApiResponse<T> error(HttpStatus httpStatus, String message) {
        return ApiResponse.<T>builder()
                .status(ResponseStatus.ERROR)
                .statusCode(httpStatus.value())
                .message(message)
                .build();
    }

    /**
     * Create error response with custom HTTP status and error details
     */
    public static <T> ApiResponse<T> error(HttpStatus httpStatus, String message, Object errors) {
        return ApiResponse.<T>builder()
                .status(ResponseStatus.ERROR)
                .statusCode(httpStatus.value())
                .message(message)
                .errors(errors)
                .build();
    }
    /**
     * Create not found error (404)
     */
    public static <T> ApiResponse<T> notFound(String message) {
        return error(HttpStatus.NOT_FOUND, message);
    }


    public static <T> ApiResponse<T> unauthorized(String message) {
        return error(HttpStatus.UNAUTHORIZED, message);
    }


    public static <T> ApiResponse<T> forbidden(String message) {
        return error(HttpStatus.FORBIDDEN, message);
    }

    public static <T> ApiResponse<T> serverError(String message) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    public enum ResponseStatus {
        SUCCESS,
        ERROR,
        WARNING
    }
}

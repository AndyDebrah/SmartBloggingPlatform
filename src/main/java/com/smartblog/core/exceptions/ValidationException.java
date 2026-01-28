
package com.smartblog.core.exceptions;

/**
 * Thrown when business rule validation fails.
 * Examples:
 * - Empty title
 * - Weak password
 * - Invalid email format
 */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}

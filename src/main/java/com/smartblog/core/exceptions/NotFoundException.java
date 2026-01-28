
package com.smartblog.core.exceptions;

/**
 * Used when something cannot be found in the system.
 * This is returned instead of returning null (bad practice).
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}

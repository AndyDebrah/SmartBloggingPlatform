
package com.smartblog.core.exceptions;

/**
 * Used when a unique constraint is violated.
 */
public class DuplicateException extends RuntimeException {
    public DuplicateException(String msg) {
        super(msg);
    }
}

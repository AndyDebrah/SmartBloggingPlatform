
package com.smartblog.core.dto;

/**
 * DTO: Safe representation of a User for UI/API.
 * Excludes passwordHash and internal timestamps.
 */
public record UserDTO(
        Long id,
        String username,
        String email,
        String role
) {}

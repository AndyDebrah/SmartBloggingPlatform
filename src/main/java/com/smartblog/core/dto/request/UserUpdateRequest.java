package com.smartblog.core.dto.request;

import jakarta.validation.constraints.*;

/**
 * Request DTO for updating an existing user.
 * <p>
 * All fields are optional (nullable) to support partial updates.
 * Only provided fields will be updated.
 * </p>
 *
 * @param username Optional new username
 * @param email Optional new email
 * @param role Optional new role
 */
public record UserUpdateRequest(
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores")
        String username,

        @Email(message = "Email must be valid")
        String email,

        @Pattern(regexp = "^(ADMIN|AUTHOR|READER)$", message = "Role must be ADMIN, AUTHOR, or READER")
        String role
) {}


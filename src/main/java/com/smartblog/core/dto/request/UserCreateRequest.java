package com.smartblog.core.dto.request;

import com.smartblog.application.validation.UniqueEmail;
import com.smartblog.application.validation.UniqueUsername;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * Request DTO for creating a new user.
 * <p>
 * Includes comprehensive validation rules and OpenAPI documentation.
 * </p>
 */
@Schema(description = "Request body for creating a new user account")
public record UserCreateRequest(
        @Schema(
                description = "Unique username for the account",
                example = "johndoe",
                required = true,
                minLength = 3,
                maxLength = 50
        )
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores")
        @UniqueUsername
        String username,

        @Schema(
                description = "User's email address",
                example = "john.doe@example.com",
                required = true,
                format = "email"
        )
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @UniqueEmail
        String email,

        @Schema(
                description = "User's password (minimum 8 characters)",
                example = "SecurePass123",
                required = true,
                minLength = 8
        )
        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        @Schema(
                description = "User role",
                example = "AUTHOR",
                allowableValues = {"ADMIN", "AUTHOR", "READER"},
                defaultValue = "READER"
        )
        @Pattern(regexp = "^(ADMIN|AUTHOR|READER)$", message = "Role must be ADMIN, AUTHOR, or READER")
        String role
) {}


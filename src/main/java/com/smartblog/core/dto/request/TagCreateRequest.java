package com.smartblog.core.dto.request;

import jakarta.validation.constraints.*;

/**
 * Request DTO for creating a new tag.
 *
 * @param name Tag name (2-50 chars, unique)
 * @param slug URL-friendly slug (optional, auto-generated if not provided)
 */
public record TagCreateRequest(
        @NotBlank(message = "Tag name is required")
        @Size(min = 2, max = 50, message = "Tag name must be between 2 and 50 characters")
        String name,

        @Size(max = 60, message = "Slug must not exceed 60 characters")
        @Pattern(regexp = "^[a-z0-9-]*$", message = "Slug can only contain lowercase letters, numbers, and hyphens")
        String slug
) {}


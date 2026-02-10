package com.smartblog.core.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Request body for creating a new blog post")
public record PostCreateRequest(
        @Schema(
                description = "Post title",
                example = "Introduction to Spring Boot 3",
                required = true,
                minLength = 3,
                maxLength = 255
        )
        @NotBlank(message = "Title is required")
        @Size(min = 3, max = 255, message = "Title must be between 3 and 255 characters")
        String title,

        @Schema(
                description = "Post content (supports markdown)",
                example = "This is a comprehensive guide to Spring Boot 3...",
                required = true,
                minLength = 10
        )
        @NotBlank(message = "Content is required")
        @Size(min = 10, message = "Content must be at least 10 characters")
        String content,

        @Schema(
                description = "Author's user ID",
                example = "1",
                required = true,
                minimum = "1"
        )
        @NotNull(message = "Author ID is required")
        @Positive(message = "Author ID must be positive")
        Long authorId,

        @Schema(
                description = "Publish immediately (true) or save as draft (false)",
                example = "false",
                defaultValue = "false"
        )
        boolean published
) {}


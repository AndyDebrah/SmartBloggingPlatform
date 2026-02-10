package com.smartblog.core.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Request body for creating a new comment")
public record CommentCreateRequest(
        @Schema(
                description = "ID of the post being commented on",
                example = "1",
                required = true,
                minimum = "1"
        )
        @NotNull(message = "Post ID is required")
        @Positive(message = "Post ID must be positive")
        Long postId,

        @Schema(
                description = "ID of the user making the comment",
                example = "2",
                required = true,
                minimum = "1"
        )
        @NotNull(message = "User ID is required")
        @Positive(message = "User ID must be positive")
        Long userId,

        @Schema(
                description = "Comment text content",
                example = "Great article! Very informative.",
                required = true,
                minLength = 1,
                maxLength = 5000
        )
        @NotBlank(message = "Comment content is required")
        @Size(min = 1, max = 5000, message = "Comment must be between 1 and 5000 characters")
        String content
) {}

package com.smartblog.core.dto;

import java.time.LocalDateTime;

/**
 * DTO: Represents a comment to display in UI/API.
 * Uses commenter username instead of raw userId.
 */
public record CommentDTO(
        Long id,
        Long postId,
        String commenterUsername,
        String content,
        LocalDateTime createdAt
) {}

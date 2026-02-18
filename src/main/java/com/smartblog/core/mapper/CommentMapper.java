package com.smartblog.core.mapper;

import com.smartblog.core.dto.CommentDTO;
import com.smartblog.core.model.Comment;

/**
 * Mapper utility for converting Comment entities to DTOs.
 */
public class CommentMapper {

    private CommentMapper() {
        // Utility class
    }

    public static CommentDTO toDTO(Comment comment) {
        if (comment == null) {
            return null;
        }

        return new CommentDTO(
                comment.getId(),
                comment.getPost() != null ? comment.getPost().getId() : null,
                comment.getUser() != null ? comment.getUser().getUsername() : null,
                comment.getContent(),
                comment.getCreatedAt());
    }
}

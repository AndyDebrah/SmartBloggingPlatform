
package com.smartblog.core.mapper;

import com.smartblog.core.dto.CommentDTO;
import com.smartblog.core.model.Comment;
import com.smartblog.core.model.User;

/**
 * Renders commenter username; hides raw userId.
 */
public final class CommentMapper {
    private CommentMapper() {}
    public static CommentDTO toDTO(Comment c, User commenter) {
        Long id = c.getId() == 0 ? null : Long.valueOf(c.getId());
        Long postId = Long.valueOf(c.getPostId());
        return new CommentDTO(
            id,
            postId,
            commenter != null ? commenter.getUsername() : null,
            c.getContent(),
            c.getCreatedAt(),
            c.getMongoId()
        );
    }
}

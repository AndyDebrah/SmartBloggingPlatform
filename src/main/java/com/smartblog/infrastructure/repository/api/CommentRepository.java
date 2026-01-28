
package com.smartblog.infrastructure.repository.api;

import com.smartblog.core.model.Comment;
import java.util.List;
import java.util.Optional;

public interface CommentRepository {
    long create(Comment c);
    Optional<Comment> findById(long id);
    List<Comment> listByPost(long postId, int page, int size);
    boolean update(Comment c);
    boolean softDelete(long id);
}

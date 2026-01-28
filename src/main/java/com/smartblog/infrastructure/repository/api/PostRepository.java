
package com.smartblog.infrastructure.repository.api;

import com.smartblog.core.model.Post;
import java.util.List;
import java.util.Optional;

public interface PostRepository {
    long create(Post p);
    Optional<Post> findById(long id);
    List<Post> list(int page, int size);
    List<Post> search(String keyword, int page, int size);
    List<Post> listByAuthor(long authorId, int page, int size);
    boolean update(Post p);
    boolean softDelete(long id);
    List<Post> searchByTag(String tag, int page, int size);
    List<Post> searchByAuthorName(String authorName, int page, int size);
    List<Post> searchCombined(String keyword, String tag, String authorName, String sortBy, int page, int size);
}

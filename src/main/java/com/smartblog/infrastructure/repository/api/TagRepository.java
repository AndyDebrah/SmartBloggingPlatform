
package com.smartblog.infrastructure.repository.api;

import com.smartblog.core.model.Tag;
import java.util.List;
import java.util.Optional;

public interface TagRepository {
    long create(Tag t);
    Optional<Tag> findById(long id);
    Optional<Tag> findBySlug(String slug);
    Optional<Tag> findByName(String name);
    List<Tag> listAll();
    boolean update(Tag t);
    boolean delete(long id);

    // relations
    boolean addTagToPost(long postId, long tagId);
    boolean removeTagFromPost(long postId, long tagId);
    List<Tag> listByPost(long postId);
}




package com.smartblog.application.service;

import java.util.Optional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;

import com.smartblog.core.dto.PostDTO;
import com.smartblog.core.model.Post;
import org.springframework.transaction.annotation.Transactional;


public interface PostService {
    long createDraft(long authorId, String title, String content);

    @Transactional
    @CacheEvict(value = "postsByAuthor", key = "#authorId")
    long createDraft_evict(long authorId, String title, String content);

    boolean publish(long postId);
    boolean update(long postId, String title, String content, boolean published);
    boolean softDelete(long postId);
    Optional<Post> getDomain(long id);
    Optional<PostDTO> getView(long id);
    Page<PostDTO> list(int page, int size);
    Page<PostDTO> search(String keyword, int page, int size);
    Page<PostDTO> listByAuthor(long authorId, int page, int size);
    Page<PostDTO> searchByTag(String tag, int page, int size);
    Page<PostDTO> searchByAuthorName(String authorName, int page, int size);
    Page<PostDTO> searchCombined(String keyword, String tag, String authorName, String sortBy, int page, int size);

}

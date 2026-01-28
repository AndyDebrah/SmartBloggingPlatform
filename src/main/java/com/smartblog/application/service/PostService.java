
package com.smartblog.application.service;

import java.util.List;
import java.util.Optional;

import com.smartblog.core.dto.PostDTO;
import com.smartblog.core.model.Post;




public interface PostService {
    long createDraft(long authorId, String title, String content);
    boolean publish(long postId);
    boolean update(long postId, String title, String content, boolean published);
    boolean softDelete(long postId);
    Optional<Post> getDomain(long id);
    Optional<PostDTO> getView(long id);
    List<PostDTO> list(int page, int size);
    List<PostDTO> search(String keyword, int page, int size);
    List<PostDTO> listByAuthor(long authorId, int page, int size);
    List<PostDTO> searchByTag(String tag, int page, int size);
    List<PostDTO> searchByAuthorName(String authorName, int page, int size);
    List<PostDTO> searchCombined(String keyword, String tag, String authorName, String sortBy, int page, int size);

}

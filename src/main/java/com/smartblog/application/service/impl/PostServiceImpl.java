package com.smartblog.application.service.impl;

import com.smartblog.application.service.PostService;
import com.smartblog.core.dto.PostDTO;
import com.smartblog.core.mapper.PostMapper;
import com.smartblog.core.model.Post;
import com.smartblog.core.model.User;
import com.smartblog.infrastructure.repository.jpa.PostJpaRepository;
import com.smartblog.infrastructure.repository.jpa.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service implementation for Post business logic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostServiceImpl implements PostService {

    private final PostJpaRepository postRepository;
    private final UserJpaRepository userRepository;

    @Override
    @Transactional
    public long createDraft(long authorId, String title, String content) {
        log.info("Creating draft post for author ID: {}", authorId);

        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("Author not found with ID: " + authorId));

        Post post = Post.builder()
                .author(author)
                .title(title)
                .content(content)
                .published(false)
                .build();

        Post savedPost = postRepository.save(post);
        log.info("Draft post created with ID: {}", savedPost.getId());

        return savedPost.getId();
    }

    @Override
    @Transactional
    public boolean publish(long postId) {
        return postRepository.findById(postId)
                .map(post -> {
                    post.setPublished(true);
                    postRepository.save(post);
                    log.info("Post published: {}", postId);
                    return true;
                })
                .orElse(false);
    }

    @Override
    @Transactional
    public boolean update(long postId, String title, String content, boolean published) {
        return postRepository.findById(postId)
                .map(post -> {
                    if (title != null && !title.isBlank()) {
                        post.setTitle(title);
                    }
                    if (content != null && !content.isBlank()) {
                        post.setContent(content);
                    }
                    post.setPublished(published);
                    postRepository.save(post);
                    log.info("Post updated: {}", postId);
                    return true;
                })
                .orElse(false);
    }

    @Override
    @Transactional
    public boolean softDelete(long postId) {
        return postRepository.findById(postId)
                .map(post -> {
                    post.softDelete();
                    postRepository.save(post);
                    log.info("Post soft-deleted: {}", postId);
                    return true;
                })
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Post> getDomain(long id) {
        return postRepository.findById(id)
                .filter(post -> !post.isDeleted());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PostDTO> getView(long id) {
        return postRepository.findById(id)
                .filter(post -> !post.isDeleted())
                .map(PostMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostDTO> list(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> postPage = postRepository.findByDeletedAtIsNull(pageable);

        return postPage.getContent().stream()
                .map(PostMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostDTO> search(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> postPage = postRepository.searchByFullText(keyword, pageable);

        return postPage.getContent().stream()
                .map(PostMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostDTO> listByAuthor(long authorId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> postPage = postRepository.findByAuthorId(authorId, pageable);

        return postPage.getContent().stream()
                .map(PostMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostDTO> searchByTag(String tag, int page, int size) {
        // TODO: Implement tag-based search
        log.warn("Tag-based search not yet implemented");
        return List.of();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostDTO> searchByAuthorName(String authorName, int page, int size) {
        // TODO: Implement author name search
        log.warn("Author name search not yet implemented");
        return List.of();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostDTO> searchCombined(String keyword, String tag, String authorName, String sortBy, int page,
            int size) {
        // TODO: Implement combined search with multiple filters
        log.warn("Combined search not yet implemented");
        return List.of();
    }
}

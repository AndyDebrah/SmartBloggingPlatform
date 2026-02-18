package com.smartblog.application.service.impl;

import java.util.Optional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartblog.application.service.PostService;
import com.smartblog.core.dto.PostDTO;
import com.smartblog.core.mapper.PostMapper;
import com.smartblog.core.model.Post;
import com.smartblog.core.model.User;
import com.smartblog.infrastructure.repository.jpa.PostJpaRepository;
import com.smartblog.infrastructure.repository.jpa.UserJpaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service implementation for Post business logic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostServiceImpl implements PostService {

    private final PostJpaRepository postRepository;
    private final UserJpaRepository userRepository;
    private final com.smartblog.infrastructure.repository.jpa.TagJpaRepository tagRepository;

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

    @Transactional
    @CacheEvict(value = "postsByAuthor", key = "#authorId")
    public long createDraft_evict(long authorId, String title, String content) {
        // Backward-compatible entry point: delegate to createDraft
        return createDraft(authorId, title, content);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "postView", key = "#postId"),
        @CacheEvict(value = "postsByAuthor", allEntries = true)
    })
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
    @Caching(evict = {
        @CacheEvict(value = "postView", key = "#postId"),
        @CacheEvict(value = "postsByAuthor", allEntries = true)
    })
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
    @Caching(evict = {
        @CacheEvict(value = "postView", key = "#postId"),
        @CacheEvict(value = "postsByAuthor", allEntries = true)
    })
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
    @Cacheable(value = "postView", key = "#id")
    public Optional<PostDTO> getView(long id) {
        return postRepository.findById(id)
                .filter(post -> !post.isDeleted())
                .map(PostMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostDTO> list(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> postPage = postRepository.findByDeletedAtIsNull(pageable);

        return postPage.map(PostMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostDTO> search(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> postPage;
        try {
            postPage = postRepository.searchByFullText(keyword, pageable);
        } catch (Exception ex) {
            postPage = postRepository.searchByTitleOrContent(keyword, pageable);
        }
        return postPage.map(PostMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "postsByAuthor", key = "#authorId + '-' + #page + '-' + #size")
    public Page<PostDTO> listByAuthor(long authorId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> postPage = postRepository.findByAuthorId(authorId, pageable);
        return postPage.map(PostMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostDTO> searchByTag(String tag, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return tagRepository.findByName(tag)
                .map(t -> postRepository.findByTagsContaining(t, pageable))
                .map(p -> p.map(PostMapper::toDTO))
                .orElseGet(() -> Page.empty());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostDTO> searchByAuthorName(String authorName, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> pageRes = postRepository.findByAuthorUsernameLike(authorName, pageable);
        return pageRes.map(PostMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostDTO> searchCombined(String keyword, String tag, String authorName, String sortBy, int page,
            int size) {
        // Prioritize keyword, then tag, then authorName — for complex combinations use Specifications
        if (keyword != null && !keyword.isBlank()) {
            return search(keyword, page, size);
        }
        if (tag != null && !tag.isBlank()) {
            return searchByTag(tag, page, size);
        }
        if (authorName != null && !authorName.isBlank()) {
            return searchByAuthorName(authorName, page, size);
        }
        return list(page, size);
    }
}

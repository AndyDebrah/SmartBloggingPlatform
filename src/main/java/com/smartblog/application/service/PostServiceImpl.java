package com.smartblog.application.service;

import java.util.List;
import java.util.Optional;

import com.smartblog.application.security.SecurityContext;
import com.smartblog.application.util.Perf;
import com.smartblog.core.dto.PostDTO;
import com.smartblog.core.exceptions.NotAuthorizedException;
import com.smartblog.core.exceptions.NotFoundException;
import com.smartblog.core.exceptions.ValidationException;
import com.smartblog.core.mapper.PostMapper;
import com.smartblog.core.model.Post;
import com.smartblog.core.model.User;
import com.smartblog.infrastructure.caching.CacheManager;
import com.smartblog.infrastructure.repository.api.PostRepository;
import com.smartblog.infrastructure.repository.api.TagRepository;
import com.smartblog.infrastructure.repository.api.UserRepository;

public class PostServiceImpl implements PostService {
    private final PostRepository posts;
    private final UserRepository users;
    private final TagRepository tags;

    public PostServiceImpl(PostRepository posts, UserRepository users, TagRepository tags) {
        this.posts = posts; this.users = users; this.tags = tags;
    }

    @Override
    public long createDraft(long authorId, String title, String content) {
        validateTitle(title); validateContent(content);
        users.findById(authorId).orElseThrow(() -> new NotFoundException("Author not found"));
        Post p = new Post();
        p.setAuthorId(authorId);
        p.setTitle(title);
        p.setContent(content);
        p.setPublished(false);
        long postId = posts.create(p);
        System.out.println("PostService.createDraft -> created id=" + postId + " title=" + title);
        p.setId(postId);
        CacheManager.postCache.put(postId, p);
        return postId;
    }

    @Override
    public boolean publish(long postId) {
        if (!SecurityContext.isAdmin()) {
            throw new NotAuthorizedException("Only admins may publish posts");
        }
        var p = posts.findById(postId).orElseThrow(() -> new NotFoundException("Post not found"));
        p.setPublished(true);
        boolean result = posts.update(p);
        System.out.println("PostService.publish -> postId=" + postId + " result=" + result);
        if (result) {
            CacheManager.postCache.put(postId, p);
        }
        return result;
    }

    @Override
    public boolean update(long postId, String title, String content, boolean published) {
        validateTitle(title); validateContent(content);
        var p = posts.findById(postId).orElseThrow(() -> new NotFoundException("Post not found"));

        User cur = SecurityContext.getUser();
        if (cur == null) throw new NotAuthorizedException("Authentication required");
        boolean owner = cur.getId() != null && cur.getId().equals(p.getAuthorId());
        if (!(SecurityContext.isAdmin() || owner)) {
            throw new NotAuthorizedException("Not allowed to update this post");
        }

        p.setTitle(title); p.setContent(content); p.setPublished(published);
        boolean result = posts.update(p);
        System.out.println("PostService.update -> postId=" + postId + " published=" + published + " result=" + result);
        if (result) {
            CacheManager.postCache.put(postId, p);
        }
        return result;
    }

    @Override
    public boolean softDelete(long postId) {
        var p = posts.findById(postId).orElseThrow(() -> new NotFoundException("Post not found"));
        User cur = SecurityContext.getUser();
        if (cur == null) throw new NotAuthorizedException("Authentication required");
        boolean owner = cur.getId() != null && cur.getId().equals(p.getAuthorId());
        if (!(SecurityContext.isAdmin() || owner)) {
            throw new NotAuthorizedException("Not allowed to delete this post");
        }
        boolean result = posts.softDelete(postId);
        if (result) {
            CacheManager.postCache.invalidate(postId);
        }
        return result;
    }

    @Override
    public Optional<Post> getDomain(long id) {
        var cached = CacheManager.postCache.getIfPresent(id);
        if (cached != null) {
            return Optional.of((Post) cached);
        }
        Optional<Post> post = posts.findById(id);
        post.ifPresent(p -> CacheManager.postCache.put(id, p));
        return post;
    }

    @Override
    public Optional<PostDTO> getView(long id) {
        var op = getDomain(id);
        if (op.isEmpty()) return Optional.empty();
        var p = op.get();
        var author = users.findById(p.getAuthorId()).orElse(null);
        var tagList = tags.listByPost(p.getId());
        return Optional.of(PostMapper.toDTO(p, author, tagList));
    }

    @Override
    public List<PostDTO> list(int page, int size) {
        return posts.list(page, size).stream()
                .map(p -> {
                    CacheManager.postCache.put(p.getId(), p);
                    return PostMapper.toDTO(p, users.findById(p.getAuthorId()).orElse(null), tags.listByPost(p.getId()));
                })
                .toList();
    }

    @Override
    public List<PostDTO> search(String keyword, int page, int size) {
        List<Post> results = Perf.measure("SearchPosts", () -> posts.search(keyword, page, size));
        return results.stream()
                .map(p -> {
                    CacheManager.postCache.put(p.getId(), p);
                    return PostMapper.toDTO(p, users.findById(p.getAuthorId()).orElse(null), tags.listByPost(p.getId()));
                })
                .toList();
    }

    @Override
    public List<PostDTO> listByAuthor(long authorId, int page, int size) {
        return posts.listByAuthor(authorId, page, size).stream()
                .map(p -> {
                    CacheManager.postCache.put(p.getId(), p);
                    return PostMapper.toDTO(p, users.findById(p.getAuthorId()).orElse(null), tags.listByPost(p.getId()));
                })
                .toList();
    }

    @Override
    public List<PostDTO> searchByAuthorName(String authorName, int page, int size) {
        List<Post> results = posts.searchByAuthorName(authorName, page, size);
        return results.stream()
                .map(p -> {
                    CacheManager.postCache.put(p.getId(), p);
                    return PostMapper.toDTO(p, users.findById(p.getAuthorId()).orElse(null), tags.listByPost(p.getId()));
                })
                .toList();
    }

    @Override
    public List<PostDTO> searchCombined(String keyword, String authorName, String tagName, String sortBy, int page, int size) {
        List<Post> results = posts.searchCombined(keyword, tagName, authorName, sortBy, page, size);
        return results.stream()
                .map(p -> {
                    CacheManager.postCache.put(p.getId(), p);
                    return PostMapper.toDTO(p, users.findById(p.getAuthorId()).orElse(null), tags.listByPost(p.getId()));
                })
                .toList();
    }

    @Override
    public List<PostDTO> searchByTag(String tagName, int page, int size) {
        List<Post> results = posts.searchByTag(tagName, page, size);
        return results.stream()
                .map(p -> {
                    CacheManager.postCache.put(p.getId(), p);
                    return PostMapper.toDTO(p, users.findById(p.getAuthorId()).orElse(null), tags.listByPost(p.getId()));
                })
                .toList();
    }

    private void validateTitle(String t) {
        if (t == null || t.isBlank()) {
            throw new ValidationException("Title required");
        }
        if (t.length() < 3 || t.length() > 255) {
            throw new ValidationException("Title length invalid");
        }
    }

    private void validateContent(String c) {
        if (c == null || c.isBlank()) {
            throw new ValidationException("Content required");
        }
        if (c.length() < 10) {
            throw new ValidationException("Content too short");
        }
    }
}
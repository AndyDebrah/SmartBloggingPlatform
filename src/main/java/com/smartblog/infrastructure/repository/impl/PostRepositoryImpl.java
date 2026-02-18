package com.smartblog.infrastructure.repository.impl;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.smartblog.core.model.Post;
import com.smartblog.core.model.Tag;
import com.smartblog.infrastructure.repository.api.PostRepository;
import com.smartblog.infrastructure.repository.jpa.PostJpaRepository;
import com.smartblog.infrastructure.repository.jpa.TagJpaRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PostRepositoryImpl implements PostRepository {

    private final PostJpaRepository postJpaRepository;
    private final TagJpaRepository tagJpaRepository;

    @Override
    public long create(Post p) {
        Post saved = postJpaRepository.save(p);
        return saved.getId();
    }

    @Override
    public Optional<Post> findById(long id) {
        return postJpaRepository.findById(id);
    }

    @Override
    public List<Post> list(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "createdAt"));
        return postJpaRepository.findAllPublished(pageable).getContent();
    }

    @Override
    public List<Post> search(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        try {
            return postJpaRepository.searchByFullText(keyword, pageable).getContent();
        } catch (Exception ex) {
            return postJpaRepository.searchByTitleOrContent(keyword, pageable).getContent();
        }
    }

    @Override
    public List<Post> listByAuthor(long authorId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "createdAt"));
        return postJpaRepository.findByAuthorId(authorId, pageable).getContent();
    }

    @Override
    public boolean update(Post p) {
        if (p.getId() == null) return false;
        if (!postJpaRepository.existsById(p.getId())) return false;
        postJpaRepository.save(p);
        return true;
    }

    @Override
    public boolean softDelete(long id) {
        Optional<Post> opt = postJpaRepository.findById(id);
        if (opt.isEmpty()) return false;
        Post p = opt.get();
        p.setDeletedAt(LocalDateTime.now());
        postJpaRepository.save(p);
        return true;
    }

    @Override
    public List<Post> searchByTag(String tagName, int page, int size) {
        Optional<Tag> tag = tagJpaRepository.findByName(tagName);
        if (tag.isEmpty()) return Collections.emptyList();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        return postJpaRepository.findByTagsContaining(tag.get(), pageable).getContent();
    }

    @Override
    public List<Post> searchByAuthorName(String authorName, int page, int size) {
        // Fallback: use a specification or a custom query higher in the stack. Here we use a simple approach.
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        return postJpaRepository.searchByTitleOrContent(authorName, pageable).getContent();
    }

    @Override
    public List<Post> searchCombined(String keyword, String tag, String authorName, String sortBy, int page, int size) {
        // Simple combined search: prioritize keyword, then tag. For complex filters use Specifications.
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

package com.smartblog.application.service.impl;

import com.smartblog.application.service.TagService;
import com.smartblog.core.dto.TagDTO;
import com.smartblog.core.mapper.TagMapper;
import com.smartblog.core.model.Post;
import com.smartblog.core.model.Tag;
import com.smartblog.infrastructure.repository.jpa.PostJpaRepository;
import com.smartblog.infrastructure.repository.jpa.TagJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service implementation for Tag business logic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TagServiceImpl implements TagService {

    private final TagJpaRepository tagRepository;
    private final PostJpaRepository postRepository;

    @Override
    @Transactional
    public long create(String name) {
        log.info("Creating tag: {}", name);

        // Generate slug from name (lowercase, replace spaces with hyphens)
        String slug = name.toLowerCase().replaceAll("\\s+", "-");

        Tag tag = Tag.builder()
                .name(name)
                .slug(slug)
                .build();

        Tag savedTag = tagRepository.save(tag);
        log.info("Tag created with ID: {}", savedTag.getId());

        return savedTag.getId();
    }

    @Override
    @Transactional
    public boolean rename(long tagId, String newName) {
        return tagRepository.findById(tagId)
                .map(tag -> {
                    tag.setName(newName);
                    tag.setSlug(newName.toLowerCase().replaceAll("\\s+", "-"));
                    tagRepository.save(tag);
                    log.info("Tag renamed: {}", tagId);
                    return true;
                })
                .orElse(false);
    }

    @Override
    @Transactional
    public boolean delete(long tagId) {
        return tagRepository.findById(tagId)
                .map(tag -> {
                    tagRepository.delete(tag);
                    log.info("Tag deleted: {}", tagId);
                    return true;
                })
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TagDTO> listAll() {
        return tagRepository.findAll().stream()
                .map(TagMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public boolean assignToPost(long postId, long tagId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new IllegalArgumentException("Tag not found"));

        post.getTags().add(tag);
        postRepository.save(post);
        log.info("Tag {} assigned to post {}", tagId, postId);
        return true;
    }

    @Override
    @Transactional
    public boolean removeFromPost(long postId, long tagId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new IllegalArgumentException("Tag not found"));

        post.getTags().remove(tag);
        postRepository.save(post);
        log.info("Tag {} removed from post {}", tagId, postId);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TagDTO> listForPost(long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        return post.getTags().stream()
                .map(TagMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TagDTO> list() {
        return listAll();
    }
}

package com.smartblog.application.service.impl;

import com.smartblog.application.service.CommentService;
import com.smartblog.core.dto.CommentDTO;
import com.smartblog.core.mapper.CommentMapper;
import com.smartblog.core.model.Comment;
import com.smartblog.core.model.Post;
import com.smartblog.core.model.User;
import com.smartblog.infrastructure.repository.jpa.CommentJpaRepository;
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

/**
 * Service implementation for Comment business logic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommentServiceImpl implements CommentService {

    private final CommentJpaRepository commentRepository;
    private final PostJpaRepository postRepository;
    private final UserJpaRepository userRepository;

    @Override
    @Transactional
    public long add(long postId, long userId, String content) {
        log.info("Adding comment to post ID: {} by user ID: {}", postId, userId);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found with ID: " + postId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        Comment comment = Comment.builder()
                .post(post)
                .user(user)
                .content(content)
                .build();

        Comment savedComment = commentRepository.save(comment);
        log.info("Comment created with ID: {}", savedComment.getId());

        return savedComment.getId();
    }

    @Override
    @Transactional
    public boolean edit(long commentId, String content) {
        return commentRepository.findById(commentId)
                .map(comment -> {
                    comment.setContent(content);
                    commentRepository.save(comment);
                    log.info("Comment edited: {}", commentId);
                    return true;
                })
                .orElse(false);
    }

    @Override
    @Transactional
    public boolean remove(long commentId) {
        return commentRepository.findById(commentId)
                .map(comment -> {
                    comment.softDelete();
                    commentRepository.save(comment);
                    log.info("Comment soft-deleted: {}", commentId);
                    return true;
                })
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentDTO> listForPost(long postId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> commentPage = commentRepository.findByPostIdAndDeletedAtIsNull(postId, pageable);

        return commentPage.getContent().stream()
                .map(CommentMapper::toDTO)
                .toList();
    }
}

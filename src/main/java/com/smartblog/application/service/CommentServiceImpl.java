package com.smartblog.application.service;

import java.time.LocalDateTime;
import java.util.List;

import com.smartblog.application.security.SecurityContext;
import com.smartblog.core.dto.CommentDTO;
import com.smartblog.core.exceptions.NotAuthorizedException;
import com.smartblog.core.exceptions.NotFoundException;
import com.smartblog.core.exceptions.ValidationException;
import com.smartblog.core.mapper.CommentMapper;
import com.smartblog.core.model.Comment;
import com.smartblog.core.model.User;
import com.smartblog.infrastructure.repository.api.CommentRepository;
import com.smartblog.infrastructure.repository.api.PostRepository;
import com.smartblog.infrastructure.repository.api.UserRepository;

public class CommentServiceImpl implements CommentService {
    private final CommentRepository comments;
    private final PostRepository posts;
    private final UserRepository users;

    public CommentServiceImpl(CommentRepository comments, PostRepository posts, UserRepository users) {
        this.comments = comments; this.posts = posts; this.users = users;
    }

    @Override
    public long add(long postId, long userId, String content) {
        if (content == null || content.isBlank()) throw new ValidationException("Comment cannot be empty");
        posts.findById(postId).orElseThrow(() -> new NotFoundException("Post not found"));
        users.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        Comment c = new Comment(null, postId, userId, content, LocalDateTime.now(), null);
        return comments.create(c);
    }

    @Override
    public boolean edit(long commentId, String content) {
        if (content == null || content.isBlank()) throw new ValidationException("Comment cannot be empty");
        var c = comments.findById(commentId).orElseThrow(() -> new NotFoundException("Comment not found"));
        User cur = SecurityContext.getUser();
        if (cur == null) throw new NotAuthorizedException("Authentication required");
        boolean owner = cur.getId() != null && cur.getId().longValue() == c.getUserId();
        if (!(SecurityContext.isAdmin() || owner)) {
            throw new NotAuthorizedException("Not allowed to edit this comment");
        }
        c.setContent(content);
        return comments.update(c);
    }

    @Override
    public boolean remove(long commentId) {
        var c = comments.findById(commentId).orElseThrow(() -> new NotFoundException("Comment not found"));
        User cur = SecurityContext.getUser();
        if (cur == null) throw new NotAuthorizedException("Authentication required");
        boolean owner = cur.getId() != null && cur.getId().longValue() == c.getUserId();
        if (!(SecurityContext.isAdmin() || owner)) {
            throw new NotAuthorizedException("Not allowed to remove this comment");
        }
        return comments.softDelete(commentId);
    }

    @Override
    public List<CommentDTO> listForPost(long postId, int page, int size) {
        return comments.listByPost(postId, page, size).stream()
                .map(c -> CommentMapper.toDTO(c, users.findById(c.getUserId()).orElse(null)))
                .toList();
    }
}
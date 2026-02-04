package com.smartblog.application.service;

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
import com.smartblog.infrastructure.repository.nosql.CommentRepositoryMongo;

public class CommentServiceImpl implements CommentService {
    private final CommentRepository comments;
    private final PostRepository posts;
    private final UserRepository users;
    private final CommentRepositoryMongo mongoComments;

    public CommentServiceImpl(CommentRepository comments, PostRepository posts, UserRepository users) {
        this(comments, posts, users, null);
    }

    public CommentServiceImpl(CommentRepository comments, PostRepository posts, UserRepository users,
                              CommentRepositoryMongo mongoComments) {
        this.comments = comments; this.posts = posts; this.users = users;
        this.mongoComments = mongoComments;
    }

    @Override
    public long add(long postId, long userId, String content) {
        if (content == null || content.isBlank()) throw new ValidationException("Comment cannot be empty");
        posts.findById(postId).orElseThrow(() -> new NotFoundException("Post not found"));
        users.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        Comment c = new Comment((int) postId, (int) userId, content);
        int createdId;
        try {
            createdId = (int) comments.create(c);
            System.out.println("[CommentService] MySQL create returned id=" + createdId);
        } catch (Exception ex) {
            System.err.println("[CommentService] ERROR creating comment in MySQL: " + ex.getMessage());
            ex.printStackTrace();
            throw ex;
        }
        c.setId(createdId);
        if (mongoComments != null) {
            try {
                mongoComments.save(c);
            } catch (Exception ex) {
                System.err.println("[CommentService] Warning: failed to write comment to MongoDB: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
        return (long) createdId;
    }

    @Override
    public boolean edit(long commentId, String content) {
        if (content == null || content.isBlank()) throw new ValidationException("Comment cannot be empty");
        var c = comments.findById(commentId).orElseThrow(() -> new NotFoundException("Comment not found"));
        User cur = SecurityContext.getUser();
        if (cur == null) throw new NotAuthorizedException("Authentication required");
        boolean owner = cur.getUserId() == c.getUserId();
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
        boolean owner = cur.getUserId() == c.getUserId();
        if (!(SecurityContext.isAdmin() || owner)) {
            throw new NotAuthorizedException("Not allowed to remove this comment");
        }
        return comments.softDelete(commentId);
    }

    @Override
    public List<CommentDTO> listForPost(long postId, int page, int size) {
        List<com.smartblog.core.model.Comment> models;
        if (mongoComments != null) {
            models = mongoComments.listByPost(postId, page, size);
        } else {
            models = comments.listByPost(postId, page, size);
        }
        return models.stream()
                .map(c -> CommentMapper.toDTO(c, users.findById(c.getUserId()).orElse(null)))
                .toList();
    }
}
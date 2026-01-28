
package com.smartblog.core.model;

import java.time.LocalDateTime;

/**
 * Represents a comment made on a post.
 * Real-world comment systems store:
 * - post reference
 * - user reference
 * - timestamps
 * - soft delete for moderation
 */
public class Comment {

    private int id;
    private int postId;
    private int userId;
    private String mongoId;
    private String content;

    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;

    public Comment() {}

    public Comment(int id, int postId, int userId, String content,
                   LocalDateTime createdAt, LocalDateTime deletedAt) {
        this.id = id;
        this.postId = postId;
        this.userId = userId;
        this.content = content;
        this.createdAt = createdAt;
        this.deletedAt = deletedAt;
    }

    public Comment(int postId, int userId, String content) {
        this(0, postId, userId, content, null, null);
    }

    public int getId() { return id; }

    public void setId(int id) { this.id = id; }

    public int getCommentId() { return id; }

    public void setCommentId(int commentId) { this.id = commentId; }

    public int getPostId() { return postId; }

    public void setPostId(int postId) { this.postId = postId; }

    public int getUserId() { return userId; }

    public void setUserId(int userId) { this.userId = userId; }

    public String getMongoId() { return mongoId; }

    public void setMongoId(String mongoId) { this.mongoId = mongoId; }

    public String getContent() { return content; }

    public void setContent(String content) { this.content = content; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getDeletedAt() { return deletedAt; }

    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
}

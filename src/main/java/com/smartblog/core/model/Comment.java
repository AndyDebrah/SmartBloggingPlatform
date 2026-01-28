
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

    private Long id;
    private Long postId;
    private Long userId;
    private String content;

    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;

    public Comment() {}

    public Comment(Long id, Long postId, Long userId, String content,
                   LocalDateTime createdAt, LocalDateTime deletedAt) {
        this.id = id;
        this.postId = postId;
        this.userId = userId;
        this.content = content;
        this.createdAt = createdAt;
        this.deletedAt = deletedAt;
    }

    public Comment(Long postId, Long userId, String content) {
        this(null, postId, userId, content, null, null);
    }

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public int getCommentId() { return id == null ? 0 : id.intValue(); }

    public void setCommentId(int commentId) { this.id = (long) commentId; }

    public int getPostId() { return postId == null ? 0 : postId.intValue(); }

    public void setPostId(int postId) { this.postId = (long) postId; }

    public void setPostId(Long postId) { this.postId = postId; }

    public int getUserId() { return userId == null ? 0 : userId.intValue(); }

    public void setUserId(int userId) { this.userId = (long) userId; }

    public String getContent() { return content; }

    public void setContent(String content) { this.content = content; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getDeletedAt() { return deletedAt; }

    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
}

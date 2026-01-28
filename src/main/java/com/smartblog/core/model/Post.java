
package com.smartblog.core.model;

import java.time.LocalDateTime;

/**
 * Represents an article in the blogging platform.
 * In real-world blogging systems, each post belongs to a user
 * and can have multiple tags and comments.
 */
public class Post {

    private Long id;
    private Long authorId;
    private String title;
    private String content;
    private boolean published;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public Post() {}

    public Post(Long id, Long authorId, String title, String content,
                boolean published, LocalDateTime createdAt,
                LocalDateTime updatedAt, LocalDateTime deletedAt) {
        this.id = id;
        this.authorId = authorId;
        this.title = title;
        this.content = content;
        this.published = published;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public Post(Long authorId, String title, String content) {
        this(null, authorId, title, content, false, null, null, null);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getPostId() {
        return id == null ? 0 : id.intValue();
    }

    public void setPostId(int postId) {
        this.id = (long) postId;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public int getUserId() {
        return authorId == null ? 0 : authorId.intValue();
    }

    public void setUserId(int userId) {
        this.authorId = (long) userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}

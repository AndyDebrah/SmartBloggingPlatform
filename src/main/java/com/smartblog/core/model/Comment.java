package com.smartblog.core.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

/**
 * JPA Entity representing a comment on a blog post.
 * <p>
 * Includes validation constraints for data integrity.
 * </p>
 *
 * <h3>Database Design:</h3>
 * <ul>
 *   <li>MySQL: Relational storage with foreign keys to posts and users</li>
 * </ul>
 *
 * <h3>Validation Rules:</h3>
 * <ul>
 *   <li>Content: Required, 1-5000 characters</li>
 *   <li>Post: Required foreign key reference</li>
 *   <li>User: Required foreign key reference</li>
 * </ul>
 */
@Entity
@Table(name = "comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"post", "user"})
public class Comment {

    /**
     * MySQL primary key (auto-increment).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * Reference to the post being commented on.
     * Many comments can belong to one post.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    @NotNull(message = "Post is required")
    private Post post;

    /**
     * Reference to the user who wrote the comment.
     * Many comments can belong to one user.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    private User user;

    /**
     * The actual comment text content.
     * Stored as TEXT in MySQL for large content support.
     */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "Comment content is required")
    @Size(min = 1, max = 5000, message = "Comment must be between 1 and 5000 characters")
    private String content;

    /**
     * Timestamp when comment was created.
     * Automatically set by Spring Data JPA auditing.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Soft delete timestamp.
     * If not null, comment is considered deleted but data is retained.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * Legacy constructor for backward compatibility with JDBC code.
     *
     * @param postId Post ID (int for legacy support)
     * @param userId User ID (int for legacy support)
     * @param content Comment text
     */
    public Comment(int postId, int userId, String content) {

    }

    /**
     * Checks if comment has been soft-deleted.
     *
     * @return true if deletedAt is not null
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Marks comment as deleted without removing from database.
     * Sets deletedAt timestamp to current time.
     */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * Restores a soft-deleted comment.
     * Clears the deletedAt timestamp.
     */
    public void restore() {
        this.deletedAt = null;
    }

    /**
     * Legacy getter for JDBC compatibility.
     *
     * @return Comment ID as int
     */
    public int getCommentId() {
        return id == null ? 0 : id.intValue();
    }

    /**
     * Legacy setter for JDBC compatibility.
     *
     * @param commentId Comment ID as int
     */
    public void setCommentId(int commentId) {
        this.id = (long) commentId;
    }

    /**
     * Legacy getter for JDBC compatibility.
     *
     * @return Post ID as int
     */
    public int getPostId() {
        return post != null && post.getId() != null ? post.getId().intValue() : 0;
    }

    /**
     * Legacy setter for JDBC compatibility.
     *
     * @param postId Post ID as int
     */
    public void setPostId(int postId) {
        if (post == null) {
            post = new Post();
        }
        post.setId((long) postId);
    }

    /**
     * Legacy getter for JDBC compatibility.
     *
     * @return User ID as int
     */
    public int getUserId() {
        return user != null && user.getId() != null ? user.getId().intValue() : 0;
    }

    /**
     * Legacy setter for JDBC compatibility.
     *
     * @param userId User ID as int
     */
    public void setUserId(int userId) {
        if (user == null) {
            user = new User();
        }
        user.setId((long) userId);
    }
}

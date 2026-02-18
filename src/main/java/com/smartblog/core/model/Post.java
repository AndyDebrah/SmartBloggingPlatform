package com.smartblog.core.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * JPA entity representing a blog post.
 * Validation is handled at the DTO layer, not at the entity level.
 *
 * Contains convenience methods for soft-delete, publish/unpublish and legacy
 * ID accessors used by DTO mappers.
 */
@Entity
@Table(name = "posts")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = { "author", "tags", "comments" })
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "published", nullable = false)
    @Builder.Default
    private boolean published = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ManyToMany(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinTable(name = "post_tags", joinColumns = @JoinColumn(name = "post_id"), inverseJoinColumns = @JoinColumn(name = "tag_id"))
    @Builder.Default
    private Set<Tag> tags = new HashSet<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Comment> comments = new HashSet<>();

    /**
     * Returns true if the post has been soft-deleted.
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Mark the post as soft-deleted by setting the deletion timestamp.
     */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * Restore a previously soft-deleted post.
     */
    public void restore() {
        this.deletedAt = null;
    }

    /**
     * Mark the post as published.
     */
    public void publish() {
        this.published = true;
    }

    /**
     * Mark the post as unpublished.
     */
    public void unpublish() {
        this.published = false;
    }

    /**
     * Associate a tag with this post (bidirectional).
     */
    public void addTag(Tag tag) {
        tags.add(tag);
        tag.getPosts().add(this);
    }

    /**
     * Remove an associated tag from this post (bidirectional).
     */
    public void removeTag(Tag tag) {
        tags.remove(tag);
        tag.getPosts().remove(this);
    }

    /**
     * Legacy integer accessor for the post id (for JDBC compatibility).
     */
    public int getPostId() {
        return id == null ? 0 : id.intValue();
    }

    /**
     * Legacy integer setter for the post id (for JDBC compatibility).
     */
    public void setPostId(int postId) {
        this.id = (long) postId;
    }

    /**
     * Get the author id if the author is present.
     */
    public Long getAuthorId() {
        return author != null ? author.getId() : null;
    }

    /**
     * Set the author id using a lightweight User placeholder. Prefer using
     * {@code setAuthor(User)} when working with JPA entities.
     */
    public void setAuthorId(Long authorId) {
        if (author == null) {
            author = new User();
        }
        author.setId(authorId);
    }

    /**
     * Legacy integer accessor for the author id (for JDBC compatibility).
     */
    public int getUserId() {
        Long authorId = getAuthorId();
        return authorId == null ? 0 : authorId.intValue();
    }

    /**
     * Legacy integer setter for the author id (for JDBC compatibility).
     */
    public void setUserId(int userId) {
        setAuthorId((long) userId);
    }
}

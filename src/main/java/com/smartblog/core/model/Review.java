package com.smartblog.core.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;


/**
 * JPA entity representing a user review for a post.
 *
 * Enforces a unique constraint per (post,user) and validates rating values
 * during persistence.
 */
@Entity
@Table(name = "reviews",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"post_id", "user_id"})
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"post", "user"})
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * Many-to-One relationship with Post
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    /**
     * Many-to-One relationship with User (reviewer)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Rating (1-5 stars)
     * Validation at application layer (Epic 3: @Min/@Max annotations)
     */
    @Column(name = "rating", nullable = false)
    private Integer rating;

    /**
     * Optional review text
     */
    @Column(name = "review_text", columnDefinition = "TEXT")
    private String reviewText;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public void restore() {
        this.deletedAt = null;
    }

    /**
     * Expose review text as `comment` for GraphQL compatibility.
     */
    public String getComment() {
        return reviewText;
    }

    /**
     * Accept `comment` value and store it as `reviewText` for compatibility
     * with GraphQL consumers.
     */
    public void setComment(String comment) {
        this.reviewText = comment;
    }

    /**
     * Validate rating before persisting/updating. Rating must be between 1-5.
     * @throws IllegalArgumentException when rating is invalid
     */
    @PrePersist
    @PreUpdate
    public void validateRating() {
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
    }
}

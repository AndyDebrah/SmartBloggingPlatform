package com.smartblog.core.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


@Entity
@Table(name = "posts", indexes = {
        @Index(name = "idx_author_id", columnList = "author_id"),
        @Index(name = "idx_created_at", columnList = "created_at"),
        @Index(name = "idx_published", columnList = "published")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"author", "tags", "comments"})
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    @NotNull(message = "Author is required")
    private User author;

    @Column(name = "title", nullable = false, length = 255)
    @NotBlank(message = "Title is required")
    @Size(min=3, max = 255, message = "Title must be at most 255 characters")
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "Content is required")
    @Size(min = 10,  message = "Content must be between 1 and 10000 characters")
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

    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "post_tags",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @Builder.Default
    private Set<Tag> tags = new HashSet<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Comment> comments = new HashSet<>();

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public void restore() {
        this.deletedAt = null;
    }

    public void publish() {
        this.published = true;
    }

    public void unpublish() {
        this.published = false;
    }


    public void addTag(Tag tag) {
        tags.add(tag);
        tag.getPosts().add(this);
    }

    public void removeTag(Tag tag) {
        tags.remove(tag);
        tag.getPosts().remove(this);
    }

    public int getPostId() {
        return id == null ? 0 : id.intValue();
    }

    public void setPostId(int postId) {
        this.id = (long) postId;
    }

    public Long getAuthorId() {
        return author != null ? author.getId() : null;
    }

    public void setAuthorId(Long authorId) {
        // For DTO mapping compatibility; prefer setAuthor(User) for JPA
        if (author == null) {
            author = new User();
        }
        author.setId(authorId);
    }

    public int getUserId() {
        Long authorId = getAuthorId();
        return authorId == null ? 0 : authorId.intValue();
    }

    public void setUserId(int userId) {
        setAuthorId((long) userId);
    }
}

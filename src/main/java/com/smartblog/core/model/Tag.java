package com.smartblog.core.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * EPIC 2: REST API DEVELOPMENT - TAG ENTITY
 * ═══════════════════════════════════════════════════════════════════════════
 * JPA Entity representing a tag/category for blog posts.
 *
 * <h2>Unique Constraints:</h2>
 * Tags must have unique names and slugs to prevent duplicates.
 * Both constraints are enforced at database level.
 *
 * <h2>Slug Strategy:</h2>
 * Slugs are URL-friendly versions of tag names:
 * - "Web Development" → "web-development"
 * - "Java Programming" → "java-programming"
 *
 * Used in REST API URLs: /api/tags/{slug}
 * Epic 2: RESTful resource naming convention
 *
 * <h2>Many-to-Many with Posts:</h2>
 * A tag can be assigned to multiple posts, and a post can have multiple tags.
 * The relationship is managed via the post_tags join table.
 * mappedBy="tags" indicates Post is the owning side of the relationship.
 */
@Entity
@Table(name = "tags", uniqueConstraints = {
        @UniqueConstraint(columnNames = "name"),
        @UniqueConstraint(columnNames = "slug")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"posts"})
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * Display name of the tag
     * Example: "Web Development", "Java"
     */
    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    /**
     * URL-friendly slug for REST API endpoints
     * Example: "web-development", "java"
     */
    @Column(name = "slug", nullable = false, unique = true, length = 100)
    private String slug;

    /**
     * Optional tag description
     */
    @Column(name = "description", length = 255)
    private String description;

    /**
     * Many-to-Many relationship with Post
     * mappedBy indicates Post entity owns the relationship
     */
    @ManyToMany(mappedBy = "tags", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Post> posts = new HashSet<>();


    /**
     * Generate slug from name
     * Called before persist/update via @PrePersist/@PreUpdate
     */
    @PrePersist
    @PreUpdate
    public void generateSlug() {
        if (slug == null || slug.isEmpty()) {
            this.slug = name.toLowerCase()
                    .replaceAll("\\s+", "-")
                    .replaceAll("[^a-z0-9-]", "")
                    .replaceAll("-+", "-");
        }
    }


    public int getTagId() {
        return id == null ? 0 : id.intValue();
    }

    public void setTagId(int tagId) {
        this.id = (long) tagId;
    }
}

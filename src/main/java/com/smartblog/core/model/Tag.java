
package com.smartblog.core.model;

/**
 * Represents a tag (category/label) assigned to posts.
 * Real-world systems use:
 * - name
 * - slug (URL-friendly name)
 * - unique constraints
 */
public class Tag {

    private Long id;
    private String name;
    private String slug;     // e.g. "web-dev", "javafx"

    public Tag() {}

    public Tag(Long id, String name, String slug) {
        this.id = id;
        this.name = name;
        this.slug = slug;
    }

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public int getTagId() { return id == null ? 0 : id.intValue(); }

    public void setTagId(int tagId) { this.id = (long) tagId; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }

    public void setSlug(String slug) { this.slug = slug; }
}

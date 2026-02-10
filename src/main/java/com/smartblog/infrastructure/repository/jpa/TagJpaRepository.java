package com.smartblog.infrastructure.repository.jpa;

import com.smartblog.core.model.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
 * EPIC 2: REST API DEVELOPMENT - TAG JPA REPOSITORY
 * â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
 * Spring Data JPA repository for Tag entity.
 *
 * <h2>RESTful Resource Access:</h2>
 * Tags can be accessed by slug in REST APIs:
 * GET /api/tags/{slug} â†’ findBySlug()
 *
 * Slugs are URL-friendly: "web-development", "java-programming"
 * More user-friendly than numeric IDs in URLs
 *
 * <h2>Tag Management:</h2>
 * - Create/update/delete tags (admin only)
 * - Assign tags to posts (authors)
 * - Browse posts by tag (readers)
 * - Tag cloud/statistics (Epic 5: Analytics)
 */
@Repository
public interface TagJpaRepository extends JpaRepository<Tag, Long> {

    /**
     * Find tag by slug (URL-friendly identifier)
     * Epic 2: RESTful resource naming
     *
     * @param slug Tag slug (e.g., "web-development")
     * @return Optional containing tag if found
     */
    Optional<Tag> findBySlug(String slug);

    /**
     * Find tag by name
     * Epic 3: Unique constraint validation
     *
     * @param name Tag name (e.g., "Web Development")
     * @return Optional containing tag if found
     */
    Optional<Tag> findByName(String name);

    /**
     * Check if tag with name exists
     * Epic 3: Validation before creating new tag
     *
     * @param name Tag name to check
     * @return true if tag exists
     */
    boolean existsByName(String name);

    /**
     * Check if tag with slug exists
     * Epic 3: Validation before creating new tag
     *
     * @param slug Tag slug to check
     * @return true if tag exists
     */
    boolean existsBySlug(String slug);

    /**
     * Search tags by name (case-insensitive partial match)
     * Epic 2: Tag search/autocomplete
     *
     * @param name Tag name pattern
     * @param pageable Pagination and sorting parameters
     * @return Page of matching tags
     */
    @Query("SELECT t FROM Tag t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Tag> searchByName(@Param("name") String name, Pageable pageable);

    /**
     * Find tags with post count (for tag cloud)
     * Epic 5: Analytics - popular tags
     *
     * @return List of tags with post counts
     */
    @Query("SELECT t, COUNT(p) as postCount FROM Tag t LEFT JOIN t.posts p " +
            "GROUP BY t.id " +
            "ORDER BY postCount DESC")
    List<Object[]> findTagsWithPostCount();

    /**
     * Find most used tags (top N)
     * Epic 2: "Popular Tags" feature
     *
     * @param pageable Pagination to limit results
     * @return Page of most used tags
     */
    @Query("SELECT t FROM Tag t LEFT JOIN t.posts p " +
            "GROUP BY t.id " +
            "ORDER BY COUNT(p) DESC")
    Page<Tag> findMostUsedTags(Pageable pageable);
}

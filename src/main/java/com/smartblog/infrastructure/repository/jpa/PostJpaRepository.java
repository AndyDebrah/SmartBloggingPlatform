package com.smartblog.infrastructure.repository.jpa;

import com.smartblog.core.model.Post;
import com.smartblog.core.model.Tag;
import com.smartblog.core.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
 * EPIC 2: REST API DEVELOPMENT - POST JPA REPOSITORY
 * â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
 * Spring Data JPA repository for Post entity with advanced querying.
 *
 * <h2>Full-Text Search Support (Epic 2: Efficient Searching):</h2>
 * The searchByFullText() method uses MySQL FULLTEXT index created by:
 * V3__fulltext_search_index.sql migration
 *
 * <pre>
 * CREATE FULLTEXT INDEX idx_posts_fulltext ON posts(title, content);
 * </pre>
 *
 * This enables efficient searching across post titles and content:
 * - Natural language mode for relevance ranking
 * - Much faster than LIKE '%keyword%' for large datasets
 * - Supports word stemming and stop words
 *
 * <h2>Performance Optimization:</h2>
 * - Indexes on author_id, created_at, published (V2__performance_indexes.sql)
 * - Lazy loading for relationships (author, tags, comments)
 * - Pageable support for large result sets
 * - @EntityGraph for optimized JOIN FETCH
 *
 * <h2>Query Methods by Category:</h2>
 * <ul>
 *   <li><b>Basic CRUD:</b> save(), findById(), delete()</li>
 *   <li><b>Filtering:</b> findByPublished(), findByAuthor(), findByTags()</li>
 *   <li><b>Searching:</b> searchByFullText(), searchByTitleOrContent()</li>
 *   <li><b>Pagination:</b> All methods accept Pageable parameter</li>
 *   <li><b>Analytics:</b> countByAuthor(), findTopRatedPosts()</li>
 * </ul>
 */
@Repository
public interface PostJpaRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {

    /**
     * Find all published (not soft-deleted) posts
     * Epic 2: Public blog view
     *
     * @param pageable Pagination and sorting parameters
     * @return Page of published posts
     */
    @Query("SELECT p FROM Post p WHERE p.published = true AND p.deletedAt IS NULL")
    Page<Post> findAllPublished(Pageable pageable);

    /**
     * Find all posts by author
     * Epic 2: Author's post management
     *
     * @param author User who authored the posts
     * @param pageable Pagination and sorting parameters
     * @return Page of posts by author
     */
    Page<Post> findByAuthor(User author, Pageable pageable);

    /**
     * Find published posts by author
     * Epic 2: Author's public profile
     *
     * @param author User who authored the posts
     * @param pageable Pagination and sorting parameters
     * @return Page of published posts by author
     */
    @Query("SELECT p FROM Post p WHERE p.author = :author AND p.published = true AND p.deletedAt IS NULL")
    Page<Post> findPublishedByAuthor(@Param("author") User author, Pageable pageable);

    /**
     * Find posts by tag
     * Epic 2: Browse posts by category
     *
     * @param tag Tag to filter by
     * @param pageable Pagination and sorting parameters
     * @return Page of posts with specified tag
     */
    @Query("SELECT p FROM Post p JOIN p.tags t WHERE t = :tag AND p.published = true AND p.deletedAt IS NULL")
    Page<Post> findByTagsContaining(@Param("tag") Tag tag, Pageable pageable);

    /**
     * Full-text search on post titles and content
     * Epic 2: Efficient searching with MySQL FULLTEXT index
     *
     * Uses MATCH...AGAINST for fast searching:
     * - Searches both title and content fields
     * - NATURAL LANGUAGE MODE for relevance ranking
     * - Requires FULLTEXT index from V3__fulltext_search_index.sql
     *
     * Performance: O(log n) vs O(n) for LIKE queries
     *
     * @param keyword Search keyword(s)
     * @param pageable Pagination and sorting parameters
     * @return Page of matching posts
     */
    @Query(value = "SELECT * FROM posts p " +
            "WHERE MATCH(p.title, p.content) AGAINST(:keyword IN NATURAL LANGUAGE MODE) " +
            "AND p.published = 1 AND p.deleted_at IS NULL",
            countQuery = "SELECT COUNT(*) FROM posts p " +
                    "WHERE MATCH(p.title, p.content) AGAINST(:keyword IN NATURAL LANGUAGE MODE) " +
                    "AND p.published = 1 AND p.deleted_at IS NULL",
            nativeQuery = true)
    Page<Post> searchByFullText(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Fallback search using LIKE (for non-FULLTEXT databases)
     * Epic 2: Alternative search implementation
     *
     * @param keyword Search keyword
     * @param pageable Pagination and sorting parameters
     * @return Page of matching posts
     */
    @Query("SELECT p FROM Post p WHERE (LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND p.published = true AND p.deletedAt IS NULL")
    Page<Post> searchByTitleOrContent(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Find posts created within date range
     * Epic 2: Date-based filtering
     *
     * @param startDate Start of date range
     * @param endDate End of date range
     * @param pageable Pagination and sorting parameters
     * @return Page of posts in date range
     */
    @Query("SELECT p FROM Post p WHERE p.createdAt BETWEEN :startDate AND :endDate " +
            "AND p.deletedAt IS NULL")
    Page<Post> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate,
                                      Pageable pageable);

    /**
     * Find all active (not soft-deleted) posts
     * Epic 2: Admin post management
     *
     * @param pageable Pagination and sorting parameters
     * @return Page of active posts
     */
    Page<Post> findByDeletedAtIsNull(Pageable pageable);

    /**
     * Count posts by author
     * Epic 5: Analytics - author productivity
     *
     * @param author User who authored the posts
     * @return Count of posts by author
     */
    long countByAuthor(User author);

    /**
     * Count published posts
     * Epic 5: Analytics - content metrics
     *
     * @return Count of published posts
     */
    long countByPublishedTrueAndDeletedAtIsNull();

    /**
     * Find recent posts (within last N days)
     * Epic 2: "New Posts" feature
     *
     * @param since Date to search from
     * @param pageable Pagination and sorting parameters
     * @return Page of recent posts
     */
    @Query("SELECT p FROM Post p WHERE p.createdAt >= :since " +
            "AND p.published = true AND p.deletedAt IS NULL " +
            "ORDER BY p.createdAt DESC")
    Page<Post> findRecentPosts(@Param("since") LocalDateTime since, Pageable pageable);

    /**
     * Find top-rated posts (with reviews)
     * Epic 2: "Top Posts" feature (requires Review entity)
     *
     * @param pageable Pagination and sorting parameters
     * @return Page of top-rated posts
     */
    @Query("SELECT p FROM Post p LEFT JOIN Review r ON r.post = p " +
            "WHERE p.published = true AND p.deletedAt IS NULL " +
            "GROUP BY p.id " +
            "ORDER BY AVG(r.rating) DESC, COUNT(r.id) DESC")
    Page<Post> findTopRatedPosts(Pageable pageable);
}

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

@Repository
public interface PostJpaRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {

        /**
         * Find all published (not soft-deleted) posts
         *
         * @param pageable Pagination and sorting parameters
         * @return Page of published posts
         */
        @Query("SELECT p FROM Post p WHERE p.published = true AND p.deletedAt IS NULL")
        Page<Post> findAllPublished(Pageable pageable);

        /**
         * Find all posts by author
         *
         * @param author   User who authored the posts
         * @param pageable Pagination and sorting parameters
         * @return Page of posts by author
         */
        Page<Post> findByAuthor(User author, Pageable pageable);

        /**
         * Find published posts by author
         *
         * @param author   User who authored the posts
         * @param pageable Pagination and sorting parameters
         * @return Page of published posts by author
         */
        @Query("SELECT p FROM Post p WHERE p.author = :author AND p.published = true AND p.deletedAt IS NULL")
        Page<Post> findPublishedByAuthor(@Param("author") User author, Pageable pageable);

        /**
         * Find posts by tag
         *
         * @param tag      Tag to filter by
         * @param pageable Pagination and sorting parameters
         * @return Page of posts with specified tag
         */
        @Query("SELECT p FROM Post p JOIN p.tags t WHERE t = :tag AND p.published = true AND p.deletedAt IS NULL")
        Page<Post> findByTagsContaining(@Param("tag") Tag tag, Pageable pageable);

        /**
         * Full-text search on post titles and content
         * 
         */
        @Query(value = "SELECT * FROM posts p " +
                        "WHERE MATCH(p.title, p.content) AGAINST(:keyword IN NATURAL LANGUAGE MODE) " +
                        "AND p.published = 1 AND p.deleted_at IS NULL", countQuery = "SELECT COUNT(*) FROM posts p " +
                                        "WHERE MATCH(p.title, p.content) AGAINST(:keyword IN NATURAL LANGUAGE MODE) " +
                                        "AND p.published = 1 AND p.deleted_at IS NULL", nativeQuery = true)
        Page<Post> searchByFullText(@Param("keyword") String keyword, Pageable pageable);

        /**
         * Fallback search using LIKE (for non-FULLTEXT databases)
         *
         * @param keyword  Search keyword
         * @param pageable Pagination and sorting parameters
         * @return Page of matching posts
         */
        @Query("SELECT p FROM Post p WHERE (LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "OR LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
                        "AND p.published = true AND p.deletedAt IS NULL")
        Page<Post> searchByTitleOrContent(@Param("keyword") String keyword, Pageable pageable);

        /**
         * Find posts created within date range
         *
         * @param startDate Start of date range
         * @param endDate   End of date range
         * @param pageable  Pagination and sorting parameters
         * @return Page of posts in date range
         */
        @Query("SELECT p FROM Post p WHERE p.createdAt BETWEEN :startDate AND :endDate " +
                        "AND p.deletedAt IS NULL")
        Page<Post> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate,
                        Pageable pageable);

        /**
         * Find all active (not soft-deleted) posts
         * 
         * @return Page of active posts
         */
        Page<Post> findByDeletedAtIsNull(Pageable pageable);

        /**
         * Count posts by author
         * 
         * @return Count of posts by author
         */
        long countByAuthor(User author);

        /**
         * Count published posts
         *
         * @return Count of published posts
         */
        long countByPublishedTrueAndDeletedAtIsNull();

        /**
         * Find recent posts (within last N days)
         * Epic 2: "New Posts" feature
         *
         * @param since    Date to search from
         * @param pageable Pagination and sorting parameters
         * @return Page of recent posts
         */
        @Query("SELECT p FROM Post p WHERE p.createdAt >= :since " +
                        "AND p.published = true AND p.deletedAt IS NULL " +
                        "ORDER BY p.createdAt DESC")
        Page<Post> findRecentPosts(@Param("since") LocalDateTime since, Pageable pageable);

        /**
         * Find top-rated posts (with reviews)
         *
         * @param pageable Pagination and sorting parameters
         * @return Page of top-rated posts
         */
        @Query("SELECT p FROM Post p LEFT JOIN Review r ON r.post = p " +
                        "WHERE p.published = true AND p.deletedAt IS NULL " +
                        "GROUP BY p.id " +
                        "ORDER BY AVG(r.rating) DESC, COUNT(r.id) DESC")
        Page<Post> findTopRatedPosts(Pageable pageable);

        /**
         * Find posts by author ID
         *
         * @param authorId ID of the author
         * @param pageable Pagination and sorting parameters
         * @return Page of posts by author
         */
        @Query("SELECT p FROM Post p WHERE p.author.id = :authorId AND p.deletedAt IS NULL")
        Page<Post> findByAuthorId(@Param("authorId") long authorId, Pageable pageable);
}

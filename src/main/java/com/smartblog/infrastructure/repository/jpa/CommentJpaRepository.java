package com.smartblog.infrastructure.repository.jpa;

import com.smartblog.core.model.Comment;
import com.smartblog.core.model.Post;
import com.smartblog.core.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
 * EPIC 2: REST API DEVELOPMENT - COMMENT JPA REPOSITORY (MySQL)
 * â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
 * Spring Data JPA repository for Comment entity (MySQL storage).
 *
 * <h2>Polyglot Persistence Note:</h2>
 * This repository handles MySQL storage. For MongoDB storage, see:
 * @see CommentMongoRepository
 *
 * The service layer decides which repository to use based on configuration.
 */
@Repository
public interface CommentJpaRepository extends JpaRepository<Comment, Long> {

    /**
     * Find all comments for a post
     * Epic 2: Display comment thread
     *
     * @param post Post to get comments for
     * @param pageable Pagination and sorting parameters
     * @return Page of comments
     */
    @Query("SELECT c FROM Comment c WHERE c.post = :post AND c.deletedAt IS NULL ORDER BY c.createdAt DESC")
    Page<Comment> findByPost(@Param("post") Post post, Pageable pageable);

    /**
     * Find all comments by user
     * Epic 2: User's comment history
     *
     * @param user User who made the comments
     * @param pageable Pagination and sorting parameters
     * @return Page of comments
     */
    @Query("SELECT c FROM Comment c WHERE c.user = :user AND c.deletedAt IS NULL ORDER BY c.createdAt DESC")
    Page<Comment> findByUser(@Param("user") User user, Pageable pageable);

    /**
     * Count comments for a post
     * Epic 5: Analytics - post engagement
     *
     * @param post Post to count comments for
     * @return Count of comments
     */
    long countByPostAndDeletedAtIsNull(Post post);

    /**
     * Count comments by user
     * Epic 5: Analytics - user engagement
     *
     * @param user User who made the comments
     * @return Count of comments
     */
    long countByUserAndDeletedAtIsNull(User user);

    /**
     * Find recent comments (within last N days)
     * Epic 2: "Recent Comments" feature
     *
     * @param since Date to search from
     * @param pageable Pagination and sorting parameters
     * @return Page of recent comments
     */
    @Query("SELECT c FROM Comment c WHERE c.createdAt >= :since AND c.deletedAt IS NULL ORDER BY c.createdAt DESC")
    Page<Comment> findRecentComments(@Param("since") LocalDateTime since, Pageable pageable);
}

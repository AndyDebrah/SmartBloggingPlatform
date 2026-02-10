package com.smartblog.infrastructure.repository.jpa;

import com.smartblog.core.model.Post;
import com.smartblog.core.model.Review;
import com.smartblog.core.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
 * EPIC 2: REST API DEVELOPMENT - REVIEW JPA REPOSITORY
 * â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
 * Spring Data JPA repository for Review entity.
 *
 * <h2>Review Business Rules:</h2>
 * - One review per user per post (unique constraint)
 * - Rating must be 1-5 stars
 * - Review text is optional
 *
 * <h2>Analytics Support:</h2>
 * - Calculate average rating per post
 * - Find top-rated posts
 * - User review history
 */
@Repository
public interface ReviewJpaRepository extends JpaRepository<Review, Long> {

    /**
     * Find review by post and user
     * Epic 2: Check if user already reviewed post
     *
     * @param post Post to check
     * @param user User to check
     * @return Optional containing review if found
     */
    Optional<Review> findByPostAndUser(Post post, User user);

    /**
     * Find all reviews for a post
     * Epic 2: Display post reviews
     *
     * @param post Post to get reviews for
     * @param pageable Pagination and sorting parameters
     * @return Page of reviews
     */
    @Query("SELECT r FROM Review r WHERE r.post = :post AND r.deletedAt IS NULL ORDER BY r.createdAt DESC")
    Page<Review> findByPost(@Param("post") Post post, Pageable pageable);

    /**
     * Find all reviews by user
     * Epic 2: User's review history
     *
     * @param user User who made the reviews
     * @param pageable Pagination and sorting parameters
     * @return Page of reviews
     */
    @Query("SELECT r FROM Review r WHERE r.user = :user AND r.deletedAt IS NULL ORDER BY r.createdAt DESC")
    Page<Review> findByUser(@Param("user") User user, Pageable pageable);

    /**
     * Calculate average rating for a post
     * Epic 5: Analytics - post rating
     *
     * @param post Post to calculate rating for
     * @return Average rating (null if no reviews)
     */
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.post = :post AND r.deletedAt IS NULL")
    Double calculateAverageRating(@Param("post") Post post);

    /**
     * Count reviews for a post
     * Epic 5: Analytics - review count
     *
     * @param post Post to count reviews for
     * @return Count of reviews
     */
    long countByPostAndDeletedAtIsNull(Post post);

    /**
     * Check if user already reviewed post
     * Epic 3: Validation before creating review
     *
     * @param post Post to check
     * @param user User to check
     * @return true if review exists
     */
    boolean existsByPostAndUser(Post post, User user);
}

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


@Repository
public interface ReviewJpaRepository extends JpaRepository<Review, Long> {

    /**
     * Find review by post and user

     */
    Optional<Review> findByPostAndUser(Post post, User user);

    /**
     * Find all reviews for a post

     * @return Page of reviews
     */
    @Query("SELECT r FROM Review r WHERE r.post = :post AND r.deletedAt IS NULL ORDER BY r.createdAt DESC")
    Page<Review> findByPost(@Param("post") Post post, Pageable pageable);

    /**
     * Find all reviews by user

     * @return Page of reviews
     */
    @Query("SELECT r FROM Review r WHERE r.user = :user AND r.deletedAt IS NULL ORDER BY r.createdAt DESC")
    Page<Review> findByUser(@Param("user") User user, Pageable pageable);

    /**

     * @return Average rating (null if no reviews)
     */
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.post = :post AND r.deletedAt IS NULL")
    Double calculateAverageRating(@Param("post") Post post);

    /**

     * @return Count of reviews
     */
    long countByPostAndDeletedAtIsNull(Post post);

    /**
     * Check if user already reviewed post

     * @return true if review exists
     */
    boolean existsByPostAndUser(Post post, User user);
}

package com.smartblog.application.service;

import com.smartblog.core.dto.ReviewDTO;
import org.springframework.data.domain.Page;

import java.util.Map;
import java.util.Optional;

/**
 * Service interface for Review business logic.
 * Handles review creation, retrieval, updates, and rating statistics.
 */
public interface ReviewService {

    /**
     * Retrieves all reviews for a specific post.
     *
     * @param postId ID of the post
     * @param page   Page number (0-indexed)
     * @param size   Number of reviews per page
     * @return Page of ReviewDTOs
     */
    Page<ReviewDTO> getReviewsByPost(Long postId, int page, int size);

    /**
     * Retrieves all reviews written by a specific user.
     *
     * @param userId ID of the user
     * @param page   Page number (0-indexed)
     * @param size   Number of reviews per page
     * @return Page of ReviewDTOs
     */
    Page<ReviewDTO> getReviewsByUser(Long userId, int page, int size);

    /**
     * Retrieves rating statistics for a post.
     * Includes average rating and total review count.
     *
     * @param postId ID of the post
     * @return Map containing averageRating and reviewCount
     */
    Map<String, Object> getPostRatingStats(Long postId);

    /**
     * Retrieves a single review by its ID.
     *
     * @param id Review ID
     * @return Optional containing the ReviewDTO if found
     */
    Optional<ReviewDTO> getReviewById(Long id);

    /**
     * Creates a new review for a post.
     *
     * @param postId     ID of the post being reviewed
     * @param userId     ID of the user creating the review
     * @param rating     Rating value (1-5)
     * @param reviewText Optional review text/comment
     * @return Created ReviewDTO
     * @throws IllegalArgumentException if rating is not between 1-5
     * @throws IllegalStateException    if user has already reviewed this post
     */
    ReviewDTO createReview(Long postId, Long userId, Integer rating, String reviewText);

    /**
     * Updates an existing review.
     *
     * @param reviewId   ID of the review to update
     * @param rating     New rating value (nullable)
     * @param reviewText New review text (nullable)
     * @return Updated ReviewDTO
     * @throws IllegalArgumentException if rating is not between 1-5
     */
    ReviewDTO updateReview(Long reviewId, Integer rating, String reviewText);

    /**
     * Soft deletes a review.
     *
     * @param reviewId ID of the review to delete
     */
    void deleteReview(Long reviewId);
}

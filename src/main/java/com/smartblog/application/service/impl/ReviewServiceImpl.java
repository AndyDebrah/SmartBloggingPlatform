package com.smartblog.application.service.impl;

import com.smartblog.application.service.ReviewService;
import com.smartblog.core.exceptions.NotFoundException;
import com.smartblog.core.model.Post;
import com.smartblog.core.model.Review;
import com.smartblog.core.model.User;
import com.smartblog.infrastructure.repository.jpa.PostJpaRepository;
import com.smartblog.infrastructure.repository.jpa.ReviewJpaRepository;
import com.smartblog.infrastructure.repository.jpa.UserJpaRepository;
import com.smartblog.core.dto.ReviewDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Service implementation for Review business logic.
 * Manages review creation, updates, and rating statistics.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    private final ReviewJpaRepository reviewRepository;
    private final PostJpaRepository postRepository;
    private final UserJpaRepository userRepository;
    @Value("${app.optimization.reviewStats.singleQuery:true}")
    private boolean singleQueryReviewStatsEnabled;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "reviewsByPost", key = "#postId + '-' + #page + '-' + #size", condition = "@optimizationToggle.cachingEnabled")
    public Page<ReviewDTO> getReviewsByPost(Long postId, int page, int size) {
        log.debug("Fetching reviews for post ID: {}", postId);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Post not found with id: " + postId));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Review> reviewPage = reviewRepository.findByPost(post, pageable);

        return reviewPage.map(this::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "reviewsByUser", key = "#userId + '-' + #page + '-' + #size", condition = "@optimizationToggle.cachingEnabled")
    public Page<ReviewDTO> getReviewsByUser(Long userId, int page, int size) {
        log.debug("Fetching reviews by user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Review> reviewPage = reviewRepository.findByUser(user, pageable);

        return reviewPage.map(this::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "reviewStatsByPost", key = "#postId", condition = "@optimizationToggle.cachingEnabled")
    public Map<String, Object> getPostRatingStats(Long postId) {
        log.debug("Calculating rating statistics for post ID: {}", postId);

        Double averageRating;
        long reviewCount;
        if (singleQueryReviewStatsEnabled) {
            var summary = reviewRepository.findPostRatingSummary(postId)
                    .orElseThrow(() -> new NotFoundException("Post not found with id: " + postId));
            averageRating = summary.getAverageRating();
            reviewCount = summary.getReviewCount();
        } else {
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new NotFoundException("Post not found with id: " + postId));
            averageRating = reviewRepository.calculateAverageRating(post);
            reviewCount = reviewRepository.countByPostAndDeletedAtIsNull(post);
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("postId", postId);
        stats.put("averageRating", averageRating != null ? Math.round(averageRating * 10.0) / 10.0 : 0.0);
        stats.put("reviewCount", reviewCount);

        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReviewDTO> getReviewById(Long id) {
        log.debug("Fetching review by ID: {}", id);
        return reviewRepository.findById(id)
                .map(this::toDTO);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "reviewStatsByPost", allEntries = true, condition = "@optimizationToggle.cachingEnabled"),
            @CacheEvict(value = "reviewsByPost", allEntries = true, condition = "@optimizationToggle.cachingEnabled"),
            @CacheEvict(value = "reviewsByUser", allEntries = true, condition = "@optimizationToggle.cachingEnabled")
    })
    public ReviewDTO createReview(Long postId, Long userId, Integer rating, String reviewText) {
        log.info("Creating review: postId={}, userId={}, rating={}", postId, userId, rating);

        // Validate rating
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        // Verify post exists
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Post not found with id: " + postId));

        // Verify user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));

        // Check for duplicate review
        if (reviewRepository.existsByPostAndUser(post, user)) {
            throw new IllegalStateException("You have already reviewed this post");
        }

        Review review = Review.builder()
                .post(post)
                .user(user)
                .rating(rating)
                .reviewText(reviewText)
                .build();

        Review savedReview = reviewRepository.save(review);
        log.info("Review created successfully with ID: {}", savedReview.getId());

        return toDTO(savedReview);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "reviewStatsByPost", allEntries = true, condition = "@optimizationToggle.cachingEnabled"),
            @CacheEvict(value = "reviewsByPost", allEntries = true, condition = "@optimizationToggle.cachingEnabled"),
            @CacheEvict(value = "reviewsByUser", allEntries = true, condition = "@optimizationToggle.cachingEnabled")
    })
    public ReviewDTO updateReview(Long reviewId, Integer rating, String reviewText) {
        log.info("Updating review ID: {}", reviewId);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review not found with id: " + reviewId));

        if (rating != null) {
            if (rating < 1 || rating > 5) {
                throw new IllegalArgumentException("Rating must be between 1 and 5");
            }
            review.setRating(rating);
        }

        if (reviewText != null) {
            review.setReviewText(reviewText);
        }

        Review updatedReview = reviewRepository.save(review);
        log.info("Review updated successfully");

        return toDTO(updatedReview);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "reviewStatsByPost", allEntries = true, condition = "@optimizationToggle.cachingEnabled"),
            @CacheEvict(value = "reviewsByPost", allEntries = true, condition = "@optimizationToggle.cachingEnabled"),
            @CacheEvict(value = "reviewsByUser", allEntries = true, condition = "@optimizationToggle.cachingEnabled")
    })
    public void deleteReview(Long reviewId) {
        log.info("Soft deleting review ID: {}", reviewId);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review not found with id: " + reviewId));

        review.softDelete();
        reviewRepository.save(review);

        log.info("Review soft deleted successfully");
    }

    /**
     * Converts Review entity to ReviewDTO.
     */
    private ReviewDTO toDTO(Review review) {
        return new ReviewDTO(
                review.getId(),
                review.getPost().getId(),
                review.getUser().getId(),
                review.getUser().getUsername(),
                review.getRating(),
                review.getReviewText());
    }
}

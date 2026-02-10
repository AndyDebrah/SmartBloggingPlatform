package com.smartblog.ui.controller;
import com.smartblog.core.dto.ApiResponse;
import com.smartblog.core.dto.PaginationMetadata;
import com.smartblog.core.model.Review;
import com.smartblog.infrastructure.repository.jpa.PostJpaRepository;
import com.smartblog.infrastructure.repository.jpa.ReviewJpaRepository;
import com.smartblog.infrastructure.repository.jpa.UserJpaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Review Management", description = "APIs for managing post reviews and ratings")
public class ReviewController {
    private final ReviewJpaRepository reviewRepository;
    private final PostJpaRepository postRepository;
    private final UserJpaRepository userRepository;

    @GetMapping("/post/{postId}")
    @Transactional(readOnly = true)
    @Operation(summary = "Get reviews by post", description = "Retrieve all reviews for a specific post")
    public ResponseEntity<ApiResponse<List<ReviewDTO>>> getReviewsByPost(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("GET /api/reviews/post/{}", postId);
        return postRepository.findById(postId)
                .map(post -> {
                    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
                    Page<Review> reviewPage = reviewRepository.findByPost(post, pageable);
                    List<ReviewDTO> reviewDTOs = reviewPage.getContent().stream()
                            .map(this::toDTO)
                            .toList();
                    return ResponseEntity.ok(
                            ApiResponse.success("Reviews retrieved successfully", reviewDTOs,
                                    PaginationMetadata.from(reviewPage))
                    );
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.notFound("Post not found with id: " + postId)));
    }


    @GetMapping("/user/{userId}")
    @Transactional(readOnly = true)
    @Operation(summary = "Get reviews by user", description = "Retrieve all reviews by a specific user")
    public ResponseEntity<ApiResponse<List<ReviewDTO>>> getReviewsByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("GET /api/reviews/user/{}", userId);
        return userRepository.findById(userId)
                .map(user -> {
                    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
                    Page<Review> reviewPage = reviewRepository.findByUser(user, pageable);
                    List<ReviewDTO> reviewDTOs = reviewPage.getContent().stream()
                            .map(this::toDTO)
                            .toList();
                    return ResponseEntity.ok(
                            ApiResponse.success("Reviews retrieved successfully", reviewDTOs,
                                    PaginationMetadata.from(reviewPage))
                    );
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.notFound("User not found with id: " + userId)));
    }

    @GetMapping("/post/{postId}/stats")
    @Transactional(readOnly = true)
    @Operation(summary = "Get post rating statistics", description = "Get average rating and review count for a post")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPostRatingStats(@PathVariable Long postId) {
        log.info("GET /api/reviews/post/{}/stats", postId);
        return postRepository.findById(postId)
                .map(post -> {
                    Double averageRating = reviewRepository.calculateAverageRating(post);
                    long reviewCount = reviewRepository.countByPostAndDeletedAtIsNull(post);
                    Map<String, Object> stats = new HashMap<>();
                    stats.put("postId", postId);
                    stats.put("averageRating", averageRating != null ? Math.round(averageRating * 10.0) / 10.0 : 0.0);
                    stats.put("reviewCount", reviewCount);
                    return ResponseEntity.ok(
                            ApiResponse.success("Rating statistics retrieved", stats)
                    );
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.notFound("Post not found with id: " + postId)));
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    @Operation(summary = "Get review by ID", description = "Retrieve a single review by ID")
    public ResponseEntity<ApiResponse<ReviewDTO>> getReviewById(@PathVariable Long id) {
        log.info("GET /api/reviews/{}", id);
        return reviewRepository.findById(id)
                .map(review -> ResponseEntity.ok(
                        ApiResponse.success("Review found", toDTO(review))
                ))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.notFound("Review not found with id: " + id)));
    }

    @PostMapping    @Transactional    @Operation(summary = "Create review", description = "Add a new review/rating for a post")
    public ResponseEntity<ApiResponse<ReviewDTO>> createReview(@RequestBody CreateReviewRequest request) {
        log.info("POST /api/reviews - postId={}, userId={}, rating={}",
                request.postId(), request.userId(), request.rating());
        // Validate rating (1-5)
        if (request.rating() < 1 || request.rating() > 5) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Rating must be between 1 and 5"));
        }
        var postOpt = postRepository.findById(request.postId());
        var userOpt = userRepository.findById(request.userId());
        if (postOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound("Post not found with id: " + request.postId()));
        }
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound("User not found with id: " + request.userId()));
        }
        // Check if user already reviewed this post
        if (reviewRepository.existsByPostAndUser(postOpt.get(), userOpt.get())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(HttpStatus.CONFLICT, "You have already reviewed this post"));
        }
        Review review = Review.builder()
                .post(postOpt.get())
                .user(userOpt.get())
                .rating(request.rating())
                .reviewText(request.reviewText())
                .build();
        Review savedReview = reviewRepository.save(review);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Review created successfully", toDTO(savedReview)));
    }

    @PutMapping("/{id}")
    @Transactional
    @Operation(summary = "Update review", description = "Update an existing review")
    public ResponseEntity<ApiResponse<ReviewDTO>> updateReview(
            @PathVariable Long id,
            @RequestBody UpdateReviewRequest request
    ) {
        log.info("PUT /api/reviews/{}", id);
        return reviewRepository.findById(id)
                .map(review -> {
                    if (request.rating() != null) {
                        if (request.rating() < 1 || request.rating() > 5) {
                            throw new IllegalArgumentException("Rating must be between 1 and 5");
                        }
                        review.setRating(request.rating());
                    }
                    if (request.reviewText() != null) {
                        review.setReviewText(request.reviewText());
                    }
                    Review updatedReview = reviewRepository.save(review);
                    return ResponseEntity.ok(
                            ApiResponse.success("Review updated successfully", toDTO(updatedReview))
                    );
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.notFound("Review not found with id: " + id)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete review", description = "Soft delete a review")
    public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable Long id) {
        log.info("DELETE /api/reviews/{}", id);
        return reviewRepository.findById(id)
                .map(review -> {
                    review.softDelete();
                    reviewRepository.save(review);
                    return ResponseEntity.ok(
                            ApiResponse.<Void>success("Review deleted successfully")
                    );
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.notFound("Review not found with id: " + id)));
    }
    private ReviewDTO toDTO(Review review) {
        return new ReviewDTO(
                review.getId(),
                review.getPost().getId(),
                review.getUser().getId(),
                review.getUser().getUsername(),
                review.getRating(),
                review.getReviewText()
        );
    }
    public record ReviewDTO(
            Long id,
            Long postId,
            Long userId,
            String username,
            Integer rating,
            String reviewText
    ) {}
    public record CreateReviewRequest(
            Long postId,
            Long userId,
            Integer rating,
            String reviewText
    ) {}
    public record UpdateReviewRequest(
            Integer rating,
            String reviewText
    ) {}
}

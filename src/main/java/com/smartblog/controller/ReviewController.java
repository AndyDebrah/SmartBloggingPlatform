package com.smartblog.controller;

import com.smartblog.application.service.ReviewService;
import com.smartblog.core.dto.ApiResponse;
import com.smartblog.core.dto.PaginationMetadata;
import com.smartblog.core.dto.ReviewDTO;
import com.smartblog.core.exceptions.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Review Management", description = "APIs for managing post reviews and ratings")
public class ReviewController {
        private final ReviewService reviewService;

        @GetMapping("/post/{postId}")
        @Operation(summary = "Get reviews by post", description = "Retrieve all reviews for a specific post")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Reviews retrieved successfully", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Post not found", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
        })
        public ResponseEntity<ApiResponse<List<ReviewDTO>>> getReviewsByPost(
                        @PathVariable Long postId,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size) {
                log.info("GET /api/reviews/post/{}", postId);
                try {
                        Page<ReviewDTO> reviewPage = reviewService.getReviewsByPost(postId, page, size);
                        return ResponseEntity.ok(
                                        ApiResponse.success("Reviews retrieved successfully", reviewPage.getContent(),
                                                        PaginationMetadata.from(reviewPage)));
                } catch (NotFoundException e) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .body(ApiResponse.notFound(e.getMessage()));
                }
        }

        @GetMapping("/user/{userId}")
        @Operation(summary = "Get reviews by user", description = "Retrieve all reviews by a specific user")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Reviews retrieved successfully", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
        })
        public ResponseEntity<ApiResponse<List<ReviewDTO>>> getReviewsByUser(
                        @PathVariable Long userId,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size) {
                log.info("GET /api/reviews/user/{}", userId);
                try {
                        Page<ReviewDTO> reviewPage = reviewService.getReviewsByUser(userId, page, size);
                        return ResponseEntity.ok(
                                        ApiResponse.success("Reviews retrieved successfully", reviewPage.getContent(),
                                                        PaginationMetadata.from(reviewPage)));
                } catch (NotFoundException e) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .body(ApiResponse.notFound(e.getMessage()));
                }
        }

        @GetMapping("/post/{postId}/stats")
        @Operation(summary = "Get post rating statistics", description = "Get average rating and review count for a post")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Statistics retrieved successfully", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Post not found", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
        })
        public ResponseEntity<ApiResponse<Map<String, Object>>> getPostRatingStats(@PathVariable Long postId) {
                log.info("GET /api/reviews/post/{}/stats", postId);
                try {
                        Map<String, Object> stats = reviewService.getPostRatingStats(postId);
                        return ResponseEntity.ok(
                                        ApiResponse.success("Rating statistics retrieved", stats));
                } catch (NotFoundException e) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .body(ApiResponse.notFound(e.getMessage()));
                }
        }

        @GetMapping("/{id}")
        @Operation(summary = "Get review by ID", description = "Retrieve a single review by ID")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Review found", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Review not found", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
        })
        public ResponseEntity<ApiResponse<ReviewDTO>> getReviewById(@PathVariable Long id) {
                log.info("GET /api/reviews/{}", id);
                return reviewService.getReviewById(id)
                                .map(reviewDTO -> ResponseEntity.ok(
                                                ApiResponse.success("Review found", reviewDTO)))
                                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                                                .body(ApiResponse.notFound("Review not found with id: " + id)));
        }

        @PostMapping
        @Operation(summary = "Create review", description = "Add a new review/rating for a post")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Review created successfully", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input or rating out of range", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Post or user not found", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "User has already reviewed this post", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
        })
        public ResponseEntity<ApiResponse<ReviewDTO>> createReview(@RequestBody CreateReviewRequest request) {
                log.info("POST /api/reviews - postId={}, userId={}, rating={}",
                                request.postId(), request.userId(), request.rating());
                try {
                        ReviewDTO reviewDTO = reviewService.createReview(
                                        request.postId(), request.userId(), request.rating(), request.reviewText());
                        return ResponseEntity.status(HttpStatus.CREATED)
                                        .body(ApiResponse.created("Review created successfully", reviewDTO));
                } catch (IllegalArgumentException e) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .body(ApiResponse.error(e.getMessage()));
                } catch (IllegalStateException e) {
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                        .body(ApiResponse.error(HttpStatus.CONFLICT, e.getMessage()));
                } catch (NotFoundException e) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .body(ApiResponse.notFound(e.getMessage()));
                }
        }

        @PutMapping("/{id}")
        @Operation(summary = "Update review", description = "Update an existing review")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Review updated successfully", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input or rating out of range", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Review not found", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
        })
        public ResponseEntity<ApiResponse<ReviewDTO>> updateReview(
                        @PathVariable Long id,
                        @RequestBody UpdateReviewRequest request) {
                log.info("PUT /api/reviews/{}", id);
                try {
                        ReviewDTO reviewDTO = reviewService.updateReview(id, request.rating(), request.reviewText());
                        return ResponseEntity.ok(
                                        ApiResponse.success("Review updated successfully", reviewDTO));
                } catch (IllegalArgumentException e) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .body(ApiResponse.error(e.getMessage()));
                } catch (NotFoundException e) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .body(ApiResponse.notFound(e.getMessage()));
                }
        }

        @DeleteMapping("/{id}")
        @Operation(summary = "Delete review", description = "Soft delete a review")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Review deleted successfully", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Review not found", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
        })
        public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable Long id) {
                log.info("DELETE /api/reviews/{}", id);
                try {
                        reviewService.deleteReview(id);
                        return ResponseEntity.ok(
                                        ApiResponse.<Void>success("Review deleted successfully"));
                } catch (NotFoundException e) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .body(ApiResponse.notFound(e.getMessage()));
                }
        }

        public record CreateReviewRequest(
                        Long postId,
                        Long userId,
                        Integer rating,
                        String reviewText) {
        }

        public record UpdateReviewRequest(
                        Integer rating,
                        String reviewText) {
        }
}

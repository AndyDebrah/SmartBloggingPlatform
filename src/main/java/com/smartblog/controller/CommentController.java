package com.smartblog.controller;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartblog.application.service.CommentService;
import com.smartblog.core.dto.ApiResponse;
import com.smartblog.core.dto.CommentDTO;
import com.smartblog.core.dto.PaginationMetadata;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST API endpoints for Comment management.
 * Base URL: /api/comments
 * 
 * REFACTORED: Now uses Service Layer pattern (CommentService) instead of direct repository access.
 */
@RestController
@RequestMapping("/api/comments")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Comment Management", description = "APIs for managing comments")
public class CommentController {
    private final CommentService commentService;

    /**
     * Get comments for a specific post.
     */
    @GetMapping("/post/{postId}")
    @Operation(summary = "Get comments by post", description = "Retrieve all comments for a specific post")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Comments retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Post not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
        public ResponseEntity<ApiResponse<List<CommentDTO>>> getCommentsByPost(
            @Parameter(description = "Post ID")
            @PathVariable Long postId,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("GET /api/comments/post/{}", postId);
        var comments = commentService.listForPost(postId, page, size);
        return ResponseEntity.ok(
                ApiResponse.success("Comments retrieved successfully", comments.getContent(), PaginationMetadata.from(comments))
        );
    }







    /**
     * Create a new comment.
     */
    @PostMapping
    @Operation(summary = "Create comment", description = "Add a new comment to a post")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Comment created successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Post or user not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<CommentDTO>> createComment(
            @Valid @RequestBody CreateCommentRequest request
    ) {
        log.info("POST /api/comments - postId={}, userId={}", request.postId(), request.userId());
        
        long commentId = commentService.add(
                request.postId(),
                request.userId(),
                request.content()
        );
        
        // For simplicity, return success without fetching the created comment
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Comment created successfully", null));
    }

    /**
     * Update an existing comment.
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update comment", description = "Update an existing comment")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Comment updated successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Comment not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<CommentDTO>> updateComment(
            @Parameter(description = "Comment ID")
            @PathVariable Long id,
            @RequestBody UpdateCommentRequest request
    ) {
        log.info("PUT /api/comments/{}", id);
        
        boolean updated = commentService.edit(id, request.content());
        if (!updated) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound("Comment not found with id: " + id));
        }
        
        return ResponseEntity.ok(
                ApiResponse.success("Comment updated successfully", null)
        );
    }

    /**
     * Soft delete a comment.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete comment", description = "Soft delete a comment")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Comment deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Comment not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @Parameter(description = "Comment ID")
            @PathVariable Long id
    ) {
        log.info("DELETE /api/comments/{}", id);
        
        boolean deleted = commentService.remove(id);
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound("Comment not found with id: " + id));
        }
        
        return ResponseEntity.ok(
                ApiResponse.<Void>success("Comment deleted successfully")
        );
    }

    // Request DTOs
    public record CreateCommentRequest(
            Long postId,
            Long userId,
            String content
    ) {}

    public record UpdateCommentRequest(
            String content
    ) {}
}

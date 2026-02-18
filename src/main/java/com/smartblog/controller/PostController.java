package com.smartblog.controller;
import com.smartblog.application.service.PostService;
import com.smartblog.core.dto.ApiResponse;
import com.smartblog.core.dto.PostDTO;
import com.smartblog.core.dto.request.PostCreateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * REST API endpoints for Post management.
 * Base URL: /api/posts
 * 
 * REFACTORED: Now uses Service Layer pattern (PostService) instead of direct repository access.
 */
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Post Management", description = "APIs for managing blog posts")
public class PostController {
    private final PostService postService;

    /**
     * Get all posts with pagination.
     */
    @GetMapping
    @Operation(summary = "Get all posts", description = "Retrieve paginated list of posts")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Posts retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<List<PostDTO>>> getAllPosts(
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("GET /api/posts - page={}, size={}", page, size);
        List<PostDTO> posts = postService.list(page, size);
        return ResponseEntity.ok(
                ApiResponse.success("Posts retrieved successfully", posts)
        );
    }

    /**
     * Search posts by keyword.
     */
    @GetMapping("/search")
    @Operation(summary = "Search posts", description = "Full-text search on post titles and content")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Search results retrieved",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid search query"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<List<PostDTO>>> searchPosts(
            @Parameter(description = "Search keyword(s)", required = true)
            @RequestParam String q,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("GET /api/posts/search?q={}", q);
        List<PostDTO> posts = postService.search(q, page, size);
        return ResponseEntity.ok(
                ApiResponse.success("Search results for: " + q, posts)
        );
    }

    /**
     * Get a post by ID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get post by ID", description = "Retrieve a single post by ID")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Post found",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Post not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<PostDTO>> getPostById(
            @Parameter(description = "Post ID")
            @PathVariable Long id
    ) {
        log.info("GET /api/posts/{}", id);
        return postService.getView(id)
                .map(postDTO -> ResponseEntity.ok(
                        ApiResponse.success("Post found", postDTO)
                ))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.notFound("Post not found with id: " + id)));
    }

    /**
     * Get posts by author.
     */
    @GetMapping("/author/{authorId}")
    @Operation(summary = "Get posts by author", description = "Retrieve all posts by a specific author")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Posts retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Author not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<List<PostDTO>>> getPostsByAuthor(
            @Parameter(description = "Author ID")
            @PathVariable Long authorId,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("GET /api/posts/author/{}", authorId);
        List<PostDTO> posts = postService.listByAuthor(authorId, page, size);
        return ResponseEntity.ok(
                ApiResponse.success("Posts by author retrieved", posts)
        );
    }



    /**
     * Create a new post.
     */
    @PostMapping
    @Operation(summary = "Create post", description = "Create a new blog post")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Post created successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Author not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<PostDTO>> createPost(
            @Valid @RequestBody PostCreateRequest request
    ) {
        log.info("POST /api/posts - title={}", request.title());
        
        long postId = postService.createDraft(
                request.authorId(),
                request.title(),
                request.content()
        );
        
        PostDTO createdPost = postService.getView(postId)
                .orElseThrow(() -> new RuntimeException("Post creation failed"));
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Post created successfully", createdPost));
    }

    /**
     * Update an existing post.
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update post", description = "Update an existing post")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Post updated successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Post not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<PostDTO>> updatePost(
            @Parameter(description = "Post ID")
            @PathVariable Long id,
            @Valid @RequestBody PostCreateRequest request
    ) {
        log.info("PUT /api/posts/{}", id);
        
        boolean updated = postService.update(
                id,
                request.title(),
                request.content(),
                request.published()
        );
        
        if (!updated) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound("Post not found with id: " + id));
        }
        
        PostDTO updatedPost = postService.getView(id)
                .orElseThrow(() -> new RuntimeException("Post update failed"));
        
        return ResponseEntity.ok(
                ApiResponse.success("Post updated successfully", updatedPost)
        );
    }

    /**
     * Soft delete a post.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete post", description = "Soft delete a post")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Post deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Post not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @Parameter(description = "Post ID")
            @PathVariable Long id
    ) {
        log.info("DELETE /api/posts/{}", id);
        
        boolean deleted = postService.softDelete(id);
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound("Post not found with id: " + id));
        }
        
        return ResponseEntity.ok(
                ApiResponse.<Void>success("Post deleted successfully")
        );
    }
}

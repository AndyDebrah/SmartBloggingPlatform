package com.smartblog.ui.controller;
import com.smartblog.core.dto.ApiResponse;
import com.smartblog.core.dto.CommentDTO;
import com.smartblog.core.dto.PaginationMetadata;
import com.smartblog.core.model.Comment;
import com.smartblog.infrastructure.repository.jpa.CommentJpaRepository;
import com.smartblog.infrastructure.repository.jpa.PostJpaRepository;
import com.smartblog.infrastructure.repository.jpa.UserJpaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/comments")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Comment Management", description = "APIs for managing comments")
public class CommentController {
    private final CommentJpaRepository commentJpaRepository;
    private final PostJpaRepository postRepository;
    private final UserJpaRepository userRepository;

    @GetMapping("/post/{postId}")
    @Transactional(readOnly = true)
    @Operation(summary = "Get comments by post", description = "Retrieve all comments for a specific post")
    public ResponseEntity<ApiResponse<List<CommentDTO>>> getCommentsByPost(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("GET /api/comments/post/{}", postId);
        return postRepository.findById(postId)
                .map(post -> {
                    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
                    Page<Comment> commentPage = commentJpaRepository.findByPost(post, pageable);
                    List<CommentDTO> commentDTOs = commentPage.getContent().stream()
                            .map(this::toDTO)
                            .toList();
                    PaginationMetadata pagination = PaginationMetadata.from(commentPage);
                    return ResponseEntity.ok(
                            ApiResponse.success("Comments retrieved successfully", commentDTOs, pagination)
                    );
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.notFound("Post not found with id: " + postId)));
    }

    @GetMapping("/user/{userId}")
    @Transactional(readOnly = true)
    @Operation(summary = "Get comments by user", description = "Retrieve all comments by a specific user")
    public ResponseEntity<ApiResponse<List<CommentDTO>>> getCommentsByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("GET /api/comments/user/{}", userId);
        return userRepository.findById(userId)
                .map(user -> {
                    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
                    Page<Comment> commentPage = commentJpaRepository.findByUser(user, pageable);
                    List<CommentDTO> commentDTOs = commentPage.getContent().stream()
                            .map(this::toDTO)
                            .toList();
                    PaginationMetadata pagination = PaginationMetadata.from(commentPage);
                    return ResponseEntity.ok(
                            ApiResponse.success("Comments retrieved successfully", commentDTOs, pagination)
                    );
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.notFound("User not found with id: " + userId)));
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    @Operation(summary = "Get comment by ID", description = "Retrieve a single comment by ID")
    public ResponseEntity<ApiResponse<CommentDTO>> getCommentById(@PathVariable Long id) {
        log.info("GET /api/comments/{}", id);
        return commentJpaRepository.findById(id)
                .map(comment -> ResponseEntity.ok(
                        ApiResponse.success("Comment found", toDTO(comment))
                ))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.notFound("Comment not found with id: " + id)));
    }

    @GetMapping("/recent")
    @Transactional(readOnly = true)
    @Operation(summary = "Get recent comments", description = "Get comments from last N days")
    public ResponseEntity<ApiResponse<List<CommentDTO>>> getRecentComments(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("GET /api/comments/recent?days={}", days);
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> commentPage = commentJpaRepository.findRecentComments(since, pageable);
        List<CommentDTO> commentDTOs = commentPage.getContent().stream()
                .map(this::toDTO)
                .toList();
        PaginationMetadata pagination = PaginationMetadata.from(commentPage);
        return ResponseEntity.ok(
                ApiResponse.success("Recent comments (last " + days + " days)", commentDTOs, pagination)
        );
    }

    @PostMapping
    @Transactional
    @Operation(summary = "Create comment", description = "Add a new comment to a post")
    public ResponseEntity<ApiResponse<CommentDTO>> createComment(@Valid @RequestBody CreateCommentRequest request) {
        log.info("POST /api/comments - postId={}, userId={}", request.postId(), request.userId());
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
        Comment comment = Comment.builder()
                .post(postOpt.get())
                .user(userOpt.get())
                .content(request.content())
                .createdAt(LocalDateTime.now())
                .build();
        Comment savedComment = commentJpaRepository.save(comment);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Comment created successfully", toDTO(savedComment)));
    }

    @PutMapping("/{id}")
    @Transactional
    @Operation(summary = "Update comment", description = "Update an existing comment")
    public ResponseEntity<ApiResponse<CommentDTO>> updateComment(
            @PathVariable Long id,
            @RequestBody UpdateCommentRequest request
    ) {
        log.info("PUT /api/comments/{}", id);
        return commentJpaRepository.findById(id)
                .map(comment -> {
                    if (request.content() != null) {
                        comment.setContent(request.content());
                    }
                    Comment updatedComment = commentJpaRepository.save(comment);
                    return ResponseEntity.ok(
                            ApiResponse.success("Comment updated successfully", toDTO(updatedComment))
                    );
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.notFound("Comment not found with id: " + id)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete comment", description = "Soft delete a comment")
    public ResponseEntity<ApiResponse<Void>> deleteComment(@PathVariable Long id) {
        log.info("DELETE /api/comments/{}", id);
        return commentJpaRepository.findById(id)
                .map(comment -> {
                    comment.softDelete();
                    commentJpaRepository.save(comment);
                    return ResponseEntity.ok(
                            ApiResponse.<Void>success("Comment deleted successfully")
                    );
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.notFound("Comment not found with id: " + id)));
    }
    // HELPER METHODS
    private CommentDTO toDTO(Comment comment) {
        return new CommentDTO(
                comment.getId(),
                comment.getPost().getId(),
                comment.getUser().getUsername(),
                comment.getContent(),
                comment.getCreatedAt()
        );
    }
    public record CreateCommentRequest(
            Long postId,
            Long userId,
            String content
    ) {}
    public record UpdateCommentRequest(
            String content
    ) {}
}

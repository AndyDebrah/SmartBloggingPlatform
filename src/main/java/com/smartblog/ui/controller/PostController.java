package com.smartblog.ui.controller;
import com.smartblog.core.dto.ApiResponse;
import com.smartblog.core.dto.PaginationMetadata;
import com.smartblog.core.dto.PostDTO;
import com.smartblog.core.dto.request.PostCreateRequest;
import com.smartblog.core.model.Post;
import com.smartblog.core.model.User;
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
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Post Management", description = "APIs for managing blog posts")
public class PostController {
    private final PostJpaRepository postRepository;
    private final UserJpaRepository userRepository;

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "Get all posts", description = "Retrieve paginated list of posts")
    public ResponseEntity<ApiResponse<List<PostDTO>>> getAllPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(required = false) Boolean published
    ) {
        log.info("GET /api/posts - page={}, size={}, sort={}, published={}",
                page, size, sort, published);
        String[] sortParams = sort.split(",");
        String sortField = sortParams[0];
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("desc")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));
        Page<Post> postPage;
        if (published != null && published) {
            postPage = postRepository.findAllPublished(pageable);
        } else {
            postPage = postRepository.findByDeletedAtIsNull(pageable);
        }
        List<PostDTO> postDTOs = postPage.getContent().stream()
                .map(this::toDTO)
                .toList();
        return ResponseEntity.ok(
                ApiResponse.success("Posts retrieved successfully", postDTOs,
                        PaginationMetadata.from(postPage))
        );
    }

    @GetMapping("/search")
    @Transactional(readOnly = true)
    @Operation(summary = "Search posts", description = "Full-text search on post titles and content")
    public ResponseEntity<ApiResponse<List<PostDTO>>> searchPosts(
            @Parameter(description = "Search keyword(s)")
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("GET /api/posts/search?q={}", q);
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> postPage = postRepository.searchByFullText(q, pageable);
        List<PostDTO> postDTOs = postPage.getContent().stream()
                .map(this::toDTO)
                .toList();
        return ResponseEntity.ok(
                ApiResponse.success("Search results for: " + q, postDTOs,
                        PaginationMetadata.from(postPage))
        );
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    @Operation(summary = "Get post by ID", description = "Retrieve a single post by ID")
    public ResponseEntity<ApiResponse<PostDTO>> getPostById(@PathVariable Long id) {
        log.info("GET /api/posts/{}", id);
        return postRepository.findById(id)
                .map(post -> ResponseEntity.ok(
                        ApiResponse.success("Post found", toDTO(post))
                ))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.notFound("Post not found with id: " + id)));
    }

    @GetMapping("/author/{authorId}")
    @Transactional(readOnly = true)
    @Operation(summary = "Get posts by author", description = "Retrieve all posts by a specific author")
    public ResponseEntity<ApiResponse<List<PostDTO>>> getPostsByAuthor(
            @PathVariable Long authorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("GET /api/posts/author/{}", authorId);
        return userRepository.findById(authorId)
                .map(author -> {
                    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
                    Page<Post> postPage = postRepository.findPublishedByAuthor(author, pageable);
                    List<PostDTO> postDTOs = postPage.getContent().stream()
                            .map(this::toDTO)
                            .toList();
                    return ResponseEntity.ok(
                            ApiResponse.success("Posts by author: " + author.getUsername(),
                                    postDTOs, PaginationMetadata.from(postPage))
                    );
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.notFound("Author not found with id: " + authorId)));
    }

    @GetMapping("/recent")
    @Transactional(readOnly = true)
    @Operation(summary = "Get recent posts", description = "Get posts from last N days")
    public ResponseEntity<ApiResponse<List<PostDTO>>> getRecentPosts(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("GET /api/posts/recent?days={}", days);
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> postPage = postRepository.findRecentPosts(since, pageable);
        List<PostDTO> postDTOs = postPage.getContent().stream()
                .map(this::toDTO)
                .toList();
        return ResponseEntity.ok(
                ApiResponse.success("Recent posts (last " + days + " days)", postDTOs,
                        PaginationMetadata.from(postPage))
        );
    }

    @PostMapping
    @Transactional
    @Operation(summary = "Create post", description = "Create a new blog post")
    public ResponseEntity<ApiResponse<PostDTO>> createPost(@Valid @RequestBody PostCreateRequest request) {
        log.info("POST /api/posts - title={}", request.title());
        return userRepository.findById(request.authorId())
                .map(author -> {
                    Post post = Post.builder()
                            .title(request.title())
                            .content(request.content())
                            .author(author)
                            .published(false)
                            .build();
                    Post savedPost = postRepository.save(post);
                    return ResponseEntity.status(HttpStatus.CREATED)
                            .body(ApiResponse.created("Post created successfully", toDTO(savedPost)));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.notFound("Author not found with id: " + request.authorId())));
    }

    @PutMapping("/{id}")
    @Transactional
    @Operation(summary = "Update post", description = "Update an existing post")
    public ResponseEntity<ApiResponse<PostDTO>> updatePost(
            @PathVariable Long id,
            @Valid @RequestBody PostCreateRequest request
    ) {
        log.info("PUT /api/posts/{}", id);
        return postRepository.findById(id)
                .map(post -> {
                    if (request.title() != null) post.setTitle(request.title());
                    if (request.content() != null) post.setContent(request.content());
                    post.setPublished(request.published());
                    Post updatedPost = postRepository.save(post);
                    return ResponseEntity.ok(
                            ApiResponse.success("Post updated successfully", toDTO(updatedPost))
                    );
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.notFound("Post not found with id: " + id)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete post", description = "Soft delete a post")
    public ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable Long id) {
        log.info("DELETE /api/posts/{}", id);
        return postRepository.findById(id)
                .map(post -> {
                    post.softDelete();
                    postRepository.save(post);
                    return ResponseEntity.ok(
                            ApiResponse.<Void>success("Post deleted successfully")
                    );
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.notFound("Post not found with id: " + id)));
    }

    private PostDTO toDTO(Post post) {
        return new PostDTO(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getAuthor().getUsername(),
                post.isPublished(),
                post.getTags().stream()
                        .map(tag -> tag.getName())
                        .toList()
        );
    }
}

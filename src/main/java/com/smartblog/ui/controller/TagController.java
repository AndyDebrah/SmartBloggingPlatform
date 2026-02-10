package com.smartblog.ui.controller;
import com.smartblog.core.dto.ApiResponse;
import com.smartblog.core.dto.PaginationMetadata;
import com.smartblog.core.dto.TagDTO;
import com.smartblog.core.model.Tag;
import com.smartblog.infrastructure.repository.jpa.TagJpaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import java.util.List;

/**
 * REST API endpoints for Tag management.
 * Base URL: /api/tags
 */
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
@Slf4j
@io.swagger.v3.oas.annotations.tags.Tag(name = "Tag Management", description = "APIs for managing tags")
public class TagController {
    private final TagJpaRepository tagRepository;

    /**
     * Get all tags with pagination and sorting.
     */
    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "Get all tags", description = "Retrieve paginated list of tags")
    public ResponseEntity<ApiResponse<List<TagDTO>>> getAllTags(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name,asc") String sort
    ) {
        log.info("GET /api/tags - page={}, size={}, sort={}", page, size, sort);
        String[] sortParams = sort.split(",");
        String sortField = sortParams[0];
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("desc")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));
        Page<Tag> tagPage = tagRepository.findAll(pageable);
        List<TagDTO> tagDTOs = tagPage.getContent().stream()
                .map(this::toDTO)
                .toList();
        return ResponseEntity.ok(
                ApiResponse.success("Tags retrieved successfully", tagDTOs,
                        PaginationMetadata.from(tagPage))
        );
    }

    /**
     * Get a tag by its ID.
     */
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    @Operation(summary = "Get tag by ID", description = "Retrieve a single tag by ID")
    public ResponseEntity<ApiResponse<TagDTO>> getTagById(@PathVariable Long id) {
        log.info("GET /api/tags/{}", id);
        return tagRepository.findById(id)
                .map(tag -> ResponseEntity.ok(
                        ApiResponse.success("Tag found", toDTO(tag))
                ))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.notFound("Tag not found with id: " + id)));
    }

    /**
     * Get a tag by its URL-friendly slug.
     */
    @GetMapping("/slug/{slug}")
    @Transactional(readOnly = true)
    @Operation(summary = "Get tag by slug", description = "Retrieve a tag by its URL-friendly slug")
    public ResponseEntity<ApiResponse<TagDTO>> getTagBySlug(@PathVariable String slug) {
        log.info("GET /api/tags/slug/{}", slug);
        return tagRepository.findBySlug(slug)
                .map(tag -> ResponseEntity.ok(
                        ApiResponse.success("Tag found", toDTO(tag))
                ))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.notFound("Tag not found with slug: " + slug)));
    }

    /**
     * Search tags by name.
     */
    @GetMapping("/search")
    @Transactional(readOnly = true)
    @Operation(summary = "Search tags", description = "Search tags by name (case-insensitive)")
    public ResponseEntity<ApiResponse<List<TagDTO>>> searchTags(
            @Parameter(description = "Search query")
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("GET /api/tags/search?q={}", q);
        Pageable pageable = PageRequest.of(page, size);
        Page<Tag> tagPage = tagRepository.searchByName(q, pageable);
        List<TagDTO> tagDTOs = tagPage.getContent().stream()
                .map(this::toDTO)
                .toList();
        return ResponseEntity.ok(
                ApiResponse.success("Search results for: " + q, tagDTOs,
                        PaginationMetadata.from(tagPage))
        );
    }

    /**
     * Get the most popular tags.
     */
    @GetMapping("/popular")
    @Transactional(readOnly = true)
    @Operation(summary = "Get popular tags", description = "Get most frequently used tags")
    public ResponseEntity<ApiResponse<List<TagDTO>>> getPopularTags(
            @Parameter(description = "Number of tags to return")
            @RequestParam(defaultValue = "10") int limit
    ) {
        log.info("GET /api/tags/popular?limit={}", limit);
        Pageable pageable = PageRequest.of(0, limit);
        Page<Tag> tagPage = tagRepository.findMostUsedTags(pageable);
        List<TagDTO> tagDTOs = tagPage.getContent().stream()
                .map(this::toDTO)
                .toList();
        return ResponseEntity.ok(
                ApiResponse.success("Popular tags retrieved", tagDTOs)
        );
    }

    /**
     * Create a new tag.
     */
    @PostMapping
    @Operation(summary = "Create tag", description = "Create a new tag")
    public ResponseEntity<ApiResponse<TagDTO>> createTag(@Valid @RequestBody CreateTagRequest request) {
        log.info("POST /api/tags - name={}", request.name());
        // Check if tag with name already exists
        if (tagRepository.existsByName(request.name())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(HttpStatus.CONFLICT, "Tag with this name already exists"));
        }
        // Check if slug already exists (if provided)
        if (request.slug() != null && tagRepository.existsBySlug(request.slug())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(HttpStatus.CONFLICT, "Tag with this slug already exists"));
        }
        Tag tag = Tag.builder()
                .name(request.name())
                .slug(request.slug())
                .build();
        // Slug will be auto-generated by @PrePersist if not provided
        Tag savedTag = tagRepository.save(tag);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Tag created successfully", toDTO(savedTag)));
    }

    /**
     * Update an existing tag.
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update tag", description = "Update an existing tag")
    public ResponseEntity<ApiResponse<TagDTO>> updateTag(
            @PathVariable Long id,
            @RequestBody UpdateTagRequest request
    ) {
        log.info("PUT /api/tags/{}", id);
        return tagRepository.findById(id)
                .map(tag -> {
                    if (request.name() != null) {
                        tag.setName(request.name());
                    }
                    if (request.slug() != null) {
                        tag.setSlug(request.slug());
                    }
                    Tag updatedTag = tagRepository.save(tag);
                    return ResponseEntity.ok(
                            ApiResponse.success("Tag updated successfully", toDTO(updatedTag))
                    );
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.notFound("Tag not found with id: " + id)));
    }

    /**
     * Delete a tag.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete tag", description = "Delete a tag")
    public ResponseEntity<ApiResponse<Void>> deleteTag(@PathVariable Long id) {
        log.info("DELETE /api/tags/{}", id);
        return tagRepository.findById(id)
                .map(tag -> {
                    tagRepository.delete(tag);
                    return ResponseEntity.ok(
                            ApiResponse.<Void>success("Tag deleted successfully")
                    );
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.notFound("Tag not found with id: " + id)));
    }

    private TagDTO toDTO(Tag tag) {
        return new TagDTO(
                tag.getId(),
                tag.getName()
        );
    }

    public record CreateTagRequest(
            String name,
            String slug  // Optional
    ) {}
    public record UpdateTagRequest(
            String name,
            String slug
    ) {}
}

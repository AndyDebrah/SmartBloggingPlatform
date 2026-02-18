package com.smartblog.controller;

import com.smartblog.application.service.TagService;
import com.smartblog.core.dto.ApiResponse;
import com.smartblog.core.dto.TagDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * REST API endpoints for Tag management.
 * Base URL: /api/tags
 * 
 * REFACTORED: Now uses Service Layer pattern (TagService) instead of direct
 * repository access.
 */
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
@Slf4j
@io.swagger.v3.oas.annotations.tags.Tag(name = "Tag Management", description = "APIs for managing tags")
public class TagController {
        private final TagService tagService;

        /**
         * Get all tags.
         */
        @GetMapping
        @Operation(summary = "Get all tags", description = "Retrieve list of all tags")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tags retrieved successfully", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<ApiResponse<List<TagDTO>>> getAllTags() {
                log.info("GET /api/tags");
                List<TagDTO> tags = tagService.listAll();
                return ResponseEntity.ok(
                                ApiResponse.success("Tags retrieved successfully", tags));
        }

        /**
         * Create a new tag.
         */
        @PostMapping
        @Operation(summary = "Create tag", description = "Create a new tag")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Tag created successfully", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Tag already exists"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<ApiResponse<TagDTO>> createTag(
                        @Valid @RequestBody CreateTagRequest request) {
                log.info("POST /api/tags - name={}", request.name());

                long tagId = tagService.create(request.name());

                // For simplicity, return success without fetching
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.created("Tag created successfully", null));
        }

        /**
         * Update an existing tag.
         */
        @PutMapping("/{id}")
        @Operation(summary = "Update tag", description = "Update an existing tag")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tag updated successfully", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Tag not found"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<ApiResponse<TagDTO>> updateTag(
                        @Parameter(description = "Tag ID") @PathVariable Long id,
                        @RequestBody UpdateTagRequest request) {
                log.info("PUT /api/tags/{}", id);

                boolean updated = tagService.rename(id, request.name());
                if (!updated) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .body(ApiResponse.notFound("Tag not found with id: " + id));
                }

                return ResponseEntity.ok(
                                ApiResponse.success("Tag updated successfully", null));
        }

        /**
         * Delete a tag.
         */
        @DeleteMapping("/{id}")
        @Operation(summary = "Delete tag", description = "Delete a tag")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tag deleted successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Tag not found"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<ApiResponse<Void>> deleteTag(
                        @Parameter(description = "Tag ID") @PathVariable Long id) {
                log.info("DELETE /api/tags/{}", id);

                boolean deleted = tagService.delete(id);
                if (!deleted) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .body(ApiResponse.notFound("Tag not found with id: " + id));
                }

                return ResponseEntity.ok(
                                ApiResponse.<Void>success("Tag deleted successfully"));
        }

        // Request DTOs
        public record CreateTagRequest(
                        String name,
                        String slug // Optional
        ) {
        }

        public record UpdateTagRequest(
                        String name,
                        String slug) {
        }
}

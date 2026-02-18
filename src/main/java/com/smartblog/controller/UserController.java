package com.smartblog.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
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

import com.smartblog.application.service.UserService;
import com.smartblog.core.dto.ApiResponse;
import com.smartblog.core.dto.UserDTO;
import com.smartblog.core.dto.request.UserCreateRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * REST API endpoints for User management.
 * Base URL: /api/users
 * 
 * REFACTORED: Now uses Service Layer pattern (UserService) instead of direct
 * repository access.
 */
@RestController
@RequestMapping("/api/users")
@Slf4j
@Tag(name = "User Management", description = "APIs for managing users")
public class UserController {
        private final UserService userService;

        public UserController(@Qualifier("userServiceImpl") UserService userService) {
                this.userService = userService;
        }

        /**
         * Get all users with pagination.
         */
        @GetMapping
        @Operation(summary = "Get all users", description = "Retrieve paginated list of active users")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Users retrieved successfully", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<ApiResponse<List<UserDTO>>> getAllUsers(
                        @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
                        @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
                log.info("GET /api/users - page={}, size={}", page, size);
                List<UserDTO> users = userService.list(page, size);
                return ResponseEntity.ok(
                                ApiResponse.success("Users retrieved successfully", users));
        }

        /**
         * Get a user by ID.
         */
        @GetMapping("/{id}")
        @Operation(summary = "Get user by ID", description = "Retrieve a single user by their ID")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User found", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<ApiResponse<UserDTO>> getUserById(
                        @Parameter(description = "User ID") @PathVariable Long id) {
                log.info("GET /api/users/{}", id);
                return userService.get(id)
                                .map(userDTO -> ResponseEntity.ok(
                                                ApiResponse.success("User found", userDTO)))
                                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                                                .body(ApiResponse.notFound("User not found with id: " + id)));
        }

        /**
         * Get a user by username.
         */
        @GetMapping("/username/{username}")
        @Operation(summary = "Get user by username", description = "Retrieve a user by their username")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User found", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<ApiResponse<UserDTO>> getUserByUsername(
                        @Parameter(description = "Username") @PathVariable String username) {
                log.info("GET /api/users/username/{}", username);
                return userService.findByUsername(username)
                                .map(userDTO -> ResponseEntity.ok(
                                                ApiResponse.success("User found", userDTO)))
                                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                                                .body(ApiResponse.notFound(
                                                                "User not found with username: " + username)));
        }

        /**
         * Create a new user account.
         * Validation is handled by @Valid annotation and custom validators
         * (@UniqueUsername, @UniqueEmail).
         */
        @PostMapping
        @Operation(summary = "Create user", description = "Create a new user account")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "User created successfully", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input (validation failed)"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Username or email already exists"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<ApiResponse<UserDTO>> createUser(
                        @Valid @RequestBody UserCreateRequest request) {
                log.info("POST /api/users - username={}", request.username());

                // Service layer handles business logic
                long userId = userService.register(
                                request.username(),
                                request.email(),
                                request.password(),
                                request.role());

                // Fetch created user
                UserDTO createdUser = userService.get(userId)
                                .orElseThrow(() -> new RuntimeException("User creation failed"));

                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.created("User created successfully", createdUser));
        }

        /**
         * Update user profile.
         */
        @PutMapping("/{id}")
        @Operation(summary = "Update user", description = "Update an existing user's profile")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User updated successfully", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<ApiResponse<UserDTO>> updateUser(
                        @PathVariable Long id,
                        @RequestBody UpdateUserRequest request) {
                log.info("PUT /api/users/{}", id);

                boolean updated = userService.updateProfile(id, request.email());
                if (!updated) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .body(ApiResponse.notFound("User not found with id: " + id));
                }

                UserDTO updatedUser = userService.get(id)
                                .orElseThrow(() -> new RuntimeException("User update failed"));

                return ResponseEntity.ok(
                                ApiResponse.success("User updated successfully", updatedUser));
        }

        /**
         * Soft delete a user.
         */
        @DeleteMapping("/{id}")
        @Operation(summary = "Delete user", description = "Soft delete a user (sets deletedAt timestamp)")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User deleted successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<ApiResponse<Void>> deleteUser(
                        @PathVariable Long id) {
                log.info("DELETE /api/users/{}", id);

                boolean deleted = userService.softDelete(id);
                if (!deleted) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .body(ApiResponse.notFound("User not found with id: " + id));
                }

                return ResponseEntity.ok(
                                ApiResponse.<Void>success("User deleted successfully"));
        }

        /**
         * Request DTO for updating a user
         */
        public record UpdateUserRequest(
                        String email) {
        }
}

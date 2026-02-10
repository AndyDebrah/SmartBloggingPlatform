package com.smartblog.ui.controller;
import com.smartblog.core.dto.ApiResponse;
import com.smartblog.core.dto.PaginationMetadata;
import com.smartblog.core.dto.UserDTO;
import com.smartblog.core.dto.request.UserCreateRequest;
import com.smartblog.core.model.User;
import com.smartblog.core.model.UserRole;
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
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * REST API endpoints for User management.
 * Base URL: /api/users
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Management", description = "APIs for managing users")
public class UserController {
    private final UserJpaRepository userRepository;

    /**
     * Get all users with pagination, sorting, and filtering.
     */
    @GetMapping
    @Operation(
            summary = "Get all users",
            description = "Retrieve paginated list of users with optional filtering and sorting"
    )
    public ResponseEntity<ApiResponse<List<UserDTO>>> getAllUsers(
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field and direction (e.g., 'username,asc')")
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @Parameter(description = "Filter by role")
            @RequestParam(required = false) UserRole role,
            @Parameter(description = "Show only active users")
            @RequestParam(defaultValue = "true") boolean activeOnly
    ) {
        log.info("GET /api/users - page={}, size={}, sort={}, role={}, activeOnly={}",
                page, size, sort, role, activeOnly);
        // Parse sort parameter (format: "field,direction")
        String[] sortParams = sort.split(",");
        String sortField = sortParams[0];
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("desc")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));
        // Apply filters
        Page<User> userPage;
        if (role != null && activeOnly) {
            userPage = userRepository.findActiveUsersByRole(role, pageable);
        } else if (role != null) {
            userPage = userRepository.findByRole(role, pageable);
        } else if (activeOnly) {
            userPage = userRepository.findByDeletedAtIsNull(pageable);
        } else {
            userPage = userRepository.findAll(pageable);
        }
        // Convert to DTOs (excluding sensitive data)
        List<UserDTO> userDTOs = userPage.getContent().stream()
                .map(this::toDTO)
                .toList();
        PaginationMetadata pagination = PaginationMetadata.from(userPage);
        return ResponseEntity.ok(
                ApiResponse.success("Users retrieved successfully", userDTOs, pagination)
        );
    }

    /**
     * Get a user by ID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Retrieve a single user by their ID")
    public ResponseEntity<ApiResponse<UserDTO>> getUserById(
            @Parameter(description = "User ID")
            @PathVariable Long id
    ) {
        log.info("GET /api/users/{}", id);
        return userRepository.findById(id)
                .map(user -> ResponseEntity.ok(
                        ApiResponse.success("User found", toDTO(user))
                ))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.notFound("User not found with id: " + id)));
    }

    /**
     * Get a user by username.
     */
    @GetMapping("/username/{username}")
    @Operation(summary = "Get user by username", description = "Retrieve a user by their username")
    public ResponseEntity<ApiResponse<UserDTO>> getUserByUsername(
            @Parameter(description = "Username")
            @PathVariable String username
    ) {
        log.info("GET /api/users/username/{}", username);
        return userRepository.findByUsername(username)
                .map(user -> ResponseEntity.ok(
                        ApiResponse.success("User found", toDTO(user))
                ))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.notFound("User not found with username: " + username)));
    }

    @GetMapping("/search")
    @Operation(summary = "Search users", description = "Search users by username or email")
    public ResponseEntity<ApiResponse<List<UserDTO>>> searchUsers(
            @Parameter(description = "Search query")
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("GET /api/users/search?q={}", q);
        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage = userRepository.searchByUsernameOrEmail(q, q, pageable);
        List<UserDTO> userDTOs = userPage.getContent().stream()
                .map(this::toDTO)
                .toList();
        PaginationMetadata pagination = PaginationMetadata.from(userPage);
        return ResponseEntity.ok(
                ApiResponse.success("Search results", userDTOs, pagination)
        );
    }

    @PostMapping
    @Operation(summary = "Create user", description = "Create a new user account")
    public <request> ResponseEntity<ApiResponse<UserDTO>> createUser(
            @Valid @RequestBody UserCreateRequest request) {
        log.info("POST /api/users - username={}", request.username());
        // Check if username already exists (Epic 3: Validation)
        if (userRepository.existsByUsername(request.username())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(HttpStatus.CONFLICT, "Username already exists"));
        }
        // Check if email already exists
        if (userRepository.existsByEmail(request.email())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(HttpStatus.CONFLICT, "Email already exists"));
        }
        // Create user (password hashing will be added in service layer)
        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .passwordHash(request.password()) // TODO: Hash with BCrypt in service layer
                .role((request.role() != null) ? UserRole.valueOf(request.role()) : UserRole.READER)                .build();
        User savedUser = userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("User created successfully", toDTO(savedUser)));
    }


    @PutMapping("/{id}")
    @Operation(summary = "Update user", description = "Update an existing user")
    public ResponseEntity<ApiResponse<UserDTO>> updateUser(
            @PathVariable Long id,
            @RequestBody UpdateUserRequest request
    ) {
        log.info("PUT /api/users/{}", id);
        return userRepository.findById(id)
                .map(user -> {
                    // Update fields
                    if (request.username() != null) {
                        user.setUsername(request.username());
                    }
                    if (request.email() != null) {
                        user.setEmail(request.email());
                    }
                    if (request.role() != null) {
                        user.setRole(request.role());
                    }
                    User updatedUser = userRepository.save(user);
                    return ResponseEntity.ok(
                            ApiResponse.success("User updated successfully", toDTO(updatedUser))
                    );
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.notFound("User not found with id: " + id)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user", description = "Soft delete a user (sets deletedAt timestamp)")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable Long id
    ) {
        log.info("DELETE /api/users/{}", id);
        return userRepository.findById(id)
                .map(user -> {
                    user.softDelete();
                    userRepository.save(user);
                    return ResponseEntity.ok(
                            ApiResponse.<Void>success("User deleted successfully")
                    );
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.notFound("User not found with id: " + id)));
    }

    private UserDTO toDTO(User user) {
        return new UserDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name()
        );
    }

    public record CreateUserRequest(
            String username,
            String email,
            String password,
            UserRole role
    ) {}
    /**
     * Request DTO for updating a user
     */
    public record UpdateUserRequest(
            String username,
            String email,
            UserRole role
    ) {}
}

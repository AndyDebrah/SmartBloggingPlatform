package com.smartblog.infrastructure.repository.jpa;

import com.smartblog.core.model.User;
import com.smartblog.core.model.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
 * EPIC 2: REST API DEVELOPMENT - USER JPA REPOSITORY
 * â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
 * Spring Data JPA repository for User entity.
 * Replaces manual JDBC implementation (UserRepositoryJdbc).
 *
 * <h2>Spring Data JPA Magic:</h2>
 * <ul>
 *   <li><b>JpaRepository<User, Long></b> - Provides CRUD methods automatically
 *     <ul>
 *       <li>save(User) - Insert or update</li>
 *       <li>findById(Long) - Find by primary key</li>
 *       <li>findAll() - Get all records</li>
 *       <li>deleteById(Long) - Delete by ID</li>
 *       <li>count() - Count total records</li>
 *     </ul>
 *   </li>
 *   <li><b>JpaSpecificationExecutor</b> - Dynamic query building
 *     <ul>
 *       <li>Supports complex filtering (Epic 2: Filtering requirement)</li>
 *       <li>findAll(Specification<User>, Pageable) for advanced queries</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <h2>Query Method Naming Convention:</h2>
 * Spring Data derives queries from method names:
 * <ul>
 *   <li>findByUsername(String) â†’ SELECT * FROM users WHERE username = ?</li>
 *   <li>findByEmail(String) â†’ SELECT * FROM users WHERE email = ?</li>
 *   <li>findByRole(UserRole) â†’ SELECT * FROM users WHERE role = ?</li>
 *   <li>findByDeletedAtIsNull() â†’ SELECT * FROM users WHERE deleted_at IS NULL</li>
 * </ul>
 *
 * <h2>Custom JPQL Queries:</h2>
 * @Query annotation allows custom JPQL for complex queries.
 * Example: findActiveUsersByRole uses WHERE clause for soft delete check.
 *
 * <h2>Pagination & Sorting (Epic 2 Requirement):</h2>
 * Methods accepting Pageable parameter support:
 * - Pagination: Page<User> findAll(Pageable.ofSize(10).withPage(0))
 * - Sorting: Page<User> findAll(Pageable.ofSize(10).withSort(Sort.by("username")))
 * - Combined: PageRequest.of(0, 10, Sort.by(Direction.DESC, "createdAt"))
 *
 * @see JpaRepository
 * @see JpaSpecificationExecutor
 */
@Repository
public interface UserJpaRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    /**
     * Find user by username (unique constraint)
     * Epic 3: Used for unique username validation
     *
     * @param username Username to search for
     * @return Optional containing user if found
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by email (unique constraint)
     * Epic 3: Used for unique email validation
     *
     * @param email Email to search for
     * @return Optional containing user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Find user by username or email (login authentication)
     * Supports login with either username or email
     *
     * @param username Username to search for
     * @param email Email to search for
     * @return Optional containing user if found
     */
    Optional<User> findByUsernameOrEmail(String username, String email);

    /**
     * Find all active (not soft-deleted) users
     * Epic 2: Soft delete support
     *
     * @param pageable Pagination and sorting parameters
     * @return Page of active users
     */
    Page<User> findByDeletedAtIsNull(Pageable pageable);

    /**
     * Find all users by role
     * Example: Get all admins, all authors
     *
     * @param role User role to filter by
     * @param pageable Pagination and sorting parameters
     * @return Page of users with specified role
     */
    Page<User> findByRole(UserRole role, Pageable pageable);

    /**
     * Find active users by role (custom JPQL query)
     * Combines role filter with soft delete check
     *
     * @param role User role to filter by
     * @param pageable Pagination and sorting parameters
     * @return Page of active users with specified role
     */
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.deletedAt IS NULL")
    Page<User> findActiveUsersByRole(@Param("role") UserRole role, Pageable pageable);

    /**
     * Search users by username or email (case-insensitive)
     * Epic 2: Searching requirement
     *
     * @param username Username pattern to search for
     * @param email Email pattern to search for
     * @param pageable Pagination and sorting parameters
     * @return Page of matching users
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :username, '%')) " +
            "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%'))")
    Page<User> searchByUsernameOrEmail(@Param("username") String username,
                                       @Param("email") String email,
                                       Pageable pageable);

    /**
     * Count active (not soft-deleted) users
     * Epic 5: Analytics and metrics
     *
     * @return Count of active users
     */
    long countByDeletedAtIsNull();

    /**
     * Count users by role
     * Epic 5: Analytics - user role distribution
     *
     * @param role User role to count
     * @return Count of users with specified role
     */
    long countByRole(UserRole role);

    /**
     * Check if username already exists (for validation)
     * Epic 3: Unique constraint validation
     *
     * @param username Username to check
     * @return true if username exists
     */
    boolean existsByUsername(String username);

    /**
     * Check if email already exists (for validation)
     * Epic 3: Unique constraint validation
     *
     * @param email Email to check
     * @return true if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Find users created within a date range
     * Epic 5: Analytics - user registration trends
     *
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return List of users created in date range
     */
    List<User> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
}

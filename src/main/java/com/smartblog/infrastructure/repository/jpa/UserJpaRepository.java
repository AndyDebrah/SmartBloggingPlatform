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


@Repository
public interface UserJpaRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    /**
     * Find user by username (unique constraint)
     * Epic 3: Used for unique username validation
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by email (unique constraint)

     */
    Optional<User> findByEmail(String email);

    /**
     * Find user by username or email (login authentication)

     */
    Optional<User> findByUsernameOrEmail(String username, String email);

    /**
     * Find all active (not soft-deleted) users

     */
    Page<User> findByDeletedAtIsNull(Pageable pageable);

    /**
     * Find all users by role

     */
    Page<User> findByRole(UserRole role, Pageable pageable);

    /**

     * @return Page of active users with specified role
     */
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.deletedAt IS NULL")
    Page<User> findActiveUsersByRole(@Param("role") UserRole role, Pageable pageable);

    /**
     * Search users by username or email (case-insensitive)

     * @return Page of matching users
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :username, '%')) " +
            "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%'))")
    Page<User> searchByUsernameOrEmail(@Param("username") String username,
                                       @Param("email") String email,
                                       Pageable pageable);

    /**
     * Count active (not soft-deleted) users
     *
     * @return Count of active users
     */
    long countByDeletedAtIsNull();

    /**
     * Count users by role

     * @return Count of users with specified role
     */
    long countByRole(UserRole role);

    /**
     * Check if username already exists (for validation)

     * @return true if username exists
     */
    boolean existsByUsername(String username);

    /**
     * Check if email already exists (for validation)

     * @return true if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Find users created within a date range

     * @return List of users created in date range
     */
    List<User> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
}

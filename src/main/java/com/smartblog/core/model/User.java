package com.smartblog.core.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * JPA Entity representing a user account in the blogging platform.
 */
@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "email"),
        @UniqueConstraint(columnNames = "username")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"posts"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores")
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Size(max = 100, message = "Email must be at most 100 characters")
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    @NotBlank(message = "Password hash is required")
    private String passwordHash;

    @Column(name = "display_name", length = 100)
    @Size(max = 100, message = "Display name must be at most 100 characters")
    private String displayName;

    @Column(name = "bio", length = 500)
    @Size(max = 500, message = "Bio must be at most 500 characters")
    private String bio;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private UserRole role = UserRole.READER;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "author", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Post> posts = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Comment> comments = new HashSet<>();

    /**
     * Checks if the user is soft-deleted.
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Performs a soft delete on this user.
     */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * Restores a soft-deleted user.
     */
    public void restore() {
        this.deletedAt = null;
    }

    /**
     * Checks if the user has admin privileges.
     */
    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    /**
     * Checks if the user can author posts.
     */
    public boolean canAuthor() {
        return role == UserRole.ADMIN || role == UserRole.AUTHOR;
    }

    /**
     * Gets the user ID as an int for legacy JDBC compatibility.
     */
    public int getUserId() {
        return id == null ? 0 : id.intValue();
    }

    /**
     * Sets the user ID from an int for legacy JDBC compatibility.
     */
    public void setUserId(int userId) {
        this.id = (long) userId;
    }

    /**
     * Gets the password hash for legacy compatibility.
     */
    public String getPassword() {
        return passwordHash;
    }

    /**
     * Sets the password hash for legacy compatibility.
     */
    public void setPassword(String password) {
        this.passwordHash = password;
    }
}

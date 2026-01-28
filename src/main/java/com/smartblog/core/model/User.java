
package com.smartblog.core.model;

import java.time.LocalDateTime;

/**
 * Represents an account in the system.
 * This is a pure domain object (POJO), meaning:
 * - It does NOT know about the database.
 * - It does NOT know about the UI.
 * - It only represents business data.
 *
 * Real-world reasons:
 * -------------------
 * In production systems, domain models are clean and isolated
 * so the business logic stays stable even if UI or database technologies change.
 */
public class User {

    private Long id;
    private String username;
    private String email;
    private String passwordHash;    // never store raw password
    private String role;            // ADMIN, AUTHOR, READER
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt; // soft delete

    public User() {}

    public User(Long id, String username, String email, String passwordHash,
                String role, LocalDateTime createdAt,
                LocalDateTime updatedAt, LocalDateTime deletedAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    /** Convenience ctor used by DemoRunner when only basics are known. */
    public User(Long id, String username, String email, String passwordHash, LocalDateTime createdAt) {
        this(id, username, email, passwordHash, "READER", createdAt, createdAt, null);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getUserId() {
        return id == null ? 0 : id.intValue();
    }

    public void setUserId(int userId) {
        this.id = (long) userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    // DAO and demo use getPassword/setPassword naming; map to passwordHash field.
    public String getPassword() {
        return passwordHash;
    }

    public void setPassword(String password) {
        this.passwordHash = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}

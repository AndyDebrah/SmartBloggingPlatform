package com.smartblog.core.model;

/**
 * â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
 * EPIC 2: USER ROLE ENUMERATION
 * â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
 * Defines user roles in the blogging platform.
 *
 * <h2>Role Hierarchy:</h2>
 * <ul>
 *   <li><b>ADMIN</b> - Full system access (CRUD all entities, manage users)</li>
 *   <li><b>AUTHOR</b> - Can create/edit own posts, moderate own comments</li>
 *   <li><b>READER</b> - Can read posts, add comments</li>
 * </ul>
 *
 * Stored as STRING in database (not ordinal) for:
 * - Database readability
 * - Migration safety (adding new roles doesn't break existing data)
 * - SQL query friendliness (WHERE role = 'ADMIN')
 */
public enum UserRole {
    ADMIN,
    AUTHOR,
    READER
}

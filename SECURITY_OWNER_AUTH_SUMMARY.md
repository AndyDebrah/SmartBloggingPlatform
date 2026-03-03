# Security Owner Authorization Summary

This document summarizes the changes made to implement resource-owner authorization for posts and comments, why those changes were necessary, the exact code changes, and how the changes solve the problem.

**Context:** The application requires that only the owner of a post (or comment) or a user with the `ADMIN` role can update that resource. Path-based `requestMatchers` are insufficient for resource-based (per-entity) authorization because they cannot examine the authenticated principal against the resource owner stored in the database. To enforce owner-or-admin semantics we added small, testable authorization beans and used method-level security with `@PreAuthorize`.

**What this file contains:**
- Problem description
- Files created
- Files modified
- Representative code snippets
- Explanation: what each change does and why it was added
- How the changes solve the problem

---

**Problem addressed**

- Cross-user updates: controllers allowed updates by any authenticated user with the correct role-path mapping, but not checked whether the user actually owns the resource.
- Path-based matchers (e.g., `requestMatchers("/api/posts/**").hasRole("AUTHOR")`) cannot implement ownership checks because authorization needs an application-level lookup of the resource owner.

**High-level solution**

- Add small Spring beans that answer the question: "is the currently authenticated user the owner of resource X?".
- Apply method-level security with Spring Security SpEL to check owner-or-admin before executing controller methods that mutate resources.
- Keep beans focused and unit-testable; avoid entangling with filters or global URL matchers.

---

**Files added**

- `src/main/java/com/smartblog/security/PostSecurity.java`
- `src/main/java/com/smartblog/security/CommentSecurity.java`

**Files modified**

- `src/main/java/com/smartblog/controller/PostController.java` — added `@PreAuthorize` on update endpoint
- `src/main/java/com/smartblog/controller/CommentController.java` — added `@PreAuthorize` on update endpoint
- `src/main/java/com/smartblog/core/convert/UserRoleConverter.java` — added converter to map DB role values case-insensitively
- `src/main/java/com/smartblog/core/model/User.java` — changed to use `@Convert(UserRoleConverter.class)`
- `src/main/resources/db/migration/V10__role_to_varchar.sql` — migration added to normalize `users.role` to `VARCHAR(20)` where needed

---

**Representative code snippets**

1) `PostSecurity` (ownership check bean)

```java
package com.smartblog.security;

import org.springframework.stereotype.Component;
import com.smartblog.repository.PostRepository;
import org.springframework.security.core.Authentication;

@Component
public class PostSecurity {

    private final PostRepository postRepository;

    public PostSecurity(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    public boolean isOwner(Long postId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return false;
        String currentUsername = authentication.getName();
        return postRepository.findById(postId)
                .map(p -> p.getAuthor().getUsername().equals(currentUsername))
                .orElse(false);
    }
}
```

2) `CommentSecurity` (ownership check bean)

```java
package com.smartblog.security;

import org.springframework.stereotype.Component;
import com.smartblog.repository.CommentRepository;
import org.springframework.security.core.Authentication;

@Component
public class CommentSecurity {

    private final CommentRepository commentRepository;

    public CommentSecurity(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    public boolean isOwner(Long commentId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return false;
        String currentUsername = authentication.getName();
        return commentRepository.findById(commentId)
                .map(c -> c.getAuthor().getUsername().equals(currentUsername))
                .orElse(false);
    }
}
```

3) Controller-level usage: `PostController` update endpoint

```java
@PutMapping("/api/posts/{id}")
@PreAuthorize("hasRole('ADMIN') or @postSecurity.isOwner(#id, authentication)")
public ResponseEntity<PostDto> updatePost(@PathVariable Long id, @RequestBody PostUpdateRequest req) {
    // ... update logic
}
```

4) Controller-level usage: `CommentController` update endpoint

```java
@PutMapping("/api/comments/{id}")
@PreAuthorize("hasRole('ADMIN') or @commentSecurity.isOwner(#id, authentication)")
public ResponseEntity<CommentDto> updateComment(@PathVariable Long id, @RequestBody CommentUpdateRequest req) {
    // ... update logic
}
```

5) `UserRoleConverter` (case-insensitive enum mapping)

```java
package com.smartblog.core.convert;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import com.smartblog.core.model.UserRole;

@Converter(autoApply = false)
public class UserRoleConverter implements AttributeConverter<UserRole, String> {

    @Override
    public String convertToDatabaseColumn(UserRole attribute) {
        return attribute == null ? null : attribute.name().toUpperCase();
    }

    @Override
    public UserRole convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        try {
            return UserRole.valueOf(dbData.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null; // or throw, depending on desired behavior
        }
    }
}
```

6) Example Flyway migration fragment (normalize roles)

```sql
-- src/main/resources/db/migration/V9__normalize_roles.sql
UPDATE users SET role = UPPER(role) WHERE role IS NOT NULL;
-- Optionally alter column to ENUM or VARCHAR as needed in following migration
```

---

**Why these changes were added**

- Ownership beans (`PostSecurity` / `CommentSecurity`) provide a single place to express "is this authenticated user the owner of the resource?". They encapsulate the repository lookup and keep controller code minimal.
- Method-level security (`@PreAuthorize`) allows us to combine role checks and resource-owner checks in a single declarative annotation: `hasRole('ADMIN') or @bean.isOwner(#id)`.
- JPA `UserRoleConverter` addresses the mismatch between database role values and application `UserRole` enum values by performing case-insensitive mapping and producing consistent DB values. This reduces schema-validation errors and avoids brittle string comparisons in code.
- Flyway migrations normalize the data so the Converter and application assumptions match the DB contents. Handling data via migrations ensures that runtime code does not need to perform mass-mutations at startup.

**How these changes solve the problem**

- Problem: Controllers allowed updates based solely on high-level role membership or path-matchers. That permits one authenticated user to update another user's resource if they have the wrong role mapping.
- Solution: The `@PreAuthorize` checks are evaluated before the controller method runs and consult the security beans which look up the resource owner. If the authenticated principal does not match the owner, access is denied unless the user has `ADMIN`.
- This approach centralizes and encapsulates ownership logic, makes it easy to unit test, and minimizes security logic duplication across controllers.

**Verification steps**

1. Start the application and ensure migration scripts have been applied.
2. Register two users, A and B.
3. As A, create a post and note the post id.
4. Try to update the post as B — expect HTTP 403 Forbidden.
5. Update the post as A — expect success.
6. Repeat similar steps for comments.

**Next recommended steps**

- Apply the same owner-or-admin check to `DELETE` endpoints for posts/comments.
- Add unit tests for `PostSecurity` and `CommentSecurity` to mock repository lookups and verify allowed/denied scenarios.
- Add integration tests (MockMvc or WebTestClient) that exercise the `@PreAuthorize` annotations with different authenticated principals.

---

**Summary**

Implemented resource-owner authorization using focused security beans and method-level `@PreAuthorize` checks. Added a case-insensitive `UserRoleConverter` and small Flyway migrations to normalize stored role values. These changes ensure only owners or admins can mutate posts/comments and prevent cross-user updates while keeping the authorization logic simple, testable, and localized.

If you want, I can now:
- Add the same protection to delete endpoints
- Create unit tests for `PostSecurity` and `CommentSecurity`
- Run a quick MockMvc integration test sequence (register A/B, create, attempt cross-update)

---

Generated on: 2026-03-03

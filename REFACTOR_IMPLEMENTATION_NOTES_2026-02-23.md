# Refactor Implementation Notes (2026-02-23)

## Scope
This document records the refactors implemented to address the supervisor feedback:
- Passwords stored without hashing
- Broken cache eviction wiring
- N+1 query risks
- Missing JPA auditing listener on `Comment`
- Performance report delivered as template instead of measured document

## Classes and Files Affected
- `src/main/java/com/smartblog/application/service/impl/UserServiceImpl.java`
- `src/main/java/com/smartblog/graphql/UserGraphQLController.java`
- `src/main/java/com/smartblog/application/service/impl/PostServiceImpl.java`
- `src/main/java/com/smartblog/application/service/PostService.java`
- `src/main/java/com/smartblog/infrastructure/repository/jpa/PostJpaRepository.java`
- `src/main/java/com/smartblog/infrastructure/repository/jpa/CommentJpaRepository.java`
- `src/main/java/com/smartblog/infrastructure/repository/jpa/ReviewJpaRepository.java`
- `src/main/java/com/smartblog/core/model/Comment.java`
- `performance_report_module6.md`

## 1) Security Refactor: Password Hashing + Authentication

### 1.1 `UserServiceImpl` (`register`, `changePassword`, `authenticate`)
Explanation:
- Registration now hashes passwords with BCrypt before persistence.
- Password change now verifies old password and stores new BCrypt hash.
- Authentication now validates BCrypt hashes.
- Backward compatibility path migrates legacy plaintext password to BCrypt on successful login.

Exact code:
```java
import org.mindrot.jbcrypt.BCrypt;

@Override
@Transactional
@Caching(evict = {
    @CacheEvict(value = "userById", allEntries = true),
    @CacheEvict(value = "userByUsername", allEntries = true)
})
public long register(String username, String email, String rawPassword, String role) {
    log.info("Registering new user: {}", username);

    UserRole userRole = (role != null && !role.isBlank())
            ? UserRole.valueOf(role.toUpperCase())
            : UserRole.READER;
    String passwordHash = BCrypt.hashpw(rawPassword, BCrypt.gensalt());

    User user = User.builder()
            .username(username)
            .email(email)
            .passwordHash(passwordHash)
            .role(userRole)
            .build();

    User savedUser = userRepository.save(user);
    log.info("User registered successfully with ID: {}", savedUser.getId());

    return savedUser.getId();
}

@Override
@Transactional
public boolean changePassword(long id, String oldRawPassword, String newRawPassword) {
    return userRepository.findById(id)
            .map(user -> {
                String existing = user.getPasswordHash();
                boolean validOldPassword;
                if (isBCryptHash(existing)) {
                    validOldPassword = BCrypt.checkpw(oldRawPassword, existing);
                } else {
                    validOldPassword = existing != null && existing.equals(oldRawPassword);
                }
                if (!validOldPassword) {
                    return false;
                }

                user.setPasswordHash(BCrypt.hashpw(newRawPassword, BCrypt.gensalt()));
                userRepository.save(user);
                log.info("Password changed for user ID: {}", id);
                return true;
            })
            .orElse(false);
}

@Override
@Transactional
public Optional<User> authenticate(String username, String rawPassword) {
    return userRepository.findByUsernameOrEmail(username, username)
            .filter(user -> !user.isDeleted())
            .flatMap(user -> {
                String stored = user.getPasswordHash();
                if (isBCryptHash(stored)) {
                    return BCrypt.checkpw(rawPassword, stored) ? Optional.of(user) : Optional.empty();
                }
                if (stored != null && stored.equals(rawPassword)) {
                    user.setPasswordHash(BCrypt.hashpw(rawPassword, BCrypt.gensalt()));
                    userRepository.save(user);
                    log.info("Migrated legacy plaintext password for user ID: {}", user.getId());
                    return Optional.of(user);
                }
                return Optional.empty();
            });
}

private boolean isBCryptHash(String value) {
    return value != null && value.startsWith("$2");
}
```

### 1.2 `UserGraphQLController` (`createUser`)
Explanation:
- GraphQL mutation no longer stores raw password text.

Exact code:
```java
import org.mindrot.jbcrypt.BCrypt;

@MutationMapping
@Transactional
public User createUser(@Argument CreateUserInput input) {
    User user = User.builder()
            .username(input.username())
            .email(input.email())
            .passwordHash(BCrypt.hashpw(input.password(), BCrypt.gensalt()))
            .role(mapRole(input.role()))
            .displayName(input.displayName())
            .bio(input.bio())
            .createdAt(LocalDateTime.now())
            .build();
    return userRepository.save(user);
}
```

## 2) Cache Eviction Refactor: Real Write Methods Evict Caches

### 2.1 `PostServiceImpl`
Explanation:
- Eviction is applied directly to write methods used by callers.
- Removed old wrapper pattern (`createDraft_evict`) that was not used.

Exact code:
```java
@Override
@Transactional
@CacheEvict(value = "postsByAuthor", allEntries = true)
public long createDraft(long authorId, String title, String content) {
    // ...
}

@Override
@Transactional
@Caching(evict = {
    @CacheEvict(value = "postView", key = "#postId"),
    @CacheEvict(value = "postsByAuthor", allEntries = true)
})
public boolean publish(long postId) {
    // ...
}
```

### 2.2 `UserServiceImpl`
Explanation:
- User write paths now evict user caches directly.

Exact code:
```java
@Override
@Transactional
@Caching(evict = {
    @CacheEvict(value = "userById", key = "#id"),
    @CacheEvict(value = "userByUsername", allEntries = true)
})
public boolean updateProfile(long id, String email) {
    // ...
}

@Override
@Transactional
@Caching(evict = {
    @CacheEvict(value = "userById", key = "#id"),
    @CacheEvict(value = "userByUsername", allEntries = true)
})
public boolean softDelete(long id) {
    // ...
}
```

### 2.3 `PostService` interface cleanup
Explanation:
- Removed obsolete `createDraft_evict` signature.

Exact code:
```java
public interface PostService {
    long createDraft(long authorId, String title, String content);
    boolean publish(long postId);
    boolean update(long postId, String title, String content, boolean published);
    boolean softDelete(long postId);
    Optional<Post> getDomain(long id);
    Optional<PostDTO> getView(long id);
    Page<PostDTO> list(int page, int size);
    Page<PostDTO> search(String keyword, int page, int size);
    Page<PostDTO> listByAuthor(long authorId, int page, int size);
    Page<PostDTO> searchByTag(String tag, int page, int size);
    Page<PostDTO> searchByAuthorName(String authorName, int page, int size);
    Page<PostDTO> searchCombined(String keyword, String tag, String authorName, String sortBy, int page, int size);
}
```

## 3) N+1 Refactor: Fetch Planning with `@EntityGraph` + Bulk Hydration

### 3.1 `CommentJpaRepository`
Explanation:
- Fetch `post` and `user` with comments to prevent per-row lazy-load lookups in mapping/use.

Exact code:
```java
@Query("SELECT c FROM Comment c WHERE c.post = :post AND c.deletedAt IS NULL ORDER BY c.createdAt DESC")
@EntityGraph(attributePaths = { "post", "user" })
Page<Comment> findByPost(@Param("post") Post post, Pageable pageable);

@Query("SELECT c FROM Comment c WHERE c.post.id = :postId AND c.deletedAt IS NULL ORDER BY c.createdAt DESC")
@EntityGraph(attributePaths = { "post", "user" })
Page<Comment> findByPostIdAndDeletedAtIsNull(@Param("postId") long postId, Pageable pageable);
```

### 3.2 `ReviewJpaRepository`
Explanation:
- Fetch `post` and `user` eagerly for review listing paths.

Exact code:
```java
@Query("SELECT r FROM Review r WHERE r.post = :post AND r.deletedAt IS NULL ORDER BY r.createdAt DESC")
@EntityGraph(attributePaths = { "post", "user" })
Page<Review> findByPost(@Param("post") Post post, Pageable pageable);

@Query("SELECT r FROM Review r WHERE r.user = :user AND r.deletedAt IS NULL ORDER BY r.createdAt DESC")
@EntityGraph(attributePaths = { "post", "user" })
Page<Review> findByUser(@Param("user") User user, Pageable pageable);
```

### 3.3 `PostJpaRepository` + `PostServiceImpl`
Explanation:
- Added `author` fetch graph on paged post queries.
- Added second-step bulk hydration method for `author + tags` by page ids.
- This avoids N+1 while also avoiding collection-fetch pagination warnings/regressions.

Exact code (repository):
```java
@Query("SELECT p FROM Post p WHERE p.author.id = :authorId AND p.deletedAt IS NULL")
@EntityGraph(attributePaths = { "author" })
Page<Post> findByAuthorId(@Param("authorId") long authorId, Pageable pageable);

@EntityGraph(attributePaths = { "author", "tags" })
@Query("SELECT DISTINCT p FROM Post p WHERE p.id IN :ids")
List<Post> findWithAuthorAndTagsByIdIn(@Param("ids") List<Long> ids);
```

Exact code (service):
```java
private Page<PostDTO> mapToDtoPage(Page<Post> postPage) {
    List<Long> ids = postPage.getContent().stream().map(Post::getId).toList();
    if (ids.isEmpty()) {
        return postPage.map(PostMapper::toDTO);
    }

    Map<Long, Post> hydratedPosts = postRepository.findWithAuthorAndTagsByIdIn(ids).stream()
            .collect(Collectors.toMap(Post::getId, Function.identity()));

    return postPage.map(post -> PostMapper.toDTO(hydratedPosts.getOrDefault(post.getId(), post)));
}
```

## 4) Auditing Refactor: `Comment.createdAt` Auto-Population

### 4.1 `Comment` entity
Explanation:
- Added auditing entity listener required for `@CreatedDate` lifecycle handling.

Exact code:
```java
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "comments")
@EntityListeners(AuditingEntityListener.class)
public class Comment {
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
```

## 5) Performance Report Refactor

### 5.1 `performance_report_module6.md`
Explanation:
- Replaced template with measured content and artifact references.
- Included historical MySQL baseline files and current H2 benchmark captures.

Exact report file:
- `performance_report_module6.md`

Supporting benchmark artifact referenced by report:
- `analysis/module6/after/h2_benchmark_2026-02-23.txt`

## Validation Commands Executed
```powershell
.\mvnw -q -DskipTests compile
.\mvnw -q "-Dtest=TestUserServiceImplTest,TestUserServiceImplProfileTest" test
.\mvnw -q "-Dtest=CachingIntegrationTest,CachingPerformanceTest,EvictionAndPagedBenchmarkTest,TestUserServiceImplTest,TestUserServiceImplProfileTest" test
```

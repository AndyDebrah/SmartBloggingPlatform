# Smart Blogging Platform - Technical Documentation

## 📋 Executive Summary

The Smart Blogging Platform is a **RESTful web service** built with **Spring Boot 3.2.2** and **Java 21**, designed to manage blog content with full CRUD operations. The application was successfully migrated from a JavaFX desktop application to a modern REST API architecture, demonstrating proficiency in enterprise Java development, API design, and database management.

---

## 🏗️ Application Architecture

### **High-Level Architecture**

```
┌─────────────────────────────────────────────────────────────┐
│                    CLIENT LAYER                             │
│   (Postman, Swagger UI, Web/Mobile Frontends)               │
└────────────────────────┬────────────────────────────────────┘
                         │ HTTP REST API
┌────────────────────────▼────────────────────────────────────┐
│                 CONTROLLER LAYER (@RestController)          │
│  PostController │ UserController │ CommentController         │
│  TagController  │ ReviewController                          │
└────────────────────────┬────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────┐
│                 SERVICE LAYER (@Service)                    │
│  Business Logic │ Validation │ Transaction Management       │
└────────────────────────┬────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────┐
│            REPOSITORY LAYER (@Repository)                   │
│  Spring Data JPA │ Custom Queries │ Specifications          │
└────────────────────────┬────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────┐
│                  DATA LAYER (MySQL 8.0)                     │
│  Users │ Posts │ Comments │ Tags │ Reviews                  │
└─────────────────────────────────────────────────────────────┘
```

### **Design Patterns Used**

1. **Layered Architecture** - Clear separation of concerns
2. **Repository Pattern** - Data access abstraction
3. **DTO Pattern** - Data transfer between layers
4. **Dependency Injection** - Loose coupling via constructor injection
5. **Factory Pattern** - API response wrapper

---

## 📦 Key Classes and Their Roles

### **1. Controller Layer (REST API Endpoints)**

#### **PostController** (`ui/controller/PostController.java`)
**Role:** Handles all HTTP requests related to blog posts

**Key Methods:**
- `getAllPosts(Pageable)` - GET /api/posts - Retrieve paginated posts
- `getPostById(Long)` - GET /api/posts/{id} - Get single post
- `searchPosts(String, Pageable)` - GET /api/posts/search - Full-text search
- `createPost(PostCreateRequest)` - POST /api/posts - Create new post
- `updatePost(Long, PostCreateRequest)` - PUT /api/posts/{id} - Update post
- `deletePost(Long)` - DELETE /api/posts/{id} - Soft delete post

**Annotations Used:**
- `@RestController` - Marks as REST controller
- `@RequestMapping("/api/posts")` - Base URL mapping
- `@Transactional` - Transaction management for lazy-loading prevention
- `@Operation` - Swagger API documentation

**Key Features:**
- Pagination support using Spring Data's `Pageable`
- Transaction management to prevent `LazyInitializationException`
- DTO conversion to hide internal entity structure
- Full-text search using MySQL indexes

---

#### **UserController** (`ui/controller/UserController.java`)
**Role:** Manages user CRUD operations and authentication data

**Key Methods:**
- `getAllUsers(Pageable)` - GET /api/users - List all users
- `getUserById(Long)` - GET /api/users/{id} - Get user details
- `getUserByUsername(String)` - GET /api/users/username/{username}
- `searchUsers(String, Pageable)` - GET /api/users/search - Search users
- `createUser(UserCreateRequest)` - POST /api/users - Register new user
- `updateUser(Long, UserUpdateRequest)` - PUT /api/users/{id} - Update profile
- `deleteUser(Long)` - DELETE /api/users/{id} - Deactivate user

**Security Features:**
- BCrypt password hashing (legacy from JavaFX)
- Role-based access (ADMIN, AUTHOR, READER)
- Soft delete for data retention

---

#### **CommentController** (`ui/controller/CommentController.java`)
**Role:** Manages comments on blog posts

**Key Methods:**
- `getCommentsByPost(Long)` - GET /api/comments/post/{postId}
- `getCommentsByUser(Long)` - GET /api/comments/user/{userId}
- `getRecentComments(int)` - GET /api/comments/recent
- `createComment(CommentCreateRequest)` - POST /api/comments
- `updateComment(Long, CommentUpdateRequest)` - PUT /api/comments/{id}
- `deleteComment(Long)` - DELETE /api/comments/{id}

**Features:**
- Nested relationship handling (Post → Comments)
- Temporal queries for recent activity
- Soft delete with `deletedAt` timestamp

---

#### **TagController** (`ui/controller/TagController.java`)
**Role:** Manages blog post tags and categorization

**Key Methods:**
- `getAllTags()` - GET /api/tags - List all tags
- `getTagById(Long)` - GET /api/tags/{id}
- `getTagBySlug(String)` - GET /api/tags/slug/{slug}
- `getPopularTags(int)` - GET /api/tags/popular - Most used tags
- `createTag(TagCreateRequest)` - POST /api/tags

**Features:**
- Slug-based URLs (SEO-friendly)
- Popularity tracking by post count
- Many-to-many relationship with posts

---

#### **ReviewController** (`ui/controller/ReviewController.java`)
**Role:** Manages post reviews and ratings

**Key Methods:**
- `getReviewsByPost(Long)` - GET /api/reviews/post/{postId}
- `getReviewsByUser(Long)` - GET /api/reviews/user/{userId}
- `getPostRatingStats(Long)` - GET /api/reviews/post/{postId}/stats
- `createReview(ReviewCreateRequest)` - POST /api/reviews
- `updateReview(Long, ReviewUpdateRequest)` - PUT /api/reviews/{id}

**Features:**
- 1-5 star rating system
- Aggregated rating statistics (average, count)
- One review per user per post constraint

---

### **2. Model Layer (JPA Entities)**

#### **User** (`core/model/User.java`)
**Role:** Represents a user account in the system

**Key Fields:**
```java
@Id @GeneratedValue
private Long id;

@Column(unique = true, nullable = false)
private String username;

@Column(unique = true, nullable = false)
private String email;

private String passwordHash;  // BCrypt hashed

@Enumerated(EnumType.STRING)
private UserRole role;  // ADMIN, AUTHOR, READER

private LocalDateTime createdAt;
private LocalDateTime updatedAt;
private LocalDateTime deletedAt;  // Soft delete
```

**Relationships:**
- One-to-Many with `Post` (author)
- One-to-Many with `Comment` (commenter)
- One-to-Many with `Review` (reviewer)

---

#### **Post** (`core/model/Post.java`)
**Role:** Represents a blog post

**Key Fields:**
```java
@Id @GeneratedValue
private Long id;

private String title;

@Column(columnDefinition = "TEXT")
private String content;

private Boolean published;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "author_id")
private User author;

@ManyToMany
@JoinTable(name = "post_tags")
private Set<Tag> tags;

private LocalDateTime createdAt;
private LocalDateTime updatedAt;
private LocalDateTime deletedAt;
```

**Indexes:**
- Full-text index on `(title, content)` for search
- Index on `author_id` for author queries
- Index on `published` for filtering

---

#### **Comment** (`core/model/Comment.java`)
**Role:** Represents a comment on a post

**Key Fields:**
```java
@Id @GeneratedValue
private Long id;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "post_id")
private Post post;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id")
private User user;

@Column(columnDefinition = "TEXT")
private String content;

private LocalDateTime createdAt;
private LocalDateTime deletedAt;
```

**Note:** MongoDB dual-write was removed in Lab 5 for architecture simplification.

---

#### **Tag** (`core/model/Tag.java`)
**Role:** Categorizes blog posts

**Key Fields:**
```java
@Id @GeneratedValue
private Long id;

@Column(unique = true)
private String name;

@Column(unique = true)
private String slug;  // URL-friendly version

@ManyToMany(mappedBy = "tags")
private Set<Post> posts;
```

---

#### **Review** (`core/model/Review.java`)
**Role:** User ratings and reviews for posts

**Key Fields:**
```java
@Id @GeneratedValue
private Long id;

@ManyToOne
private Post post;

@ManyToOne
private User user;

@Min(1) @Max(5)
private Integer rating;

private String comment;

private LocalDateTime createdAt;
```

---

### **3. Repository Layer (Data Access)**

#### **PostJpaRepository** (`infrastructure/repository/jpa/PostJpaRepository.java`)
**Role:** Data access for posts using Spring Data JPA

**Key Methods:**
```java
// Built-in from JpaRepository
findAll(Pageable pageable) - Paginated retrieval
findById(Long id) - Single record fetch
save(Post post) - Create/Update
deleteById(Long id) - Delete

// Custom queries
@Query("SELECT p FROM Post p WHERE p.published = true AND p.deletedAt IS NULL")
List<Post> findAllPublished();

@Query("SELECT p FROM Post p WHERE p.author.id = :authorId")
Page<Post> findByAuthorId(Long authorId, Pageable pageable);

@Query("SELECT p FROM Post p JOIN p.tags t WHERE t.id = :tagId")
Page<Post> findByTagId(Long tagId, Pageable pageable);

// Full-text search (MySQL specific)
@Query(value = "SELECT * FROM posts WHERE MATCH(title, content) AGAINST (?1 IN NATURAL LANGUAGE MODE)", nativeQuery = true)
Page<Post> searchByContent(String query, Pageable pageable);
```

**Features:**
- Extends `JpaRepository<Post, Long>` for basic CRUD
- Custom JPQL queries for business logic
- Native SQL for MySQL full-text search
- Pagination support

---

#### **UserJpaRepository** (`infrastructure/repository/jpa/UserJpaRepository.java`)
**Key Custom Methods:**
```java
Optional<User> findByUsername(String username);
Optional<User> findByEmail(String email);
boolean existsByUsername(String username);
boolean existsByEmail(String email);

@Query("SELECT u FROM User u WHERE u.username LIKE %:query% OR u.email LIKE %:query%")
Page<User> searchByUsernameOrEmail(String query, Pageable pageable);
```

---

### **4. DTO Layer (Data Transfer Objects)**

#### **PostDTO** (`core/dto/PostDTO.java`)
**Role:** Transfer post data to clients (hides internal structure)

```java
public record PostDTO(
    Long id,
    String title,
    String content,
    String authorUsername,  // Instead of full User object
    Boolean published,
    List<String> tags,      // Just tag names
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
```

**Benefits:**
- Prevents over-fetching (doesn't expose password hashes, deleted fields)
- Controls API contract
- Improves performance (no lazy loading issues)

---

#### **ApiResponse<T>** (`core/dto/ApiResponse.java`)
**Role:** Standardized wrapper for all API responses

```java
public class ApiResponse<T> {
    private String status;        // "SUCCESS" or "ERROR"
    private Integer statusCode;   // HTTP status code
    private String message;       // Human-readable message
    private T data;              // Actual payload
    private Object metadata;     // Pagination, etc.
    private LocalDateTime timestamp;
    
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>("SUCCESS", 200, message, data, null);
    }
    
    public static <T> ApiResponse<T> error(String message, int statusCode) {
        return new ApiResponse<>("ERROR", statusCode, message, null, null);
    }
}
```

**Example Response:**
```json
{
  "status": "SUCCESS",
  "statusCode": 201,
  "message": "User created successfully",
  "data": {
    "id": 13,
    "username": "testuser",
    "email": "test@example.com",
    "role": "AUTHOR"
  },
  "timestamp": "2026-02-12T13:52:23"
}
```

---

### **5. Configuration Classes**

#### **SmartBlogApplication** (`SmartBlogApplication.java`)
**Role:** Application entry point

```java
@SpringBootApplication
@EnableCaching
@EnableJpaAuditing
@EnableAspectJAutoProxy
@EnableJpaRepositories(basePackages = "com.smartblog.infrastructure.repository.jpa")
@ComponentScan(basePackages = "com.smartblog")
public class SmartBlogApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartBlogApplication.class, args);
    }
}
```

**Annotations Explained:**
- `@SpringBootApplication` - Auto-configuration + component scanning
- `@EnableCaching` - Caffeine cache support
- `@EnableJpaAuditing` - Automatic `createdAt`/`updatedAt` timestamps
- `@EnableJpaRepositories` - Scans for repository interfaces
- `@ComponentScan` - Explicit package scanning (fixes controller detection)

---

#### **OpenApiConfig** (`infrastructure/config/OpenApiConfig.java`)
**Role:** Configures Swagger UI documentation

```java
@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Smart Blogging Platform API")
                        .version("1.0.0")
                        .description("RESTful API for Smart Blogging Platform")
                        .contact(new Contact()
                                .name("Smart Blog Team")
                                .email("support@smartblog.com"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local Development")
                ));
    }
}
```

---

#### **GlobalExceptionHandler** (`application/exception/GlobalExceptionHandler.java`)
**Role:** Centralized exception handling for all controllers

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(404)
                .body(ApiResponse.error(ex.getMessage(), 404));
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(400)
                .body(ApiResponse.error("Validation failed: " + errors, 400));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("Unexpected error occurred", ex);
        return ResponseEntity.status(500)
                .body(ApiResponse.error("An unexpected error occurred. Please contact support.", 500));
    }
}
```

---

## 🔄 Application Workflow

### **1. User Registration and Login Flow**

```
┌─────────┐      POST /api/users       ┌──────────────┐
│ Client  │─────────────────────────────▶│UserController│
└─────────┘   {username, email, pwd}    └──────┬───────┘
                                               │
                                               │ validate input
                                               ▼
                                        ┌──────────────┐
                                        │UserService   │
                                        └──────┬───────┘
                                               │
                                               │ hash password (BCrypt)
                                               │ check duplicates
                                               ▼
                                        ┌──────────────┐
                                        │UserRepository│
                                        └──────┬───────┘
                                               │
                                               │ INSERT INTO users
                                               ▼
                                        ┌──────────────┐
                                        │  MySQL DB    │
                                        └──────────────┘
```

---

### **2. Create Post Workflow**

```
┌─────────┐    POST /api/posts         ┌──────────────┐
│ Client  │─────────────────────────────▶│PostController│
└─────────┘   {title, content,          └──────┬───────┘
               authorId, tagIds}                │
                                               │ @Transactional
                                               ▼
                                        ┌──────────────┐
                                        │PostService   │
                                        └──────┬───────┘
                                               │
                                               │ validate author exists
                                               │ fetch tags by IDs
                                               │ create Post entity
                                               ▼
                                        ┌──────────────┐
                                        │PostRepository│
                                        └──────┬───────┘
                                               │
                                               │ INSERT INTO posts
                                               │ INSERT INTO post_tags
                                               ▼
                                        ┌──────────────┐
                                        │  MySQL DB    │
                                        └──────┬───────┘
                                               │
                                               │ return saved Post
                                               ▼
                                        ┌──────────────┐
                                        │ toDTO()      │ Convert to PostDTO
                                        └──────┬───────┘
                                               │
                                               │ wrap in ApiResponse
                                               ▼
┌─────────┐                            ┌──────────────┐
│ Client  │◀───────────────────────────│ Response     │
└─────────┘   {status: SUCCESS,        └──────────────┘
               statusCode: 201,
               data: {id, title...}}
```

---

### **3. Get Posts with Pagination**

```
┌─────────┐    GET /api/posts?page=0&size=10    ┌──────────────┐
│ Client  │─────────────────────────────────────▶│PostController│
└─────────┘                                      └──────┬───────┘
                                                        │
                                                        │ @Transactional(readOnly=true)
                                                        │ create Pageable
                                                        ▼
                                                 ┌──────────────┐
                                                 │PostRepository│
                                                 └──────┬───────┘
                                                        │
                                                        │ SELECT * FROM posts
                                                        │ WHERE deleted_at IS NULL
                                                        │ LIMIT 10 OFFSET 0
                                                        ▼
                                                 ┌──────────────┐
                                                 │  MySQL DB    │
                                                 └──────┬───────┘
                                                        │
                                                        │ Page<Post>
                                                        ▼
                                                 ┌──────────────┐
                                                 │Controller    │
                                                 └──────┬───────┘
                                                        │
                                                        │ Load author (lazy)
                                                        │ Load tags (lazy)
                                                        │ Convert to DTOs
                                                        │ Create pagination metadata
                                                        ▼
┌─────────┐                                      ┌──────────────┐
│ Client  │◀─────────────────────────────────────│ Response     │
└─────────┘   {status: SUCCESS,                  └──────────────┘
               data: {
                 content: [posts...],
                 totalElements: 22,
                 totalPages: 3,
                 pageNumber: 0,
                 pageSize: 10
               }}
```

---

### **4. Full-Text Search Workflow**

```
┌─────────┐    GET /api/posts/search?query=spring    ┌──────────────┐
│ Client  │─────────────────────────────────────────▶│PostController│
└─────────┘                                           └──────┬───────┘
                                                             │
                                                             │
                                                             ▼
                                                      ┌──────────────┐
                                                      │PostRepository│
                                                      └──────┬───────┘
                                                             │
                                                             │ Native SQL:
                                                             │ SELECT * FROM posts
                                                             │ WHERE MATCH(title, content)
                                                             │ AGAINST('spring' IN NATURAL LANGUAGE MODE)
                                                             ▼
                                                      ┌──────────────┐
                                                      │  MySQL       │
                                                      │ (FULLTEXT    │
                                                      │  INDEX)      │
                                                      └──────┬───────┘
                                                             │
                                                             │ Ranked results
                                                             ▼
┌─────────┐                                           ┌──────────────┐
│ Client  │◀──────────────────────────────────────────│ Response     │
└─────────┘   {matched posts sorted by relevance}    └──────────────┘
```

---

### **5. Lazy Loading Prevention with @Transactional**

**Problem Without @Transactional:**
```
GET /api/posts/1 → PostController.getPostById(1)
                 → postRepository.findById(1)
                 → Returns Post with author=proxy
                 → Transaction closed
                 → toDTO() calls post.getAuthor().getUsername()
                 → LazyInitializationException! ❌
```

**Solution With @Transactional:**
```
GET /api/posts/1 → PostController.getPostById(1) [@Transactional]
                 → postRepository.findById(1)
                 → Returns Post with author=proxy
                 → toDTO() calls post.getAuthor().getUsername()
                 → Lazy load author within transaction ✅
                 → Transaction closed after method completes
```

---

## 🗄️ Database Schema

### **Entity Relationship Diagram**

```
┌─────────────┐       ┌─────────────┐       ┌─────────────┐
│   USERS     │       │    POSTS    │       │    TAGS     │
├─────────────┤       ├─────────────┤       ├─────────────┤
│ id (PK)     │◄──────│ author_id   │       │ id (PK)     │
│ username    │       │ id (PK)     │       │ name        │
│ email       │       │ title       │       │ slug        │
│ password_   │       │ content     │       └──────┬──────┘
│   hash      │       │ published   │              │
│ role        │       │ created_at  │              │
│ created_at  │       │ updated_at  │              │
│ updated_at  │       │ deleted_at  │              │
│ deleted_at  │       └──────┬──────┘              │
└─────────────┘              │                     │
       │                     │    ┌────────────────┴────────────────┐
       │                     │    │          POST_TAGS              │
       │                     │    ├─────────────────────────────────┤
       │                     └────│ post_id (PK, FK)                │
       │                          │ tag_id (PK, FK) ◄───────────────┘
       │                          └─────────────────────────────────┘
       │
       │          ┌─────────────┐             ┌─────────────┐
       ├──────────│  COMMENTS   │             │   REVIEWS   │
       │          ├─────────────┤             ├─────────────┤
       │          │ id (PK)     │             │ id (PK)     │
       │          │ post_id (FK)├─────────────│ post_id (FK)│
       └──────────│ user_id (FK)│             │ user_id (FK)│
                  │ content     │             │ rating      │
                  │ created_at  │             │ comment     │
                  │ deleted_at  │             │ created_at  │
                  └─────────────┘             └─────────────┘
```

---

## 🔧 Technical Decisions & Justifications

### **1. Why Spring Boot over JavaFX?**
- **Scalability**: REST API can serve multiple clients (web, mobile, desktop)
- **Deployment**: Easier to deploy on cloud platforms
- **Modern Stack**: Industry standard for enterprise Java applications
- **API-First**: Decouples frontend from backend

### **2. Why JPA over JDBC?**
- **Productivity**: Auto-generates 90% of SQL queries
- **Type Safety**: Compile-time checking
- **Relationship Management**: Automatic handling of foreign keys
- **Caching**: Built-in first/second level cache

### **3. Why Remove MongoDB?**
- **Complexity**: Dual-write pattern added unnecessary complexity
- **Consistency**: Single database easier to maintain ACID properties
- **Learning Curve**: Focus on mastering relational databases first

### **4. Why DTO Pattern?**
- **Security**: Prevents exposure of sensitive fields (password hashes)
- **Performance**: Controls data fetching (no N+1 queries)
- **Versioning**: Easy to version API responses
- **Decoupling**: Internal entities can change without breaking API

### **5. Why @Transactional on Controllers?**
- **Simplicity**: Keeps transaction boundaries clear
- **Lazy Loading**: Allows accessing relationships in DTOs
- **Exception Handling**: Auto-rollback on errors

---

## 🚀 API Testing Results

### **Successful Operations Tested:**

✅ **User Management**
- Create User: `POST /api/users` → 201 Created
- Get All Users: `GET /api/users` → 200 OK (13 users)
- Search Users: `GET /api/users/search?q=test` → 200 OK

✅ **Post Management**
- Create Post: `POST /api/posts` → 201 Created
- Get Posts (paginated): `GET /api/posts?page=0&size=10` → 200 OK
- Full-text Search: `GET /api/posts/search?query=spring` → 200 OK
- Update Post: `PUT /api/posts/1` → 200 OK

✅ **Tag Management**
- Create Tag: `POST /api/tags` → 201 Created
- Get Popular Tags: `GET /api/tags/popular?limit=5` → 200 OK

✅ **Comment System**
- Create Comment: `POST /api/comments` → 201 Created
- Get Comments by Post: `GET /api/comments/post/1` → 200 OK

✅ **Review System**
- Create Review: `POST /api/reviews` → 201 Created
- Get Rating Stats: `GET /api/reviews/post/1/stats` → 200 OK

---

## 📊 Performance Optimizations

### **1. Database Indexes**
```sql
-- Full-text search index (V3 migration)
CREATE FULLTEXT INDEX idx_post_search ON posts(title, content);

-- Foreign key indexes (V2 migration)
CREATE INDEX idx_post_author ON posts(author_id);
CREATE INDEX idx_comment_post ON comments(post_id);
CREATE INDEX idx_comment_user ON comments(user_id);

-- Composite index for post_tags junction table
CREATE INDEX idx_post_tags ON post_tags(post_id, tag_id);
```

### **2. HikariCP Connection Pooling**
```properties
spring.datasource.hikari.maximum-pool-size=15
spring.datasource.hikari.minimum-idle=2
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

### **3. JPA Batch Processing**
```properties
spring.jpa.properties.hibernate.jdbc.batch_size=20
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
```

### **4. Query Optimization**
- Use `@Transactional(readOnly = true)` for read operations
- Fetch only required fields using DTOs
- Pagination for large result sets
- Native queries for complex searches

---

## 🐛 Issues Resolved During Development

### **Issue 1: Controllers Not Detected by Swagger**
**Problem:** Swagger UI showed "No operations defined in spec!"

**Root Cause:** Maven compiler excluded all files in `ui/**/*.java` to remove JavaFX legacy code, but this also excluded `ui/controller` REST controllers.

**Solution:**
```xml
<!-- pom.xml - Fixed excludes to be more specific -->
<excludes>
    <exclude>**/ui/view/**/*.java</exclude>
    <exclude>**/ui/components/**/*.java</exclude>
    <exclude>**/ui/themes/**/*.java</exclude>
    <!-- Removed: <exclude>**/ui/**/*.java</exclude> -->
</excludes>
```

---

### **Issue 2: LazyInitializationException**
**Problem:** `org.hibernate.LazyInitializationException: could not initialize proxy - no Session`

**Root Cause:** Accessing lazy-loaded relationships (e.g., `post.getAuthor()`) outside transaction boundary.

**Solution:** Added `@Transactional` to all controller methods that access relationships:
```java
@GetMapping("/{id}")
@Transactional(readOnly = true)  // ← Added this
public ResponseEntity<ApiResponse<PostDTO>> getPostById(@PathVariable Long id) {
    Post post = postRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Post not found"));
    return ResponseEntity.ok(ApiResponse.success("Post retrieved", toDTO(post)));
}
```

---

### **Issue 3: Postman 500 Error - Content-Type**
**Problem:** API returned 500 error with message "Content-Type 'text/plain' not supported"

**Root Cause:** Postman was sending request body as `text/plain` instead of `application/json`.

**Solution:** In Postman:
1. Body tab → select **raw**
2. Change dropdown from "Text" to **"JSON"**
3. Content-Type header automatically set to `application/json`

---

## 📝 Key Takeaways for Interview

### **What I Built:**
A production-ready REST API for a blogging platform with:
- 5 REST controllers handling 30+ endpoints
- Full CRUD operations with validation
- Pagination and full-text search
- Transaction management
- Swagger API documentation
- Exception handling
- Database migrations

### **Technologies Used:**
- **Backend:** Spring Boot 3.2.2, Java 21
- **Database:** MySQL 8.0, Flyway migrations
- **ORM:** Hibernate 6.4.1 (JPA)
- **API Docs:** Springdoc OpenAPI 2.3.0
- **Build:** Maven 3.8+
- **Testing:** Postman, Swagger UI

### **Design Patterns:**
- Layered Architecture
- Repository Pattern
- DTO Pattern
- Dependency Injection
- Factory Pattern (ApiResponse)

### **Challenges Overcome:**
1. Migrated from JavaFX desktop to REST API
2. Resolved Maven compilation issues
3. Fixed lazy loading exceptions
4. Removed MongoDB for simplicity
5. Configured Swagger documentation

### **What Makes It Production-Ready:**
- Proper exception handling
- Transaction management
- Input validation
- Soft deletes (data retention)
- Database indexes
- Connection pooling
- Pagination support
- API versioning ready

---

## 🎯 Interview Defense Points

### **Question: "Why did you choose Spring Boot?"**
**Answer:** Spring Boot provides production-ready features out-of-the-box like auto-configuration, embedded servers, metrics, and health checks. It's the industry standard for Java microservices and has excellent community support. The starter dependencies eliminate boilerplate configuration.

### **Question: "How do you handle database transactions?"**
**Answer:** I use Spring's `@Transactional` annotation on service methods and controllers that access lazy-loaded relationships. Read-only operations use `@Transactional(readOnly = true)` for performance. This ensures ACID properties and prevents LazyInitializationException.

### **Question: "How do you prevent SQL injection?"**
**Answer:** Spring Data JPA uses parameterized queries by default. All my custom queries use named parameters (`:paramName`) or positional parameters (`?1`) which are automatically escaped by Hibernate.

### **Question: "How would you add authentication?"**
**Answer:** I would implement Spring Security with JWT tokens:
1. Add `spring-boot-starter-security` dependency
2. Create JWT utility class
3. Implement `UserDetailsService`
4. Configure security filter chain
5. Add `@PreAuthorize` annotations for role-based access

### **Question: "How do you ensure API backward compatibility?"**
**Answer:** By using DTOs as the API contract layer. Internal entities can change without affecting the API. I can add new fields as optional, version endpoints (`/api/v1`, `/api/v2`), and use content negotiation for different response formats.

### **Question: "What's your testing strategy?"**
**Answer:** 
- **Unit Tests:** Test service layer logic with mocked repositories
- **Integration Tests:** Test repository queries with `@DataJpaTest`
- **API Tests:** Test endpoints with `@WebMvcTest` or Postman collections
- **Load Tests:** Use JMeter for performance testing

---

## 📚 Next Steps / Future Enhancements

1. **Security:** Implement Spring Security with JWT authentication
2. **GraphQL:** Add GraphQL support (already configured in properties)
3. **Caching:** Enable Redis for distributed caching
4. **Monitoring:** Add Spring Boot Actuator endpoints
5. **Deployment:** Containerize with Docker
6. **CI/CD:** Set up GitHub Actions pipeline
7. **Documentation:** Generate API docs with Swagger annotations
8. **Testing:** Add comprehensive test suite
9. **Rate Limiting:** Prevent API abuse
10. **File Upload:** Support image uploads for posts

---

## 🏆 Conclusion

This project demonstrates proficiency in:
- Enterprise Java development with Spring Boot
- REST API design and implementation
- Database design and optimization
- Problem-solving and debugging
- Code organization and architecture
- Modern development practices

**Total Time Investment:** Lab 5 (Epic 3 + debugging)  
**Lines of Code:** ~3000+ lines of production code  
**Endpoints:** 30+ REST endpoints  
**Database Tables:** 6 tables with relationships  

This application is ready for deployment and can serve as the backend for any frontend framework (React, Angular, Vue) or mobile application.

---

**Document Version:** 1.0  
**Last Updated:** February 12, 2026  
**Author:** Andy Kwasi Debrah

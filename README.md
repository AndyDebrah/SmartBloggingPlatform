# 📝 SmartBloggingPlatform

A modern, enterprise-grade blogging platform built with **Java 21**, **Spring Boot 3.2.2**, and **MySQL 8.0**. Originally a JavaFX desktop application, now transformed into a RESTful web service with GraphQL support (Lab 5).

![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.2-brightgreen?style=flat-square&logo=springboot)
![MySQL](https://img.shields.io/badge/MySQL-8.x-blue?style=flat-square&logo=mysql)
![Maven](https://img.shields.io/badge/Maven-3.8+-red?style=flat-square&logo=apachemaven)
![Swagger](https://img.shields.io/badge/Swagger-OpenAPI%203.0-green?style=flat-square&logo=swagger)
![GraphQL](https://img.shields.io/badge/GraphQL-16.2.0-E10098?style=flat-square&logo=graphql)

---

## 📋 Table of Contents

- [Lab 5 Achievements](#-lab-5-achievements)
- [Features](#-features)
- [Architecture](#-architecture)
- [Technology Stack](#-technology-stack)
- [API Documentation](#-api-documentation)
- [Database Schema](#-database-schema)
- [Getting Started](#-getting-started)
- [Configuration](#-configuration)
- [Project Structure](#-project-structure)
- [Testing](#-testing)
- [Contributing](#-contributing)

---

## 🎓 Lab 5 Achievements

### Epic 3: REST API Development ✅ COMPLETE

#### What We Built
Transformed the JavaFX desktop application into a modern RESTful web service with the following features:

**1. REST Controllers Implemented:**
- ✅ **PostController** - Full CRUD for blog posts with pagination and search
- ✅ **CommentController** - Comment management with post/user filtering
- ✅ **UserController** - User operations with role-based access
- ✅ **TagController** - Tag management with popularity tracking
- ✅ **ReviewController** - Post reviews and rating statistics

**2. Key Technical Achievements:**
- ✅ **Swagger UI Integration** - Interactive API documentation at `/swagger-ui.html`
- ✅ **Transaction Management** - All endpoints properly annotated with `@Transactional`
- ✅ **Lazy Loading Fix** - Resolved all `LazyInitializationException` errors across controllers
- ✅ **DTO Pattern** - Clean separation using `PostDTO`, `CommentDTO`, `UserDTO`, etc.
- ✅ **Pagination Support** - Implemented `Pageable` for all list endpoints
- ✅ **MongoDB Removal** - Simplified architecture by removing dual-write complexity
- ✅ **Legacy Code Cleanup** - Deleted all JavaFX UI code, JDBC repositories, and mappers

**3. Successfully Tested Operations:**
| Operation | Endpoint | Status | Notes |
|-----------|----------|--------|-------|
| Create User | `POST /api/users` | ✅ | User "Prince" created (id=7) |
| Create Post | `POST /api/posts` | ✅ | Post "Introduction to Programming" (id=74) |
| Update Post | `PUT /api/posts/74` | ✅ | Updated title and published status |
| Get All Posts | `GET /api/posts` | ✅ | Returns 22 published posts with pagination |
| Search Posts | `GET /api/posts/search` | ✅ | Full-text search working |

**4. Architecture Improvements:**
```
Before (Lab 1-4):                After (Lab 5):
JavaFX Desktop App               RESTful Web Service
├── UI Layer (FXML)              ├── REST Controllers
├── Service Layer                ├── Service Layer (unchanged)
├── JDBC Repositories            ├── JPA Repositories
├── MongoDB (dual-write)         ├── MySQL only
└── MySQL                        └── Swagger Documentation
```

**5. Configuration Highlights:**
- **HikariCP Connection Pool**: max-pool-size=15, optimized for concurrent requests
- **Flyway Migrations**: 3 versions applied (init, indexes, full-text search)
- **Spring Data JPA**: Hibernate 6.4.1.Final with MySQL8Dialect
- **Profile-Based Config**: `application-local.properties` for development
- **Transaction Isolation**: `READ_COMMITTED` for consistent reads

### Epic 4: GraphQL Integration 🔄 IN PROGRESS

**Status:** Blocked by `pom.xml` syntax errors

**Planned Implementation:**
1. ⏳ Add `spring-boot-starter-graphql` dependency (currently blocked)
2. 🔜 Create GraphQL schema file (`schema.graphqls`)
3. 🔜 Implement GraphQL controllers with `@QueryMapping` and `@MutationMapping`
4. 🔜 Enable GraphiQL interface at `/graphiql`
5. 🔜 Test queries and mutations for User, Post, Comment, Tag, Review

**Expected Benefits:**
- Flexible data fetching (clients request only needed fields)
- Reduced over-fetching compared to REST
- Single endpoint for all operations
- Strong typing with GraphQL schema

---

## ✨ Features

### Core REST API Functionality (Lab 5)
- 📝 **Blog Post Management** - Create, update, delete, publish posts via REST endpoints
- 👥 **User Management** - RESTful user CRUD with role support (Admin, Author)
- 💬 **Comments System** - Full comment management with JPA persistence
- 🏷️ **Tag Management** - Organize posts with tags and slug-based URLs
- ⭐ **Review System** - Post reviews with rating statistics
- 🔍 **Full-Text Search** - MySQL full-text indexing on posts (title + content)
- 📊 **Pagination Support** - All list endpoints support page/size parameters
- 📖 **Swagger UI** - Interactive API documentation and testing at `/swagger-ui.html`

### Technical Features
- 🗄️ **Spring Data JPA** - Repository pattern with Hibernate ORM
- ⚡ **Transaction Management** - Proper `@Transactional` annotations preventing lazy-loading errors
- 🔄 **Database Migrations** - Flyway for version-controlled schema management
- 🔐 **Secure Authentication** - BCrypt password hashing (legacy from JavaFX era)
- 📈 **HikariCP Connection Pool** - Optimized database connection management
- 🎯 **DTO Pattern** - Clean data transfer with validation annotations
- 🚀 **GraphQL Support** - Coming in Epic 4 (in progress)

### Deprecated Features (Removed in Lab 5)
- ❌ **JavaFX Desktop UI** - Migrated to REST API
- ❌ **MongoDB Dual-Write** - Simplified to MySQL-only persistence
- ❌ **JDBC Repositories** - Replaced with Spring Data JPA
- ❌ **Caffeine Caching** - Removed during architecture simplification

---

## 🏗️ Architecture

The application follows a **Spring Boot layered architecture** with REST API endpoints:

```
┌─────────────────────────────────────────────────────────────┐
│                      REST API Layer                         │
│   (Controllers, Swagger UI, GraphQL - coming soon)          │
├─────────────────────────────────────────────────────────────┤
│                   Application Layer                         │
│   (Services, DTOs, Security, Utilities)                     │
├─────────────────────────────────────────────────────────────┤
│                   Infrastructure Layer                      │
│   (JPA Repositories, Transaction Management, Flyway)        │
├─────────────────────────────────────────────────────────────┤
│                     Data Layer                              │
│                  ┌─────────────────┐                        │
│                  │     MySQL       │                        │
│                  │ (Smart_Blog DB) │                        │
│                  └─────────────────┘                        │
└─────────────────────────────────────────────────────────────┘
```

### Key Design Patterns
- **Repository Pattern** - Spring Data JPA repositories
- **Service Layer Pattern** - Business logic encapsulation
- **DTO Pattern** - Data transfer with Java Records and validation
- **Controller Pattern** - REST endpoints with `@RestController`
- **Transaction Management** - Declarative `@Transactional` annotations

### Migration Journey (Lab 5)
```
JavaFX Desktop (Labs 1-4)  →  Spring Boot REST API (Lab 5)
├── FXML Controllers       →  REST Controllers
├── JDBC Repositories      →  JPA Repositories  
├── MongoDB (dual-write)   →  MySQL only
├── Desktop UI             →  Swagger UI + GraphQL (coming)
└── Synchronous calls      →  HTTP REST/GraphQL APIs
```

---

## 🛠️ Technology Stack

| Category | Technology | Version | Purpose |
|----------|------------|---------|---------|
| **Language** | Java (OpenJDK) | 21.0.9 | Core programming language |
| **Framework** | Spring Boot | 3.2.2 | Application framework |
| **Web** | Spring Web | 6.1.3 | REST API support |
| **GraphQL** | Spring GraphQL | 16.2.0 | GraphQL API (Epic 4) |
| **ORM** | Hibernate | 6.4.1.Final | JPA implementation |
| **Data Access** | Spring Data JPA | 3.2.2 | Repository abstraction |
| **Validation** | Jakarta Validation | 3.0 | DTO validation |
| **Build Tool** | Maven | 3.8+ | Dependency management |
| **Database** | MySQL | 8.0 | Primary relational database |
| **Connection Pool** | HikariCP | 5.1.0 | Database connection pooling |
| **DB Migrations** | Flyway | 9.22.3 | Schema version control |
| **API Docs** | Springdoc OpenAPI | 2.3.0 | Swagger UI generation |
| **Security** | jBCrypt | 0.4 | Password hashing |
| **Lombok** | Lombok | 1.18.34 | Boilerplate reduction |
| **Testing** | JUnit Jupiter | 5.11.4 | Unit testing |

### Removed Dependencies (Lab 5 Cleanup)
- ~~JavaFX~~ - UI layer removed
- ~~MongoDB~~ - Simplified to MySQL-only
- ~~Caffeine Cache~~ - Removed during architecture simplification
- ~~ControlsFX, FormsFX, ValidatorFX~~ - Desktop UI libraries

---

## 📖 API Documentation

### Swagger UI
Access interactive API documentation at: **http://localhost:8080/swagger-ui.html**

### Available REST Endpoints

#### User Management (`/api/users`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/users` | Get all users (paginated) |
| `GET` | `/api/users/{id}` | Get user by ID |
| `GET` | `/api/users/username/{username}` | Get user by username |
| `GET` | `/api/users/search` | Search users by query |
| `POST` | `/api/users` | Create new user |
| `PUT` | `/api/users/{id}` | Update user |
| `DELETE` | `/api/users/{id}` | Delete user |

#### Post Management (`/api/posts`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/posts` | Get all posts (paginated) |
| `GET` | `/api/posts/{id}` | Get post by ID |
| `GET` | `/api/posts/search` | Full-text search posts |
| `GET` | `/api/posts/author/{authorId}` | Get posts by author |
| `GET` | `/api/posts/tag/{tagId}` | Get posts by tag |
| `POST` | `/api/posts` | Create new post |
| `PUT` | `/api/posts/{id}` | Update post |
| `DELETE` | `/api/posts/{id}` | Delete post |

#### Comment Management (`/api/comments`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/comments/post/{postId}` | Get comments by post |
| `GET` | `/api/comments/user/{userId}` | Get comments by user |
| `GET` | `/api/comments/{id}` | Get comment by ID |
| `GET` | `/api/comments/recent` | Get recent comments |
| `POST` | `/api/comments` | Create new comment |
| `PUT` | `/api/comments/{id}` | Update comment |
| `DELETE` | `/api/comments/{id}` | Delete comment |

#### Tag Management (`/api/tags`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/tags` | Get all tags |
| `GET` | `/api/tags/{id}` | Get tag by ID |
| `GET` | `/api/tags/slug/{slug}` | Get tag by slug |
| `GET` | `/api/tags/search` | Search tags |
| `GET` | `/api/tags/popular` | Get popular tags |
| `POST` | `/api/tags` | Create new tag |

#### Review Management (`/api/reviews`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/reviews/post/{postId}` | Get reviews by post |
| `GET` | `/api/reviews/user/{userId}` | Get reviews by user |
| `GET` | `/api/reviews/{id}` | Get review by ID |
| `GET` | `/api/reviews/post/{postId}/stats` | Get post rating statistics |
| `POST` | `/api/reviews` | Create new review |
| `PUT` | `/api/reviews/{id}` | Update review |

### Example API Calls

**Create User:**
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "Prince",
    "email": "Prince@gmail.com",
    "password": "password123",
    "role": "AUTHOR"
  }'
```

**Response:**
```json
{
  "status": "SUCCESS",
  "statusCode": 201,
  "data": {
    "id": 7,
    "username": "Prince",
    "email": "Prince@gmail.com",
    "role": "AUTHOR"
  }
}
```

**Create Post:**
```bash
curl -X POST http://localhost:8080/api/posts \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Introduction to Programming",
    "content": "Programming is the language computer understands",
    "authorId": 7,
    "published": false,
    "tagIds": []
  }'
```

**Get All Posts (Paginated):**
```bash
curl "http://localhost:8080/api/posts?page=0&size=10"
```

**Response:**
```json
{
  "status": "SUCCESS",
  "statusCode": 200,
  "data": {
    "content": [
      {
        "id": 74,
        "title": "Introduction to Programming - Updated",
        "content": "Programming is the language computer understands",
        "authorUsername": "Prince",
        "published": true,
        "tags": []
      }
    ],
    "pageable": {...},
    "totalElements": 22,
    "totalPages": 3,
    "number": 0,
    "size": 10
  }
}
```

### GraphQL API (Coming in Epic 4)
GraphQL endpoint will be available at: **http://localhost:8080/graphql**  
GraphiQL interface: **http://localhost:8080/graphiql**

---

## 💾 Database Schema

### Entity Relationship Diagram

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
                             │    ┌────────────────┴────────────────┐
                             │    │          POST_TAGS              │
                             │    ├─────────────────────────────────┤
                             │    │ post_id (PK, FK) ◄──────────────┤
                             │    │ tag_id (PK, FK)                 │
                             │    └─────────────────────────────────┘
                             │
                             ▼
                    ┌─────────────┐
                    │  COMMENTS   │
                    ├─────────────┤
                    │ id (PK)     │
                    │ post_id (FK)│
                    │ user_id (FK)│
                    │ content     │
                    │ created_at  │
                    │ deleted_at  │
                    └─────────────┘
```

### MySQL Tables

#### `users`
| Column | Type | Constraints |
|--------|------|-------------|
| `id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT |
| `username` | VARCHAR(100) | NOT NULL, UNIQUE |
| `email` | VARCHAR(255) | NOT NULL, UNIQUE |
| `password_hash` | VARCHAR(255) | NOT NULL |
| `role` | VARCHAR(50) | NOT NULL, DEFAULT 'AUTHOR' |
| `created_at` | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP |
| `updated_at` | DATETIME | NULL |
| `deleted_at` | DATETIME | NULL (Soft delete) |

#### `posts`
| Column | Type | Constraints |
|--------|------|-------------|
| `id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT |
| `author_id` | BIGINT | NOT NULL, FK → users(id) |
| `title` | VARCHAR(255) | NOT NULL |
| `content` | TEXT | NOT NULL |
| `published` | BOOLEAN | NOT NULL, DEFAULT FALSE |
| `created_at` | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP |
| `updated_at` | DATETIME | NULL |
| `deleted_at` | DATETIME | NULL (Soft delete) |

**Indexes:** Full-text index on `(title, content)`

#### `tags`
| Column | Type | Constraints |
|--------|------|-------------|
| `id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT |
| `name` | VARCHAR(100) | NOT NULL, UNIQUE |
| `slug` | VARCHAR(120) | NOT NULL, UNIQUE |

#### `post_tags` (Junction Table)
| Column | Type | Constraints |
|--------|------|-------------|
| `post_id` | BIGINT | PK, FK → posts(id) ON DELETE CASCADE |
| `tag_id` | BIGINT | PK, FK → tags(id) ON DELETE CASCADE |

#### `comments`
| Column | Type | Constraints |
|--------|------|-------------|
| `id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT |
| `post_id` | BIGINT | NOT NULL, FK → posts(id) ON DELETE CASCADE |
| `user_id` | BIGINT | NOT NULL, FK → users(id) ON DELETE CASCADE |
| `content` | TEXT | NOT NULL |
| `created_at` | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP |
| `deleted_at` | DATETIME | NULL (Soft delete) |

#### `reviews` (Lab 5 Addition)
| Column | Type | Constraints |
|--------|------|-------------|
| `id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT |
| `post_id` | BIGINT | NOT NULL, FK → posts(id) ON DELETE CASCADE |
| `user_id` | BIGINT | NOT NULL, FK → users(id) ON DELETE CASCADE |
| `rating` | INT | NOT NULL, CHECK (1-5) |
| `comment` | TEXT | NULL |
| `created_at` | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP |

### Database Relationships

| Relationship | Type | Description |
|--------------|------|-------------|
| User → Posts | One-to-Many | A user can author multiple posts |
| Post → Comments | One-to-Many | A post can have multiple comments |
| User → Comments | One-to-Many | A user can write multiple comments |
| Post → Reviews | One-to-Many | A post can have multiple reviews (Lab 5) |
| User → Reviews | One-to-Many | A user can write multiple reviews (Lab 5) |
| Post ↔ Tags | Many-to-Many | Posts and tags are linked via `post_tags` |

### Flyway Migrations (Lab 5)
- **V1__init.sql** - Initial schema creation
- **V2__performance_indexes.sql** - Added performance indexes
- **V3__fulltext_search_index.sql** - Full-text search on posts (title, content)

---

## 🚀 Getting Started

### Prerequisites

- **Java 21** (OpenJDK 21.0.9 recommended)
- **Maven 3.8+**
- **MySQL 8.x** running on `localhost:3306`

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/SmartBloggingPlatform.git
   cd SmartBloggingPlatform
   ```

2. **Create the MySQL database**
   ```sql
   CREATE DATABASE smart_blog;
   ```

3. **Configure database credentials**
   Edit `src/main/resources/application-local.properties`:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/smart_blog
   spring.datasource.username=root
   spring.datasource.password=your_password
   ```

4. **Build the project**
   ```bash
   ./mvnw clean install
   ```

5. **Run the application**
   ```bash
   ./mvnw spring-boot:run
   ```

   Or using Maven wrapper:
   ```bash
   mvnw.cmd spring-boot:run  # Windows
   ./mvnw spring-boot:run     # Linux/Mac
   ```

6. **Access the API**
   - Swagger UI: http://localhost:8080/swagger-ui.html
   - REST API Base: http://localhost:8080/api
   - GraphiQL (Epic 4): http://localhost:8080/graphiql (coming soon)

### Quick Test via Swagger

1. Navigate to http://localhost:8080/swagger-ui.html
2. Try the **POST /api/users** endpoint:
   ```json
   {
     "username": "testuser",
     "email": "test@example.com",
     "password": "password123",
     "role": "AUTHOR"
   }
   ```
3. Test **GET /api/posts** to see existing blog posts
4. Explore other endpoints using the interactive documentation

---

## ⚙️ Configuration

Configuration is managed via profile-based properties files:

### `application.properties` (Global)
```properties
# Application Settings
spring.application.name=SmartBloggingPlatform
server.port=8080

# Active Profile
spring.profiles.active=local

# JPA Configuration
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

### `application-local.properties` (Development)
```properties
# MySQL Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/smart_blog?useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=your_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# HikariCP Connection Pool
spring.datasource.hikari.maximum-pool-size=15
spring.datasource.hikari.minimum-idle=2
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
spring.jpa.properties.hibernate.jdbc.batch_size=20

# Flyway Migrations
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.validate-on-migrate=false

# Swagger/OpenAPI
springdoc.api-docs.path=/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
```

### Configuration Options

| Property | Description | Default |
|----------|-------------|---------|
| `server.port` | HTTP server port | 8080 |
| `spring.datasource.hikari.maximum-pool-size` | Max DB connections | 15 |
| `spring.jpa.show-sql` | Log SQL queries | true (dev) |
| `spring.flyway.enabled` | Run migrations on startup | true |
| `spring.flyway.validate-on-migrate` | Validate migration checksums | false (dev) |

---

## 📁 Project Structure

```
SmartBloggingPlatform/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── module-info.java (deprecated - to be removed)
│   │   │   └── com/smartblog/
│   │   │       ├── SmartBlogApplication.java   # Spring Boot entry point
│   │   │       ├── application/
│   │   │       │   ├── security/               # Security context (legacy)
│   │   │       │   ├── service/                # Business logic services
│   │   │       │   │   ├── UserService.java
│   │   │       │   │   ├── PostService.java
│   │   │       │   │   ├── CommentService.java
│   │   │       │   │   ├── TagService.java
│   │   │       │   │   └── ReviewService.java
│   │   │       │   └── util/                   # Utilities
│   │   │       ├── core/
│   │   │       │   ├── dto/                    # Data Transfer Objects
│   │   │       │   │   ├── UserDTO.java
│   │   │       │   │   ├── PostDTO.java
│   │   │       │   │   ├── CommentDTO.java
│   │   │       │   │   ├── TagDTO.java
│   │   │       │   │   └── ReviewDTO.java
│   │   │       │   ├── exceptions/             # Custom exceptions
│   │   │       │   └── model/                  # JPA Entities
│   │   │       │       ├── User.java
│   │   │       │       ├── Post.java
│   │   │       │       ├── Comment.java
│   │   │       │       ├── Tag.java
│   │   │       │       └── Review.java
│   │   │       ├── infrastructure/
│   │   │       │   ├── datasource/             # Data source config
│   │   │       │   ├── migration/              # Flyway migrations
│   │   │       │   └── repository/             # Spring Data JPA
│   │   │       │       ├── jpa/
│   │   │       │       │   ├── UserJpaRepository.java
│   │   │       │       │   ├── PostJpaRepository.java
│   │   │       │       │   ├── CommentJpaRepository.java
│   │   │       │       │   ├── TagJpaRepository.java
│   │   │       │       │   └── ReviewJpaRepository.java
│   │   │       └── ui/
│   │   │           └── controller/             # REST Controllers
│   │   │               ├── UserController.java
│   │   │               ├── PostController.java
│   │   │               ├── CommentController.java
│   │   │               ├── TagController.java
│   │   │               └── ReviewController.java
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-local.properties
│   │       └── db/migration/
│   │           ├── V1__init.sql
│   │           ├── V2__performance_indexes.sql
│   │           └── V3__fulltext_search_index.sql
│   └── test/
│       └── java/                               # Unit tests
├── target/                                     # Compiled output
├── pom.xml                                     # Maven dependencies
├── mvnw / mvnw.cmd                             # Maven wrapper
└── README.md
```

### Key Directories (Lab 5)

| Directory | Purpose |
|-----------|---------|
| `ui/controller/` | REST API endpoints (`@RestController`) |
| `application/service/` | Business logic layer |
| `infrastructure/repository/jpa/` | Spring Data JPA repositories |
| `core/model/` | JPA entities (`@Entity`) |
| `core/dto/` | Data Transfer Objects (Java Records) |
| `resources/db/migration/` | Flyway SQL migration scripts |

### Removed Directories (Lab 5 Cleanup)
- ❌ `ui/view/` - JavaFX FXML views
- ❌ `ui/components/` - JavaFX UI components
- ❌ `ui/themes/` - CSS theme files
- ❌ `infrastructure/repository/jdbc/` - Legacy JDBC repositories
- ❌ `infrastructure/nosql/` - MongoDB client factory
- ❌ `infrastructure/caching/` - Caffeine cache manager
- ❌ `core/mapper/` - Manual entity-DTO mappers
- ❌ `bootstrap/` - Dependency injection (replaced by Spring)

---

## Performance & Caching (Module 6)

 - **Overview:** Module 6 adds service-layer caching (Caffeine via Spring Cache) for common read paths (`postView`, `postsByAuthor`, `userById`, `userByUsername`) and includes integration tests and benchmarks.
 - **Run caching integration tests:**
   - `mvn -Dspring.profiles.active=test -Dtest=com.smartblog.cache.CachingIntegrationTest test`
 - **Run eviction & paged benchmark:**
   - `mvn -Dspring.profiles.active=test -Dtest=com.smartblog.cache.EvictionAndPagedBenchmarkTest test`
 - **Performance report template:** See `performance_report_module6.md` for commands and EXPLAIN guidance to run against MySQL/staging.

### Swagger UI Testing (Lab 5)
Interactive API testing via Swagger UI at http://localhost:8080/swagger-ui.html

**Tested Scenarios:**
1. ✅ Create User (POST /api/users) - Created user "Prince" with id=7
2. ✅ Create Post (POST /api/posts) - Created post id=74 "Introduction to Programming"
3. ✅ Update Post (PUT /api/posts/74) - Updated title and published status
4. ✅ Get All Posts (GET /api/posts) - Retrieved 22 published posts with pagination
5. ✅ Search Posts (GET /api/posts/search) - Full-text search working

### Manual API Testing with cURL

**Create User:**
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","email":"test@example.com","password":"pass123","role":"AUTHOR"}'
```

**Get All Posts:**
```bash
curl "http://localhost:8080/api/posts?page=0&size=10"
```

**Search Posts:**
```bash
curl "http://localhost:8080/api/posts/search?query=programming&page=0&size=10"
```

### Unit Testing
JUnit 5 tests are located in `src/test/java/`. Run with:
```bash
./mvnw test
```

---

## 🔧 Known Issues & Future Work

### Current Blockers (Lab 5)
- ⚠️ **pom.xml Syntax Error**: GraphQL dependency blocked by XML syntax issues (// comments, misplaced tags)
- 🔜 **Epic 4 Incomplete**: GraphQL schema and controllers not yet implemented

### Deprecated Code to Remove
- ⚠️ `module-info.java` - No longer needed for Spring Boot non-modular builds
- ⚠️ Legacy security context classes (to be replaced with Spring Security)
- ⚠️ Old performance report text files in workspace root

### Future Enhancements
- 🔐 Spring Security integration (JWT authentication)
- 📧 Email notifications for comments
- 🖼️ Image upload for post content
- 📊 Analytics dashboard (REST endpoints for metrics)
- 🔔 GraphQL subscriptions for real-time updates
- 🐳 Docker containerization
- ☁️ Azure deployment configuration

---

## 📈 Performance Notes

### Database Optimizations (Lab 5)
- ✅ HikariCP connection pool (15 max connections)
- ✅ Full-text indexes on posts (title, content)
- ✅ Composite indexes on post_tags (V2 migration)
- ✅ Batch inserts enabled (batch_size=20)
- ✅ Transaction isolation: READ_COMMITTED

### Known Performance Considerations
- Full-text search requires MySQL InnoDB full-text indexes (enabled in V3 migration)
- Lazy loading relationships require `@Transactional` to prevent N+1 queries
- Pagination recommended for large result sets (default size=10)

---

## 🎨 UI Screenshots (Deprecated - JavaFX Removed in Lab 5)

### Modern Dark Theme
The original desktop application featured a sleek dark theme with:
- **Dark backgrounds**: `#12151a`, `#1a1d23`, `#1e222a`
- **High-contrast text**: White (`#ffffff`) and light gray (`#e2e8f0`)
- **Vibrant accent gradients**:
  - Author Dashboard: Indigo → Purple (`#6366f1` → `#8b5cf6` → `#a855f7`)
  - Admin Dashboard: Pink → Rose → Orange (`#ec4899` → `#f43f5e` → `#f97316`)
- **Modern cards with subtle shadows and borders**

### Key Views
- **Login View** - Split-panel design with gradient accents
- **Author Dashboard** - Post management, stats, and quick actions
- **Admin Dashboard** - User management, drafts approval, system stats
- **Post Editor** - Rich text editing with tag assignment
- **Performance Report** - Benchmark results with color-coded metrics

**Note**: All JavaFX UI code has been removed in Lab 5 in favor of REST/GraphQL APIs with Swagger UI.

---

## 🔄 MongoDB Migration (Deprecated)

MongoDB dual-write functionality was removed in Lab 5 for architecture simplification. The project now uses MySQL-only persistence.

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 👨‍💻 Author

**Andy Kwasi Debrah**

---

<p align="center">
  Built with ❤️ using Java, Spring Boot, and REST/GraphQL APIs
</p>

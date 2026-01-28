# 📝 SmartBloggingPlatform

A modern, feature-rich desktop blogging platform built with **Java 21**, **JavaFX**, and a **polyglot persistence** architecture using both **MySQL** and **MongoDB**.

![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk)
![JavaFX](https://img.shields.io/badge/JavaFX-21.0.6-blue?style=flat-square)
![MySQL](https://img.shields.io/badge/MySQL-8.x-blue?style=flat-square&logo=mysql)
![MongoDB](https://img.shields.io/badge/MongoDB-4.10-green?style=flat-square&logo=mongodb)
![Maven](https://img.shields.io/badge/Maven-3.8+-red?style=flat-square&logo=apachemaven)

---

## 📋 Table of Contents

- [Features](#-features)
- [Architecture](#-architecture)
- [Technology Stack](#-technology-stack)
- [Database Schema](#-database-schema)
- [Getting Started](#-getting-started)
- [Configuration](#-configuration)
- [Project Structure](#-project-structure)
- [UI Screenshots](#-ui-screenshots)
- [Performance Benchmarking](#-performance-benchmarking)
- [Contributing](#-contributing)

---

## ✨ Features

### Core Functionality
- 📝 **Blog Post Management** - Create, edit, publish, and delete blog posts with rich text content
- 👥 **User Management** - Multi-role system (Admin, Author) with secure authentication
- 💬 **Comments System** - Full commenting with dual-write to MySQL and MongoDB
- 🏷️ **Tag Management** - Organize posts with tags and slug-based URLs
- 🔍 **Full-Text Search** - MySQL full-text indexing for fast content search
- 📊 **Analytics Dashboard** - View post statistics and engagement metrics

### Technical Features
- 🗄️ **Polyglot Persistence** - MySQL for relational data, MongoDB for flexible comment storage
- ⚡ **High-Performance Caching** - Caffeine cache for optimized query performance
- 🔄 **Database Migrations** - Flyway for version-controlled schema management
- 🔐 **Secure Authentication** - BCrypt password hashing
- 🎨 **Modern Dark Theme** - Sleek, high-contrast UI with gradient accents
- 📈 **Performance Benchmarking** - Built-in benchmark tool for query performance analysis

---

## 🏗️ Architecture

The application follows a **layered architecture** with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────────┐
│                        UI Layer                             │
│   (JavaFX Views, FXML Controllers, CSS Themes)              │
├─────────────────────────────────────────────────────────────┤
│                   Application Layer                         │
│   (Services, DTOs, Security Context, Utilities)             │
├─────────────────────────────────────────────────────────────┤
│                   Infrastructure Layer                      │
│   (Repositories, DAOs, Caching, Data Sources)               │
├─────────────────────────────────────────────────────────────┤
│                     Data Layer                              │
│   ┌─────────────────┐       ┌─────────────────┐            │
│   │     MySQL       │       │    MongoDB      │            │
│   │  (Relational)   │       │   (Document)    │            │
│   └─────────────────┘       └─────────────────┘            │
└─────────────────────────────────────────────────────────────┘
```

### Key Design Patterns
- **Repository Pattern** - Abstraction over data access
- **Service Layer Pattern** - Business logic encapsulation
- **DTO Pattern** - Data transfer between layers using Java Records
- **Factory Pattern** - Database connection and client creation
- **Dual-Write Pattern** - Synchronized writes to MySQL and MongoDB

---

## 🛠️ Technology Stack

| Category | Technology | Version |
|----------|------------|---------|
| **Language** | Java | 21 |
| **UI Framework** | JavaFX | 21.0.6 |
| **Build Tool** | Maven | 3.8+ |
| **Primary Database** | MySQL | 8.x |
| **Secondary Database** | MongoDB | 4.x+ |
| **Connection Pool** | HikariCP | 5.1.0 |
| **DB Migrations** | Flyway | 10.10.0 |
| **Caching** | Caffeine | 3.1.8 |
| **Password Hashing** | jBCrypt | 0.4 |
| **Logging** | SLF4J | 2.0.13 |
| **Testing** | JUnit Jupiter | 5.12.1 |

### Additional UI Libraries
- **ControlsFX** - Enhanced JavaFX controls
- **FormsFX** - Form handling
- **ValidatorFX** - Input validation
- **Ikonli** - Icon fonts
- **BootstrapFX** - Bootstrap-inspired styling
- **TilesFX** - Dashboard tiles

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

### MongoDB Collections

#### `comments` Collection
Comments are dual-written to MongoDB for flexible querying and future scalability.

```json
{
  "_id": ObjectId("..."),
  "mysqlId": 123,
  "postId": 45,
  "userId": 7,
  "username": "john_doe",
  "content": "Great post!",
  "createdAt": ISODate("2026-01-28T10:30:00Z")
}
```

### Database Relationships

| Relationship | Type | Description |
|--------------|------|-------------|
| User → Posts | One-to-Many | A user can author multiple posts |
| Post → Comments | One-to-Many | A post can have multiple comments |
| User → Comments | One-to-Many | A user can write multiple comments |
| Post ↔ Tags | Many-to-Many | Posts and tags are linked via `post_tags` |

---

## 🚀 Getting Started

### Prerequisites

- **Java 21** or higher
- **Maven 3.8+**
- **MySQL 8.x** running on `localhost:3306`
- **MongoDB 4.x+** running on `localhost:27017`

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

3. **Configure database credentials** (see [Configuration](#-configuration))

4. **Build the project**
   ```bash
   ./mvnw clean compile
   ```

5. **Run the application**
   ```bash
   ./mvnw javafx:run
   ```

   Or run directly:
   ```bash
   java --module-path target/classes -m SmartBloggingPlatform/com.smartblog.App
   ```

---

## ⚙️ Configuration

Configuration is managed via `src/main/resources/application.properties`:

```properties
# Environment
app.env=DEV

# MySQL Configuration
db.url=jdbc:mysql://localhost:3306/smart_blog?useSSL=false&serverTimezone=UTC
db.user=root
db.password=your_password

# HikariCP Connection Pool
db.pool.max=15
db.pool.min=2
db.pool.idleTimeoutMs=600000
db.pool.maxLifetimeMs=1800000

# Flyway Migrations
flyway.locations=filesystem:src/main/resources/db/migration
flyway.enabled=true

# MongoDB Configuration
comments.nosql.enabled=true
mongodb.uri=mongodb://localhost:27017
mongodb.database=smart_blog_nosql
```

### Configuration Options

| Property | Description | Default |
|----------|-------------|---------|
| `app.env` | Environment (DEV/PROD) | DEV |
| `db.url` | MySQL JDBC connection URL | - |
| `db.pool.max` | Maximum pool connections | 15 |
| `comments.nosql.enabled` | Enable MongoDB dual-write | true |
| `flyway.enabled` | Run migrations on startup | true |

---

## 📁 Project Structure

```
SmartBloggingPlatform/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── module-info.java
│   │   │   └── com/smartblog/
│   │   │       ├── App.java                 # Application entry point
│   │   │       ├── application/
│   │   │       │   ├── security/            # Security context
│   │   │       │   ├── service/             # Business logic services
│   │   │       │   └── util/                # Utilities (Slugs, Perf)
│   │   │       ├── bootstrap/
│   │   │       │   ├── AppBootstrap.java    # Dependency wiring
│   │   │       │   └── DevSmokeTest.java    # Development entry
│   │   │       ├── core/
│   │   │       │   ├── config/              # Connection managers
│   │   │       │   ├── dao/                 # Data Access Objects
│   │   │       │   ├── dto/                 # Data Transfer Objects
│   │   │       │   ├── exceptions/          # Custom exceptions
│   │   │       │   ├── mapper/              # Entity ↔ DTO mappers
│   │   │       │   └── model/               # Domain models
│   │   │       ├── infrastructure/
│   │   │       │   ├── caching/             # Caffeine cache manager
│   │   │       │   ├── datasource/          # Data source config
│   │   │       │   ├── migration/           # Flyway + MongoDB migrator
│   │   │       │   ├── nosql/               # MongoDB client factory
│   │   │       │   └── repository/          # Repository implementations
│   │   │       └── ui/
│   │   │           ├── components/          # Reusable UI components
│   │   │           ├── navigation/          # Navigation service
│   │   │           ├── themes/              # CSS themes
│   │   │           │   ├── styles-dark.css
│   │   │           │   ├── styles-light.css
│   │   │           │   └── variables.css
│   │   │           └── view/                # FXML views + controllers
│   │   │               ├── admin/           # Admin dashboard
│   │   │               ├── analytics/       # Analytics views
│   │   │               ├── authors/         # Author dashboard
│   │   │               ├── comments/        # Comment management
│   │   │               ├── login/           # Login screen
│   │   │               ├── main/            # Main layout
│   │   │               ├── performance/     # Benchmark reports
│   │   │               ├── posts/           # Post management
│   │   │               ├── search/          # Search functionality
│   │   │               ├── tags/            # Tag management
│   │   │               └── users/           # User management
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── db.properties
│   │       └── db/migration/
│   │           └── V1__init.sql             # Initial schema
│   └── test/
│       └── java/                            # Unit tests
├── pom.xml
├── mvnw / mvnw.cmd                          # Maven wrapper
└── README.md
```

---

## 🎨 UI Screenshots

### Modern Dark Theme
The application features a sleek dark theme with:
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

---

## 📈 Performance Benchmarking

The built-in Performance Benchmark tool measures query performance:

### Features
- **Cold + Warm Run Modes** - Test with and without cache
- **Color-Coded Results**:
  - 🟢 Green: < 10ms (Excellent)
  - 🟡 Orange: 10-50ms (Good)
  - 🔴 Red: > 50ms (Needs optimization)
- **Export Reports** - Save benchmark results

### Running Benchmarks
1. Navigate to Admin Dashboard
2. Click "⚡ Performance Report"
3. Click "Run Cold + Warm" for comprehensive testing
4. Review detailed results table

---

## 🔄 MongoDB Migration

To migrate existing MySQL comments to MongoDB:

```java
// Run the migration utility
java -cp target/classes com.smartblog.infrastructure.migration.CommentMongoMigrator
```

This copies all comments from MySQL to MongoDB with the `mysqlId` field for reference tracking.

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
  Built with ❤️ using Java and JavaFX
</p>

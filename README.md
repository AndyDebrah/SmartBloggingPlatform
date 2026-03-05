# SmartBloggingPlatform

SmartBloggingPlatform is a Spring Boot backend for blogging features (users, posts, comments, tags, reviews) with layered architecture, MySQL persistence, REST APIs, GraphQL endpoint support, and production-oriented security.

## Table of Contents

- [1. Project Overview](#1-project-overview)
- [2. Architecture](#2-architecture)
- [3. Technology Stack](#3-technology-stack)
- [4. Getting Started](#4-getting-started)
- [5. Configuration](#5-configuration)
- [6. API Documentation](#6-api-documentation)
- [7. Data Model Summary](#7-data-model-summary)
- [8. Testing Guide](#8-testing-guide)
- [9. Performance Notes](#9-performance-notes)
- [10. Module 7 Security Enhancement Report (Epic 1-5)](#10-module-7-security-enhancement-report-epic-1-5)
- [11. Module 8 Advanced Optimization Report (Epic 1-5)](#11-module-8-advanced-optimization-report-epic-1-5)

## 1. Project Overview

This project was evolved from a desktop-first implementation to a backend-first service architecture.

Core capabilities:
- REST APIs for users, posts, comments, tags, reviews
- OpenAPI/Swagger documentation
- GraphQL endpoint and GraphiQL UI
- Spring Security with JWT and OAuth2 (Google)
- RBAC (`ADMIN`, `AUTHOR`, `READER`)
- CSRF strategy for browser form flows
- Security monitoring/reporting endpoint for administrators

## 2. Architecture

The backend uses layered architecture:

1. Web layer
- REST controllers and security entry points

2. Application layer
- Business services, DTOs, validation, auth/token services

3. Infrastructure layer
- JPA repositories, Flyway migrations, optional Redis-backed revocation

4. Data layer
- MySQL primary relational database

## 3. Technology Stack

- Java 21
- Spring Boot 3.2.x
- Spring Web + Spring Data JPA + Spring Security
- OAuth2 Client (Google login)
- jjwt (JWT generation/validation)
- Flyway (DB migrations)
- MySQL 8
- Optional Redis for distributed revocation store
- springdoc-openapi + Swagger UI
- JUnit 5 / MockMvc tests

## 4. Getting Started

### Prerequisites

- Java 21
- Maven 3.8+
- MySQL 8 running on localhost

### Run locally

1. Create database:

```sql
CREATE DATABASE smart_blog;
```

2. Create local secrets file (`.env`) in project root:

```env
DB_URL=jdbc:mysql://localhost:3306/smart_blog?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USER=root
DB_PASS=your_mysql_password

GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
```

3. Build and run:

```bash
./mvnw clean install
./mvnw spring-boot:run
```

Windows:

```powershell
.\mvnw.cmd clean install
.\mvnw.cmd spring-boot:run
```

4. Verify app:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- GraphQL endpoint: `http://localhost:8080/graphql`
- GraphiQL: `http://localhost:8080/graphiql`

## 5. Configuration

Active profile defaults to `local`.

`application-local.properties` imports root `.env`:

```properties
spring.config.import=optional:file:.env[.properties],optional:file:./config/application-local-secrets.properties
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/smart_blog?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC}
spring.datasource.username=${DB_USER:${DB_USERNAME:root}}
spring.datasource.password=${DB_PASS:${DB_PASSWORD:}}
```

Security-related configuration highlights:
- Stateless session policy for APIs
- CORS allowed origins for local clients
- CSRF enabled for browser form demonstration routes
- CSRF ignored for JWT API routes (`/auth/**` etc.)
- OAuth2 Google client registration via environment variables

## 6. API Documentation

### Primary endpoint groups

- Auth: `/auth/*`
- Security test endpoints: `/protected/*`
- CSRF form demo: `/form/*`
- Monitoring report: `/admin/security-report`
- Core business APIs: `/api/users`, `/api/posts`, `/api/comments`, `/api/tags`, `/api/reviews`

### Security and auth endpoints

- `POST /auth/register`
- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/revoke`
- `POST /auth/logout`
- OAuth start: `GET /oauth2/authorization/google`

### RBAC test endpoints

- `GET /protected/admin-test` (`ADMIN`)
- `GET /protected/author-test` (`AUTHOR` or `ADMIN`)
- `GET /protected/reader-test` (`READER`, `AUTHOR`, or `ADMIN`)

## 7. Data Model Summary

Primary entities:
- `User`
- `Post`
- `Comment`
- `Tag`
- `Review`

Relationships:
- User -> Posts (1:N)
- User -> Comments (1:N)
- User -> Reviews (1:N)
- Post -> Comments (1:N)
- Post -> Reviews (1:N)
- Post <-> Tags (N:N)

Migrations managed with Flyway under `src/main/resources/db/migration`.

## 8. Testing Guide

### Automated tests

Run all tests:

```bash
./mvnw test
```

Security-focused tests include:
- `ProtectedControllerRBACTest`
- `FormCsrfIntegrationTest`
- `OAuth2UserServiceImplTest`
- `OAuth2AuthenticationSuccessHandlerTest`
- `SecurityReportControllerTest`

### Postman testing

Use `DEMONSTRATION_GUIDE.md` for complete step-by-step verification with expected responses.

High-level order:
1. Register/login
2. RBAC endpoint checks
3. Refresh token rotation checks
4. CSRF browser/form checks
5. OAuth2 login flow
6. Security report retrieval

## 9. Performance Notes

- HikariCP connection pooling configured
- Full-text and supporting indexes applied via Flyway
- Pagination supported on list endpoints
- Optional Redis revocation store for distributed deployments

## 10. Module 7 Security Enhancement Report (Epic 1-5)

This section is structured to match Module 7 requirements and deliverables.

### 10.1 Requirement Objectives Coverage

From Module 7 objectives, this project implements:
- Spring Security configuration for authentication and authorization
- JWT-based authentication and token validation
- Google OAuth2 login integration
- BCrypt password hashing
- CORS and CSRF strategy (Postman + browser workflow)
- RBAC enforcement with role-scoped access
- DSA-oriented security optimization (hashing, concurrent maps, counters)
- Security event logging and report endpoint

### 10.2 Epic Implementation Summary

| Epic | Requirement Focus | Implementation Status | Key Evidence |
|---|---|---|---|
| Epic 1 | SecurityFilterChain, CORS, BCrypt | COMPLETE | `SecurityConfig`, CORS policy, password encoder bean |
| Epic 2 | JWT login/validation/claims | COMPLETE | `/auth/login`, `JwtService`, `JwtAuthenticationFilter`, revoke/logout/refresh |
| Epic 3 | CSRF strategy + CORS/CSRF understanding | COMPLETE | `/form/csrf`, `/form/submit`, CSRF ignored for stateless JWT routes |
| Epic 4 | Google OAuth2 + RBAC | COMPLETE | OAuth2 login wiring, local user mapping, role assignment, `@PreAuthorize` |
| Epic 5 | DSA security optimization + monitoring | COMPLETE | revocation map/Redis option, security metrics service, `/admin/security-report` |

### 10.3 Epic 1 - Security Configuration and Access Policies

Implemented:
- Custom `SecurityFilterChain`
- Public route allowances for auth/docs/form demo
- Restricted route policies by role
- BCrypt encoder for password hashing
- Global CORS policy with controlled origins/methods/headers

Requirement alignment:
- Public and restricted endpoints explicitly defined
- Unauthorized/disallowed access returns correct codes (`401`/`403`)

### 10.4 Epic 2 - JWT-Based Authentication

Implemented:
- `POST /auth/login` returns JWT access token + refresh token
- JWT contains identity and expiration claims; signed using HS256
- Per-request JWT validation via filter
- Tampered/expired/revoked token rejection path
- Token revoke/logout endpoints
- Refresh token rotation endpoint

Requirement alignment:
- Signed token issuance and validation operational
- Token structure verifiable in Postman

### 10.5 Epic 3 - CSRF and Session Security

Implemented:
- Stateless JWT APIs with CSRF ignored for API routes
- CSRF token demonstration endpoint for browser/form scenario:
  - `GET /form/csrf`
  - `POST /form/submit`

Requirement alignment:
- Practical CORS vs CSRF behavior documented and testable
- Demonstrates correct threat-model split between API and cookie/form flows

#### 10.5.1 CORS vs CSRF Technical Explanation (Required Documentation)

- CORS (Cross-Origin Resource Sharing) controls which browser origins are allowed to call the backend. It is a browser enforcement mechanism based on response headers.
- CSRF (Cross-Site Request Forgery) protects state-changing requests in cookie/session-based authentication flows from forged cross-site submissions.
- Why CSRF is different for JWT APIs:
  - In this project, JWT is sent in `Authorization: Bearer ...`, and APIs are stateless.
  - Since authentication is not based on automatically attached session cookies, CSRF protection is not required for those JWT API routes.
- Why CSRF is enabled for form/stateful scenarios:
  - Browser form submissions and session-style flows are vulnerable to CSRF because browsers auto-send cookies.
  - Therefore CSRF token validation is enabled and demonstrated on `/form/*`.

#### 10.5.2 Practical Browser-Based CSRF Demonstration

1. Open `http://localhost:8080/form/csrf` in a browser.
2. Confirm the returned HTML form includes a hidden `_csrf` field.
3. Submit the form.
4. Expected result: `201 Created` and response body like `received:<message>`.
5. Browser should hold `XSRF-TOKEN` cookie for this CSRF-protected flow.

What this proves:
- CSRF token mechanism is active for form-based state-changing requests.

#### 10.5.3 Practical Postman Tests (CORS + CSRF)

CSRF negative test:
1. Send `POST http://localhost:8080/form/submit` without CSRF token.
2. Expected result: `403 Forbidden`.

CSRF positive test:
1. Send `GET http://localhost:8080/form/csrf`.
2. Copy `_csrf` token value from HTML response and keep returned cookie.
3. Send `POST http://localhost:8080/form/submit` with `x-www-form-urlencoded` body:
   - `_csrf=<copied_token>`
   - `message=epic3-pass`
4. Expected result: `201 Created`.

CORS preflight simulation:
1. Send `OPTIONS http://localhost:8080/auth/login` with headers:
   - `Origin: http://localhost:3000`
   - `Access-Control-Request-Method: POST`
2. Expected result: allowed preflight.
3. Repeat with `Origin: http://evil.example.com`.
4. Expected result: rejected preflight (`403`).

### 10.6 Epic 4 - OAuth2 and RBAC

Implemented:
- Google OAuth2 client integration through Spring Security OAuth2
- OAuth2 user mapping to local user model and persistence
- Default role assignment for first-time OAuth users
- JWT + refresh token issuance on OAuth success
- Method-level RBAC enforcement using `@PreAuthorize`

Requirement alignment:
- Google login integrated with persistence and role mapping
- Role-based access validated through endpoint behavior

### 10.7 Epic 5 - DSA and Security Optimization

Implemented:
- BCrypt hashing for passwords
- Revocation store using `ConcurrentHashMap` (in-memory)
- Optional Redis-backed revocation for production-like deployments
- Scheduled cleanup of expired revocation entries
- `SecurityEventMetricsService` with:
  - `AtomicLong` counters
  - per-principal failure map
  - suspicious principal thresholding
- Admin-only security report endpoint:
  - `GET /admin/security-report`

Requirement alignment:
- Hashing + map-based lookup structures applied
- Authentication monitoring and access-pattern visibility provided

### 10.8 Module 7 Deliverables Mapping

| Deliverable (Module 7) | Project Implementation |
|---|---|
| Spring Security Integration | Configured filter chain and access rules |
| JWT Authentication System | Login, validation filter, refresh, revoke, logout |
| CORS & CSRF Configuration | CORS policy + CSRF strategy and demonstration routes |
| OAuth2 (Google Login) | OAuth2 client flow, user persistence, role mapping |
| RBAC Enforcement | Role-guarded endpoints + method security |
| Security Event Logging | Structured security event logs and counters |
| DSA Implementation | Concurrent maps, hashing, token validation logic |
| README & OpenAPI Docs | This README + Swagger/OpenAPI endpoints |

### 10.9 Evaluation Readiness (Panel Demonstration)

Module 7 evaluation categories are covered through implementation and repeatable testing:
- Security configuration (CORS/CSRF)
- JWT implementation
- OAuth2 integration
- RBAC
- DSA application in security logic
- Testing and logging evidence
- Documentation quality

## 11. Module 8 Advanced Optimization Report (Epic 1-5)

This section documents the full Module 8 performance journey from baseline profiling to final observability and production-readiness verification.

### 11.1 Module 8 Scope and Objective

Module 8 goals implemented in this project:
- Profile and identify bottlenecks with evidence.
- Introduce asynchronous execution for read-heavy API paths.
- Ensure thread safety under concurrent token and request operations.
- Optimize data-path performance with caching and query improvements.
- Build runtime metrics collection and reporting workflow.

Primary optimized endpoint scenarios:
- `GET /api/posts?page=0&size=20` (post retrieval)
- `GET /api/comments/post/1?page=0&size=20` (comment loading)
- `GET /api/reviews/post/1/stats` (user analytics/review stats)

### 11.2 Epic-by-Epic Implementation Summary

| Epic | Focus | Key Implementation | Status |
|---|---|---|---|
| Epic 1 | Bottleneck analysis | Baseline profiling script + JFR/latency/thread/heap artifacts | Complete |
| Epic 2 | Async programming | `CompletableFuture` controller paths + custom executor + async A/B benchmark | Complete |
| Epic 3 | Concurrency + thread safety | Token-store race hardening + concurrency tests + pool tuning matrix | Complete |
| Epic 4 | Data optimization | Review stats single-query path + cache strategy + optimization benchmark | Complete |
| Epic 5 | Metrics and reporting | Actuator/Prometheus metrics pipeline + final report artifacts | Complete |

### 11.3 Epic 1 - Performance Bottleneck Analysis

Implemented:
- Baseline harness: `scripts/epic1_baseline_capture.ps1`
- Evidence set:
  - `analysis/epic-tests/epic-1/baseline-summary.json`
  - `analysis/epic-tests/epic-1/baseline-latency-raw.csv`
  - `analysis/epic-tests/epic-1/epic-1-baseline.jfr`
  - `analysis/epic-tests/epic-1/epic-1-threaddump.txt`
  - `analysis/epic-tests/epic-1/epic-1-heap-info.txt`
  - JMC screenshots in `analysis/epic-tests/epic-1/Screenshots/`

Outcome:
- Established a reproducible baseline.
- Identified highest tail latency focus areas for subsequent epics.

### 11.4 Epic 2 - Asynchronous Programming

Implemented:
- Async executor configuration:
  - `src/main/java/com/smartblog/config/AsyncConfig.java`
- Async endpoint execution in controllers:
  - `src/main/java/com/smartblog/controller/PostController.java`
  - `src/main/java/com/smartblog/controller/CommentController.java`
  - `src/main/java/com/smartblog/controller/ReviewController.java`
- JWT async dispatch compatibility:
  - `src/main/java/com/smartblog/auth/JwtAuthenticationFilter.java`
- Benchmark tooling:
  - `scripts/epic2_async_ab_test.ps1`
  - `analysis/epic-tests/epic-2/epic-2-async-ab-summary.json`

Result:
- Async mode improved concurrent average latency and p95 against sync mode in controlled A/B tests.
- No data loss/corruption symptoms observed in benchmark runs.

### 11.5 Epic 3 - Concurrency and Thread Safety

Implemented:
- Thread-safe refresh token handling:
  - `src/main/java/com/smartblog/auth/InMemoryRefreshTokenService.java`
  - `src/main/java/com/smartblog/auth/RedisRefreshTokenService.java`
- Concurrency validation:
  - `src/test/java/com/smartblog/auth/InMemoryRefreshTokenServiceConcurrencyTest.java`
- Thread-pool tuning matrix:
  - `scripts/epic3_threadpool_tuning.ps1`
  - `analysis/epic-tests/epic-3/epic-3-threadpool-tuning-summary.json`

Result:
- One-time token consumption behavior validated under contention.
- Optimal async pool profile selected from measured CPU/memory/latency trade-offs.

### 11.6 Epic 4 - Data and Algorithmic Optimization

Implemented:
- Single-query review stats aggregation:
  - `src/main/java/com/smartblog/infrastructure/repository/jpa/ReviewJpaRepository.java`
  - `src/main/java/com/smartblog/application/service/impl/ReviewServiceImpl.java`
- Caching for hot read paths:
  - `src/main/java/com/smartblog/application/service/impl/CommentServiceImpl.java`
  - `src/main/java/com/smartblog/application/service/impl/ReviewServiceImpl.java`
  - `src/main/java/com/smartblog/config/CacheConfig.java`
  - `src/main/java/com/smartblog/config/OptimizationToggle.java`
- Verification tests:
  - `src/test/java/com/smartblog/application/service/ReviewServiceImplOptimizationTest.java`
  - `src/test/java/com/smartblog/config/Epic4CachingContractTest.java`

Result:
- Significant gains on comment loading and analytics paths.
- One known regression (`post_retrieval`) tracked as accepted Epic 4 risk with documented rationale.

### 11.7 Epic 5 - Final Metrics, Stability, and Readiness

Implemented:
- Metrics dependencies and `@Timed` support:
  - `pom.xml`
  - `src/main/java/com/smartblog/config/MetricsConfig.java`
  - timed endpoints/services in post paths
- Runtime exposure:
  - `src/main/resources/application.properties`
  - `src/main/resources/application-prod.properties`
  - `src/main/resources/application-test.properties`
- Metrics/reporting harness:
  - `scripts/epic5_metrics_reporting.ps1`
  - `analysis/epic-tests/epic-5/epic-5-metrics-summary.json`
  - `analysis/epic-tests/epic-5/epic-5-metrics-report.md`
  - `analysis/epic-tests/epic-5/prom-before.txt`
  - `analysis/epic-tests/epic-5/prom-after.txt`

Key verified run snapshot:
- Requests: `240`
- Errors: `0`
- Overall avg latency: `57.12 ms`
- Overall p95 latency: `102.70 ms`
- Throughput estimate: `24.33 req/s`

### 11.8 Final Performance and Stability Interpretation

What improved:
- Higher concurrency resilience through async decoupling and tuned pools.
- Better hot-path response times via cache + query consolidation.
- Stronger operational confidence through structured metrics collection.

What was validated:
- Functional and security behavior remained intact.
- No critical runtime instability observed across epic benchmark runs.
- Performance evidence is reproducible via script-driven artifacts.

### 11.9 Module 8 Deliverables Mapping

| Module 8 Deliverable | Project Implementation |
|---|---|
| Bottleneck profiling | Epic 1 baseline harness + JFR/thread/heap artifacts |
| Async optimization | Async controllers + executor + A/B benchmark artifacts |
| Thread safety and tuning | Token concurrency hardening + tuning matrix + tests |
| Data-path optimization | Cache strategy + single-query aggregation + optimization report |
| Metrics and reporting | Actuator/Prometheus pipeline + final metrics report package |

### 11.10 Reproducibility Commands

Windows PowerShell:

```powershell
.\scripts\epic1_baseline_capture.ps1 -Profile local -Port 8085 -WarmupIterations 5 -MeasureIterations 25
.\scripts\epic2_async_ab_test.ps1 -Profile local -Port 8110 -Users 12 -RequestsPerUser 10 -TomcatMaxThreads 8
.\scripts\epic3_threadpool_tuning.ps1 -Profile local -StartPort 8120 -Users 12 -RequestsPerUser 10 -TomcatMaxThreads 8
.\scripts\epic4_data_optimization_compare.ps1 -Profile local -Port 8130 -Users 10 -RequestsPerUser 20 -TomcatMaxThreads 8
.\scripts\epic5_metrics_reporting.ps1 -Profile local -Port 8140 -Users 8 -RequestsPerUser 10 -TomcatMaxThreads 8
```

Expected outputs:
- `analysis/epic-tests/epic-1` through `analysis/epic-tests/epic-5` artifact directories.

### 11.11 Final Module 8 Status

- Epic 1: completed and verified
- Epic 2: completed and verified
- Epic 3: completed and verified
- Epic 4: completed and verified
- Epic 5: completed and verified

Project conclusion:
- Module 8 implementation is complete, evidence-backed, and defense-ready for performance optimization evaluation.



---

Author: Andy Kwasi Debrah

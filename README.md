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



---

Author: Andy Kwasi Debrah

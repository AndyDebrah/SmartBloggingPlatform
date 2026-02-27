# SmartBlog Backend Demonstration Guide

## 1. System Overview
SmartBlog is a Spring Boot backend with layered architecture (web -> service -> repository), secured by Spring Security.

- Stateless JWT security for API access
- Google OAuth2 login integration with local user mapping
- RBAC using `ADMIN`, `AUTHOR`, `READER`
- Refresh token rotation (single-use refresh)
- Security monitoring counters and admin report endpoint

Base URL used in examples:

```bash
BASE_URL="http://localhost:8080"
```

Note: this project currently exposes auth endpoints as `/auth/*` (not `/api/auth/*`). If you use a gateway prefix, replace accordingly in Postman.

---

## 2. Epic 1 – User Registration & Login

### 2.1 Register a New User

```bash
curl -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "reader_demo",
    "email": "reader_demo@example.com",
    "password": "Password123!",
    "role": "READER"
  }'
```

Expected response (`201 Created`):

```json
{
  "token": "<jwt_access_token>",
  "refreshToken": "<refresh_token_uuid>"
}
```

Internal behavior:
- Request is validated, user is created, and password is hashed with BCrypt.
- User is persisted to MySQL.
- A JWT + refresh token is issued immediately for a seamless first login.

Security/architecture principle:
- Secure credential storage (BCrypt), strong default identity onboarding, stateless auth bootstrap.

### 2.2 Login with Username & Password

```bash
curl -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "reader_demo",
    "password": "Password123!"
  }'
```

Expected response (`200 OK`):

```json
{
  "token": "<jwt_access_token>",
  "refreshToken": "<refresh_token_uuid>"
}
```

Internal behavior:
- `AuthenticationManager` authenticates credentials via `UserDetailsService`.
- On success, JWT is signed (HS256) and includes identity claims.
- Refresh token is created for later rotation flow.

Security/architecture principle:
- Stateless authentication with signed token integrity and reduced session coupling.

---

## 3. Epic 2 – Role-Based Access Control (RBAC)

### 3.1 Access Protected Endpoint as READER

Use a READER token from login:

```bash
READER_TOKEN="<paste_reader_token>"
curl -X GET "$BASE_URL/protected/admin-test" \
  -H "Authorization: Bearer $READER_TOKEN"
```

Expected response (`403 Forbidden`):

```json
{
  "status": "ERROR",
  "statusCode": 403,
  "message": "Access is denied",
  "timestamp": "..."
}
```

Internal behavior:
- JWT filter validates token and sets authenticated principal.
- Method-level `@PreAuthorize("hasRole('ADMIN')")` denies READER.

Security/architecture principle:
- Principle of Least Privilege: authenticated does not mean authorized.

### 3.2 Access as ADMIN

```bash
ADMIN_TOKEN="<paste_admin_token>"
curl -X GET "$BASE_URL/protected/admin-test" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

Expected response (`200 OK`):

```json
"admin-access"
```

Internal behavior:
- Role from user authorities satisfies `hasRole('ADMIN')`.

Security/architecture principle:
- Deterministic RBAC enforcement via Spring Security method authorization.

---

## 4. Epic 3 – Refresh Token Rotation

### 4.1 Use Refresh Token

```bash
REFRESH_TOKEN="<paste_refresh_token>"
curl -X POST "$BASE_URL/auth/refresh" \
  -H "Content-Type: application/json" \
  -d "{
    \"password\": \"$REFRESH_TOKEN\"
  }"
```

Expected response (`200 OK`):

```json
{
  "token": "<new_access_token>",
  "refreshToken": "<new_refresh_token>"
}
```

Internal behavior:
- Refresh token is validated and consumed (single use).
- New access token and new refresh token are issued.
- Old refresh token becomes invalid.

Security/architecture principle:
- Rotation limits replay window and strengthens token lifecycle control.

### 4.2 Attempt Reuse of Old Refresh Token

Use the previously consumed token again:

```bash
curl -X POST "$BASE_URL/auth/refresh" \
  -H "Content-Type: application/json" \
  -d "{
    \"password\": \"$REFRESH_TOKEN\"
  }"
```

Expected response (`401 Unauthorized`):

```json
"Invalid or expired refresh token"
```

Internal behavior:
- Service cannot validate consumed/expired refresh token.

Security/architecture principle:
- Replay-attack prevention for long-lived token artifacts.

---

## 5. Epic 4 – OAuth2 Login (Google)

### 5.1 Login via OAuth2

Browser flow entry point:

```bash
curl -i "$BASE_URL/oauth2/authorization/google"
```

Expected behavior:
- Redirect to Google consent/login.
- On success, callback triggers local user upsert and token issuance.

Internal behavior:
- Google identity is federated into local `User` domain.
- First login creates local user and assigns default `READER` role.

Security/architecture principle:
- Separation of identity provider (Google) from local authorization model (RBAC).

### 5.2 JWT Issuance After OAuth2 Success

Expected JSON response at successful OAuth2 completion:

```json
{
  "token": "<jwt_access_token>",
  "refreshToken": "<refresh_token_uuid>"
}
```

Internal behavior:
- OAuth principal is mapped to local user.
- Success handler issues standard JWT + refresh token, same as username/password flow.

Security/architecture principle:
- Unified stateless token model across multiple authentication methods.

---

## 6. Epic 5 – Security Monitoring & Metrics

### 6.1 Trigger Failed Login Attempts

Run multiple failed attempts:

```bash
curl -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "reader_demo",
    "password": "wrong-password"
  }'
```

Expected response (`401 Unauthorized`):

```json
{
  "status": "ERROR",
  "statusCode": 401,
  "message": "Invalid credentials",
  "timestamp": "..."
}
```

Internal behavior:
- Failure counters increment.
- Failed principal map (`loginFailuresByPrincipal`) updates.
- Structured security log is emitted.

Security/architecture principle:
- Built-in security observability and attack-signal capture.

### 6.2 View Security Metrics Endpoint

Use ADMIN token:

```bash
curl -X GET "$BASE_URL/admin/security-report" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

Expected response (`200 OK`):

```json
{
  "status": "SUCCESS",
  "statusCode": 200,
  "message": "Security report generated",
  "data": {
    "loginAttempts": 8,
    "loginSuccesses": 2,
    "loginFailures": 6,
    "oauth2Successes": 1,
    "refreshSuccesses": 1,
    "refreshFailures": 1,
    "tokenValidationSuccesses": 10,
    "tokenValidationFailures": 3,
    "tokenRevocations": 1,
    "unauthorizedEvents": 2,
    "accessDeniedEvents": 1,
    "loginFailuresByPrincipal": {
      "reader_demo": 6
    },
    "suspiciousPrincipals": [
      "reader_demo"
    ],
    "bruteForceThreshold": 5
  },
  "timestamp": "..."
}
```

Internal behavior:
- Snapshot aggregates in-memory counters and failure map.
- Principals above threshold are flagged as suspicious.

Security/architecture principle:
- Operational security readiness through measurable telemetry and admin-only access.

---

## 7. Token Structure Explanation

A JWT has 3 parts: `header.payload.signature`

- Header: algorithm and token type
- Payload: claims (`sub`, `roles`, `iat`, `exp`)
- Signature: integrity protection (HS256 with server secret)

Example decoded JWT:

```json
{
  "header": {
    "alg": "HS256",
    "typ": "JWT"
  },
  "payload": {
    "sub": "reader_demo",
    "roles": ["ROLE_READER"],
    "iat": 1771947908,
    "exp": 1771951508
  },
  "signature": "HMAC_SHA256(base64Url(header) + '.' + base64Url(payload), secret)"
}
```

What this demonstrates:
- `sub` identifies principal.
- `roles` drive RBAC decisions.
- `exp` enforces token lifetime.
- Signature prevents tampering.

---

## 8. Architectural Principles Demonstrated

- Stateless session management:
  each request carries auth via bearer token; no server HTTP session dependency.
- Separation of identity and authorization:
  OAuth2 handles identity proof; local RBAC decides access.
- Defense in depth:
  JWT validation + revocation checks + method-level authorization + exception mapping.
- Least privilege:
  role-specific endpoint restrictions and admin-only monitoring endpoint.
- Refresh token rotation:
  single-use refresh tokens reduce replay risk.
- Observability integration:
  structured security events, counters, and report endpoint.

---

## Postman Execution Notes (for your demo panel)

1. Create an environment with `baseUrl`, `adminToken`, `readerToken`, `refreshToken`.
2. Test in this order: register -> login -> RBAC checks -> refresh rotation -> monitoring.
3. Save each successful response as example evidence.
4. For OAuth2, use browser flow and then test protected endpoints with returned JWT.
5. During presentation, show both success and intentional failure cases (`401`, `403`) to prove controls are active.

---

## Epic 3 Required CSRF Demonstration (Exact Postman Procedure)

Use this exact sequence for the Epic 3 CSRF acceptance check.

1. Create a new Postman request:
- `GET http://localhost:8080/form/csrf`

2. Click **Send**.

3. In response body, find hidden token line like:
- `name="_csrf" value="...TOKEN..."`
- Copy only the token value.

4. In the same GET response, open **Cookies** (near Send button) for `localhost`.
- Find cookie `XSRF-TOKEN` and copy its value.

5. Create second request:
- `POST http://localhost:8080/form/submit`

6. Go to **Body** -> select **x-www-form-urlencoded** and add:
- key `_csrf` value `<token from step 3>`
- key `message` value `epic3-pass`

7. Go to **Headers** and add:
- `X-XSRF-TOKEN = <same token from step 3>`

8. Open **Cookies** for this POST request (`localhost`) and set:
- `XSRF-TOKEN = <same value from step 4>`

9. Ensure both requests use exactly:
- `http://localhost:8080`
- Do not mix with `127.0.0.1`.

10. Send POST immediately (do not run GET again first).

Expected result:
- `201 Created`
- body: `received:epic3-pass`

If it still fails, capture screenshots of:
- POST Headers
- POST Body
- POST Cookies

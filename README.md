# Rate Limiting API — Spring Boot

A backend system built to explore how real-world APIs handle authentication and abuse prevention.
Implements JWT-based auth and per-user rate limiting using the Token Bucket algorithm, backed by Redis.

---

## Why I built this

Most tutorials show JWT auth in isolation. I wanted to see how auth, rate limiting, and request filtering
work together in a single request lifecycle — and how Redis fits into that beyond just caching.

---

## Tech Stack

- Java 21 + Spring Boot 3
- Spring Security 6
- MySQL + JPA (Flyway migrations)
- Redis + Bucket4j (rate limiting)
- Docker + Docker Compose

---

## How it works

Every request goes through two filters in order:
```
Request → JwtAuthenticationFilter → RateLimitFilter → Controller
```

**JwtAuthenticationFilter** — validates the token, extracts userId, sets Spring Security context

**RateLimitFilter** — fetches the user's plan from Redis, checks remaining tokens, allows or blocks

If limit is hit:
```json
{
  "Status": "429 TOO_MANY_REQUESTS",
  "Description": "API request limit linked to your current plan has been exhausted."
}
```

---

## Plans & Limits

| Plan         | Requests/Hour |
|--------------|---------------|
| FREE         | 20            |
| BUSINESS     | 40            |
| PROFESSIONAL | 100           |

---

## Rate Limit Headers

| Header                            | What it tells you                 |
|-----------------------------------|-----------------------------------|
| X-Rate-Limit-Remaining            | Tokens left in current window     |
| X-Rate-Limit-Retry-After-Seconds  | Seconds until limit resets        |

---

## What I added

- **Redis caching layer** (`CachedLookupService`) on top of the existing rate limiter
    - User auth details cached by email — avoids DB hit on every login
    - Active plan limit cached by userId — avoids DB hit on every API request
    - Cache eviction wired to plan updates so stale data is never served
    - 10 minute TTL configured via Spring Cache abstraction

---

## Running locally

**Option 1 — Pull from Docker Hub (recommended)**
```bash
docker-compose up -d
```

**Option 2 — Build locally**
```bash
docker-compose -f docker-compose.dev.yml up -d
```

Swagger UI: `http://localhost:8080/swagger-ui.html`

---

## Live Demo

Swagger UI: https://rate-limiting-api-production.up.railway.app/swagger-ui/index.html

### Test credentials
To test the API:

1. GET `/api/v1/plan` — copy any Plan ID
2. POST `/api/v1/user` — create account:
```json
{
  "EmailId": "test@gmail.com",
  "Password": "Test@1234",
  "PlanId": "<plan-id-from-above>"
}
```
3. POST `/api/v1/auth/login` — get token:
```json
{
  "EmailId": "test@gmail.com",
  "Password": "Test@1234"
}
```
4. Click **Authorize** in Swagger → paste token as `Bearer <token>`

## What I learned

- How Spring Security filter chain actually works
- Where JWT validation fits vs where rate limiting fits
- Why Redis makes rate limiting stateless and scalable
- How Token Bucket algorithm handles bursts vs sustained traffic
- How to layer Redis caching cleanly without touching existing logic
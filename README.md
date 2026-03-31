# Rate Limiting API — Spring Boot

A backend system built to explore how real-world APIs handle authentication and abuse prevention.

Implements JWT-based auth and per-user rate limiting using the Token Bucket algorithm, backed by Redis.

---

## Why I built this

Most tutorials show JWT auth in isolation. I wanted to see how auth, rate limiting, and request filtering
work together in a single request lifecycle — and how Redis fits into that beyond just caching.

---

## Tech Stack

- Java 17 + Spring Boot 3
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

| Header                          | What it tells you                        |
|---------------------------------|------------------------------------------|
| X-Rate-Limit-Remaining          | Tokens left in current window            |
| X-Rate-Limit-Retry-After-Seconds| Seconds until limit resets               |

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

## What I learned

- How Spring Security filter chain actually works
- Where JWT validation fits vs where rate limiting fits
- Why Redis makes rate limiting stateless and scalable
- How Token Bucket algorithm handles bursts vs sustained traffic
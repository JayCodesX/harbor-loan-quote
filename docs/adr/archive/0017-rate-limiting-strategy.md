# ADR 0017: Rate Limiting — Per-IP Only vs. Layered Per-IP and Per-User

## Status
Archived — valid, deferred to production readiness phase

Rate limiting remains an important production concern. Will be
revisited during Phase 3.

## Date
Fill in when you write this

## Phase
1 — Foundation

---

## Context

WRITE THIS YOURSELF

Prompts to answer:
- What rate limiting exists today?
  (Nginx: limit_req_zone $binary_remote_addr zone=api_limit:10m rate=20r/s with burst=40)
- What is the corporate NAT problem?
  (A company with 500 employees behind one NAT IP shares one rate limit bucket.
   30 simultaneous users exceed 20r/s and get 429 responses. Legitimate users blocked.)
- What is the distributed abuse problem?
  (An attacker with 10 accounts from 10 IPs can make 200 req/s total.
   Per-IP limit provides no protection against authenticated multi-account abuse.)
- What is a lead aggregator's specific rate limiting concern?
  (Rate scraping: competitors programmatically pulling rates across all credit tiers,
   loan amounts, and property types to build a rate comparison database.
   Bot traffic submitting fake leads to pollute the lead pipeline.)
- What library implements per-user rate limiting in Spring?
  (Bucket4j — token bucket algorithm, Redis-backed for distributed enforcement)


---

## Decision

WRITE THIS YOURSELF

What is the decision? Be concrete — a sentence or two that could stand alone.
Describe the *what*, not the *why* (why belongs in Rationale).

---

## Alternatives Considered

### Alternative: Per-IP only (current state)
**Strengths:** Zero application code, handled entirely at Nginx, no Redis dependency for rate limiting
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- Corporate NAT: 500 employees behind one IP. The entire company shares one rate limit bucket.
  30 employees using the quote tool simultaneously = 429 errors for all of them.
  Per-IP rate limiting punishes legitimate corporate users.
- Authenticated abuse: an attacker with 10 accounts from 10 IPs or a rotating proxy
  can bypass the per-IP limit entirely. Each IP is under the limit; the abuse is not.
- Lead aggregators are specifically targeted by competitors and fraud rings who
  create many accounts from many IPs to scrape pricing data.

### Alternative: Per-user rate limiting only (no IP layer)
**Strengths:** Correct for authenticated traffic, no corporate NAT problem
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- Unauthenticated endpoints (public quote form, login, registration) are not covered
  by per-user limits since there is no authenticated user.
- DDoS from a single IP against the login endpoint or public quote endpoint
  is not protected without an IP-level limit.
- The IP limit is the first line of defense against volumetric attacks.
  Per-user is the second layer for authenticated abuse. Both are needed.

### Alternative: API Gateway rate limiting (AWS API Gateway or Kong)
**Strengths:** Centralized, configurable without code changes, supports complex rules
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- API Gateway adds infrastructure complexity inappropriate for Phase 1 VPS deployment.
- Bucket4j + Redis is sufficient for the application's scale and is library-level —
  no additional infrastructure required (Redis already exists for other purposes).
- Phase 3 can evaluate AWS WAF rate limiting rules at the ALB/CloudFront level
  as a complement to application-level limits.

---

## Rationale

### Different Rate Limiting Concerns Require Different Rate Limiting Strategies

WRITE THIS YOURSELF
The three layers address three distinct threats:
1. IP layer: volumetric attacks, credential stuffing, bot floods against public endpoints
2. User layer: authenticated scraping, abuse of the quote API to mine rate data, lead fraud
3. Session layer: anonymous scraping of the public quote form before account creation

Each layer operates on a different identity — IP address, user ID, session token.
A single layer cannot address all three threats simultaneously.
The layered model applies the minimum necessary restriction at each layer:
20r/s per IP is generous for a single user, punitive for a bot flood.
100 req/min per user is generous for legitimate use, punitive for automated scraping.

---

## Consequences

### Positive

WRITE THIS YOURSELF
- List the positive consequences of this decision.

### Negative

WRITE THIS YOURSELF
- List the negative consequences. Be honest — no decision is without trade-offs.

---

## Follow-up

WRITE THIS YOURSELF
- List follow-up actions, related ADRs to write, or open questions to resolve.

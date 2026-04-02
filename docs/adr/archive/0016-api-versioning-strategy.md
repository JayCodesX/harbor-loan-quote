# ADR 0016: API Versioning Strategy — URL Path Prefix vs. Header vs. None

## Status
Archived — valid, deferred

API versioning strategy remains applicable. Will be implemented
as the API surface stabilizes.

## Date
Fill in when you write this

## Phase
1 — Foundation

---

## Context

WRITE THIS YOURSELF

Prompts to answer:
- What does the current API look like?
  (All routes are unversioned: /api/quotes, /api/quotes/{id}/refine,
   /api/directory/partners, /api/auth/login, etc.)
- What consumers will exist for this API?
  (Borrower-facing web app, admin-web, potential lender partner integrations in Phase 2)
- What is a breaking change?
  (Renaming a field, removing a field, changing a field's type, reordering enum values,
   requiring a previously optional field)
- What is the cost of adding versioning now vs. later?
  (Now: update Nginx config, update controller mappings, update frontend calls.
   After external integrations exist: coordinate with all partners, maintain old routes, migration project.)
- What do the major mortgage API providers do?
  (Most use URL path versioning: /v1/, /v2/ — it is visible, debuggable, and cacheable)


---

## Decision

WRITE THIS YOURSELF

What is the decision? Be concrete — a sentence or two that could stand alone.
Describe the *what*, not the *why* (why belongs in Rationale).

---

## Alternatives Considered

### Alternative: No versioning (current state)
**Strengths:** Zero implementation cost today, no prefix in URLs, simpler initial routing
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- "We'll add it when we need it" is the most expensive versioning strategy.
  The cost of adding versioning after external integrations exist is:
  coordinate cutover with every partner, maintain old routes indefinitely,
  or break integrations.
- The cost of adding versioning before external integrations exist is:
  a one-day change to Nginx config, controller mappings, and frontend API calls.
- Mortgage lenders and broker partners build integrations against stable APIs.
  Telling a partner "we changed the URL structure" is a professional embarrassment.
- Add versioning now. The cost is trivial. The regret of not doing it is guaranteed.

### Alternative: Content negotiation versioning (Accept header)
**Strengths:** Follows REST principles more strictly — URL identifies the resource, headers describe representation
Example: `Accept: application/vnd.harbor.v1+json`
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- Header-based versioning is invisible in browser address bars and server logs.
  Debugging "which version is being called?" requires inspecting headers, not the URL.
- Most API gateways, proxies, and caching layers operate on URL, not headers.
  URL versioning is cache-key friendly; header versioning requires Vary headers for correct caching.
- Partner developers find URL versioning significantly easier to work with.
  "Call /v1/quotes" is unambiguous. "Set Accept: application/vnd.harbor.v1+json" requires documentation.
- REST purity is less valuable than developer ergonomics for an API that partners integrate with.

### Alternative: Query parameter versioning (?version=1)
**Strengths:** Backward compatible — existing routes continue to work without the parameter
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- Query parameters are often stripped by CDN caches, logging configurations, and API gateways.
- Version as a query parameter looks like a filter, not a structural part of the API contract.
- Not a widely recognized pattern — URL path versioning is the industry default.

---

## Rationale

### URL Path Versioning Is Visible and Debuggable

WRITE THIS YOURSELF
Every request in every log shows /v1/ explicitly.
A developer can look at a Nginx access log line and immediately know which API version was called.
A partner can read a curl command and immediately understand which version to use.
Visibility reduces support burden and debugging time.

### Partner Integration Stability Is a Business Requirement

WRITE THIS YOURSELF
In Phase 2, lenders and brokers will integrate against the partner-facing pricing API
(rate sheet queries, eligibility checks, quote requests).
These integrations represent business relationships. Breaking changes destroy trust.
Versioning allows Harbor to evolve the API (add Phase 3 features, deprecate old fields)
without breaking existing partner integrations.
The `/v1/` prefix is a contractual commitment that v1 behavior will not change unexpectedly.
When a v2 is needed, `/v2/` is introduced, partners migrate on their own schedule,
and `/v1/` is deprecated with a sunset date.

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

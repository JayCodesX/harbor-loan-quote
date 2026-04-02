# ADR 0005: Redis Rate Sheet Cache in pricing-service

## Status
Superseded

Redis caching strategy for the pricing service will be redesigned
as part of the pricing engine rebuild in Phase 2. See ADR-0019
(Rate Sheet Data Model) for the current pricing direction.

## Date
Fill in when you write this

## Phase
1 — Foundation

---

## Context

WRITE THIS YOURSELF — your mortgage domain knowledge is critical here.

Prompts to answer:
- What is a rate sheet and how often does it change?
  (Published by investors 1-4 times per day, sometimes mid-day during volatile markets.
   Has an effective window — valid from time X until superseded or expired.)
- How many times is a rate sheet read per day vs. how many times is it written?
  (Written: 1-4 times per day per investor.
   Read: Every quote calculation reads it. Could be hundreds or thousands of times.)
- What is the cost of reading rate sheet data from MySQL on every quote request?
  (Rate sheets have many rows — hundreds of rate/price pairs per rate sheet.
   A MySQL query per quote request, with joins across rate_sheets → rate_sheet_entries
   → product_terms → llpa_adjustments, under concurrent load, adds up.)
- What is the acceptable staleness window?
  (A rate sheet is valid for hours. Showing a rate that is 30 seconds stale
   is operationally acceptable. Rate changes are published events, not continuous.)
- What data specifically gets cached?
  (The active rate sheets for today: rate/price pairs, LLPA matrices, lock period
   adjustments — all pre-loaded and structured for fast in-memory lookup.)
- When does the cache get invalidated?
  (On rate sheet ingestion — when a new rate sheet is loaded and becomes ACTIVE,
   the cache is warmed with the new data. This is an event-driven invalidation.)


---

## Decision

WRITE THIS YOURSELF

What is the decision? Be concrete — a sentence or two that could stand alone.
Describe the *what*, not the *why* (why belongs in Rationale).

---

## Alternatives Considered

### Alternative: No cache — query MySQL on every quote request
**Strengths:** Always returns the absolute latest data, no cache invalidation logic, simpler implementation, no Redis dependency
**Rejected because:**
WRITE THIS YOURSELF
Key points:
- Rate sheet data involves multiple table joins per quote (rate_sheets, rate_sheet_entries,
  product_terms, llpa_adjustments, lock_period_adjustments)
- Under concurrent load (multiple quote requests simultaneously), these joins compete
  for MySQL connection pool slots and buffer pool
- Rate sheets change at most 4 times per day. Reading from the database thousands of
  times for data that changes 4 times a day is unnecessary I/O
- The "always latest" argument is a false benefit — rate sheets have effective windows
  measured in hours, not seconds. 30 seconds of cache staleness is operationally irrelevant.

### Alternative: JPA second-level cache (Hibernate + Ehcache/Caffeine)
**Strengths:** Built into JPA, no separate infrastructure, automatic invalidation on entity updates
**Rejected because:**
WRITE THIS YOURSELF
Key points:
- JPA second-level cache lives in-process (JVM heap). With multiple instances
  of pricing-service, each instance has its own cache with no synchronization.
- When a new rate sheet is ingested, only the instance that processed the ingestion
  invalidates its cache. Other instances continue serving stale data.
- Redis is a shared external cache — all instances read from and write to the same
  cache, so all instances see the same data after invalidation.
- For a rate sheet (which must be consistent across all instances), a shared external
  cache is architecturally correct.

### Alternative: Cache at the harbor-api level (cache quote results)
**Strengths:** Eliminates pricing-service call entirely for repeated identical requests, maximum latency reduction
**Rejected because:**
WRITE THIS YOURSELF
Key points:
- Quote requests include borrower-specific parameters (credit score tier, LTV, loan amount,
  property type) that vary per request. Cache hit rate would be extremely low.
- Caching at the rate data level (inside pricing-service) is the right granularity —
  the data that is actually stable and reused is the rate sheets, not the quote results.
- Caching quote results would also cache stale rates after a rate sheet change —
  a borrower could get a quoted rate that is no longer valid.

---

## Rationale

### Rate Sheet Data Has the Ideal Caching Profile

The three properties that make data a good cache candidate are:
1. **Read far more often than written** ✅ — thousands of reads, 1-4 writes per day
2. **Expensive to recompute or re-fetch** ✅ — multi-table joins under concurrent load
3. **Acceptable staleness within a time window** ✅ — rate sheets are valid for hours

WRITE THIS YOURSELF
Expand on why each property applies to rate sheet data specifically.
Use concrete numbers from the mortgage domain where you can:
- How many rate/price pairs in a typical rate sheet?
- How many LLPA adjustments does a typical investor have?
- What is the real cost of re-reading all of that on every quote?

### Event-Driven Invalidation Matches Rate Sheet Business Logic

WRITE THIS YOURSELF
Key idea: Rate sheets are not updated continuously — they are replaced atomically.
An investor publishes a new rate sheet, it becomes effective at a specific time,
and the previous sheet is superseded. This is a domain event: RateSheetActivated.

The cache strategy maps directly to this business event:
- New rate sheet ingested → parsed → validated → written to DB as ACTIVE
- On ACTIVE status: warm Redis with the new rate/price pairs and LLPA matrices
- Old cache entries are replaced atomically
- All instances of pricing-service see the new data on their next request

This is cleaner than TTL-based invalidation (where stale data lives until expiry)
and cleaner than polling (where you check for changes on every request).

### Shared Redis Guarantees Consistency Across Instances

WRITE THIS YOURSELF
Key idea: With multiple pricing-service instances (horizontal scaling),
in-process caches diverge. Redis as a shared cache means all instances
serve the same rates after a single invalidation event.
This matters for a financial product — all users should see the same rates
at the same time, not rates that vary by which instance served their request.

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

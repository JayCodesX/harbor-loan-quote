# ADR 0048: Dynamic Rate Sheet Update Strategy

## Status
Proposed

## Date
2026-04-02

## Phase
2 — Pricing Engine

---

## Context

Rate sheets are currently ingested via manual admin upload. Investors publish rate sheet updates 1-4 times per day — and more frequently during volatile markets. A manual process at that cadence is error-prone and does not scale. A missed upload means borrowers are quoted stale rates; an upload at the wrong time can disrupt active sessions.

There are two sub-problems. First, detection: how does the system know a new rate sheet has been published? Second, propagation: once detected, how does the system invalidate stale cached rates and notify borrowers who are actively comparing rates?

A rate change during an active borrower session should surface within minutes — a borrower comparing rates needs to know the numbers have moved. A rate change overnight is acceptable to surface on next session. This ADR covers the internal detection and propagation strategy. Borrower-facing alert subscriptions are covered by ADR-0026; real-time SSE delivery is covered by ADR-0049.

---

## Decision

Rate sheet ingestion is triggered manually by an admin in Phase 2 — formal investor API agreements required for automated webhook delivery are a Phase 3 dependency. When a rate sheet is ingested and activated, pricing-service publishes a `RateSheetActivated` event on the message broker. Downstream consumers handle two concerns: the Redis cache is invalidated for the previous rate sheet and warmed with the new rates, and borrowers who do not have a locked price are notified that rates have changed. Webhook-triggered ingestion replaces the manual trigger in Phase 3 once investor integrations are formalized.

---

## Alternatives Considered

### Alternative: Scheduled polling — pricing-service polls investor feed on a cron
**Strengths:** Simple to implement, no webhook infrastructure needed, predictable load pattern
**Rejected because:**
Rate sheets arrive on the investor's schedule, not a predictable cron interval. Polling assumes you know when to check — you don't. Any polling interval creates a staleness window that is outside the system's control. A rate change published by an investor at 10:02 AM is invisible until the next poll fires. For intraday repricing where a borrower is actively comparing rates, that window is unacceptable. Detection must happen as the feed comes in, not on a timer.

### Alternative: Webhook push — investor pushes rate sheet to a pricing-service endpoint
**Strengths:** Lowest latency (sub-second), event-driven, no polling overhead
**Deferred to Phase 3 because:**
Webhook delivery requires a formal investor integration — the investor's system must know the ingest endpoint, and the payload must be signature-verified to prevent spoofed rate sheets. Those integrations require investor API agreements that do not exist in Phase 2. This is the right ingestion trigger for Phase 3 once investor relationships are formalized. The internal `RateSheetActivated` event pipeline remains unchanged when the trigger swaps — the propagation architecture is stable.

### Alternative: Admin-triggered manual upload (current state — partially retained)
**Strengths:** Already implemented, no external dependencies, works for demo and Phase 1
**Partially retained because:**
Manual CSV upload remains the ingestion trigger in Phase 2 — webhook delivery from investors is deferred to Phase 3. What changes is what happens after upload: previously the rate sheet was written to the database and nothing else. Now, ingestion publishes a `RateSheetActivated` event to the message broker, which triggers cache invalidation and borrower notification downstream. The manual trigger is a Phase 2 constraint; the propagation pipeline it feeds into is production-grade. This baseline is superseded in Phase 3 when webhook push replaces the manual upload.

### Alternative: Event-driven via internal message broker (chosen approach)
**Strengths:** Decouples ingestion from notification, allows fan-out to multiple consumers
**Accepted because:**
Publishing a `RateSheetActivated` event the moment ingestion completes is faster and less taxing than any polling-based approach. pricing-service doesn't need to know which downstream services care about rate changes — it publishes the event and moves on. The message broker fans it out: one consumer invalidates Redis and warms the cache with new rates, another triggers borrower notifications for unlocked quotes. The services are decoupled, the event is immediate, and adding a new consumer in the future requires no changes to the ingestion path.

---

## Rationale

### Cache Invalidation Must Be Event-Driven, Not TTL-Based

TTL-based cache expiry means Redis continues serving the old rate until the timer fires — there is no connection between when an investor publishes a new rate sheet and when the cache reflects it. A borrower could be quoting against, or attempting to lock, a rate that has already been superseded. That is a serious business problem: a locked price should reflect a rate the investor is still honoring.

Rate sheet activation is a discrete event — an investor publishes a new sheet, the previous one is superseded, and that change is known the moment ingestion completes. Cache invalidation must happen at that moment, not on the next TTL cycle. The `RateSheetActivated` event triggers immediate invalidation of the previous rate sheet in Redis and warms the cache with the new rates before any borrower requests come in.

### Ingestion Trigger Strategy Should Match Phase

The goal is a real-time system that prevents price mismatches caused by inconsistent data flowing through the architecture. The ingestion trigger is the variable — manual upload in Phase 2, webhook in Phase 3 — but the downstream pipeline is stable in both phases. When a rate sheet is ingested, regardless of how it arrived, a `RateSheetActivated` event is published and the same cache invalidation and notification logic runs. Swapping the trigger in Phase 3 requires no changes to those downstream consumers. Designing the pipeline this way eliminates the risk of a price mismatch caused by one part of the system being on different data than another.

### Downstream Notification Is a Separate Concern

When rates change, borrowers need to know — a quote that's no longer valid needs to be flagged, and a borrower with a locked price needs confirmation their rate is safe. That borrower-facing logic requires knowing who is connected, who has a locked price, and which delivery channel to use (SSE for active sessions, email for closed ones). That context belongs to notification-service, not pricing-service.

pricing-service owns ingestion, validation, cache warming, and event publish. notification-service owns borrower alert delivery. The `RateSheetActivated` event is the handoff point. Keeping these concerns separated means each service has one job and neither needs to know how the other works. Borrower-facing delivery is covered by ADR-0049 (SSE) and ADR-0026 (rate alert subscriptions).

---

## Consequences

### Positive

- Each phase moves the system closer to real-time pricing. Phase 2 establishes the event-driven propagation pipeline; Phase 3 replaces the manual ingestion trigger with webhook delivery once investor relationships and supported integrations are known. The architecture doesn't need to be redesigned — only the trigger changes.
- Cache invalidation and borrower notification are immediate on rate sheet activation, eliminating price mismatches from stale cached rates.

### Negative

- Manual upload in Phase 2 still requires an admin to act when an investor publishes new rates. There is a window between when the investor publishes and when the admin uploads — the system cannot close that gap until Phase 3 webhook integrations are in place.
- Investor webhook formats are unknown today. Phase 3 ingestion will require building and maintaining a separate adapter per investor feed, which is non-trivial integration work.

---

## Follow-up
- **ADR-0026 (rate alert subscription model).** Defines how borrowers subscribe to rate change alerts — the consumer side of the `RateSheetActivated` event.
- **ADR-0049 (real-time notification via SSE).** Defines how notification-service delivers rate change alerts to active borrower sessions.
- **Investor feed format.** Phase 3 webhook ingestion requires defining which investors are supported and what format their rate sheet payloads use (Fannie/Freddie standard, custom CSV, proprietary API). This is unknown until investor relationships are formalized.
- **Ingestion failure handling.** Define what happens if ingestion fails mid-parse — whether the previous ACTIVE rate sheet remains in place, whether a partial ingestion is rolled back, and how the failure is surfaced to the admin.
- **Partial ingestion rollback.** If a rate sheet is partially ingested and the `RateSheetActivated` event is published before the failure is detected, downstream cache invalidation may have already occurred. A rollback strategy is needed to restore the previous rate sheet as ACTIVE.

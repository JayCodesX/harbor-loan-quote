# ADR 0003: Synchronous HTTP for Quote Calculation, Not Message Queue

## Status
Accepted

## Date
2026-04-01

## Phase
1 — Foundation

---

## Context

The system supports a two-phase quote flow.

**Phase 1 — Initial Quote:** The borrower provides a few data points (credit score, property type, location, estimated loan amount, loan product type — Conventional, FHA, VA, USDA, ARM) without a subject property. The system runs an eligibility check against those inputs — credit score floors, property type and location constraints, investor product offerings — and returns a rate from the current rate sheet. This gives the borrower a real-market-data rate indication with no commitment required.

**Phase 2 — Refined Quote:** The borrower has a subject property and full loan details. The system runs the full pricing engine: rate sheet lookup, LLPA adjustments (LTV, lock period, add-ons, margins), and product-specific eligibility. The borrower can then select a lender and real estate agent.

Both phases are handled by harbor-api calling pricing-service. The calculation in both cases is fast — eligibility is in-memory rule evaluation, and rate and LLPA data are Redis-cached. This caching strategy is load-bearing: it eliminates read/write contention between rate sheet ingestion (which writes to MySQL then warms Redis) and quote calculations (which read from Redis). Sub-100ms is achievable when built correctly.

The original design used an SQS message queue for the quote calculation path — `harbor-api → SQS → pricing-service → result queue → harbor-api`. This existed because of a misapplication of async thinking: the queue was the right tool for rate sheet propagation (re-evaluating open quotes when rates change) but was incorrectly applied to the synchronous quote calculation itself. The result was 4-5 seconds of polling jitter latency for an operation that takes under 100ms.


---

## Decision

harbor-api calls pricing-service via synchronous HTTP for quote calculation — both initial and refined quotes. Message queue is not used in the quote calculation path. Async message queue is reserved for rate propagation (re-evaluating open quotes when a new rate sheet becomes active).

---

## Alternatives Considered

### Alternative: Async via Message Queue (original SQS design)
**Strengths:** Decouples caller from pricing service, natural retry via DLQ, pricing-service can be slow without blocking api threads, supports fan-out to multiple consumers
**Rejected because:**
Three specific problems drove the rejection: latency, stuck quotes, and DB locking.

Latency: the borrower is waiting at a loading screen. The async path added polling jitter — approximately 1.5 seconds per hop across 3 hops — turning a sub-100ms calculation into a 4-5 second wait with no benefit. The async design moved the wait from the HTTP response into a polling loop without reducing it.

Stuck quotes: if pricing-service failed after consuming the message, the quote was left in a pending state with no clean recovery. Detecting and resolving stuck quotes required a saga/timeout job — complexity that was entirely self-inflicted. With synchronous HTTP, a pricing-service failure returns an immediate exception to harbor-api, which handles it with a circuit breaker and returns a clean error to the borrower. No stuck state, no timeout job.

DB locking: the async path did not eliminate write contention — it deferred it. The calculation still has to run and write a result. Synchronous HTTP with Redis-cached rate data separates the read path (Redis) from the write path (MySQL), which is the correct solution to the contention concern.

### Alternative: Async via CompletableFuture / parallel calls
**Strengths:** If showing rates from multiple investors simultaneously, parallel calls could reduce total latency (call 5 investors at once instead of sequentially)
**Rejected because:**
The parallelism this alternative is trying to solve already lives inside pricing-service. The borrower has no visibility into which investor is providing their rate — investor abstraction is entirely internal to pricing-service, which loops through eligible investors and products and returns all results in a single response. If parallel execution is needed, it belongs inside pricing-service (parallel streams or internal CompletableFuture), not at the network boundary. harbor-api makes one synchronous call and gets all results back. There is no case where harbor-api should be making per-investor calls.

---

## Rationale

### Quote Calculation Does Not Meet the Criteria for Async

The three legitimate reasons to use async messaging are: the operation takes too long for the caller to wait, the caller does not need the result to continue, or the result needs to fan out to multiple consumers.

Quote calculation fails all three. The calculations are near-instant — eligibility is in-memory rule evaluation against cached data. The borrower wants the initial quote immediately; introducing a message queue creates latency where there doesn't need to be any. And there is one consumer: harbor-api. There is no fan-out scenario in the quote calculation path.

Async messaging is the right tool for rate propagation — where rates change and the system must re-evaluate potentially many open quotes, push updates to connected borrowers, and fan out across notification channels. That is a different workflow with different characteristics. It should not be conflated with the quote calculation itself.

### Synchronous HTTP Preserves the Request/Response Contract

With async, the borrower submits and lands on a processing page. They wait. They have no feedback on whether anything is happening — is the system broken? How long will this take? The uncertainty degrades trust in the product before they've even seen a rate.

With synchronous HTTP, the borrower submits and goes directly to the results page. The experience is instant. There is no intermediate state to manage in the UI, no polling loop to implement, no loading screen to design around.

A borrower submitting a quote request and expecting a rate back is a natural request/response interaction. Adding a message queue in the middle breaks that contract artificially — the queue forces the UI to reassemble a response that should never have been split in the first place.

### Failure Handling Is Simpler and More Predictable

With async, a pricing-service failure leaves the quote stuck in a pending state. The borrower has no feedback — they sit on a processing page with no indication of what happened. With no result coming, they leave and get a quote somewhere else. From an ops perspective, detecting and recovering stuck quotes requires a timeout job and DLQ monitoring — infrastructure complexity that exists purely to handle a failure mode that synchronous HTTP doesn't have.

With synchronous HTTP, a pricing-service failure returns an immediate error to harbor-api. The system can detect the failure instantly and communicate it clearly to the borrower — retry, system temporarily unavailable, whatever is appropriate. The borrower knows what happened and what to do next. No stuck quotes, no timeout jobs, no silent failures.

---

## Consequences

### Positive

- Borrower experience is significantly better — results are instant, errors are communicated clearly, and there is no ambiguous processing state.
- Simpler architecture with fewer moving parts. No message broker in the quote calculation path means fewer failure modes, less infrastructure to operate, and an easier system to reason about and modify in the future.

### Negative

- The Redis cache is load-bearing for this decision. If Redis is unavailable, the system falls back to MySQL reads. If MySQL is simultaneously under write pressure — rate sheet ingestion, quote result writes — borrowers can be stuck waiting for a quote. This is particularly painful during a rate lock scenario where timing matters. Mitigations include Redis HA (ADR-0039), circuit breaking on the pricing-service call, and graceful degradation messaging to the borrower.
- pricing-service is a hard synchronous dependency in the quote path. If pricing-service is down, no quotes can be calculated. A message queue would buffer requests until the service recovers; synchronous HTTP cannot. A circuit breaker with a fallback response is the mitigation.

---

## Follow-up

- **Microservice consolidation is the primary prerequisite.** This decision is predicated on the 3-service architecture (harbor-api, pricing-service, notification-service). Until the 6-service model is fully collapsed, the quote calculation path may still have intermediate hops that undermine the synchronous design.
- Implement circuit breaker on harbor-api → pricing-service call to handle pricing-service unavailability gracefully.
- Redis HA must be in place before this is production-ready — see ADR-0039.
- Async rate propagation (re-evaluating open quotes on rate sheet change) is a separate workflow and requires its own design — see ADR-0048.

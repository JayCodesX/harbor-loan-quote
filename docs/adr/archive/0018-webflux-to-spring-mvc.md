# ADR 0018: Replace WebFlux With Spring MVC in harbor-api

## Status
Superseded by ADR-0004 (Spring MVC vs WebFlux Placement)

The decision to move from WebFlux to Spring MVC was finalized in
ADR-0004.

## Date
Fill in when you write this

## Phase
1 — Foundation

---

## Context

WRITE THIS YOURSELF

Prompts to answer:
- What framework does harbor-api currently use?
  (Spring WebFlux with reactive controllers returning Mono<T>/Flux<T>)
- What persistence layer does it use?
  (Spring Data JPA with JDBC — fundamentally blocking I/O)
- Why is WebFlux + JPA a correctness problem, not just a performance concern?
  (WebFlux's event loop is a small fixed-size thread pool (2 × CPU cores).
   JPA/JDBC blocks a thread while waiting for MySQL to respond.
   A blocked event loop thread cannot accept new connections or process other requests.
   At 16 concurrent requests on an 8-core server, all event loop threads are blocked.
   Request 17 waits for a thread to free up.
   The system behaves as if it has a concurrency limit of 16 — worse than Spring MVC.)
- What does harbor-api actually do that would require WebFlux?
  (Short-lived request/response operations: save quote, read quote, call pricing-service.
   No long-lived connections. No SSE. No streaming.)
- What changed in Java 21 that is relevant?
  (Virtual threads via Project Loom.
   spring.threads.virtual.enabled=true gives Spring MVC non-blocking I/O throughput
   with blocking-style code. JPA blocking calls are handled by the JVM, not the event loop.)
- Cross-reference: ADR-0004 for the service-by-service WebFlux vs. MVC placement decision


---

## Decision

WRITE THIS YOURSELF

What is the decision? Be concrete — a sentence or two that could stand alone.
Describe the *what*, not the *why* (why belongs in Rationale).

---

## Alternatives Considered

### Alternative: Retain WebFlux and migrate JPA to R2DBC
**Strengths:** Truly non-blocking end-to-end, consistent with WebFlux programming model, no event loop blocking
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- R2DBC is a complete replacement for JPA. Different annotations, different repositories,
  Mono<T>/Flux<T> return types everywhere, reactive transactions, no @Transactional annotation.
  The migration cost is 100% of the persistence layer code.
- Java 21 virtual threads solve the blocking I/O problem at the JVM level, making
  R2DBC's advantage specific to extreme concurrency scenarios harbor-api will not reach.
- The team is clearly more comfortable with imperative code — the presence of JPA in
  a WebFlux service is evidence that the reactive model is not natural here.
- R2DBC's reactive chain debugging (reactor stack frames obscure business code location)
  and transaction management complexity are ongoing maintenance costs with no benefit
  at harbor-api's scale.
Reference ADR-0004 for the full WebFlux vs. MVC analysis.

### Alternative: Keep WebFlux, wrap blocking JPA calls in Schedulers.boundedElastic()
**Strengths:** Technically correct — wrapping blocking code offloads it to a separate thread pool,
freeing the event loop. No framework migration required.
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- This is a workaround for using the wrong framework, not a solution.
- Every JPA call must be wrapped: Mono.fromCallable(() -> repo.save(entity)).subscribeOn(Schedulers.boundedElastic()).
  This is verbose, easy to forget, and invisible in code review.
- The business logic is now written in reactive style (flatMap chains) for no benefit —
  harbor-api is still doing request/response operations, just expressed in reactor syntax.
- Spring MVC with virtual threads is architecturally correct and requires zero wrapping.
  The workaround is a permanent tax on code readability for a problem that has a clean solution.

### Alternative: Hybrid — Spring MVC for most endpoints, WebFlux for specific streaming endpoints
**Strengths:** Incremental migration, keeps the option open for future SSE in harbor-api
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- Spring MVC and WebFlux cannot be mixed in the same Spring Boot application
  (you can use WebClient from MVC, but not WebFlux controllers).
  The framework choice is per-application, not per-endpoint.
- If harbor-api ever needs SSE (unlikely — notification-service handles this),
  Spring MVC supports SSE via SseEmitter.
  It is less efficient than WebFlux for thousands of concurrent SSE connections,
  but harbor-api is not expected to hold thousands of long-lived SSE connections.

---

## Rationale

### WebFlux + JPA Is a Correctness Problem That Manifests Under Load

WRITE THIS YOURSELF
Walk through the failure mode precisely:
1. WebFlux event loop: 2 × CPU cores threads = 16 threads on 8-core server
2. JPA findById call: blocks one event loop thread for ~5ms (MySQL round trip)
3. 16 concurrent quote requests: all 16 event loop threads blocked
4. Request 17: waits for a thread. The system is effectively at capacity.
5. This is worse than Spring MVC's 200-thread default pool — MVC would handle
   request 17 with one of the 184 remaining available threads.
This is not a theoretical concern. It manifests at ~16 concurrent users on an 8-core server.
The fix is not configuration tuning — it is using the right framework for the workload.

### Virtual Threads Provide Non-Blocking Throughput With Blocking Code

WRITE THIS YOURSELF
Java 21 virtual threads (Project Loom) explanation:
- JVM manages millions of virtual threads with minimal overhead
- When a virtual thread blocks on I/O (JDBC call), the JVM parks it
  and moves the carrier OS thread to another virtual thread
- From the application code perspective: plain blocking JDBC call
- From the JVM perspective: non-blocking, the OS thread never sits idle
- Result: harbor-api with Spring MVC + virtual threads handles the same
  concurrency as WebFlux + R2DBC, with blocking-style code and the full
  JPA/Spring ecosystem working correctly
One property: spring.threads.virtual.enabled=true. Zero code changes to JPA layer.

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

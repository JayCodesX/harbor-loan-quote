# ADR 0004: Spring MVC in harbor-api and pricing-service, WebFlux in notification-service

## Status
Accepted

## Date
2026-04-01

## Phase
1 — Foundation

---

## Context

The three services have meaningfully different connection lifecycles, and that difference drives the framework placement decision.

**harbor-api:** A connection opens when a borrower requests a quote. The system processes it — eligibility check, pricing call, result — and the connection closes. Short-lived request/response. The connection is never idle for long.

**pricing-service:** A connection opens from harbor-api, the rate calculation runs, the result is returned, and the connection closes. When rate sheets are updated, pricing-service pushes an event to notification-service via async message queue and the connection closes. Also short-lived.

**notification-service:** Connections from borrower browsers are held open using Server-Sent Events (SSE) for the duration of their session. These connections sit idle most of the time, waiting for a rate update or notification event. When one arrives, WebFlux on Netty reacts — routing it to the right subscribers on a small event loop thread pool — and the connections remain open. This is fundamentally different from the request/response pattern of the other two services.

Spring MVC uses a thread-per-request model. Holding thousands of idle SSE connections in Spring MVC means thousands of threads sitting idle — expensive and wasteful. WebFlux on Netty handles those as reactive subscriptions on a small fixed thread pool, which is the right model for this workload.

The danger of WebFlux is the inverse: blocking I/O on the event loop threads causes starvation. harbor-api and pricing-service both use JPA/JDBC, which is blocking. Running blocking I/O inside WebFlux stalls the event loop and is worse than Spring MVC — you get reactive complexity with none of the throughput benefit. With Java 21 virtual threads, Spring MVC gains non-blocking behavior for free: the JVM parks a virtual thread on I/O and moves the OS thread to other work. This eliminates the primary throughput argument for WebFlux in request/response services.


---

## Decision

Spring MVC with Java 21 virtual threads is used for harbor-api and pricing-service. WebFlux on Netty is used for notification-service. The framework placement follows the connection lifecycle of each service — request/response for the former, long-lived SSE connections for the latter.

---

## Alternatives Considered

### Alternative: WebFlux in all three services (original design)
**Strengths:** Consistent programming model across all services, theoretically maximum throughput, non-blocking throughout
**Rejected because:**
harbor-api and pricing-service both use JPA/JDBC which is blocking. Running blocking I/O inside a WebFlux event loop keeps threads occupied and makes the system prone to event loop starvation — the event loop threads get tied up on database calls and cannot route other requests. This is the worst of both worlds: reactive complexity in the code without any of the throughput benefit. Java 21 virtual threads make WebFlux unnecessary for these services — Spring MVC handles concurrency efficiently without the starvation risk.

### Alternative: Spring MVC in all three services
**Strengths:** Completely consistent model, simplest codebase, every developer knows it
**Rejected because:**
Spring MVC with Java 21 virtual threads works well for harbor-api and pricing-service — connections open, work happens, connections close. But notification-service holds SSE connections open for the duration of a borrower's session. Spring MVC assigns a thread per connection, meaning thousands of idle SSE connections require thousands of threads sitting idle. Even with virtual threads, this is the wrong model — virtual threads still carry scheduling overhead at scale. WebFlux on Netty handles those same connections as reactive subscriptions on a fixed event loop thread pool. The concurrency model should match the connection lifecycle, and notification-service's lifecycle is fundamentally different.

### Alternative: R2DBC + WebFlux in harbor-api (fully reactive)
**Strengths:** Truly non-blocking end to end, consistent with WebFlux model, no event loop blocking risk
**Rejected because:**
R2DBC is a full replacement for JPA — different annotations, different repositories, `Mono<T>` and `Flux<T>` return types everywhere, reactive transactions, no `@Transactional`. This migration is only worth it if the team already knows R2DBC. Otherwise it is a significant investment to solve a problem that Java 21 virtual threads already solve. We would be forcing WebFlux into a service where it is not needed. Exception handling also becomes significantly more complex in the reactive model, which slows development and increases the surface area for subtle bugs. The case for R2DBC collapses entirely when virtual threads are available.

---

## Rationale

### The Right Model Depends on Connection Lifecycle

The fundamental question is: **how long does a connection stay open?**

Spring MVC with Java 21 virtual threads handles multiple concurrent users in harbor-api and pricing-service without the service deteriorating. Connections open, work happens, connections close — virtual threads handle the concurrency efficiently without blocking the system under load.

WebFlux solves a different problem in notification-service: keeping connections open and reacting when pricing updates arrive, without holding a thread per connection. The connection stays open but is only active when there is something to push.

Swapping the frameworks creates problems in both directions. Using WebFlux in harbor-api leaves it vulnerable to event loop starvation from blocking JPA/JDBC calls — a waste of resources for a service that doesn't need it. Using Spring MVC for notification-service means every open SSE connection occupies a thread. Once all virtual threads are consumed by idle SSE connections, there is nothing left to process incoming notifications — they fail to reach their destination.

### Java 21 Virtual Threads Eliminate WebFlux's Advantage in Request/Response Services

For harbor-api and pricing-service, virtual threads match the connection flow — open, do work, close. When a virtual thread blocks on I/O (a JDBC call, a pricing-service HTTP call), the JVM parks it and moves the OS thread to other work. The concurrency is handled at the JVM level, not at the framework level.

This means developers working on harbor-api and pricing-service write straightforward blocking-style code — standard JPA, `@Transactional`, familiar exception handling — without having to reason about reactive streams or concurrent user load. The JVM handles it. There is no reactive complexity to maintain and no risk of event loop starvation. Virtual threads give the throughput benefit of WebFlux with none of the operational or development overhead.

### WebFlux Is Correct for notification-service's Specific Problem

notification-service needs to hold thousands of open SSE connections without consuming system resources for each one. WebFlux accomplishes this through the event loop — idle connections are registered as reactive subscriptions, not held by threads. The event loop threads are free until an event arrives, at which point they route it to the relevant subscribers and move on.

The result is that thousands of borrowers can maintain open connections simultaneously without the system deteriorating. The connections exist, they are ready to receive, but they cost almost nothing while idle. This is precisely the problem WebFlux on Netty was designed to solve.

---

## Consequences

### Positive

- Each service uses the right tool for its workload — no framework mismatch, no wasted resources.
- harbor-api and pricing-service benefit from Java 21 virtual threads: familiar blocking-style code, simple exception handling, and JPA/Spring ecosystem fully available without reactive complexity.
- notification-service handles thousands of idle SSE connections efficiently via the WebFlux event loop without consuming a thread per connection.

### Negative

- Two different programming models exist in the same codebase. There is no one-size-fits-all solution — developers need to understand both Spring MVC with virtual threads and WebFlux to work across all three services. This raises the knowledge bar for onboarding and increases the surface area for mistakes.
- notification-service carries event loop starvation risk. Any blocking I/O accidentally introduced into the WebFlux event loop — a JPA call, a slow computation — will stall the event loop and affect all connected borrowers. This requires discipline and awareness from anyone touching notification-service.

---

## Follow-up

- **Microservice consolidation is the primary prerequisite.** The 3-service split (harbor-api, pricing-service, notification-service) must be completed before this decision is fully realized. See ADR-0001.
- notification-service needs to be built out — SSE connection management, event routing from the message queue, and borrower subscription handling. See ADR-0049.
- Enforce a no-blocking-I/O rule in notification-service. Any database access or external HTTP calls must be offloaded to a bounded elastic scheduler, not run on the event loop threads.

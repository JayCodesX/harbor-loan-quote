# ADR 0001: Three-Service Architecture — harbor-api, pricing-service, notification-service

## Status
Accepted

## Date
2026-04-01

## Phase
1 — Foundation

---

## Context

The business domain of this application is loan quote aggregation + rate pricing + lead capture.

Initially the plan was to create 6 microservices:
- borrower service
- lead service
- pricing service
- notification service
- auth service
- admin service

However, the result was just a monolith without pricing service. The system had low-to-medium traffic, with domain complexity in pricing — not in infrastructure. The cost of over-decomposition was network hops, inter-service auth, operational overhead, and deployment complexity. Team size is small.

Rate sheets, investors, LLPAs, and product terms have a different operational lifecycle than quote requests and lead captures. This is the core justification for separating pricing-service — not technology, but data lifecycle and change rate.

---

## Decision

The decision is to consolidate the microservices into 3 services:
1. harbor-api — loan quote and lead capture
2. pricing-service — rate sheet ingestion, LLPA calculation, pricing engine
3. notification-service — real-time borrower notifications via SSE

---

## Alternatives Considered

### Alternative: Single Monolith (1 service)
**Strengths:** Simplest deployment, no network latency between components, easiest to test end-to-end, appropriate for many applications at this scale
**Rejected because:**
Over-engineering loan quote and lead capture to fit the pricing lifecycle when it should be broken out. Consolidating loan quote and lead capture into a single service reduces overhead and over-engineering. The pricing service is complex, needs to handle updates based on rate sheets being updated at any time, and operates independently of any user action. Rate sheet data changes should not require redeploying the quote API. The LLPA calculation engine and rate sheet data model are complex enough to justify ownership — they could be consumed by a future broker portal too.

### Alternative: Six Microservices (original architecture)
**Strengths:** Maximum isolation, independent scaling per service, follows microservice orthodoxy
**Rejected because:**
There wasn't much that the lead service did that harbor-api couldn't do, and adding an SQS workflow added complexity that wasn't warranted. The calculation could have been done in Java without creating an event for it — it is mostly a CRUD + math problem. This path essentially created a distributed monolith anti-pattern. All the cost, none of the benefit.

### Alternative: Two Services (api + pricing-service, no notification-service)
**Strengths:** Simpler than three, pricing separation is clearly justified, eliminates the SSE question
**Rejected because:**
Although this is a better alternative than six microservices or a single monolith, it has workload characteristics that would make it fundamentally incompatible with the request/response model — specifically long-lived SSE connections. harbor-api could serve SSE endpoints alongside its normal API endpoints, but that would mean mixing WebFlux and Spring MVC in one service, or forcing Spring MVC to hold thousands of idle threads for open SSE connections.

---

## Rationale

### Pricing Data Has a Different Lifecycle Than Quote Data

Rate sheets are ingested on a schedule (not user-triggered). They are valid for a time window, can change multiple times per day, and are read thousands of times per rate sheet (every quote calculation reads them).

This is fundamentally different from a quote record, which is written once and read rarely. Separating them lets us: update rates without touching the quote API, cache aggressively in pricing-service's own Redis instance, and eventually expose the pricing API to other consumers.

### SSE Connections Require a Different I/O Model

harbor-api and pricing-service are request/response services. Each request arrives, work happens in under 100ms, response is sent, connection closes.

notification-service is different — it holds connections open for minutes or hours. WebFlux on Netty (non-blocking event loop) is the right model for long-lived connections. Spring MVC (thread-per-request) would consume one thread per open SSE connection. Mixing these models in a single service is architecturally incorrect.

### Each Service Has a Justifiable Independent Deployment Reason

The test for a good service boundary is: "Can I deploy this service without touching the others?" For each of the three:
- **pricing-service:** Yes — load new rate sheets, update LLPA tables, no api deploy needed
- **notification-service:** Yes — update SSE delivery logic, no pricing or api deploy needed
- **harbor-api:** Yes — update quote workflow, lead routing, no pricing deploy needed

---

## Consequences

### Positive

- 6 services consolidated into 3 that each require different system design and are independent of one another.
- The service boundaries are more clearly defined — the distributed monolith anti-pattern is removed and blocking I/O in harbor-api is prevented.
- Significantly cheaper to run and allows for easier scaling changes in the future.

### Negative

- Requires more time to be production-ready and requires a larger refactor.
- Requires a design of mortgage pricing data that has not yet been introduced.

---

## Follow-up

- Pricing service needs to handle all permutations of a real pricing API: rate sheets, product sheets, product terms, product offerings, investors, APOR and APR ARM index types, Fannie and Freddie products, interest-only options, buydown options, buydown schedule types, etc.

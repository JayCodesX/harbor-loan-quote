# ADR 0006: Circuit Breaker on harbor-api → pricing-service HTTP Call

## Status
Superseded

Circuit breaker approach for the pricing service will be revisited
as part of the pricing engine rebuild. The 3-service architecture
changes the failure boundaries.

## Date
Fill in when you write this

## Phase
1 — Foundation

---

## Context

WRITE THIS YOURSELF

Prompts to answer:
- What is the dependency relationship between harbor-api and pricing-service?
  (harbor-api makes a synchronous HTTP call to pricing-service on every quote request.
   If pricing-service is slow or unavailable, every quote request in harbor-api waits.)
- What happens without a circuit breaker when pricing-service degrades?
  (Threads in harbor-api's pool are held waiting for pricing-service to respond.
   All threads eventually exhaust. harbor-api becomes unresponsive to ALL requests —
   not just quote requests. This is called cascading failure.)
- Why is this dependency specifically risky?
  (pricing-service has an operational characteristic harbor-api does not:
   rate sheet ingestion is a write-heavy background operation that could cause
   temporary slowdowns in pricing-service during ingestion runs.)
- What is the desired behavior when pricing-service is degraded?
  (Show the borrower an error or a fallback message immediately — not after a timeout.
   Do not let pricing-service's problem become harbor-api's problem.)
- What library implements this? (Resilience4j — standard for Spring Boot 3)


---

## Decision

WRITE THIS YOURSELF

What is the decision? Be concrete — a sentence or two that could stand alone.
Describe the *what*, not the *why* (why belongs in Rationale).

---

## Alternatives Considered

### Alternative: No circuit breaker — rely on HTTP timeout only
**Strengths:** Simpler implementation, no additional library, timeout alone prevents indefinite waiting
**Rejected because:**
WRITE THIS YOURSELF
Key points:
- A timeout stops one request from waiting forever, but does not stop the next
  request from also waiting for the full timeout duration.
- If pricing-service takes 10 seconds to respond and the timeout is 5 seconds:
  every quote request waits 5 seconds before failing. Under concurrent load,
  threads pile up waiting for their 5 seconds, pool exhausts, harbor-api crashes.
- A circuit breaker detects the pattern (50% of calls failing) and opens the circuit.
  Subsequent calls fail immediately — no thread held, no pool exhaustion.
- A timeout is necessary but not sufficient. Circuit breaker + timeout is the complete solution.

### Alternative: Retry with exponential backoff
**Strengths:** Handles transient failures (momentary network blip, single bad deploy), self-healing
**Rejected because:**
WRITE THIS YOURSELF
Key points:
- Retries are appropriate for transient failures (< 1 second blip).
  They are harmful for systemic failures (pricing-service is down or overloaded).
- Retrying an overloaded service adds more load to an already-struggling service.
  This is called a "retry storm" and it makes the outage worse.
- Retries can be used together with a circuit breaker for the transient case,
  but the circuit breaker must be the primary protection against systemic failure.
- For a quote request, a retry that adds 2-3 seconds of latency degrades UX.
  Better to fail fast and show the user a clear error.

### Alternative: Async messaging to decouple pricing-service availability from harbor-api
**Strengths:** If the queue accepts the message, harbor-api is isolated from pricing-service availability
**Rejected because:**
WRITE THIS YOURSELF
Reference ADR-0003 here.
The decision to use synchronous HTTP was made in ADR-0003.
The circuit breaker is the correct resilience pattern for synchronous dependencies.
Async messaging solves availability isolation but at the cost of response time,
complexity, and the stuck-quote problem — costs already evaluated and rejected.

---

## Rationale

### Cascading Failure Is the Core Risk of Synchronous Dependencies

WRITE THIS YOURSELF
Explain the cascading failure pattern:
1. pricing-service slows down (rate sheet ingestion causing DB pressure)
2. harbor-api threads wait for pricing-service response
3. All threads eventually held waiting
4. harbor-api cannot accept new requests — including auth, admin, lead capture
5. Everything is down because of pricing-service — even things that don't use it

The circuit breaker breaks step 2 → 3 by detecting the failure pattern and
stopping new calls to pricing-service before thread pool exhaustion.

### Pricing-Service Has a Known Operational Variability

WRITE THIS YOURSELF
Specific to this system: pricing-service runs @Scheduled rate sheet ingestion jobs.
During ingestion, it is parsing, validating, and writing to MySQL AND warming Redis.
This is predictable additional load at predictable times.
A circuit breaker tuned to pricing-service's normal response time
will protect harbor-api during these windows without manual intervention.

### The Fallback Must Be Meaningful

WRITE THIS YOURSELF
Define what the fallback behavior actually is:
Option A: Return an error to the borrower ("Rates temporarily unavailable, try again in a moment")
Option B: Return cached rates from harbor-api's own last-known-good response
Option C: Return a partial response indicating pricing is degraded

Which option did you choose and why?
Consider: a mortgage rate is a commitment-adjacent number. Showing stale rates
as if they were current could be misleading. Option A (honest error) is often safer
than Option B (potentially stale rates shown as current) for a financial product.

---

## Circuit Breaker Configuration

```yaml
resilience4j:
  circuitbreaker:
    instances:
      pricingService:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
        permittedNumberOfCallsInHalfOpenState: 3
        slowCallDurationThreshold: 2s
        slowCallRateThreshold: 80
  timelimiter:
    instances:
      pricingService:
        timeoutDuration: 3s
```


EXPLAIN EACH SETTING — you should be able to justify every value:

slidingWindowSize: 10
  We evaluate the last 10 calls. Why 10? Small enough to detect failure quickly,
  large enough to not trip on a single slow request.

failureRateThreshold: 50
  If 50% of the last 10 calls failed or timed out, open the circuit.
  Why 50%? Aggressive enough to protect under real failure, tolerant enough
  for brief transient issues.

waitDurationInOpenState: 30s
  Wait 30 seconds before allowing test traffic through (half-open state).
  Why 30s? Pricing-service needs time to recover from ingestion load.
  Too short = circuit flaps. Too long = unnecessary degradation.

timeoutDuration: 3s
  Any call taking > 3 seconds is treated as a failure.
  Why 3s? A quote calculation with Redis-cached data should take <50ms.
  3 seconds is extremely generous and only triggers on genuine problems.

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

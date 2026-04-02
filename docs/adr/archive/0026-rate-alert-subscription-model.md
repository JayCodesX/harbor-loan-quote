# ADR 0026: Rate Alert Subscription Model and Evaluation Strategy

## Status
Archived — valid, pending dynamic repricing implementation

Rate alert subscriptions are core to the dynamic rate sheet
update feature. Will be activated when repricing and push
notifications are built.

## Date
Fill in when you write this

## Phase
3 — Scale and Operations

---

## Context

WRITE THIS YOURSELF

Prompts to answer:
- What is a rate alert from a borrower's perspective?
  ("Alert me when 30-year fixed rates for a $450K purchase in my credit tier drop below 6.5%."
   The borrower saves their loan scenario and a threshold. When rates hit the threshold,
   they receive a notification.)
- What triggers alert evaluation?
  (A new rate sheet is published and becomes ACTIVE in pricing-service.
   Every rate sheet publication is a potential trigger for every subscriber's threshold.)
- How many subscribers could there be?
  (Early stage: dozens. At scale: thousands. The evaluation strategy must scale.)
- What is the difference between "poll-based" and "event-driven" evaluation?
  (Poll-based: a scheduled job runs every N minutes and re-prices all subscriptions.
   Event-driven: pricing-service publishes a RateSheetActivated event;
   notification-service consumes it and evaluates all subscriptions against the new rates.)
- What is the user-facing notification?
  (SSE push if the browser is open. Email if the browser is closed.
   Push notification (Phase 3+) if the mobile app is installed.)


---

## Decision

WRITE THIS YOURSELF

What is the decision? Be concrete — a sentence or two that could stand alone.
Describe the *what*, not the *why* (why belongs in Rationale).

---

## Alternatives Considered

### Alternative: Scheduled job polling all subscriptions
**Strengths:** Simple, no event infrastructure needed, runs on a known schedule
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- Poll-based evaluation decouples the evaluation timing from when rates actually change.
  A job running every 5 minutes evaluates subscriptions even when rates haven't changed.
  At 1,000 subscribers, this is 1,000 unnecessary price calculations every 5 minutes.
- When rates change, notification delay = time until the next poll run (up to 5 minutes).
  Event-driven evaluation fires within seconds of the rate sheet becoming active.
- Rate changes are the correct semantic trigger. Use them as the trigger.

### Alternative: Evaluate subscriptions synchronously during rate sheet ingestion
**Strengths:** Immediate evaluation, no separate consumer needed, simple flow
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- Rate sheet ingestion is a write operation that should complete quickly.
  Evaluating thousands of subscriptions synchronously during ingestion adds
  unbounded latency to the ingestion job.
- A slow subscription evaluation should not delay the rate sheet becoming ACTIVE
  for new quotes.
- Decoupling ingestion from evaluation (via the event) means each can fail and retry
  independently. Ingestion success ≠ evaluation success.

---

## Rationale

### Event-Driven Evaluation Matches the Business Semantic

WRITE THIS YOURSELF
Rate alerts are by definition triggered by rate changes.
Using the RateSheetActivated event as the trigger is semantically correct:
evaluation happens when and only when it needs to happen.
This also justifies the existence of notification-service as an event consumer
with its own subscription to the rate sheet event — it has a legitimate reason
to exist beyond just serving SSE connections.

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

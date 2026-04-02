# ADR 0009: Distributed Workflow Compensation — Timeout Job vs. Orchestration Saga

## Status
Superseded

The 6-service distributed workflow has been simplified to a 3-service
architecture with asynchronous SQS communication, reducing the need
for complex compensation flows.

## Date
Fill in when you write this

## Phase
1 — Foundation

---

## Context

WRITE THIS YOURSELF

Prompts to answer:
- What does the async quote workflow look like step by step?
  (api publishes → pricing-service consumes → pricing-service publishes result →
   api consumes → api publishes → lead-service consumes → lead-service publishes result →
   api consumes and finalizes — 5 message hops, 4 services)
- What happens if a service crashes mid-processing?
  (Message is ACK'd, service is dead, quote is stuck in PRICING_PENDING or LEAD_PENDING forever.
   No retry. No notification to the borrower. No recovery trigger.)
- What does the DLQ handle vs. what it doesn't?
  (DLQ handles: messages that fail before ACK (unprocessed messages).
   DLQ doesn't handle: messages that were ACK'd but the service crashed before writing to DB.)
- What is the user-visible symptom?
  ("Processing..." on the UI forever. No error. No retry option.)
- What is the business impact?
  (Lead is never captured, lender never gets the referral, borrower abandons.)


---

## Decision

WRITE THIS YOURSELF

What is the decision? Be concrete — a sentence or two that could stand alone.
Describe the *what*, not the *why* (why belongs in Rationale).

---

## Alternatives Considered

### Alternative: Orchestration Saga (api as coordinator)
**Strengths:** Strongest guarantees — api explicitly tracks each workflow step, publishes compensating events on failure, can attempt partial retries, handles complex rollback semantics
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- Saga orchestration requires api to persist explicit saga state per quote
  (step number, last completed step, compensating action per step).
  This is a significant additional data model and state machine.
- The workflow has only 4 services and 5 hops — it is not complex enough to
  justify the overhead of a full saga coordinator.
- "Compensating" a failed pricing calculation means... nothing. There is no state
  to roll back. Pricing is a read operation. Lead capture is an insert.
  If lead capture fails, the compensation is to mark the quote failed and
  let the borrower retry — which the timeout job also achieves.
- Revisit this if the workflow gains genuine multi-step write operations
  that require true rollback semantics (e.g., external CRM push on lead creation).

### Alternative: Per-message TTL with DLQ-driven failure notification
**Strengths:** Broker-native, no application code for timeout detection, DLQ already exists
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- TTL at the message level means the quote may expire in the broker before
  the consumer ever sees it — a different failure mode than mid-processing crash.
- DLQ-driven notification still requires something to consume the DLQ and
  update the quote status in the database. That consumer is effectively the timeout job.
- The DLQ is for unprocessed messages. A message that was ACK'd before the service
  crashed never reaches the DLQ. The timeout job is the only mechanism that
  catches the ACK'd-but-incomplete scenario.

### Alternative: Do nothing — rely on manual DLQ replay
**Strengths:** Zero implementation cost, DLQ replay scripts exist (S3-02 in migration board)
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- Manual intervention for stuck quotes means operator awareness and action
  required for every stuck quote. At any meaningful scale this is unsustainable.
- The borrower-facing UX is broken indefinitely until an operator notices and intervenes.
- The mortgage lead has a time value — a borrower stuck waiting for 2 hours
  has already visited a competitor.

---

## Rationale

### The Timeout Job Solves the Actual Failure Mode

WRITE THIS YOURSELF
The real failure scenario is not "message never delivered" (DLQ handles that).
The real failure is "message delivered, ACK'd, service crashed before finalizing."
The quote is in PRICING_PENDING or LEAD_PENDING in the database.
There is no message in the broker — it was consumed. The DLQ is empty.
The timeout job is the only mechanism that detects this by looking at the DB,
not the broker.

### Simplicity Matches the Workflow Complexity

WRITE THIS YOURSELF
The workflow has four services. Each step is either:
a) A calculation (pricing) — stateless, retry-safe
b) A record creation (lead) — idempotent with proper upsert design

Neither step requires rollback semantics. If pricing fails, the user needs
to see an error and retry — not a compensating transaction that undoes pricing
(pricing wrote nothing). The timeout job transitions the quote to FAILED,
which enables retry. That is the full compensation required.

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

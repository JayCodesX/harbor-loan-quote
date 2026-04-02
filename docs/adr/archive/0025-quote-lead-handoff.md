# ADR 0025: Quote-to-Lead Handoff — Internal Record vs. CRM Push vs. Dual-Write

## Status
Deprecated

Lead service was removed in the 3-service consolidation. Lead
functionality has been folded into the Loan API.

## Date
Fill in when you write this

## Phase
2 — Pricing Engine

---

## Context

WRITE THIS YOURSELF

Prompts to answer:
- What is a "lead" in the context of this platform?
  (A borrower who has reviewed rates and submitted their contact information
   (name, email, phone) to be contacted by a lender or broker. The lead record
   captures: who they are, what they want (quote parameters), and which
   lender/product they selected or were matched to.)
- Who receives the lead?
  (A partner lender or broker from the directory. The routing decision is based on:
   loan type, geographic area, the borrower's credit tier, the partner's coverage area,
   and any exclusive partnership arrangements.)
- What is a CRM in this context?
  (Salesforce, HubSpot, or similar — a system used by loan officers to manage
   their lead pipeline. Partners may require leads delivered to their CRM.)
- What is the timing requirement?
  (Lead delivery latency matters — a lead that arrives 4 hours after submission is cold.
   Real-time or near-real-time delivery is a quality differentiator.)
- What happens if lead delivery fails?
  (The partner never gets the lead. Revenue-impacting. Requires a retry and DLQ mechanism.)


---

## Decision

WRITE THIS YOURSELF

What is the decision? Be concrete — a sentence or two that could stand alone.
Describe the *what*, not the *why* (why belongs in Rationale).

---

## Alternatives Considered

### Alternative: Real-time CRM push as primary delivery
**Strengths:** Lead lands directly in the loan officer's workflow tool, no manual lead distribution step
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- CRM integrations are partner-specific. Different partners use different CRMs.
  Building 3-4 CRM integrations before validating that partners want them is speculative.
- CRM API dependencies create a hard coupling: if Salesforce's API is unavailable,
  lead capture fails. The internal lead record should be the source of truth first.
- Webhook delivery to a partner endpoint is simpler and more universal:
  every partner can receive a POST request regardless of their internal CRM.
- CRM push is the right Phase 3 addition, not the Phase 2 foundation.

### Alternative: Async lead delivery via message queue
**Strengths:** Decouples lead capture from lead delivery, natural retry via DLQ
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- Lead delivery to a webhook endpoint is not a long-running operation.
  A synchronous POST with retry is simpler and provides immediate feedback.
- The message queue (RabbitMQ) exists for the quote workflow — it should not
  become a general-purpose delivery mechanism for all async operations.
  Each queue addition adds DLQ management overhead.
- For Phase 2 with a small number of partners, synchronous webhook with retry is sufficient.
  If partner count grows to hundreds, async delivery via queue is the right Phase 3 evolution.

---

## Rationale

### Internal Lead Record Is the System of Record

WRITE THIS YOURSELF
Regardless of delivery mechanism, the internal MortgageLead record is the authority.
It enables: admin reporting on lead volume, partner performance, conversion rates.
It provides a retry source if webhook delivery fails.
It is the record shown in the admin dashboard.
The delivery mechanism is a side effect of lead creation, not the lead itself.

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

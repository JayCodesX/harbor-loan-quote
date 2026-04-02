# ADR 0022: Rate Sheet Versioning and Quote Audit Reproducibility

## Status
Superseded

Rate sheet versioning will be addressed as part of the dynamic rate
sheet update strategy in a future ADR covering cadence-based
repricing and stale price detection.

## Date
Fill in when you write this

## Phase
2 — Pricing Engine

---

## Context

WRITE THIS YOURSELF

Prompts to answer:
- Why must quotes be reproducible after the fact?
  (Rate lock disputes: a borrower claims they were quoted 6.875% but the lender says
   current rates are 7.0%. You must prove what rates were shown at the time of the quote.
   Regulatory audits may require demonstrating what a consumer was shown.)
- What does "reproducible" mean technically?
  (Given a quote ID from 3 months ago, the system can re-run the pricing calculation
   and produce the same result: same rate, same price, same LLPA adjustments.)
- What must be stored to enable this?
  (At minimum: a reference to the rate_sheet_id that was ACTIVE when the quote was calculated.
   Ideally also: the LLPA matrix version used, the lock period, the quote parameters.)
- What happens if rate sheets are deleted?
  (Reproducibility breaks. Rate sheets must be retained for a compliance-relevant period.)
- What is the difference between "superseded" and "deleted"?
  (Superseded: the sheet is no longer active but is retained for historical reference.
   Deleted: gone — all quotes that referenced it lose their audit trail.)


---

## Decision

WRITE THIS YOURSELF

What is the decision? Be concrete — a sentence or two that could stand alone.
Describe the *what*, not the *why* (why belongs in Rationale).

---

## Alternatives Considered

### Alternative: Store full rate snapshot on the PricingResult (denormalized)
**Strengths:** Completely self-contained — reproducibility does not depend on rate sheet retention
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- Rate sheets contain hundreds of rate/price pairs per investor/product/term.
  Storing the full snapshot per pricing result multiplies storage significantly.
- With the rate_sheet_id reference and an immutability guarantee, the snapshot
  IS available on demand without duplication.
- Full snapshot storage is the right choice if immutability cannot be guaranteed.
  With our immutable rate sheet design (ADR-0019), the reference is sufficient.

### Alternative: Event-sourced pricing log
**Strengths:** Every price calculated is stored as an immutable event with full context
**Rejected because:**

WRITE THIS YOURSELF
Reference ADR-0019 note on event sourcing.
The rate_sheet_id reference on PricingResult is a simpler mechanism that achieves
the same audit goal without event sourcing infrastructure.

---

## Rationale

### Immutable Rate Sheet + Reference Is Sufficient for Reproducibility

WRITE THIS YOURSELF
The combination of:
1. Immutable rate sheets (ADR-0019 — never mutated, only superseded)
2. rate_sheet_id stored on PricingResult
3. Quote parameters stored on LoanQuote (loan amount, credit tier, LTV, property type, etc.)
...provides complete reproducibility:
Re-run the LLPA calculation with the same parameters against the referenced rate sheet
= identical result. No snapshots needed.

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

# ADR 0035: Anonymous-to-Authenticated Quote Continuity

## Status
Deprecated

Anonymous quote continuity approach will be revisited in the
simplified 3-service architecture.

## Date
Fill in when you write this

## Phase
1 — Foundation

---

## Context

WRITE THIS YOURSELF

Prompts to answer:
- What is the anonymous quote flow?
  (A borrower can get a rate quote without creating an account.
   They fill in loan parameters, see rates, then decide whether to create an account.
   If they create an account, their pre-account quotes should be associated with them.)
- How is the anonymous session tracked?
  (X-Session-Id header — a client-generated or server-issued UUID that identifies
   the anonymous session. Stored on the LoanQuote during creation.)
- What is the "claim" operation?
  (When the borrower creates an account or logs in, the system takes all quotes
   with matching session_id and sets their borrower_id to the new account.
   The anonymous quotes are now part of their account history.)
- What is the failure mode without claim?
  (Borrower gets a quote anonymously, creates an account, logs in, and sees no quote history.
   They go back to the home page and re-enter everything. Friction, frustration, abandonment.)
- What is the current implementation using Redis for?
  (Redis stores the session → borrower mapping during the claim window.
   After the claim, the mapping is deleted.)


---

## Decision

WRITE THIS YOURSELF

What is the decision? Be concrete — a sentence or two that could stand alone.
Describe the *what*, not the *why* (why belongs in Rationale).

---

## Alternatives Considered

### Alternative: Redis session map (current state)
**Strengths:** In-memory, fast lookup, TTL-based automatic expiry of unclaimed sessions
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- The session_id is already stored on LoanQuote in the database.
  A direct UPDATE query against the database achieves the claim without Redis.
- Redis as an intermediary adds a dependency for an operation that can be
  fully expressed as a database query.
- Redis unavailability (ADR-0005 discusses Redis SPOF) would break quote claim
  even though the data is already in MySQL.
- The database claim is atomic within a transaction. The Redis-based claim
  requires a Redis read + MySQL write — two operations, not one.

### Alternative: Re-prompt the user to identify themselves before showing quote
**Strengths:** No session tracking needed, user always knows which quotes are theirs
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- Requiring login before showing rates is a conversion killer for a lead aggregator.
  The value exchange is: show me rates first, I'll give you my info after if I like what I see.
- Frictionless anonymous quote access is a product requirement. Session continuity
  makes the account creation step feel seamless rather than punitive.

---

## Rationale

### Database-Native Claim Is Simpler and More Resilient

WRITE THIS YOURSELF
The session_id is a column on loan_quotes — it was always going to be in MySQL.
The claim operation is: UPDATE loan_quotes SET borrower_id = ? WHERE session_id = ?
This is a single atomic transaction. No Redis. No race condition.
If the UPDATE affects 0 rows, there were no anonymous quotes to claim.
If the UPDATE affects N rows, N quotes are now linked to the account.
The simplest implementation that is also the most resilient.

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

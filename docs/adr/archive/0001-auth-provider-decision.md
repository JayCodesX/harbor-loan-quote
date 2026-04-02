# ADR 0001: Internal JWT as Default, OIDC as Upgrade Path

## Status
Superseded by ADR-0010 (Service JWT Signing Algorithm)

Auth provider selection was revisited when the platform adopted
service-to-service JWT authentication as the primary internal
security model.

## Date
Fill in when you write this

## Phase
1 — Foundation

---

## Context

WRITE THIS YOURSELF

Prompts to answer:
- What are the two token validation modes the system supports?
  (internal: tokens issued by auth-service, oidc: tokens issued by an external provider like Keycloak)
- Where is each mode used in the codebase today?
  (The system uses internal JWTs for local development and demo workflows.
   The same codebase also includes an OIDC validation path in api and borrower-service.)
- What are the immediate product priorities that drove this decision?
  (borrower quote continuity, consent/subscription support, admin operations,
   async quote workflow stability — identity complexity must not outrun the product domain)
- When would OIDC become relevant?
  (enterprise identity integration, centralized session policy, external IdP governance,
   future cloud deployment requiring standards-based auth)

---

## Decision

WRITE THIS YOURSELF

What is the decision? Be concrete — a sentence or two that could stand alone.
Describe the *what*, not the *why* (why belongs in Rationale).

---

## Rationale

### Why internal JWT stays the default now

WRITE THIS YOURSELF

Prompts to answer:
- Is the internal JWT already implemented and tested?
- What does using internal JWT avoid during the current phase?
  (blocking product work on IdP setup, keeping the local stack smaller)
- Is it sufficient for current borrower and admin flows?

### Why OIDC remains in the design

WRITE THIS YOURSELF

Prompts to answer:
- Does the system already have role-compatible token validation hooks for OIDC?
- What will enterprise deployment likely require?
- What is Keycloak's role as a local stand-in for future identity decisions?

## Consequences

### Positive

WRITE THIS YOURSELF
- List the positive consequences of this decision.

### Negative

WRITE THIS YOURSELF
- List the negative consequences. Be honest — no decision is without trade-offs.

## Follow-up

WRITE THIS YOURSELF
- List follow-up actions, related ADRs to write, or open questions to resolve.

# ADR 0024: Pricing API Design for Partner/External Consumption

## Status
Archived — valid, pending pricing engine buildout

Partner API design for investor pricing integration will be
revisited as the pricing engine matures.

## Date
Fill in when you write this

## Phase
2 — Pricing Engine

---

## Context

WRITE THIS YOURSELF

Prompts to answer:
- Who are the external consumers of the pricing API?
  (Mortgage brokers building custom quote tools, lender portals embedding Harbor rates,
   lead aggregator partners who want to display Harbor rates on their sites)
- What does a partner need from the pricing API?
  (Submit loan parameters → receive available rates for all eligible products.
   Potentially also: check if a specific loan is eligible for a product,
   retrieve current rate sheet data for display.)
- What authentication model applies to partners?
  (API keys per partner — not user JWTs. Partners are organizations, not individuals.
   API key → partner identity → authorized investor/product access.)
- What versioning is critical for a partner API?
  (Partners build integrations against a stable contract.
   Breaking the API breaks their integration — a business relationship issue.
   The partner API must be more strictly versioned than the borrower-facing API.)
- What rate limiting applies to partners?
  (Per-API-key rate limiting, potentially tiered by partner agreement.)


---

## Decision

WRITE THIS YOURSELF

What is the decision? Be concrete — a sentence or two that could stand alone.
Describe the *what*, not the *why* (why belongs in Rationale).

---

## Alternatives Considered

### Alternative: Partner calls harbor-api, which calls pricing-service internally
**Strengths:** Single entry point, Nginx rate limiting applies uniformly, simpler partner documentation
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- harbor-api is the borrower-facing service. Mixing partner integrations into the same
  service means partner traffic competes with borrower traffic for resources.
- pricing-service is the canonical pricing authority. Partners querying pricing directly
  eliminates an unnecessary network hop through harbor-api.
- Partner-specific features (bulk rate queries, rate sheet export, LLPA breakdowns)
  are pricing-service concerns, not quote workflow concerns.

### Alternative: GraphQL API for flexible partner queries
**Strengths:** Partners can request exactly the fields they need, reduces over-fetching,
single endpoint for all partner query patterns
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- GraphQL adds complexity (schema definition, resolver implementation, depth limiting)
  that REST handles adequately for this use case.
- Partners making rate queries have well-defined input/output contracts.
  The query shape does not vary enough to justify GraphQL's flexibility overhead.
- REST with URL versioning is more familiar to mortgage industry partners
  who may not have GraphQL experience.

---

## Rationale

### pricing-service Is the Correct Owner of the Partner Rate API

WRITE THIS YOURSELF
The partner's need is: "given these loan parameters, what rates are available?"
That is precisely what pricing-service calculates. Routing partner traffic through
harbor-api would make harbor-api a proxy for partner requests — adding latency and
coupling partner API stability to the borrower quote workflow.
pricing-service owning the partner API matches the service's bounded context.

### Partner API Keys Provide Organization-Level Authentication

WRITE THIS YOURSELF
Partners are organizations, not individual users. They authenticate with API keys
that identify their organization and control their rate access (which investors they can see,
which products they can query). This is different from user JWT authentication.
API keys can be issued, rotated, and revoked independently of the user auth system.

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

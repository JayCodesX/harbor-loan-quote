# ADR 0023: Product Eligibility Rule Evaluation — Java Logic vs. DSL vs. Drools

## Status
Archived — valid, pending pricing engine buildout

Product eligibility rules are directly relevant to the pricing
engine. Will be revisited as the investor/product data model
matures. See ADR-0019 and ADR-0020.

## Date
Fill in when you write this

## Phase
2 — Pricing Engine

---

## Context

WRITE THIS YOURSELF

Prompts to answer:
- What is product eligibility in mortgage pricing?
  (Before showing a rate, you must verify the loan qualifies for that product.
   Fannie Mae conventional: min 620 credit score, max 97% LTV, 1-4 unit properties,
   primary/secondary occupancy only, max loan amount per county.
   FHA: min 580 credit score (or 500 with 10% down), any occupancy.
   VA: eligible veterans only, no minimum credit score (lender overlays apply),
   no PMI, specific entitlement calculations.)
- What are "lender overlays"?
  (Restrictions a lender adds on top of investor guidelines. E.g., a lender may
   require 640 minimum credit score for FHA even though FHA allows 580.
   These vary by investor relationship and are business-configurable.)
- How often do eligibility rules change?
  (Investor guidelines change annually or when regulators update requirements.
   Lender overlays can change more frequently based on risk appetite.)
- What is a "conditional eligibility" scenario?
  (A loan may be eligible for conventional but not FHA, eligible for 30yr but not 15yr ARM,
   eligible for primary residence but not investment property.
   The eligibility check must run per product-term combination.)


---

## Decision

WRITE THIS YOURSELF

What is the decision? Be concrete — a sentence or two that could stand alone.
Describe the *what*, not the *why* (why belongs in Rationale).

---

## Alternatives Considered

### Alternative: Database-driven DSL (same model as LLPA conditions)
**Strengths:** Admin-manageable without code deployment, consistent with LLPA approach in ADR-0020
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- Phase 2 is building the eligibility engine for the first time.
  Eligibility rules are more complex than LLPA adjustments — they involve
  conditional logic, cross-field relationships, and per-state variations.
- Building a general-purpose rule evaluator for eligibility before understanding
  the full rule complexity is premature generalization.
- Start with hard-coded Java strategy pattern to understand the rule complexity,
  then extract to data-driven conditions in Phase 3 once the patterns are clear.
- The strategy pattern makes the transition to data-driven rules incremental:
  each EligibilityRule implementation is a candidate for externalization.

### Alternative: Drools rule engine
**Strengths:** Industry-standard for financial eligibility rules, handles complex rule interactions,
rules can be updated without code deployment, separates business rules from application code
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- Drools is a significant dependency with a steep learning curve (DRL syntax, KieSession management).
- For a portfolio project, Drools demonstrates "I know this tool exists" rather than
  "I understand the eligibility domain well enough to model it correctly."
- The Phase 3 goal (database-driven conditions) achieves the same admin-manageability
  without the Drools infrastructure.
- If the client base grows to the point where investor-specific rule complexity justifies Drools,
  the strategy pattern makes it straightforward to plug in a Drools-backed implementation.

---

## Rationale

### Strategy Pattern Provides Structure Without Premature Generalization

WRITE THIS YOURSELF
The strategy pattern (one EligibilityRule per product type) provides:
- Clear code organization: ConventionalEligibilityRule, FhaEligibilityRule, VaEligibilityRule
- Testability: each rule is unit-testable with mock quote parameters
- Extensibility: adding a new product type means adding a new rule implementation
- Interview-ability: clean object-oriented design, easy to walk through on a whiteboard

Without forcing a rule engine or DSL before the rule complexity is understood.

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

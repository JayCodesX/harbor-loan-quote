# ADR 0020: LLPA Modeling — Flat Table vs. Matrix vs. Rule Engine

## Status
Accepted

## Date
2026-04-02

## Phase
2 — Pricing Engine

---

## Context

LLPAs (Loan Level Price Adjustments) are price adjustments in points applied on top of the base rate sheet price based on loan characteristics. They are published by investors — primarily Fannie Mae and Freddie Mac for conforming loans — and are additive: all applicable adjustments for a given loan scenario are summed to produce the total price adjustment.

The primary loan characteristics that trigger LLPA adjustments are:
- **Credit score** — borrowers below 680-700 incur progressively higher adjustments
- **LTV (Loan-to-Value)** — higher LTV and lower down payments incur higher adjustments
- **Occupancy type** — investment properties and second homes carry higher fees than primary residences
- **Loan purpose** — cash-out refinances incur higher adjustments than purchase or rate/term refinance
- **Property type** — multi-unit properties typically carry higher adjustments than single family homes

Fannie Mae publishes LLPAs as matrices — a grid where rows are LTV bands and columns are credit score bands, with the price adjustment at each intersection. Multiple matrices exist for different loan categories. LLPA matrices change less frequently than rate sheets — announced with advance notice on a specific effective date, updated quarterly or as needed.

The system must be able to: load all active LLPA adjustments for an investor, evaluate each condition against the loan scenario parameters, and sum all matching adjustments to produce the total LLPA applied to the base rate sheet price.


---

## Decision

LLPAs are modeled as a flat condition table with JSON condition expressions:

- `llpa_adjustment`: `id`, `investor_id`, `product_type` (nullable = applies to all), `adjustment_category`, `condition_json`, `price_adjustment` (DECIMAL), `effective_at`, `expires_at`

Each row represents one LLPA rule. Evaluation: load all active adjustments for the investor, evaluate each condition against the loan scenario parameters, sum all matching adjustments. The total is applied on top of the base rate sheet price.

Example rows:
- `(FANNIE_MAE, CREDIT_SCORE, {"min": 680, "max": 699}, 0.50)`
- `(FANNIE_MAE, LTV, {"min": 75.01, "max": 80.00}, 0.25)`
- `(FANNIE_MAE, OCCUPANCY, {"equals": "INVESTMENT"}, 2.00)`
- `(FANNIE_MAE, LOAN_PURPOSE, {"equals": "CASH_OUT_REFI"}, 0.375)`
- `(FANNIE_MAE, PROPERTY_TYPE, {"equals": "MULTI_UNIT"}, 0.75)`


---

## Alternatives Considered

### Alternative: Multi-dimensional matrix (rows × columns grid)
**Strengths:** Matches exactly how Fannie Mae publishes LLPAs (credit score × LTV grid),
direct import from investor-published spreadsheets, natural representation for range intersections
**Rejected because:**
The matrix model is too rigid. Different LLPA types have different dimensionality — credit score × LTV is a 2D grid, but occupancy type, loan purpose, and property type are single-dimension lookups. A matrix model handles 2D cases naturally but forces 1D cases into an awkward fit. More importantly, the model has to account for what investor products are actually available to a borrower and what borrowers want — that combination of factors doesn't map cleanly to a fixed grid structure. Adding a new LLPA category introduced by Fannie Mae may require schema changes. The flat condition table handles all dimensionalities uniformly with no schema changes for new rule types.

### Alternative: Drools or similar rule engine
**Strengths:** Externalized business rules, admin can modify rules without code deployment,
handles complex rule interactions (exclusions, caps, floors), widely used in financial services
**Rejected because:**
The flat condition table already provides the core benefit of a rule engine — admins can add, modify, or deactivate LLPA adjustments through the admin UI without engineering involvement or a code deployment. Drools adds a significant dependency and requires the team to learn DRL (Drools Rule Language), a specialized skill, for a problem that is already solved by a simpler model. The LLPA logic is well-understood and well-bounded — it does not require a general-purpose rule engine. If rule complexity grows significantly in Phase 3+, the flat condition table can be migrated to DRL incrementally.

### Alternative: Hard-coded Java LLPA logic
**Strengths:** Fastest execution, type-safe, no JSON parsing, easy to unit test
**Rejected because:**
LLPA values change as investors update their matrices and as compliance requirements in the mortgage industry evolve. Hard-coding them means every LLPA change requires a code deployment — downtime during a rate-sensitive window costs borrowers who are actively quoting and investors whose products may be temporarily mispriced or unavailable. A data change should never require a deployment. The flat condition table allows admins to add, modify, or deactivate LLPA adjustments through the admin UI without engineering involvement and without any system downtime.

---

## Rationale

### The Flat Condition Table Handles All Standard LLPA Types Uniformly

Every LLPA type — regardless of its dimensionality — maps to the same pattern: a JSON condition expression and a decimal price adjustment. Credit score ranges use `{"min": 680, "max": 699}`, occupancy type uses `{"equals": "INVESTMENT"}`, property type uses `{"equals": "MULTI_UNIT"}`. The evaluator iterates all active adjustments for the investor, evaluates each condition against the loan scenario, and sums the matches. No special handling is required for different categories.

This uniformity provides flexibility as the mortgage industry evolves. When Fannie Mae introduces a new LLPA category, or a new investor with different adjustment structures is onboarded, new rows are added to the table — no schema changes, no code changes, no deployment. The system adapts to industry changes through data, not through engineering work.

### Admin-Manageable LLPA Data Is a Business Requirement

When investors publish LLPA matrix changes, it is lenders and pricing managers who are responsible for updating the adjustments in the system — not engineering. Requiring a code deployment for what is fundamentally a data update creates a bottleneck that slows down the business and introduces downtime risk during rate-sensitive windows. The flat condition table stored in MySQL and editable through the admin UI meets this requirement directly. Lenders can add, modify, or deactivate LLPA adjustments without engineering involvement, and changes take effect immediately without a deployment.

---

## Consequences

### Positive

- Accurate pricing with no friction — lenders and pricing managers can update LLPA adjustments immediately through the admin UI when investors publish matrix changes. No deployment, no downtime, no engineering bottleneck.
- The flat model is flexible enough to accommodate new LLPA categories as the industry evolves without schema or code changes.

### Negative

- The system is only as accurate as the LLPA data in it. Lenders must stay on top of industry-wide changes — when Fannie Mae or Freddie Mac publishes updated matrices, the adjustments must be updated promptly. Stale LLPA data means mispriced quotes, which has downstream consequences for both borrowers and investors.
- JSON condition evaluation adds a small runtime cost compared to hard-coded logic. At scale, the evaluator loading and iterating all active adjustments per quote request should be monitored for performance.

---

## Follow-up

- **Flyway migration for `llpa_adjustment`.** This table is foundational to pricing-service. Schema creation is Phase 1 work and must be in place before Phase 2 development begins alongside the rate sheet tables from ADR-0019.
- **Admin UI for LLPA management.** Lenders need to add, modify, and deactivate adjustments without engineering involvement. The admin interface for the `llpa_adjustment` table must be built as part of Phase 2.
- **Seed data for Fannie Mae and Freddie Mac LLPA matrices.** The public LLPA matrices (available from Fannie Mae's single family pricing page) should be loaded as seed data so the system has accurate baseline adjustments from day one.
- **ADR-0019 (Rate Sheet Data Model).** LLPA effective windows must align with rate sheet effective windows — same investor, same discipline. Review ADR-0019 for the immutability and versioning model that applies here as well.

# ADR 0019: Rate Sheet Data Model and Effective Window Design

## Status
Accepted

## Date
2026-04-02

## Phase
2 — Pricing Engine

---

## Context

A rate sheet is published by an investor and shows available rates and their corresponding prices (in points) for each product and term combination. For a given product, multiple rate/price pairs are offered — the borrower is choosing a trade-off between upfront cost and ongoing rate. A borrower with upfront cash can buy down the rate by paying points; a borrower who wants to minimize closing costs can take a higher rate in exchange for lender credits. Additional product features like interest-only periods and temporary buydowns are priced on top of this base structure. This is the baseline model used by industry PPEs (Product and Pricing Engines) including LoanPASS, Optimal Blue, and Mortech.

Rate sheets are published 1-4 times per day, sometimes more frequently during volatile markets. When an investor publishes a new sheet, the previous one is superseded. For borrowers mid-quote when a rate change fires, the Redis cache is invalidated and warmed with new rates (per ADR-0048) before their submission — the change is transparent to them. A quote that has already been submitted reflects the rate sheet that was active at calculation time. That rate is an estimate until locked — it cannot be honored by the investor until a rate lock is confirmed.

This creates an audit requirement: the system must be able to reproduce exactly what rates were shown to the borrower at the time of their quote. Storing `rate_sheet_id` on the pricing result is the mechanism — given the rate sheet ID, the calculation can be reproduced at any time. This is also relevant for compliance: rate sheets are financial data and must be treated as immutable once published.


---

## Decision

Rate sheets are modeled as two tables:

- `rate_sheet`: `id`, `investor_id`, `effective_at`, `expires_at`, `status` (ACTIVE / SUPERSEDED / EXPIRED), `imported_at`, `source`
- `rate_sheet_entry`: `id`, `rate_sheet_id`, `product_term_id`, `rate` (DECIMAL 5,4), `price` (DECIMAL 6,4)

Rate sheets are immutable once published. A new investor publication creates a new `rate_sheet` record and supersedes the previous one — the old record is never modified. `rate_sheet_id` is stored on every pricing result for audit reproducibility: given the ID, the exact rates shown to the borrower at the time of their quote can be reproduced. LLPAs are applied on top of the base rate sheet price and are modeled separately per ADR-0020.


---

## Alternatives Considered

### Alternative: Mutable rate table (UPDATE rates in place)
**Strengths:** Simpler schema, no versioning complexity, always shows current rates
**Rejected because:**
Updating rates in place destroys the history of what rates were and what they are now. A borrower who quoted at 7.0% on Monday has no record of that rate if Tuesday's update overwrites it. The system loses the ability to reproduce what was shown to the borrower at the time of their quote — which is both a compliance requirement and a product feature. Showing a borrower that rates have moved since their initial quote is a key incentive to lock. Mutability eliminates both the audit trail and that borrower-facing value. Immutable rate sheets with versioning are the industry standard for exactly this reason.

### Alternative: Store full rate snapshot on each quote (denormalized)
**Strengths:** Quotes are self-contained — no dependency on rate sheet history, total reproducibility, enables quote comparisons
**Rejected because:**
A rate sheet contains hundreds of rate/price pairs across all products and terms. Storing a full snapshot per quote is significant data duplication at scale. The `rate_sheet_id` foreign key on the pricing result provides the same reproducibility at a fraction of the storage cost — as long as rate sheets are immutable and never deleted. The critical operational discipline this requires: superseded rate sheets must never be deleted. Foreign key integrity must be enforced at the schema level. A full snapshot approach may be revisited in a future phase if quote comparison features justify the storage trade-off, but for Phase 2 the reference model is sufficient.

### Alternative: Event-sourced rate sheet history (all changes as events)
**Strengths:** Complete audit trail, time-travel queries, natural fit for event-driven architecture
**Rejected because:**
Rate sheet publications are already discrete atomic snapshots — each publication IS the event. Storing them as immutable records in the database already provides the audit trail and time-travel capability event sourcing offers. Adding a separate event store and replay mechanism is wasteful and inefficient for what borrowers actually need: the current active rate sheet served fast. Reconstructing rate sheet state by replaying events adds read complexity with no benefit over a direct query against an immutable record. The immutable table model achieves the same properties without the overhead.

---

## Rationale

### Immutability Is the Correct Model for Published Financial Data

A rate sheet is valid for a specific date/time window — it goes active at a point in time and is superseded when the investor publishes a new one. That cadence is unpredictable: it can change multiple times in a day, or not at all for days. When a new sheet is published, the investor has not modified the previous one — they have published a new one. The 9:00 AM sheet remains exactly what it was when it was active. Quotes calculated against it are still reproducible. The immutable model matches this business reality directly: each publication is a new record, and superseded records are retained permanently as the historical record of what rates were available and when.

### Effective Windows Enable Accurate Rate-at-Time-of-Quote Reporting

Storing `rate_sheet_id` on the pricing result is the mechanism that ties a quote to the exact version of rates that produced it. Even when that rate sheet is superseded and outdated, the reference remains valid — the record still exists and is immutable. Loading the `rate_sheet_id` from a quote, even one calculated months ago, reproduces exactly what the borrower was shown at that moment. The `effective_at` and `expires_at` timestamps on the rate sheet answer "what was active when?" and the foreign key on the pricing result answers "what was used for this specific quote?" Together they provide a complete, auditable, legally defensible record of rate history.

---

## Consequences

### Positive

- Quote comparisons become possible — the system can show a borrower how the rate on their prior quote compares to today's rate, creating a concrete incentive to lock.
- Prior rate sheets are easy to retrieve from the database by ID or by effective window. Historical queries are straightforward — no reconstruction or event replay required.
- Audit and compliance are satisfied by design. The `rate_sheet_id` on every pricing result is a permanent, reproducible reference to what rates were shown.

### Negative

- Rate sheet data accumulates permanently — superseded records are never deleted. Table size grows over time and will require archival or partitioning strategy at scale.
- Foreign key discipline is critical. If a rate sheet record is ever accidentally deleted, the pricing results referencing it lose their reproducibility. Schema-level constraints must enforce this.

---

## Follow-up

- **Flyway migrations for `rate_sheet` and `rate_sheet_entry`.** These tables are foundational — pricing-service cannot be built without them. Schema creation is Phase 1 work and must be in place before Phase 2 development begins.
- **Schema-level foreign key constraint.** Enforce `rate_sheet_id` on `rate_sheet_entry` and on pricing results at the database level. Accidental deletion of a rate sheet must be blocked by the schema, not just by convention.
- **Archival strategy for superseded rate sheets.** Define at what volume superseded records are moved to a cold storage or partitioned table. This is a Phase 3 concern but should be designed for from the start.
- **ADR-0020 (LLPA Modeling).** LLPAs are applied on top of the base rate sheet price. The LLPA data model must align with the rate sheet model — same investor, same effective window discipline.

# ADR 0036: Admin Reporting Query Isolation — Operational DB vs. Read Replica vs. CQRS

## Status
Deprecated

Admin app was removed in the 3-service consolidation. Reporting
will be handled through the Loan API if needed.

## Date
Fill in when you write this

## Phase
2/3

---

## Context

WRITE THIS YOURSELF

Prompts to answer:
- What does AdminReportService query?
  (Aggregations over loan_quotes: total quotes by date, quotes by credit tier,
   quote funnel conversion (submitted → priced → lead captured → completed),
   average rates by product type, partner performance metrics.)
- What is the problem with running these queries on the primary database?
  (Aggregation queries do full table scans when results are not indexed.
   As loan_quotes grows (100K, 1M rows), COUNT(*) GROUP BY queries take seconds.
   During those seconds, they compete with quote creation writes for MySQL thread pool and buffer pool.)
- What is a read replica?
  (A MySQL replica that receives binary log replication from the primary.
   Read queries are directed to the replica, leaving the primary for writes.
   Replication lag is typically < 1 second for write-light workloads.)
- What is CQRS (Command Query Responsibility Segregation)?
  (Separate the write model (commands: create quote, update status) from the read model
   (queries: reporting aggregations). The read model is populated by consuming events
   and maintaining pre-aggregated counters — queries become O(1) lookups.)
- At what scale does each approach become necessary?
  (Operational DB: < 100K rows and < 10 concurrent admin users.
   Read replica: 100K-10M rows or admin reports causing observable query slowdowns.
   CQRS: 10M+ rows or admin reporting needs sub-second response regardless of data size.)


---

## Decision

WRITE THIS YOURSELF

What is the decision? Be concrete — a sentence or two that could stand alone.
Describe the *what*, not the *why* (why belongs in Rationale).


---

## Alternatives Considered

### Alternative: CQRS projection from the start
**Strengths:** Admin reports are always O(1) lookups, primary database is completely isolated from reporting load
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- CQRS requires an event pipeline: quote lifecycle events (QuoteCreated, QuoteCompleted,
  QuoteFailed) must be published and consumed by a reporting projection service.
- At Phase 1-2 scale (< 100K quotes), CQRS is over-engineering.
  A read replica achieves the isolation goal at a fraction of the implementation cost.
- Add complexity when the simpler solution has actually proven insufficient.
  "100K rows is slow" is the signal to add a read replica.
  "Read replica is still slow" is the signal to consider CQRS.

### Alternative: Dedicated reporting database (separate schema, ETL pipeline)
**Strengths:** True analytical workload isolation, can use columnar storage optimized for aggregations
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- ETL pipeline adds significant infrastructure (batch jobs, transformation logic, scheduling).
- For the reporting volume of a mortgage lead aggregator, a MySQL read replica
  is more than adequate. Columnar storage (Redshift, BigQuery) is justified at
  data warehouse scale — not at a few hundred thousand rows.

---

## Rationale

### Staged Approach Matches Actual Scale Requirements

WRITE THIS YOURSELF
The cost of premature optimization:
- Building CQRS before it is needed adds months of development time.
- An unused projection pipeline adds maintenance overhead.
The cost of delayed optimization:
- Running aggregation queries on the primary adds query competition.
- At early scale this is unnoticeable. At production scale it degrades write performance.
The read replica is the right middle ground: low cost to add (RDS read replica is a few clicks),
directly solves the isolation problem, no application code changes required
(just point AdminReportService at the replica connection string).

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

# ADR 0008: Database Isolation Strategy — Shared Instance vs. Per-Service Instances

## Status
Superseded

Database isolation strategy is being revised for the 3-service
architecture. A new ADR will document the updated approach to
per-service schema ownership.

## Date
Fill in when you write this

## Phase
1 (shared instance) → 3 (per-service instances)

---

## Context

WRITE THIS YOURSELF

Prompts to answer:
- How many logical databases exist in the system?
  (5: mortgage_auth, mortgage_borrower, mortgage_quote_workflow, mortgage_pricing, mortgage_lead)
- What is the current state?
  (All 5 schemas run in a single MySQL container — single process, single disk, shared connections)
- What are the failure modes of a shared instance?
  (MySQL OOM = all services lose DB simultaneously.
   One slow query in pricing = buffer pool eviction affects auth queries.
   ALTER TABLE in one schema = shared thread pool affects other schemas.
   Max connections exhausted in one service = others cannot connect.)
- What is the cost constraint in Phase 1?
  (Single VPS — separating MySQL instances means either more containers on the same
   server (sharing the same disk anyway) or separate servers (higher cost))
- What does "per-service RDS instances" mean in Phase 3?
  (Each service gets its own RDS instance in AWS — true resource isolation,
   independent scaling, independent failure domains)


---

## Decision

WRITE THIS YOURSELF

What is the decision? Be concrete — a sentence or two that could stand alone.
Describe the *what*, not the *why* (why belongs in Rationale).

---

## Alternatives Considered

### Alternative: Per-service MySQL containers on the VPS (Phase 1)
**Strengths:** True process isolation even on a single server, failure domain per service, closer to Phase 3 target state
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- Five MySQL containers on a single VPS compete for the same disk I/O and RAM.
  The isolation is process-level only — the underlying hardware is still shared.
- Each MySQL instance has its own buffer pool (minimum ~128MB).
  Five instances = ~640MB minimum for buffer pools alone on a server with 8-16GB RAM.
- Operational overhead: 5 MySQL containers to monitor, backup, and upgrade.
- The failure mode this prevents (one DB crashing another) is low probability on a
  lightly loaded VPS with no runaway queries.
- The accepted risk of shared instance is appropriate for Phase 1 traffic volume.

### Alternative: Stay on shared instance through Phase 3 (never separate)
**Strengths:** Lowest cost, simplest operations, no migration work
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- At production scale on AWS, the shared instance becomes a single point of failure
  for the entire platform. MySQL OOM takes down every service simultaneously.
- Connection pool exhaustion is real — five services competing for max_connections
  under concurrent load causes request failures in services that have nothing to do
  with the overloaded service.
- InnoDB buffer pool contention: admin reporting full-table scans evict hot rows
  from the auth cache, degrading login performance during report runs.
- Per-service RDS instances in AWS allow independent scaling, independent maintenance
  windows, and proper blast radius containment.

### Alternative: PostgreSQL instead of MySQL
**Strengths:** Better JSON support, more advanced window functions, richer EXPLAIN output, preferred by many developers
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- The application is already written and tested against MySQL 8.4.
  Migrating to PostgreSQL requires testing all Flyway migrations, all JPA
  queries, and all MySQL-specific syntax against a new engine.
- This project is a portfolio project demonstrating Spring microservices,
  not a database comparison.
- MySQL 8 is a production-grade database adequate for this workload.
- This decision can be revisited per-service in Phase 3 since each service
  will have its own RDS instance (and could choose its engine independently).

---

## Rationale

### Phase 1: The Shared Instance Risk Is Acceptable at Low Traffic

WRITE THIS YOURSELF
At Phase 1 traffic (< 100 daily active users, likely < 1,000 quote requests/day):
- MySQL connection pool contention is not a real risk
- Buffer pool eviction from admin queries is not a real risk
- Runaway queries that OOM the shared instance are unlikely in a controlled dev/early prod environment
The cost and operational simplicity of a single container outweigh the theoretical risks
that only materialize under production load.
The Phase 3 migration plan is the commitment that this is not a permanent state.

### Phase 3: Service Isolation Is Required at Production Scale

WRITE THIS YOURSELF
Three specific scenarios that justify separate RDS instances:
1. Rate sheet ingestion in pricing-service writes hundreds of rows in a batch.
   On a shared instance, this write burst competes with auth login queries.
   On separate instances, pricing-service's write burst is invisible to auth-service.
2. Admin reporting in api runs aggregation queries over mortgage_quote_workflow.
   Full table scans evict InnoDB buffer pool entries.
   On a shared instance, this slows queries in other schemas.
   With a read replica on the api's RDS instance, admin queries hit the replica.
3. A deployment migration on mortgage_pricing (ALTER TABLE) acquires table locks.
   On a shared instance, this affects the shared connection thread pool.
   On a separate instance, only pricing-service is affected.

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

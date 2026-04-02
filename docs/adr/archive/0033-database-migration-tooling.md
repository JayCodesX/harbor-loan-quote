# ADR 0033: Database Migration Tooling — Flyway vs. Liquibase vs. Hibernate Auto-DDL

## Status
Archived — valid, deferred

Database migration tooling selection will be revisited as the
schema stabilizes.

## Date
Fill in when you write this

## Phase
1 — Foundation

---

## Context

WRITE THIS YOURSELF

Prompts to answer:
- What is the current state of DDL management?
  (spring.jpa.hibernate.ddl-auto: update or create-drop in dev.
   This is acceptable in development only — it allows schema drift,
   does not provide rollback, and does not track migration history.)
- What is a database migration tool?
  (A tool that tracks which SQL scripts have been applied to a database
   and applies pending scripts in version order. Ensures schema is consistent
   across all environments: dev, CI, staging, production.)
- What is Flyway?
  (SQL-first migration tool. Migration files are plain SQL scripts named
   V1__description.sql, V2__description.sql. Spring Boot auto-runs on startup.
   Tracks applied migrations in the flyway_schema_history table.)
- What is Liquibase?
  (XML/YAML/JSON-based changeset system. More complex, better rollback support,
   database-agnostic changesets that can generate MySQL or PostgreSQL DDL.)
- Why must ddl-auto never be update in production?
  (Hibernate's ddl-auto: update makes additive schema changes automatically but
   cannot handle destructive changes safely, does not version the schema,
   and will not roll back failed deployments. Production schema changes must be
   deliberate and auditable.)


---

## Decision

WRITE THIS YOURSELF

What is the decision? Be concrete — a sentence or two that could stand alone.
Describe the *what*, not the *why* (why belongs in Rationale).

---

## Alternatives Considered

### Alternative: Liquibase
**Strengths:** XML/YAML changesets are database-agnostic, better rollback support, diffs and preconditions
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- Liquibase's XML/YAML format adds a layer of indirection — you write a changeset
  description and Liquibase generates SQL. With Flyway, the SQL is the migration.
- For a team that knows SQL well (which a Spring/JPA team does), Flyway's
  SQL-first approach is more readable and reviewable.
- Liquibase rollback requires either a pre-defined rollback changeset or manual SQL.
  Flyway Pro has rollback support; Flyway Community does not. For most migrations,
  "rollback" means applying a new forward migration that reverses the change.
- Flyway is simpler, more widely used in the Spring community, and sufficient
  for this project's schema management needs.

### Alternative: Hibernate ddl-auto: update (current dev state, retained for production)
**Strengths:** Zero migration files to write, Hibernate keeps the schema current automatically
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- ddl-auto: update cannot DROP columns or tables — it only adds.
  A renamed column requires a migration; Hibernate would add the new column
  and leave the old one, resulting in schema drift.
- No migration history — there is no record of what changes were applied or when.
- Zero-downtime deployments require careful migration ordering
  (add column before deploy, deploy, remove old column after).
  This requires explicit migration files, not auto-DDL.
- If the database schema and entity classes get out of sync, ddl-auto: update
  may make destructive or incorrect changes.
  ddl-auto: validate catches this at startup instead of silently mishandling it.

---

## Rationale

### SQL-First Migrations Are Explicit and Reviewable

WRITE THIS YOURSELF
A Flyway migration file is a SQL script. Every developer on the team can read it.
Every code reviewer can verify it. Every DBA can audit it.
There is no generated DDL, no abstraction layer, no surprises.
V001__create_loan_quotes.sql is self-documenting in a way that a Liquibase XML changeset is not.
SQL is the universal language of relational databases — use it directly.

### Migration History Is Required for Production Operations

WRITE THIS YOURSELF
The flyway_schema_history table records every migration ever applied:
version, description, checksum, executed_at, success.
At any point, you can answer: "What schema version is production running?"
Rolling back a bad deploy is possible by identifying the migration version
and writing a compensating migration.
Without Flyway, the answer to "what changed the schema and when?" is unknown.

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

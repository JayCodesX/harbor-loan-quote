# ADR 0012: Integration Test Database — H2 vs. Testcontainers MySQL

## Status
Accepted

## Date
2026-04-01

## Phase
1 — Foundation

---

## Context

The system uses H2 in MySQL compatibility mode for integration tests — an in-memory Java database that starts in milliseconds with no Docker dependency. H2 supports a `MODE=MySQL` setting that emulates some MySQL behavior, but the emulation is incomplete. JSON functions, window functions, generated columns, strict mode enforcement, and several other MySQL 8 behaviors are not covered.

The practical consequence is dialect drift: a query that H2 accepts may fail on production MySQL. Tests pass, CI is green, and the bug surfaces in production. This is a false confidence failure — the test suite signals safety it cannot actually guarantee. In effect, H2 tests are testing H2, not MySQL. No meaningful integration signal is produced for the database engine the system actually runs on.

Testcontainers is a Java library that spins up real Docker containers in tests — MySQL 8.4, Flyway migrations applied, tests run, container torn down. The startup overhead is approximately 10-15 seconds per suite with a `@ServiceConnection` singleton (one container shared across all tests in a suite, not one per test). This is the cost of eliminating an entire class of dialect-mismatch bugs.


---

## Decision

Integration tests run against a real MySQL 8.4 instance managed by Testcontainers. H2 is removed from the test stack. All services use a shared `@ServiceConnection` singleton container per test suite run so startup overhead is paid once, not per test.

---

## Alternatives Considered

### Alternative: Retain H2 in MySQL compatibility mode
**Strengths:** Zero Docker dependency, sub-second startup, no internet required to pull images, CI runs without Docker daemon
**Rejected because:**
H2's MySQL compatibility mode does not fully emulate MySQL 8. As the codebase grows — more complex queries, window functions for LLPA calculations, JSON columns — the coverage gap widens silently. Each new MySQL-specific feature added is another blind spot H2 cannot see. The team accumulates invisible risk: the test suite stays green while the gap between what is tested and what runs in production grows larger. At scale, this erodes trust in the test suite entirely — a green CI run stops meaning anything.

### Alternative: Mock all repositories with @MockBean
**Strengths:** Fastest possible tests, no database dependency at all, pure unit tests
**Rejected because:**
Mocking repositories doesn't test the database — it tests that your service code calls the repository method. The schema is never touched. A required foreign key, a NOT NULL constraint, a missing column in a Flyway migration, or a `@Query` annotation with invalid SQL all pass mocked tests without issue and fail at runtime. Mocking is appropriate for unit tests of isolated business logic. Integration tests must hit a real database — that is their purpose.

### Alternative: Testcontainers with PostgreSQL (opportunistic migration)
**Strengths:** If switching engines anyway, PostgreSQL offers better JSON support and developer tooling
**Rejected because:**
PostgreSQL is overkill for this workload and introduces team complexity that has nothing to do with the problem being solved. The goal is test correctness against the production database engine — not an engine migration. Switching to PostgreSQL means rewriting all Flyway migrations from MySQL DDL, and adds a learning curve for any team member unfamiliar with PostgreSQL. MySQL 8.4 handles the workload, is well understood, and is what runs in production. If per-service engine decisions are revisited in Phase 3 (each service on its own RDS instance), that is the right time to evaluate PostgreSQL per service.

---

## Rationale

### Tests Must Use the Same Database Engine as Production

A test suite that passes on H2 but would fail on MySQL is not a passing test suite — it is a false signal. The tests are effectively worthless for catching database-layer bugs because they are not testing the database layer that runs in production. An integration test that goes green on H2 while hiding a MySQL syntax error, a constraint violation, or a Flyway migration failure is worse than no test at all — it provides confidence that hasn't been earned.

Testcontainers with MySQL 8.4 eliminates this class of problem entirely. Flyway migrations run against the real engine. Queries execute against real MySQL behavior. Schema constraints are enforced. Every test failure is a real failure, and every passing test is a genuine signal.

### The Startup Cost Is Acceptable

The counterargument to Testcontainers is always startup time — H2 is instant, Testcontainers adds approximately 10-15 seconds per suite. This cost is acceptable because what it buys is real test signal. A 15-second wait that catches a broken migration or an invalid query is worth more than an instant run that silently misses both.

The overhead is also bounded. With a `@ServiceConnection` singleton, one container starts per suite run — not per test. Across all services, the total added time is measured in minutes, not hours. That is the cost of actually testing the database schema, and it is the right trade-off.

---

## Consequences

### Positive

- Integration tests simulate production accurately. Every test that passes is a genuine signal — schema constraints, Flyway migrations, and query correctness are all exercised against the real database engine.

### Negative

- Slower test runs. The Testcontainers startup overhead is real, and Docker must be available in every environment where tests run (local, CI). Developers without Docker running locally cannot run integration tests.
- Additional maintenance surface — container versions must be kept in sync with the production MySQL version. Over time this pays for itself in bugs caught early, but the overhead is real upfront.

---

## Follow-up

- **Remove H2 from all service test configurations.** Drop the H2 dependency and `MODE=MySQL` test properties across all services. Replace with Testcontainers `@ServiceConnection` setup.
- **CI must have Docker available.** Any CI pipeline running integration tests requires a Docker daemon. Verify this is in place before removing H2 — otherwise the pipeline breaks with no fallback.

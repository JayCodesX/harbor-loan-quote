# ADR 0037: Package Architecture Enforcement — ArchUnit vs. Module System vs. None

## Status
Archived — valid, deferred

Package architecture enforcement remains a good engineering
practice. Will be revisited as the codebase matures.

## Date
Fill in when you write this

## Phase
1 — Foundation

---

## Context

WRITE THIS YOURSELF

Prompts to answer:
- What are the domain packages inside harbor-api?
  (quote, directory, consent, subscription, admin, auth — each owns its own entities,
   services, and controllers. The risk is cross-domain direct class references
   that should go through service interfaces instead.)
- What is the "distributed monolith" risk inside a single service?
  (If the admin package directly imports from the quote package's JPA repositories,
   and the quote package directly imports from the consent package's entities,
   the packages become tightly coupled. Extracting them later (Phase 2 directory-service)
   requires untangling those dependencies.)
- What is ArchUnit?
  (A Java library for writing architectural tests. Tests like:
   "classes in the admin package must not access classes in the quote.repository package."
   These run in the test suite and fail the build if architecture rules are violated.)
- What is the Java module system (JPMS)?
  (Java 9+ module system. module-info.java declares which packages a module exports
   and which modules it requires. Enforces encapsulation at the JVM level.
   Significant adoption barrier with Spring Boot — most Spring libraries are not modularized.)


---

## Decision

WRITE THIS YOURSELF

What is the decision? Be concrete — a sentence or two that could stand alone.
Describe the *what*, not the *why* (why belongs in Rationale).

---

## Alternatives Considered

### Alternative: Java Platform Module System (JPMS)
**Strengths:** JVM-level enforcement, strongest possible encapsulation, cannot be bypassed by reflection
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- Spring Boot and most Spring libraries are not JPMS modularized.
  Integrating JPMS with Spring Boot requires significant configuration
  and often breaks with Spring's extensive use of reflection and classpath scanning.
- The barrier to adoption is too high relative to the benefit for a Spring Boot project.
- ArchUnit provides build-time enforcement without JVM-level complications.

### Alternative: No enforcement (code review only)
**Strengths:** Zero implementation cost, no test maintenance
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- Code review catches violations only when a reviewer notices. Architecture rules
  that are not enforced by tooling drift over time as new code is added.
- "Don't call the quote repository directly from the admin controller" is a rule
  that is easy to accidentally violate and hard to notice in a PR review.
- ArchUnit rules document the intended architecture and enforce it automatically.
  They serve double duty: specification and enforcement.

---

## Rationale

### Architecture Rules Are Tests — They Belong in the Test Suite

WRITE THIS YOURSELF
The service boundary decision (ADR-0002) defines which services exist.
ArchUnit enforces the domain boundaries within a service.
Both are architectural decisions. Both should be verified automatically.
An ArchUnit rule like "services in the quote package must not be called directly from admin"
mirrors the microservice boundary decision at the intra-service level.
If the directory package is later extracted to directory-service (Phase 2 plan),
the ArchUnit rules will have kept its dependencies clean and the extraction will be easier.

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

# ADR 0007: Messaging Transport Abstraction

## Status
Accepted

## Date
2026-04-01

## Phase
1 — Foundation

---

## Context

The system needs async messaging for rate propagation and notifications — workflows driven by pricing-service in Phase 2. The broker that makes sense for a self-contained VPS deployment (Phase 2) is different from the one that makes sense on AWS (Phase 3). The broker choice will need to change between phases.

Building Phase 2 messaging directly against a specific broker creates a coupling problem. If business publishers and consumers reference SQS or RabbitMQ SDK code directly, swapping brokers in Phase 3 requires touching every publisher and consumer in the codebase. That is a high-cost, high-risk migration that slows production delivery and burns time and money that should be spent on product features.

The transport abstraction pattern solves this by making the broker a plug-and-play implementation detail. Business code talks to a single broker-agnostic interface — publish an event, subscribe to a queue. Broker-specific code (RabbitMQ adapter, SQS adapter) lives in one place behind that interface. Swapping brokers between phases means swapping the adapter and updating config — nothing in business code changes.

---

## Decision

All async messaging in the system is built against a broker-agnostic transport abstraction interface. No business code references SQS or RabbitMQ directly. The broker implementation is swappable via configuration, allowing the platform to pivot between brokers as deployment needs and scale evolve.

---

## Alternatives Considered

### Alternative: Build messaging directly against a specific broker (SQS or RabbitMQ)
**Strengths:** Simpler upfront — no abstraction layer to design, direct use of broker SDK
**Rejected because:**
Building directly against a broker couples every publisher and consumer to that broker's SDK. Swapping brokers in Phase 3 requires touching all of them — a high-cost, high-risk migration that burns time and money and slows getting the platform to production. The abstraction costs one interface and two adapters upfront. The savings on the Phase 3 migration are significant.

### Alternative: Defer the abstraction decision to Phase 2
**Strengths:** Don't pay the design cost until messaging is actually needed
**Rejected because:**
Phase 1 doesn't implement the abstraction — it defines the interface shape so the transport abstraction is ready to use when Phase 2 starts. If that interface isn't decided in Phase 1, Phase 2 will build publishers and consumers directly against whatever broker is available. By the time the abstraction feels necessary, the coupling has already accumulated across the codebase and retrofitting it becomes a significant refactor. The interface design is a Phase 1 architectural guardrail. The broker adapters and business messaging code are Phase 2 work.

---

## Rationale

### The Broker Is an Implementation Detail, Not a Business Concern

Business code calls publish or subscribe — that's it. It doesn't know which broker is active, which SDK is in use, or how the message is delivered. The transport abstraction handles all of that. The RabbitMQ adapter and SQS adapter are interchangeable behind the interface, and swapping between them is a configuration change with no impact on business code.

### Upfront Abstraction Protects the Phase 3 Migration

With the transport abstraction in place, the Phase 3 SQS migration is simple — configure SQS as the transport and the system uses it. No business code changes. Without the abstraction, that same migration touches every publisher and consumer in the codebase. The abstraction is identified as the first and most critical step in the messaging migration plan (see docs/rabbitmq-migration-ticket-board.md, S2-01) for exactly this reason.

---

## Consequences

### Positive

- Phase 1 can be completed without committing to a broker. The broker decision is deferred to Phase 2 where it belongs, without losing the architectural protection.
- Phase 2 and Phase 3 can change the messaging transport without rewriting business code. The abstraction absorbs the change.

### Negative

- If the same broker is used across all phases, the abstraction was unnecessary overhead — an extra layer designed to solve a problem that never materialized.
- Any developer adding messaging to the system must understand the abstraction first. If they bypass it and publish directly against a broker SDK, the pattern is violated and the Phase 3 migration becomes harder. The abstraction only works if the whole team uses it consistently.

---

## Follow-up

- Broker selection (which broker for Phase 2 VPS, which for Phase 3 AWS) is a separate decision — see ADR-0050.
- The transport abstraction interface must be designed and implemented before any Phase 2 messaging is built.
- See docs/rabbitmq-migration-ticket-board.md for the implementation plan.

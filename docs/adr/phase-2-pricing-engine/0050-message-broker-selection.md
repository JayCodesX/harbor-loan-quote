# ADR 0050: Message Broker Selection — RabbitMQ (Phase 2) and SQS (Phase 3)

## Status
Accepted

## Date
2026-04-02

## Phase
2 — Pricing Engine

---

## Context

Phase 2 introduces pricing-service and notification-service, which means async messaging is now real. Two workflows require a message broker: rate propagation (pricing-service → notification-service when a rate sheet is activated) and notifications (harbor-api → notification-service for borrower alerts).

The existing dev setup uses LocalStack to emulate SQS locally. LocalStack is a development tool — it mimics SQS behavior for local testing and is not viable in a production environment. Real SQS requires an AWS account, an internet dependency, and incurs cost at any volume. Phase 2 targets a single VPS running Docker Compose with no AWS services. A broker that runs in that environment is required.

Phase 3 migrates to AWS, at which point SQS is the right managed service. ADR-0007 established a transport abstraction interface precisely so the broker can be swapped at that point without touching business code.

---

## Decision

RabbitMQ is deployed as a Docker Compose service on the Phase 2 VPS and serves as the message broker for all async workflows. Once the product is proven and generating revenue from clients, the system migrates to AWS SQS in Phase 3. The transport abstraction from ADR-0007 makes this a configuration swap — no business code changes at migration time.

---

## Alternatives Considered

### Alternative: AWS SQS (retain current LocalStack design for production)
**Strengths:** Already integrated in dev, managed service, no broker to operate, mature DLQ support
**Rejected because:**
LocalStack is a development tool — it mimics SQS locally and is not production-viable. Real SQS requires an AWS account, an internet dependency, and incurs cost at any volume. Phase 2 runs on a VPS with Docker Compose and no AWS services. Introducing a real AWS dependency before the product has proven revenue is premature operational cost. SQS is the right choice in Phase 3 when the platform migrates to AWS.

### Alternative: In-process Spring Application Events
**Strengths:** Zero infrastructure, no container, synchronous or async within the JVM
**Rejected because:**
Spring Application Events are in-process — they cannot cross service boundaries over the network. The async workflows here span harbor-api, pricing-service, and notification-service running as separate JVMs. An event published in pricing-service cannot be received by notification-service. Additionally, Spring Events have no DLQ, no retry, and no redelivery semantics — events are lost on JVM restart. This is appropriate for intra-service event handling, not cross-service async messaging.

### Alternative: Apache ActiveMQ
**Strengths:** Mature, JMS-based, Spring has first-class JMS support
**Rejected because:**
ActiveMQ is primarily JMS-oriented, which narrows its flexibility. RabbitMQ supports AMQP natively and can speak JMS, STOMP, and MQTT via plugins — more protocol flexibility with less lock-in to a single messaging model. RabbitMQ is also more widely adopted in Docker Compose environments and has a larger ecosystem of Spring integration examples. For a team already planning a Phase 3 SQS migration, RabbitMQ's AMQP model maps more cleanly to that transition than JMS.

---

## Rationale

### RabbitMQ Eliminates the AWS Runtime Dependency in Phase 2

The decision is revenue-gated and intentionally so. AWS managed services carry cost at any volume — cost that is not justified before the product has proven clients. RabbitMQ runs as a Docker Compose container on the same VPS as the rest of the platform, with no external dependencies and no per-message cost. It gives the team financial flexibility to build and iterate while the product is being proven. SQS becomes the right choice in Phase 3, when AWS infrastructure is already in place and the revenue exists to justify it.

### RabbitMQ Has Feature Parity With SQS for This Workflow

RabbitMQ covers the messaging requirements for Phase 2 — DLQ via dead-letter exchanges, retry via TTL policies, and at-least-once delivery via consumer ACK/NACK with durable queues. The difference from SQS is operational: SQS handles these as managed service defaults, while RabbitMQ requires explicit configuration. That configuration overhead is the honest cost of running self-hosted. The message contracts (messageId, correlationId, schemaVersion) are defined at the transport abstraction layer and are broker-agnostic — they work the same on RabbitMQ in Phase 2 and SQS in Phase 3.

### The Transport Abstraction Makes Phase 3 a Config Change

When Phase 3 arrives, swapping RabbitMQ for SQS is a configuration change, not a code change. The SQS adapter implements the same transport interface as the RabbitMQ adapter — drop in the SQS configuration, point the code there, done. Business logic in pricing-service and notification-service never referenced a broker directly. This is the payoff of the transport abstraction established in ADR-0007.

---

## Consequences

### Positive

- Financial flexibility — no AWS dependency or per-message cost while the product is being proven. RabbitMQ runs on the existing VPS at no additional infrastructure cost.
- Code flexibility — the transport abstraction means the broker is swappable. Business logic is never coupled to RabbitMQ or SQS.

### Negative

- Self-hosting RabbitMQ carries operational responsibility the team does not have with SQS — uptime, DLQ configuration, retry policy setup, and monitoring are all manual.
- The Phase 3 migration to SQS, while minimal in code changes due to the abstraction, still requires work: AWS queue provisioning, IAM policies, environment config updates, and end-to-end testing of the new transport.

---

## Follow-up
- **RabbitMQ setup and configuration.** DLQ via dead-letter exchanges, retry via TTL policies, and durable queues must be configured before Phase 2 async workflows are live. See docs/rabbitmq-migration-ticket-board.md.
- **ADR-0007 (transport abstraction).** The RabbitMQ adapter must implement the transport interface defined there. No business code should reference RabbitMQ directly.
- **ADR-0028 (AWS migration strategy).** Phase 3 SQS swap is gated on the AWS migration. Review ADR-0028 for sequencing.

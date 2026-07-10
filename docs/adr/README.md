# Architecture Decision Records

This project records significant architectural decisions as ADRs. Each ADR captures the **context**, the **decision**, the **alternatives considered**, and the **rationale** — so the reasoning behind the system is auditable, not just the result.

ADRs are grouped by delivery phase:

- **Phase 1 — Foundation:** service boundaries, messaging abstraction, auth, testing
- **Phase 2 — Pricing Engine:** rate sheets, LLPA pricing, real-time updates, broker selection
- **Phase 3 — Scale & Operations:** AWS migration and operational concerns

> **Status legend:** `Accepted` — decided and in effect (in design or code) · `Proposed` — drafted, not finalized.

## Phase 1 — Foundation

| ADR | Title | Status | Date |
|----|-------|--------|------|
| [0001](./phase-1-foundation/0001-service-decomposition.md) | Three-Service Architecture — api, pricing-service, notification-service | Accepted | 2026-04-01 |
| [0003](./phase-1-foundation/0003-synchronous-quote-calculation.md) | Synchronous HTTP for Quote Calculation, Not Message Queue | Accepted | 2026-04-01 |
| [0004](./phase-1-foundation/0004-spring-mvc-vs-webflux-placement.md) | Spring MVC in api/pricing-service, WebFlux in notification-service | Accepted | 2026-04-01 |
| [0007](./phase-1-foundation/0007-messaging-transport-abstraction.md) | Messaging Transport Abstraction | Accepted | 2026-04-01 |
| [0010](./phase-1-foundation/0010-service-jwt-signing-algorithm.md) | Service-to-Service JWT Signing — HMAC vs. RSA Asymmetric | Accepted | 2026-04-01 |
| [0012](./phase-1-foundation/0012-test-database-strategy.md) | Integration Test Database — H2 vs. Testcontainers MySQL | Accepted | 2026-04-01 |
| [0034](./phase-1-foundation/0034-consent-audit-log.md) | Consent and Privacy Audit Log Durability | Accepted | 2026-04-01 |

## Phase 2 — Pricing Engine

| ADR | Title | Status | Date |
|----|-------|--------|------|
| [0019](./phase-2-pricing-engine/0019-rate-sheet-data-model.md) | Rate Sheet Data Model and Effective Window Design | Accepted | 2026-04-02 |
| [0020](./phase-2-pricing-engine/0020-llpa-modeling-approach.md) | LLPA Modeling — Flat Table vs. Matrix vs. Rule Engine | Accepted | 2026-04-02 |
| [0048](./phase-2-pricing-engine/0048-dynamic-rate-sheet-update-strategy.md) | Dynamic Rate Sheet Update Strategy | Accepted | 2026-04-02 |
| [0049](./phase-2-pricing-engine/0049-real-time-notification-via-sse.md) | Real-Time Borrower Notification via Server-Sent Events (SSE) | Accepted | 2026-04-01 |
| [0050](./phase-2-pricing-engine/0050-message-broker-selection.md) | Message Broker Selection — RabbitMQ (Phase 2) and SQS (Phase 3) | Accepted | 2026-04-02 |
| [0052](./phase-2-pricing-engine/0052-kafka-rate-change-event-stream.md) | Kafka (Redpanda) for the Rate-Change Event Stream, Complementing RabbitMQ | Accepted | 2026-07-09 |

## Phase 3 — Scale & Operations

| ADR | Title | Status | Date |
|----|-------|--------|------|
| [0028](./phase-3-scale-and-operations/0028-aws-migration-strategy.md) | AWS Migration Strategy — Trigger Criteria and Migration Path | Proposed | — |
| [0030](./phase-3-scale-and-operations/0030-observability-strategy.md) | Observability — OpenTelemetry + Grafana vs. AWS X-Ray vs. Datadog | Accepted | 2026-07-09 |
| [0055](./phase-3-scale-and-operations/0055-oracle-cloud-cloudflare-tunnel-deployment.md) | Deployment Topology — Oracle Cloud Always-Free + Cloudflare Tunnel | Accepted | 2026-07-09 |

## Phase 4 — AI Agent Integration

| ADR | Title | Status | Date |
|----|-------|--------|------|
| [0051](./phase-4-ai-integration/0051-expose-pricing-via-mcp.md) | Expose the Pricing Engine to AI Agents via a Model Context Protocol (MCP) Server | Accepted | 2026-07-09 |
| [0053](./phase-4-ai-integration/0053-secure-and-rate-limit-the-mcp-boundary.md) | Secure and Rate-Limit the Agent-Facing MCP Boundary — API Key + Bucket4j | Accepted | 2026-07-09 |
| [0054](./phase-4-ai-integration/0054-mcp-client-agent-demo.md) | MCP Client Demo — an LLM Agent Calling the Pricing Tools (Ollama) | Accepted | 2026-07-09 |

---

### A note on implementation status
These ADRs capture **design decisions**. The 6→3 service consolidation is complete: harbor-api now owns auth, borrowers, leads, and quotes in-process; the standalone auth-service, borrower-service, and lead-service modules are gone. The async messaging layer runs locally over RabbitMQ — harbor-api and pricing-service publish to topic exchanges, and notification-service consumes both flows (ADR-0007, ADR-0050). SQS remains the Phase-3 target adapter, available behind the `integration` profile. See the [architecture overview](../architecture.md) for full detail.

### Archive
The [`archive/`](./archive) folder holds earlier exploratory ADRs that are **no longer active** and not slated for implementation. They are retained for historical context only and are not part of the current design index above.

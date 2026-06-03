# Architecture

Harbor Loan Quotes is a mortgage quote and lead-generation platform built as a set of independently-deployable Spring Boot services behind an Nginx edge, with two React frontends (a borrower-facing app and an admin app).

This document expands on the high-level overview in the [README](../README.md).

> **Implementation status.** The synchronous paths (quote creation/retrieval, calculators, auth, borrower APIs, lead capture, metrics, admin) are implemented and run in harbor-api. The **asynchronous messaging layer runs locally over RabbitMQ** (the default transport in Docker Compose): harbor-api publishes quote notification snapshots and pricing-service publishes rate-sheet-activated events; notification-service consumes both and pushes SSE to the frontend. SQS is the Phase-3 target adapter, available behind the `integration` profile. See [ADR-0007](./adr/phase-1-foundation/0007-messaging-transport-abstraction.md) and [ADR-0050](./adr/phase-2-pricing-engine/0050-message-broker-selection.md).

## System diagram

```mermaid
flowchart LR
    B["Borrower App (web)"] --> E["Nginx Edge"]
    A["Admin App (admin-web)"] --> E
    E --> API["harbor-api"]
    E --> NOTIFY["notification-service"]
    API --> REDIS["Redis"]
    API --> MYSQL["mortgage_quote_workflow\n(auth/borrower/lead/quote tables)"]
    API -->|"sync HTTP"| PRICING["pricing-service"]
    API -->|"quote.notification.events\nQUOTE_NOTIFICATION_SNAPSHOT"| MQ["RabbitMQ"]
    PRICING --> MYSQLP["mortgage_pricing"]
    PRICING --> REDIS
    PRICING -->|"rate-sheet.events\nRATE_SHEET_ACTIVATED"| MQ
    MQ -->|"quote.notification.snapshot\nrate-sheet.activated"| NOTIFY
    NOTIFY --> REDIS
    NOTIFY --> E
```

## Design principles

- **Service-per-domain.** Each service owns one business capability and one database schema. No service reads another service's tables — cross-service data flows over HTTP (sync) or a message broker (async).
- **Lead capture in-process.** Lead creation from a refined quote runs synchronously inside harbor-api. No lead queues exist. (ADR-0003)
- **Synchronous quote pricing.** harbor-api calls pricing-service over HTTP. Pricing is fast and consistent; async indirection for pricing adds latency without benefit at this scale. (ADR-0003)
- **Async for notifications and rate-sheet events.** Notification snapshot delivery and rate-sheet-activated fan-out are decoupled through RabbitMQ so the quote request path returns immediately and the frontend receives SSE updates asynchronously.
- **Read models for fast reads.** `harbor-api` maintains a quote read model and `notification-service` keeps a Redis snapshot, so status checks never block on downstream workers.
- **Broker-agnostic transport.** Async messaging is built against a single transport interface with swappable adapters (NoOp / RabbitMQ / SQS), so the broker is a config choice rather than a code dependency ([ADR-0007](./adr/phase-1-foundation/0007-messaging-transport-abstraction.md)). Message contracts carry `schemaVersion` and `messageId` for versioned, idempotent delivery.
- **Pluggable auth.** User authentication runs in either a self-issued JWT mode or an OIDC (Keycloak) mode without code changes.

## Request flow (quote lifecycle)

1. An anonymous user requests a public mortgage quote. *(implemented)*
2. `harbor-api` deduplicates repeated requests per session and persists quote state in `mortgage_quote_workflow`. *(implemented)*
3. `harbor-api` calls `pricing-service` synchronously over HTTP; `pricing-service` prices the scenario against its persisted product catalog and returns a result. *(implemented)*
4. `harbor-api` updates the quote read model with the pricing result. *(implemented)*
5. If an authenticated user refines the quote, `harbor-api` creates the lead in-process. *(implemented — in-process, no async round-trip)*
6. `harbor-api` publishes a `QUOTE_NOTIFICATION_SNAPSHOT` event to the `quote.notification.events` RabbitMQ exchange. *(implemented)*
7. `notification-service` consumes from queue `quote.notification.snapshot`, stores the latest snapshot in Redis, and pushes an SSE update to the frontend. *(implemented)*
8. `admin-web` reads aggregated metrics through `harbor-api`. *(implemented)*

## Service responsibilities

| Service | Owns | Store |
|---|---|---|
| `harbor-api` (`api`) | Public quote creation/retrieval, refinement orchestration, calculators, session dedupe, metrics aggregation, notification publishing, service-JWT issuance; auth (registration, login, token issuance); borrower creation/retrieval/metrics; lead creation/metrics | `mortgage_quote_workflow` (auth/borrower/lead/quote tables) |
| `pricing-service` | Synchronous quote pricing (called by harbor-api over HTTP), pricing catalog (products, rate sheets, adjustment rules), Redis pricing cache, rate-sheet-activated event publishing | `mortgage_pricing` |
| `notification-service` | Quote notification snapshot consumption, rate-sheet-activated event consumption, Redis snapshot cache, snapshot fetch endpoint, SSE stream | Redis only |

## Database ownership

Each service owns its schema; there is no cross-service table access.

- `harbor-api` → `mortgage_quote_workflow` (contains auth, borrower, lead, and quote tables)
- `pricing-service` → `mortgage_pricing`
- `notification-service` → Redis only

## Messaging

Async messaging is built against a **broker-agnostic transport abstraction** ([ADR-0007](./adr/phase-1-foundation/0007-messaging-transport-abstraction.md)): a `MessageTransport` interface selected by `app.transport.type`, with adapters for:

- `noop` — no messages published; async layer inert (used in test environments)
- `rabbitmq` — **active default** in Docker Compose; the local Phase-2 broker ([ADR-0050](./adr/phase-2-pricing-engine/0050-message-broker-selection.md))
- `sqs` — Phase-3 target; LocalStack emulates SQS locally behind the `integration` profile

### Active RabbitMQ topology

**Flow A — Quote notifications** (harbor-api → notification-service):

| Component | Name |
|---|---|
| Exchange | `quote.notification.events` (topic, durable) |
| Routing key | `QUOTE_NOTIFICATION_SNAPSHOT` |
| Queue | `quote.notification.snapshot` (durable) |

**Flow B — Rate-sheet activation** (pricing-service → notification-service):

| Component | Name |
|---|---|
| Exchange | `rate-sheet.events` (topic, durable) |
| Routing key | `RATE_SHEET_ACTIVATED` |
| Queue | `rate-sheet.activated` (durable) |

harbor-api declares the `quote.notification.events` exchange and publishes only. notification-service declares all exchanges, queues, and bindings for both flows. pricing-service declares the `rate-sheet.events` exchange independently; AMQP is idempotent on re-declaration of matching durable exchanges.

### Contract hardening

Each payload carries `schemaVersion` and `messageId`. Consumers reject unsupported schema versions, dedupe on `messageId`, and route poison messages to DLQs. The LocalStack-SQS DLQ tooling is described in the [operations runbook](./ops-runbook.md).

### Lead queues removed
Lead processing was consolidated in-process inside harbor-api (ADR-0003). The `quote-lead-requests` and `quote-lead-results` queues no longer exist.

## Security model

**User authentication** — two interchangeable modes for protected endpoints in `harbor-api`:

- `internal`: harbor-api issues HMAC-signed JWTs.
- `oidc`: local Keycloak issues OIDC access tokens validated against a configured JWK set URI.

**Service-to-service authentication** — internal calls use service JWTs validated on issuer, audience, scope, and type. Examples: `harbor-api → pricing-service` (sync HTTP job token), `harbor-api → notification-service` (RabbitMQ message token, validated by the consumer).

## Session tracking & deduplication

The borrower frontend generates and persists a session ID, sent as `X-Session-Id`. `harbor-api` uses Redis to track active sessions, detect in-flight duplicate quote requests, return the existing `quoteId` when a duplicate is already `QUEUED`/`PROCESSING`, and maintain fast status snapshots.

## Technology choices

- **Java 17 / Spring Boot** for all backend services
- **MySQL** for per-service relational persistence
- **Redis** for session state, dedupe, caching, metrics counters, and notification snapshots
- **RabbitMQ** (active local broker) via the broker-agnostic transport abstraction; SQS for Phase-3 (LocalStack emulates it locally)
- **Keycloak** for the optional OIDC profile
- **React + Vite** for the borrower and admin apps
- **Nginx** as the public edge reverse proxy (HTTP `8088`, TLS `8443`)
- **Docker Compose** for local orchestration; **Jenkins** for CI

## Design history

The system was built incrementally and every significant decision is captured as an Architecture Decision Record under [`docs/adr`](./adr), organized by phase (foundation → pricing engine → scale & operations).

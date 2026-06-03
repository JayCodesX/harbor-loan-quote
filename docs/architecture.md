# Architecture

Harbor Loan Quotes is an event-driven mortgage quote and lead-generation platform built as a set of independently-deployable Spring Boot services behind an Nginx edge, with two React frontends (a borrower-facing app and an admin app).

This document expands on the high-level overview in the [README](../README.md).

> **Implementation status.** The synchronous paths (quote creation/retrieval, calculators, auth, borrower APIs, metrics, admin) are implemented. The **asynchronous messaging layer is scaffolded but not yet wired end to end**: a broker-agnostic transport abstraction and adapters exist, but the default transport is `noop`, so the cross-service async pipeline (pricing/lead/notification over a broker) does not yet run. Enabling it locally via LocalStack SQS or RabbitMQ is the next step. Diagrams and flows below describe the **target design**; see [ADR-0007](./adr/phase-1-foundation/0007-messaging-transport-abstraction.md) and [ADR-0050](./adr/phase-2-pricing-engine/0050-message-broker-selection.md).

## System diagram (target design)

```mermaid
flowchart LR
    B["Borrower App (web)"] --> E["Nginx Edge"]
    A["Admin App (admin-web)"] --> E
    E --> API["api"]
    E --> AUTH["auth-service"]
    E --> BORR["borrower-service"]
    E --> NOTIFY["notification-service"]
    API --> REDIS["Redis"]
    API --> MYSQLQ["mortgage_quote_workflow"]
    API --> SQS["LocalStack SQS"]
    AUTH --> MYSQLA["mortgage_auth"]
    BORR --> MYSQLB["mortgage_borrower"]
    SQS --> PRICING["pricing-service"]
    PRICING --> MYSQLP["mortgage_pricing"]
    PRICING --> REDIS
    PRICING --> SQS
    SQS --> LEAD["lead-service"]
    LEAD --> MYSQLL["mortgage_lead"]
    LEAD --> REDIS
    API --> SQS
    SQS --> NOTIFY
    NOTIFY --> REDIS
    NOTIFY --> E
```

## Design principles

- **Service-per-domain.** Each service owns one business capability and one database schema. No service reads another service's tables — cross-service data flows over HTTP (sync) or, in the target design, a message broker (async).
- **Async-capable by design for expensive work.** The architecture is built to decouple pricing, lead creation, and notifications from the request path so a quote request can return immediately while work completes in the background. *(Designed; the async layer is not yet wired — see Implementation status.)*
- **Read models for fast reads.** `api` maintains a quote read model and `notification-service` keeps a Redis snapshot, so status checks never block on downstream workers.
- **Broker-agnostic transport.** Async messaging is built against a single transport interface with swappable adapters (NoOp / RabbitMQ / SQS), so the broker is a config choice rather than a code dependency ([ADR-0007](./adr/phase-1-foundation/0007-messaging-transport-abstraction.md)). Message contracts are designed to be versioned (`schemaVersion`) and idempotent (`messageId`), with dead-letter queues for poison messages.
- **Pluggable auth.** User authentication runs in either a self-issued JWT mode or an OIDC (Keycloak) mode without code changes.

## Request flow (quote lifecycle) — target design

> Steps 3–7 depend on the async messaging layer, which is **scaffolded but not yet wired** (default transport `noop`). They describe the intended flow once a broker transport is enabled.

1. An anonymous user requests a public mortgage quote. *(implemented)*
2. `api` deduplicates repeated requests per session and persists quote state in `mortgage_quote_workflow`. *(implemented)*
3. `api` publishes a pricing job and returns a `quoteId` immediately. *(designed)*
4. `pricing-service` prices the scenario against its own persisted product catalog and publishes a result event. *(designed)*
5. `api` consumes the pricing result and updates the quote read model. *(designed)*
6. If an authenticated user refines the quote, `lead-service` creates a lead asynchronously and publishes a lead result event. *(designed)*
7. `notification-service` stores the latest snapshot and pushes updates to the frontend over SSE. *(designed)*
8. `admin-web` reads aggregated metrics through `api`. *(implemented)*

## Service responsibilities

| Service | Owns | Store |
|---|---|---|
| `api` | Public quote creation/retrieval, refinement orchestration, calculators, session dedupe, metrics aggregation, notification publishing, service-JWT issuance | `mortgage_quote_workflow` |
| `auth-service` | Registration, login, access-token issuance, internal JWT mode | `mortgage_auth` |
| `borrower-service` | Borrower creation/retrieval, existence checks, borrower metrics | `mortgage_borrower` |
| `pricing-service` | Pricing job consumption, pricing catalog (products, rate sheets, adjustment rules), Redis pricing cache, lead-job publishing | `mortgage_pricing` |
| `lead-service` | Lead creation from refined quotes, lead metrics | `mortgage_lead` |
| `notification-service` | Quote snapshot cache, snapshot fetch endpoint, quote SSE stream | Redis only |

## Database ownership

Each service owns its schema; there is no cross-service table access.

- `auth-service` → `mortgage_auth`
- `borrower-service` → `mortgage_borrower`
- `api` → `mortgage_quote_workflow`
- `pricing-service` → `mortgage_pricing`
- `lead-service` → `mortgage_lead`
- `notification-service` → Redis only

## Messaging (target design — not yet wired)

Async messaging is built against a **broker-agnostic transport abstraction** ([ADR-0007](./adr/phase-1-foundation/0007-messaging-transport-abstraction.md)): a `MessageTransport` interface selected by `app.transport.type`, with adapters for:

- `noop` — **current default**; no messages are published (async layer inert)
- `rabbitmq` — intended broker for the self-hosted Phase-2 deployment ([ADR-0050](./adr/phase-2-pricing-engine/0050-message-broker-selection.md))
- `sqs` — intended managed broker for the AWS Phase-3 deployment

**Current state:** the interface and adapters are scaffolded, but the async pipeline is not functional end to end. To exercise it locally you would select a transport (LocalStack SQS or RabbitMQ) and enable the consumers — this is pending work.

**Designed queue topology** (SQS naming, mirrored by RabbitMQ exchanges):

| Work queue | Result queue | Dead-letter queue |
|---|---|---|
| `quote-pricing-requests` | `quote-pricing-results` | `quote-pricing-results-dlq` |
| `quote-lead-requests` | `quote-lead-results` | `quote-lead-results-dlq` |
| `quote-notification-events` | — | `quote-notification-events-dlq` |

**Designed contract hardening.** Each payload is intended to carry `schemaVersion` and `messageId`; consumers reject unsupported schema versions, dedupe on `messageId`, and route poison messages to the matching DLQ. The LocalStack-SQS DLQ tooling is described in the [operations runbook](./ops-runbook.md).

## Security model

**User authentication** — two interchangeable modes for protected endpoints in `api` and `borrower-service`:

- `internal`: `auth-service` issues HMAC-signed JWTs.
- `oidc`: local Keycloak issues OIDC access tokens validated against a configured JWK set URI.

**Service-to-service authentication** — internal calls use service JWTs validated on issuer, audience, scope, and type. Examples: `api → borrower-service`, `api → pricing-service` (job token), `pricing-service → lead-service` (job token), `api → notification-service` (job token).

## Session tracking & deduplication

The borrower frontend generates and persists a session ID, sent as `X-Session-Id`. `api` uses Redis to track active sessions, detect in-flight duplicate quote requests, return the existing `quoteId` when a duplicate is already `QUEUED`/`PROCESSING`, and maintain fast status snapshots for the async flow.

## Technology choices

- **Java 17 / Spring Boot** for all backend services
- **MySQL** for per-service relational persistence
- **Redis** for session state, dedupe, caching, metrics counters, and notification snapshots
- **Broker-agnostic transport** (NoOp / RabbitMQ / SQS adapters) for the designed async layer — RabbitMQ for Phase 2, SQS for Phase 3; LocalStack emulates SQS locally *(scaffolded, not yet wired)*
- **Keycloak** for the optional OIDC profile
- **React + Vite** for the borrower and admin apps
- **Nginx** as the public edge reverse proxy (HTTP `8088`, TLS `8443`)
- **Docker Compose** for local orchestration; **Jenkins** for CI

## Design history

The system was built incrementally and every significant decision is captured as an Architecture Decision Record under [`docs/adr`](./adr), organized by phase (foundation → pricing engine → scale & operations).

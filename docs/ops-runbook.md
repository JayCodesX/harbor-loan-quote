# Operations Runbook

Operator-facing procedures for running, verifying, and recovering the Harbor Loan Quotes stack. For system design, see [architecture.md](./architecture.md).

> **Status note.** The asynchronous messaging layer is currently **scaffolded but not wired** — the default transport is `noop`, so the async pricing/lead/notification pipeline does not run yet. The "async pipeline" and "dead-letter queue" sections below describe the **intended** operator workflow for the LocalStack-SQS path once a transport is enabled. The synchronous flows (quotes, calculators, auth, borrower APIs, metrics, admin) run today.

## Run modes

**Lightweight local stack** — borrower app, auth, borrower APIs, MySQL, Redis, and edge routing:

```bash
docker compose up -d --build
```

**Full integration stack** — adds async pricing, lead generation, notifications, admin-web, and LocalStack queues:

```bash
docker compose --env-file .env.integration --profile integration up -d --build
# or:
make up
```

Stop everything:

```bash
docker compose --env-file .env.integration --profile integration down
# or:
make down
```

The [`.env.integration`](../.env.integration) file forces the async quote flow on so `api` publishes pricing jobs consistently for demos and CI.

## Local URLs

- Borrower app: `http://localhost:8088`
- Borrower app (TLS): `https://localhost:8443`
- Admin app (TLS): `https://localhost:8443/admin/`
- Health: `https://localhost:8443/actuator/health`

If local TLS certs are missing:

```bash
./scripts/generate-local-certs.sh
```

## Health checks

After the stack is up, confirm services are live:

```bash
curl -k https://localhost:8443/actuator/health
```

Then run the smoke check (verifies borrower quote flow, auth redirect, admin login, admin workspace access):

```bash
./scripts/local-smoke.sh
# or:
make smoke
```

## Verifying the async pipeline *(applies once a transport is enabled)*

> Prerequisite: a real transport (`rabbitmq` or `sqs`) must be configured and consumers enabled. With the default `noop` transport this section does not apply.

A healthy quote flows `QUEUED → PROCESSING → PRICED`. If a quote is stuck in `QUEUED`/`PROCESSING`:

1. Confirm `pricing-service`, `lead-service`, and `notification-service` are running:
   ```bash
   docker compose --profile integration ps
   ```
2. Check consumer logs for schema-version rejections or processing errors:
   ```bash
   docker compose logs --tail=100 pricing-service
   docker compose logs --tail=100 notification-service
   ```
3. Inspect the relevant dead-letter queue (see below).

## Dead-letter queues (DLQ) *(LocalStack-SQS path, once enabled)*

> The `dlq-*.sh` scripts operate against LocalStack SQS. They apply to the SQS transport path once the async layer is wired; they do not cover the RabbitMQ path.

Poison messages — those that fail processing or carry an unsupported schema version — are routed to a DLQ.

**Inspect a DLQ:**

```bash
./scripts/dlq-inspect.sh quote-pricing-results-dlq
./scripts/dlq-inspect.sh quote-lead-results-dlq
./scripts/dlq-inspect.sh quote-notification-events-dlq
```

**Replay a DLQ** back onto its source queue after the underlying cause is fixed:

```bash
./scripts/dlq-replay.sh quote-pricing-results-dlq      quote-pricing-results
./scripts/dlq-replay.sh quote-lead-results-dlq         quote-lead-results
./scripts/dlq-replay.sh quote-notification-events-dlq  quote-notification-events
```

### Replay checklist
1. **Inspect first** — read the DLQ messages and confirm the failure cause is understood.
2. **Fix the root cause** — deploy the consumer fix or correct the bad data before replaying.
3. **Replay** — messages return to the source queue; consumers dedupe on `messageId`, so a duplicate delivery is safe.
4. **Re-inspect** — confirm the DLQ is drained and the source queue processed cleanly.

## Tests

```bash
# Backend (all Java services)
mvn -Dmaven.repo.local=.m2 test

# Frontend unit tests
cd web && npm run test:ci
cd admin-web && npm run test:ci

# End-to-end (Playwright, against the integration stack)
make e2e
```

## CI

[`Jenkinsfile`](../Jenkinsfile) runs JUnit suites for all Java services, frontend unit tests and production builds for `web` and `admin-web`, and the Playwright E2E suite on the integration-profile Docker stack.

## Common issues

| Symptom | Likely cause | Action |
|---|---|---|
| Quote stuck in `QUEUED`/`PROCESSING` | Worker down or message in DLQ | Check worker logs, inspect the matching DLQ, replay after fix |
| TLS errors on `https://localhost:8443` | Missing local certs | Run `./scripts/generate-local-certs.sh` |
| Admin endpoints return 403 | Missing `ADMIN` role token | Authenticate as an admin user; confirm token role |
| OIDC login fails | Keycloak not started or env mismatch | Start the `oidc` profile; verify issuer/audience/JWK URI env vars |
| Async flow never triggers | Integration profile/env not used | Start with `--env-file .env.integration --profile integration` |

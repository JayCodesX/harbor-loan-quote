# Operations Runbook

Operator-facing procedures for running, verifying, and recovering the Harbor Loan Quotes stack. For system design, see [architecture.md](./architecture.md).

> **Status note.** The asynchronous messaging layer runs locally over RabbitMQ (the default transport in Docker Compose). harbor-api publishes quote notification snapshots; pricing-service publishes rate-sheet-activated events; notification-service consumes both. SQS/LocalStack is the optional Phase-3 adapter path, available behind the `integration` profile.

## Run modes

**Default stack — full 3-service RabbitMQ stack:**

```bash
docker compose up -d --build
```

Brings up: `rabbitmq`, `mysql`, `redis`, `api`, `pricing-service`, `notification-service`, `admin-web`, `web`, `edge`.

**Optional LocalStack/SQS profile (Phase-3 path):**

```bash
docker compose --env-file .env.integration --profile integration up -d --build
# or:
make up
```

Stop everything:

```bash
docker compose down
# or (integration profile):
docker compose --env-file .env.integration --profile integration down
# or:
make down
```

The [`.env.integration`](../.env.integration) file switches the transport to SQS so `api` publishes pricing jobs to LocalStack queues for demos and CI.

## Local URLs

- Borrower app: `http://localhost:8088`
- Borrower app (TLS): `https://localhost:8443`
- Admin app (TLS): `https://localhost:8443/admin/`
- Health: `https://localhost:8443/actuator/health`
- RabbitMQ management UI: `http://localhost:15672` (guest/guest)

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

## Verifying the async pipeline

A healthy quote notification flows through: harbor-api publishes → RabbitMQ `quote.notification.events` exchange → queue `quote.notification.snapshot` → notification-service → SSE to frontend.

To verify locally:

1. Open the RabbitMQ management UI at **http://localhost:15672** (guest/guest) and confirm queues `quote.notification.snapshot` and `rate-sheet.activated` exist and have consumers attached.
2. Submit a quote request via the borrower app or API. Watch the `quote.notification.snapshot` queue in the management UI for message throughput.
3. Check consumer logs for schema-version rejections or processing errors:
   ```bash
   docker compose logs --tail=100 notification-service
   docker compose logs --tail=100 pricing-service
   ```
4. If a quote is stuck without an SSE update, inspect notification-service logs for consumer errors and confirm `APP_RABBITMQ_CONSUMER_ENABLED=true` is set.

## Dead-letter queues (DLQ)

### RabbitMQ (default local path)
RabbitMQ dead-letter exchange (DLX) configuration is a follow-up. For now, use the management UI at `http://localhost:15672` to inspect unroutable or nacked messages, and service logs to diagnose failures.

### SQS / LocalStack (Phase-3 path)

> The `dlq-*.sh` scripts operate against LocalStack SQS. They apply to the `integration` profile only.

Poison messages — those that fail processing or carry an unsupported schema version — are routed to a DLQ.

**Inspect a DLQ:**

```bash
./scripts/dlq-inspect.sh quote-pricing-results-dlq
./scripts/dlq-inspect.sh quote-notification-events-dlq
```

**Replay a DLQ** back onto its source queue after the underlying cause is fixed:

```bash
./scripts/dlq-replay.sh quote-pricing-results-dlq      quote-pricing-results
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
| No SSE update after quote | RabbitMQ consumer down or misconfigured | Check `docker compose logs notification-service`; confirm `APP_RABBITMQ_CONSUMER_ENABLED=true`; inspect queues in management UI at http://localhost:15672 |
| Quote stuck in `QUEUED`/`PROCESSING` | pricing-service or notification-service down | Check service logs; confirm rabbitmq is healthy (`docker compose ps`) |
| RabbitMQ queues missing after restart | Queues are durable but exchange/queue declaration runs at startup | Restart notification-service so it re-declares the topology |
| TLS errors on `https://localhost:8443` | Missing local certs | Run `./scripts/generate-local-certs.sh` |
| Admin endpoints return 403 | Missing `ADMIN` role token | Authenticate as an admin user; confirm token role |
| OIDC login fails | Keycloak not started or env mismatch | Start the `oidc` profile; verify issuer/audience/JWK URI env vars |

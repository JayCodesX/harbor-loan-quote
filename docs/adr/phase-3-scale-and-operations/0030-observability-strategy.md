# ADR 0030: Observability — OpenTelemetry + Grafana vs. AWS X-Ray vs. Datadog

## Status
Accepted

Originally drafted in Phase 1 and deferred to production readiness. Activated in
2026-07 because the AI-agent (MCP) and event-stream (Kafka) work in ADR-0051 and
ADR-0052 introduces distributed call paths that are only debuggable with
end-to-end tracing, making observability a now-concern rather than a later one.

## Date
2026-04-02 (drafted) · 2026-07-09 (activated)

## Phase
3 — Scale and Operations

## Context
Harbor is a multi-service system (harbor-api, pricing-service, notification-service)
with asynchronous flows over RabbitMQ and, per ADR-0052, a planned Kafka event
stream. The agent-facing MCP layer (ADR-0051) adds a new call path:
agent → MCP server → pricing service → cache/DB. Two flows in particular are
impossible to reason about without distributed tracing:

1. An agent tool call fanning out through MCP → pricing → Redis/MySQL.
2. A rate-sheet activation fanning out through Kafka → cache invalidation, SSE
   push, and audit consumers.

We need traces (one span tree per request/event across services), metrics
(latency, error rate, cache hit ratio, consumer lag), and logs, correlated by a
common trace/span id. The decision is which observability stack to standardize
on, given a self-hosted / cost-sensitive deployment target and a preference for
avoiding vendor lock-in.

## Decision
Standardize on **OpenTelemetry (OTel) for instrumentation + the Grafana stack
for storage and visualization** (Tempo for traces, Loki for logs, Prometheus/Mimir
for metrics).

- **Instrumentation:** Spring Boot 3.x Actuator + Micrometer Tracing bridged to
  OpenTelemetry (`micrometer-tracing-bridge-otel`) + the OTLP exporter. Services
  emit OTLP; nothing in application code is tied to a vendor.
- **Local / demo:** the single `grafana/otel-lgtm` container (Grafana + Tempo +
  Loki + Prometheus, OTLP-ready) — one command to a full observability stack.
- **Production:** the same OTLP output points at either self-hosted Grafana or
  Grafana Cloud's free tier, unchanged, because OTel decouples emission from
  backend.

## Alternatives Considered
1. **AWS X-Ray.** Native to AWS and low-effort there, but couples tracing to a
   single cloud, has weaker metrics/logs correlation than the Grafana stack, and
   does not fit a provider-neutral / local-first development story. Rejected:
   lock-in and a partial picture (traces without unified metrics/logs).
2. **Datadog (or New Relic).** Excellent, fully-managed, best-in-class UX. But it
   is a paid SaaS with usage-based cost that scales unfavorably for a
   cost-sensitive, self-hosted deployment, and it is a proprietary agent.
   Rejected on cost and vendor neutrality; re-evaluate if this were a funded
   production system where operator time outweighs license cost.
3. **Self-hosted ELK (Elasticsearch/Logstash/Kibana) + Jaeger.** Capable, but
   heavier to operate (Elasticsearch alone is memory-hungry) and splits tracing
   (Jaeger) from logs (ELK) with less seamless correlation than the unified
   Grafana stack. Rejected: operational weight on a constrained node.
4. **Metrics-only (Prometheus + Actuator), no tracing.** Cheapest, but leaves the
   distributed agent and event-fan-out paths opaque — exactly the flows this
   system most needs to see. Rejected: insufficient for the new call graphs.

## Rationale
- **Vendor neutrality via OTel.** Application code emits a standard; the backend
  is swappable (local otel-lgtm → Grafana Cloud → self-hosted) with config only.
  No re-instrumentation if the backend changes.
- **Cost.** OTel and the Grafana OSS stack are free; Grafana Cloud has a usable
  free tier. Fits the deployment budget without a per-host SaaS bill.
- **Unified traces + metrics + logs** correlated by trace id in one UI (Grafana),
  which is what makes the MCP and Kafka fan-out flows actually debuggable.
- **Low local-dev friction.** `grafana/otel-lgtm` gives the whole stack in one
  container, so the observability experience is identical from laptop to prod.
- **Spring-native.** Actuator + Micrometer Tracing is first-class in Spring Boot
  3.x, so instrumentation is largely auto-configured.

## Consequences
- A small instrumentation dependency and OTLP configuration are added per service;
  spans/metrics carry a minor runtime overhead (acceptable, sampled if needed).
- An observability backend must run (one container locally; a node or Grafana
  Cloud in production). This is new operational surface, justified by the
  distributed call paths introduced in ADR-0051 and ADR-0052.
- Trace context must propagate across the async boundaries (RabbitMQ, Kafka) so a
  producer span links to its consumer spans; Micrometer/OTel instrumentation for
  the messaging clients handles most of this, but it must be verified end to end.
- This decision enables the concrete observability deliverables in the AI-integration
  work: a single trace spanning agent → MCP → pricing → DB, and another spanning
  rate-sheet activation → Kafka → SSE push.

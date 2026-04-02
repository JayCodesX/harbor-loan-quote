# ADR 0042: Metrics Collection Strategy — Micrometer + Prometheus vs. CloudWatch vs. Datadog

## Status
Archived — valid, deferred to production readiness phase

Metrics collection strategy will be revisited during Phase 3.

## Date
Fill in when you write this

## Phase
3 — Scale and Operations

---

## Context

WRITE THIS YOURSELF

Prompts to answer:
- What metrics does this system need to expose?
  (JVM heap/GC metrics, Spring Boot actuator metrics (http.server.requests latency/count/errors),
   custom business metrics (quotes_created_total, pricing_requests_total, leads_captured_total,
   circuit_breaker_state for pricing-service), RabbitMQ/SQS queue depth, Redis cache hit rate.)
- What is Micrometer?
  (A vendor-neutral metrics facade for JVM applications. Spring Boot Auto-configures Micrometer.
   Metrics are defined once in application code using Micrometer's Counter/Timer/Gauge API.
   The backend (Prometheus, CloudWatch, Datadog) is swapped by changing the MeterRegistry dependency.
   spring-boot-actuator exposes a /actuator/metrics endpoint and a /actuator/prometheus endpoint.)
- What is Prometheus?
  (Open-source pull-based metrics system. Prometheus scrapes /actuator/prometheus endpoints
   on a configured interval. Stores time-series data. PromQL for ad-hoc queries.
   Grafana is the standard visualization layer for Prometheus data.)
- What is CloudWatch Metrics?
  (AWS-native time-series metrics. ECS auto-emits container CPU/memory metrics.
   Applications can push custom metrics via the CloudWatch SDK (PutMetricData).
   Micrometer has a CloudWatch MeterRegistry that auto-pushes metrics.
   CloudWatch dashboards and alarms are native to AWS.)
- What is Datadog?
  (SaaS observability platform: metrics, traces, logs, APM in one dashboard.
   Micrometer Datadog registry pushes metrics to Datadog's ingest endpoint.
   $15-23/host/month for infrastructure monitoring. Full APM is more expensive.)
- Why does Prometheus + Grafana complement the OpenTelemetry decision (ADR-0030)?
  (ADR-0030 uses Grafana Tempo or Zipkin for tracing. If Grafana is already deployed for
   Prometheus metrics dashboards, adding Tempo for traces and optionally Loki for logs
   achieves a unified observability stack (the "Grafana LGTM stack") without additional tooling.)


---

## Decision

WRITE THIS YOURSELF

What is the decision? Be concrete — a sentence or two that could stand alone.
Describe the *what*, not the *why* (why belongs in Rationale).


---

## Alternatives Considered

### Alternative: CloudWatch Metrics (Micrometer CloudWatch registry)
**Strengths:** Zero additional infrastructure, native AWS integration, auto-scaling triggers on CloudWatch metrics, ECS container metrics included
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- CloudWatch custom metrics cost $0.30/metric/month. At 20+ custom business metrics across
  3 services, that is $6+ per month for metrics alone — acceptable, but Prometheus is free.
- CloudWatch Metrics requires push-based emission from the application (PutMetricData).
  Prometheus pull model is simpler: no SDK calls in application code, just an actuator endpoint.
- Grafana is the standard visualization layer for both CloudWatch and Prometheus —
  using Prometheus keeps all observability in a consistent tool.
- CloudWatch dashboards are adequate but less flexible than Grafana for custom visualizations.
- Keep CloudWatch for ECS infrastructure metrics (CPU/memory); use Prometheus for application metrics.
  These are complementary, not competing.

### Alternative: Datadog
**Strengths:** Unified metrics + traces + logs + APM in one SaaS platform, minimal operational overhead, excellent dashboards out of the box
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- Datadog infrastructure monitoring is $15-23/host/month × 3+ ECS task families = $45-70/month
  just for metrics. APM (distributed traces) adds cost per host.
- Datadog is the right choice for teams that want to minimize operational overhead and have budget.
  For a portfolio project or early-stage startup, the cost is not justified.
- Micrometer's vendor-neutral facade means Datadog can be added later by swapping the registry
  dependency — no application code changes required. The decision is reversible.
- Prometheus + Grafana achieves 90% of the observability benefit at near-zero cost.

---

## Rationale

### Prometheus + Grafana Is the Standard for Spring Boot Observability

WRITE THIS YOURSELF
Spring Boot's Micrometer integration is designed around Prometheus:
- `spring-boot-actuator` auto-configures the Prometheus registry when `micrometer-registry-prometheus` is on the classpath.
- `/actuator/prometheus` is the standard scrape endpoint — Prometheus configuration is a single job:
  scrape_configs:
    - job_name: 'harbor-api'
      static_configs:
        - targets: ['harbor-api:8080']
- Grafana's Spring Boot community dashboard (dashboard ID 19004) provides JVM, HTTP, and
  system metrics visualization with zero dashboard configuration.
- Custom business metrics (quotes_created_total, leads_captured_total) are defined with:
  Counter.builder("quotes.created").register(meterRegistry).increment()
  — one line per metric, visible immediately in Prometheus and Grafana.
Prometheus + Grafana is the standard open-source observability stack for Spring Boot applications.
It is what engineers expect to see in a production-grade portfolio project.

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

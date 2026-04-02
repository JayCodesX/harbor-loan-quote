# ADR 0030: Observability — OpenTelemetry + Grafana vs. AWS X-Ray vs. Datadog

## Status
Archived — valid, deferred to production readiness phase

Observability strategy remains applicable. Will be revisited
during Phase 3.

## Date
Fill in when you write this

## Phase
3 — Scale and Operations

---

## Context

WRITE THIS YOURSELF

Prompts to answer:
- What is observability and what are its three pillars?
  (Logs: structured text records of events. Metrics: numeric measurements over time.
   Traces: the path of a single request across multiple services.)
- What is the debugging pain without distributed tracing in this system?
  (A quote request touches: Nginx → harbor-api → pricing-service → harbor-api → lead routing.
   Without a shared trace ID propagated across all hops, debugging requires grepping
   logs in 3-4 separate services and manually correlating by timestamp. Multi-hour per incident.)
- What is OpenTelemetry?
  (A vendor-neutral observability standard. SDK instruments your application to emit traces,
   metrics, and logs. OTLP protocol exports to any compatible backend.
   Spring Boot has first-class OTel support via Micrometer Tracing.)
- What is the difference between VPS (Phase 1) and AWS (Phase 3) observability needs?
  (Phase 1: basic structured logging + Uptime Robot for availability.
   Phase 3: distributed tracing across ECS tasks, metrics dashboards, log aggregation.)


---

## Decision

WRITE THIS YOURSELF

What is the decision? Be concrete — a sentence or two that could stand alone.
Describe the *what*, not the *why* (why belongs in Rationale).


---

## Alternatives Considered

### Alternative: AWS X-Ray
**Strengths:** Native ECS integration, auto-instrumented via X-Ray daemon sidecar,
no separate backend to manage, integrates with CloudWatch Service Map
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- X-Ray SDK is AWS-proprietary. Instrumenting with X-Ray creates vendor lock-in at the
  application code level. OpenTelemetry is vendor-neutral — switching backends is a config change.
- X-Ray has limited trace sampling and query capabilities compared to Grafana Tempo.
- The OpenTelemetry approach works on both VPS (Phase 1) and AWS (Phase 3)
  with the same instrumentation code — X-Ray only works on AWS.
- If the team ever runs services locally for debugging, OpenTelemetry exports to a local Jaeger
  instance. X-Ray does not have a local equivalent.

### Alternative: Datadog
**Strengths:** Best-in-class APM, unified platform (logs + metrics + traces + dashboards),
excellent Spring Boot auto-instrumentation, lowest-configuration setup
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- Datadog cost at Phase 3 scale: ~$23/host/month for infrastructure + additional
  per-GB for APM and log management. For 7 services on ECS, this accumulates quickly.
- Grafana Cloud provides equivalent capabilities at significantly lower cost
  (free tier covers small deployments; paid tier is significantly cheaper than Datadog).
- OpenTelemetry + Grafana is now a well-established, enterprise-grade stack.
  For a portfolio project, demonstrating OpenTelemetry shows vendor-neutral
  observability architecture knowledge — more impressive than "I used Datadog."

### Alternative: CloudWatch Logs and Metrics only (no distributed tracing)
**Strengths:** Native AWS, zero additional infrastructure, free within AWS free tier limits
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- CloudWatch provides logs and metrics but no distributed tracing.
  Without traces, debugging multi-service quote flow failures requires manual
  log correlation across multiple CloudWatch log groups.
- The first multi-service production incident without tracing will take hours to debug.
  Adding tracing after the fact is harder than implementing it upfront.
- OpenTelemetry's trace propagation via HTTP headers is automatic once the SDK is added —
  the instrumentation cost is low, the benefit is high.

---

## Rationale

### OpenTelemetry Provides the Complete Picture Across Both Phases

WRITE THIS YOURSELF
The same OTel SDK code works in Phase 1 (exporting to a local Jaeger for dev debugging)
and Phase 3 (exporting to Grafana Cloud for production observability).
No instrumentation code changes between phases.
The correlationId → traceId linkage in message payloads means traces follow the
request through queue boundaries — a complete trace from browser request to
notification-service SSE push, visible in one timeline.

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

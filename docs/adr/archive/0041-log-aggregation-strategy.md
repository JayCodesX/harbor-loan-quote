# ADR 0041: Log Aggregation Strategy — CloudWatch Logs vs. Grafana Loki vs. ELK Stack

## Status
Archived — valid, deferred to production readiness phase

Log aggregation strategy will be revisited during Phase 3.

## Date
Fill in when you write this

## Phase
3 — Scale and Operations

---

## Context

WRITE THIS YOURSELF

Prompts to answer:
- What is the current logging setup in Phase 1?
  (Each service logs to stdout/stderr. Docker Compose captures logs locally.
   Developers run docker compose logs -f service-name to tail logs.
   No aggregation — logs on the VPS only, not searchable across services.)
- Why does Phase 3 require centralized log aggregation?
  (With ECS Fargate, container stdout is ephemeral. When a task restarts or scales down,
   its logs are gone unless forwarded to a persistent store.
   Debugging requires correlating logs across harbor-api, pricing-service, and notification-service
   for a single request — correlation ID / trace ID (ADR-0030) ties them together in a log query.)
- What is CloudWatch Logs?
  (AWS-native log aggregation. ECS tasks use the awslogs log driver to ship logs to CloudWatch.
   Zero configuration beyond an IAM permission and a log group name.
   Log Insights provides SQL-like query syntax over structured log data.)
- What is Grafana Loki?
  (Open-source log aggregation system from Grafana Labs. Stores log labels (like Prometheus metrics)
   and compresses log streams. Queries via LogQL. Integrates with Grafana dashboards that already
   display metrics from Prometheus (ADR-0042). All observability in one pane.)
- What is ELK (Elasticsearch + Logstash + Kibana)?
  (Full-text search over logs using inverted index. Extremely powerful for ad-hoc log querying.
   Also the most expensive and operationally complex option — significant infrastructure overhead.)
- What is the correlation between this ADR and ADR-0030 (OpenTelemetry)?
  (ADR-0030 establishes traceId/spanId in log MDC via micrometer-tracing. Centralized logs
   must be searchable by traceId to enable "show me all logs for request abc123 across all services."
   Both CloudWatch Logs Insights and Loki/LogQL support filtering by structured JSON fields.)


---

## Decision

WRITE THIS YOURSELF

What is the decision? Be concrete — a sentence or two that could stand alone.
Describe the *what*, not the *why* (why belongs in Rationale).


---

## Alternatives Considered

### Alternative: Grafana Loki
**Strengths:** LogQL integrates with Grafana dashboards, single pane for metrics + logs, lower storage cost than CloudWatch at high volume
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- Loki requires deploying and operating a Loki cluster, or using Grafana Cloud (managed SaaS).
- In Phase 3 where ECS and CloudWatch are already part of the infrastructure, adding a separate
  log storage system increases operational surface area.
- CloudWatch Logs is zero-configuration from ECS: add the awslogs driver to the task definition,
  grant the task execution role logs:CreateLogGroup/PutLogEvents, and logs appear.
- Preferred migration path: if ADR-0042 adopts Grafana Cloud or self-hosted Grafana for metrics,
  Loki can be added to centralize logs and metrics in the same Grafana instance at Phase 3+.

### Alternative: ELK Stack (Elasticsearch, Logstash, Kibana)
**Strengths:** Full-text search over logs, rich Kibana visualization, industry standard for log management
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- Self-hosted Elasticsearch requires significant infrastructure: 3-node cluster minimum for HA,
  16GB+ RAM per node, index lifecycle management, snapshot backups.
  AWS OpenSearch (managed Elasticsearch) starts at ~$100-200/month for a production cluster.
- The primary log query pattern for this system is: "show me all logs for traceId X" and
  "show ERROR logs in the last hour for harbor-api." These are structured field filters,
  not full-text search over log bodies. CloudWatch Log Insights handles this adequately.
- ELK is justified when the team needs full-text search over arbitrary log content at petabyte scale.
  That is not this system's requirement.

---

## Rationale

### CloudWatch Logs Is the Zero-Friction Choice for ECS Phase 3

WRITE THIS YOURSELF
The awslogs log driver is native to ECS. Task definition change:
  "logConfiguration": {
    "logDriver": "awslogs",
    "options": {
      "awslogs-group": "/ecs/harbor-api",
      "awslogs-region": "us-east-1",
      "awslogs-stream-prefix": "ecs"
    }
  }
That is the entire configuration. IAM permission grants the task execution role PutLogEvents.
Log Insights query for a trace:
  fields @timestamp, @message
  | filter traceId = "abc123def456"
  | sort @timestamp asc
This is sufficient for Phase 3 operational debugging without additional infrastructure.
Cost: ~$0.50/GB ingested + $0.03/GB stored/month — at typical log volumes (few GB/day), this is negligible.

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

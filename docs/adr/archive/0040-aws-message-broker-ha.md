# ADR 0040: AWS Message Broker Selection — SQS vs. Amazon MQ vs. MSK Kafka

## Status
Archived — valid, deferred to production readiness phase

Message broker HA is a production concern. Will be revisited
during AWS migration. See ADR-0028.

## Date
Fill in when you write this

## Phase
3 — Scale and Operations

---

## Context

WRITE THIS YOURSELF

Prompts to answer:
- What messages does this system currently route through RabbitMQ?
  (QuoteRequested → pricing-service for pricing calculation.
   PricingCompleted → harbor-api to update quote status and trigger lead flow.
   LeadCaptured → notification-service to send confirmation email/SSE.
   RateSheetActivated → notification-service to evaluate and deliver rate alerts.
   These are point-to-point and pub/sub patterns with low-to-moderate throughput.)
- What is the transport abstraction layer (ADR-0007)?
  (The application publishes and consumes through a MessagePublisher interface and
   a MessageConsumer abstraction. Switching brokers requires swapping the infrastructure
   bean, not rewriting business logic. This was established to make RabbitMQ → SQS migration
   straightforward in Phase 3.)
- What is Amazon SQS?
  (AWS-managed queue service. Virtually unlimited throughput, no infrastructure to manage.
   At-least-once delivery, configurable visibility timeout, built-in DLQ support.
   No pub/sub fanout natively — SNS is required for fan-out patterns.)
- What is Amazon MQ?
  (AWS-managed ActiveMQ or RabbitMQ. Managed infrastructure for organizations that need
   AMQP protocol compatibility or existing RabbitMQ/ActiveMQ tooling.
   Higher cost than SQS; more operational overhead than SQS.)
- What is MSK (Managed Streaming for Kafka)?
  (AWS-managed Apache Kafka. High-throughput event streaming with persistent log.
   Replay capabilities, consumer group offset management.
   Justified for high-volume event streaming — significantly more complex and expensive than SQS.)
- What is the message throughput of this system at Phase 3?
  (Quote creation rate drives message volume. Even at 10,000 quotes/day,
   that is ~7 messages/minute — nowhere near the justification threshold for Kafka.
   SQS handles millions of messages/day trivially.)


---

## Decision

WRITE THIS YOURSELF

What is the decision? Be concrete — a sentence or two that could stand alone.
Describe the *what*, not the *why* (why belongs in Rationale).


---

## Alternatives Considered

### Alternative: Amazon MQ (managed RabbitMQ)
**Strengths:** Drop-in replacement for existing RabbitMQ; zero application code changes; AMQP protocol compatibility
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- Amazon MQ requires provisioned broker instances (mq.m5.large ≈ $150/mo for HA pair).
  SQS is pay-per-request with no provisioning cost.
- Amazon MQ is operationally heavier than SQS — broker patching, configuration, monitoring.
- The transport abstraction layer (ADR-0007) was specifically built to avoid protocol lock-in.
  Migrating to SQS is straightforward; keeping RabbitMQ semantics via Amazon MQ is unnecessary.
- Amazon MQ is the right choice for lift-and-shift migrations where AMQP compatibility is required.
  This system was designed to swap brokers at Phase 3 — SQS is the correct target.

### Alternative: MSK (Managed Streaming for Kafka)
**Strengths:** Persistent log with replay, high throughput (millions/second), event sourcing capabilities, consumer group flexibility
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- MSK minimum cluster cost is ~$200-400/month (3-broker minimum requirement).
- The message volume for this system at Phase 3 is measured in messages per minute, not
  per second. Kafka's throughput advantage is irrelevant at this scale.
- Kafka adds significant operational complexity: topic partitioning, offset management,
  consumer group rebalancing, schema registry for event contracts.
- Kafka is the right choice when event replay (reprocess all quotes from last week) is a
  product requirement, or when throughput exceeds SQS comfort zone (>10K messages/second).
  Neither applies here.
- Revisit if the pricing engine adds audit replay or event sourcing requirements.

---

## Rationale

### SQS Matches the Operational Maturity and Scale of Phase 3

WRITE THIS YOURSELF
SQS is the default choice for inter-service messaging on AWS for well-understood reasons:
- Fully managed: no broker provisioning, no patching, no capacity planning
- Pay per request: ~$0.40 per million messages — at 10,000 quotes/day this is negligible
- Built-in DLQ: failed messages automatically routed to DLQ after N retries (matching RabbitMQ DLQ behavior from ADR-0007)
- SNS fan-out: RateSheetActivated needs to notify both notification-service and any future consumers.
  SNS topic + SQS subscriptions achieves exactly this without a separate exchange configuration.
- Visibility timeout replaces RabbitMQ's ack/nack mechanism semantically.
The transport abstraction layer means the migration is an infrastructure change, not a business logic change.
Swap the MessagePublisher and MessageConsumer beans, update the integration tests, deploy.

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

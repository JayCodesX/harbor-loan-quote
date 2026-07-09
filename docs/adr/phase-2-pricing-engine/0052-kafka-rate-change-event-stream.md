# ADR 0052: Introduce Kafka (Redpanda) for the Rate-Change Event Stream, Complementing RabbitMQ

## Status
Proposed

## Date
2026-07-09

## Phase
2 — Pricing Engine

## Context
ADR-0050 selected RabbitMQ (Phase 2) with an SQS adapter (Phase 3) as Harbor's
message broker. That decision remains correct for the workloads it targets:
point-to-point task dispatch (quote-job processing) and notification delivery —
work-queue patterns with per-message acknowledgement and routing.

A new requirement changes the picture for one specific flow. When a rate sheet
is activated (`RateSheetActivatedPublisher`), several **independent** consumers
need the same event:

1. **Cache invalidation** — evict stale pricing decisions in Redis.
2. **Real-time push** — notify connected clients and AI agents that pricing
   changed, via the SSE channel (ADR-0049, ADR-0051).
3. **Audit / analytics** — retain a durable, replayable record of every rate
   change for compliance and later reprocessing.

These consumers are independent, must not interfere with one another, and at
least one (audit) needs **retention and replay** — the ability to re-read the
full history to rebuild state or onboard a new consumer. RabbitMQ deletes a
message once consumed; modeling multiple independent, replayable readers over a
work-queue broker is awkward and loses history.

## Decision
Introduce **Kafka, via Redpanda** (Kafka-API-compatible), for the **rate-change
event stream only**. Publish rate-sheet activation to a `pricing.rate-sheet.activated`
topic; add independent consumer groups for cache invalidation, SSE push, and
audit. **Keep RabbitMQ** for quote-job dispatch and notification work queues —
each broker is used for the pattern it fits.

Use **Redpanda** rather than Apache Kafka because it is a single Go binary with
no JVM or ZooKeeper/KRaft overhead, materially lighter to operate on a small
deployment target, while exposing the identical Kafka API and client libraries
(Spring for Apache Kafka).

## Alternatives Considered
1. **Keep everything on RabbitMQ.** Simplest, no new infrastructure. But
   multiple independent, replayable consumers of one event stream is not
   RabbitMQ's strength; retention/replay for audit would require external
   persistence and bespoke fan-out. Rejected for this flow (retained for work
   queues).
2. **Apache Kafka (full distribution).** The canonical choice, but heavy: JVM +
   KRaft/ZooKeeper, higher memory and operational footprint — poorly matched to
   a constrained single-node deployment. Rejected in favor of the compatible,
   lighter Redpanda.
3. **AWS Kinesis / SQS fan-out (SNS→SQS).** Viable on AWS (aligns with the
   Phase-3 SQS direction in ADR-0050), but ties the event-stream design to a
   cloud provider and does not give a local, provider-neutral development story.
   Deferred as a possible Phase-3 managed option.
4. **Replace RabbitMQ entirely with Kafka.** Over-corrects. Work-queue dispatch
   (quote jobs) is well served by RabbitMQ's acknowledgement and routing; moving
   it to Kafka gains nothing and loses ergonomics. Rejected.

## Rationale
- **Right tool per pattern.** Task dispatch → RabbitMQ (ack, routing, work
  distribution). Event stream with fan-out, retention, and replay → Kafka. Using
  both deliberately is a stronger design than forcing one broker to do both.
- **Replay and retention** are first-class in the Kafka log, satisfying the
  audit requirement and letting new consumers re-read history without a bespoke
  store.
- **Independent consumer groups** let cache invalidation, SSE push, and audit
  scale and fail independently.
- **Redpanda** keeps the operational cost proportional to a single-node
  deployment while preserving Kafka API compatibility, so the design and skills
  transfer directly to a managed Kafka later if scale demands it.

## Consequences
- A second broker technology is introduced; the team now operates RabbitMQ and
  Kafka/Redpanda. This is justified by the distinct workload patterns but adds
  operational surface and a clear boundary that must be documented (which flow
  uses which broker).
- The rate-change flow gains at-least-once delivery with consumer-tracked
  offsets; consumers must be **idempotent** (aligns with Harbor's existing
  idempotency posture) since replays and redeliveries are expected.
- Observability should trace an event end-to-end (rate-sheet activation →
  Kafka → SSE push) to make the fan-out visible; this pairs with the
  observability work (ADR-0030).
- Local development adds a Redpanda container to `docker-compose`; production
  adds a single Redpanda node (or managed Kafka) sized to the event volume.

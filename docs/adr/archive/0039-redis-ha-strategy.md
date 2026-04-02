# ADR 0039: Redis High Availability — ElastiCache Sentinel vs. Cluster vs. Serverless

## Status
Archived — valid, deferred to production readiness phase

Redis high availability is a production concern. Will be
revisited during Phase 3.

## Date
Fill in when you write this

## Phase
3 — Scale and Operations

---

## Context

WRITE THIS YOURSELF

Prompts to answer:
- What is Redis used for in this system?
  (Rate sheet caching in pricing-service (ADR-0005). Per-user rate limiting tokens (ADR-0017).
   Phase 3: potentially session store or distributed lock. Multiple use cases with different
   availability requirements.)
- What happens if Redis becomes unavailable?
  (Rate sheet cache miss → pricing-service falls back to MySQL query (slower but functional).
   Rate limiting: Bucket4j can degrade gracefully (allow-on-failure mode) or block-on-failure.
   Redis is used for performance, not correctness — degraded operation without it is acceptable.)
- What is ElastiCache Sentinel (Replication Group)?
  (A primary Redis node with one or more read replicas. Sentinel monitors the primary.
   On primary failure, Sentinel promotes a replica. Automatic failover in ~30-60 seconds.
   DNS endpoint updates to point to the new primary. Application reconnects automatically.)
- What is ElastiCache Cluster Mode (Redis Cluster)?
  (Data partitioned across multiple shards (primary + replicas per shard).
   Scales writes horizontally. Required for datasets > ~25GB or very high write throughput.
   More complex: cluster-aware clients required, MULTI/EXEC transactions limited to single shard.)
- What is ElastiCache Serverless?
  (AWS-managed Redis that auto-scales capacity. No node sizing required. Pay per ECU and GB.
   Minimum cost ~$90/month even with minimal usage. Appropriate for unpredictable workloads.)
- What is the sizing required for this use case?
  (Rate sheet cache: a few hundred rate sheets × ~10KB each = a few MB.
   Rate limiting: one counter per user per window — thousands of small keys.
   Total dataset well under 1GB. A single cache.t4g.small is more than sufficient.)


---

## Decision

WRITE THIS YOURSELF

What is the decision? Be concrete — a sentence or two that could stand alone.
Describe the *what*, not the *why* (why belongs in Rationale).


---

## Alternatives Considered

### Alternative: ElastiCache Cluster Mode (Redis Cluster)
**Strengths:** Horizontal write scaling, handles datasets > 25GB, no single-shard write bottleneck
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- The dataset for rate sheet caching and rate limiting is in the low MB range.
  Redis Cluster is designed for datasets and write volumes this system will never approach.
- Cluster Mode adds client complexity (cluster-aware client required) and limits
  MULTI/EXEC transactions to single-shard keys — which complicates Bucket4j rate limiting.
- A Replication Group (Sentinel) provides adequate HA with no client complexity.
- Revisit only if rate sheet dataset exceeds ~10GB or write throughput saturates a single node.

### Alternative: ElastiCache Serverless
**Strengths:** Auto-scaling, no capacity planning, zero operational overhead
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- ElastiCache Serverless has a minimum cost of approximately $90/month regardless of usage.
- A cache.t4g.small ($0.034/hr ≈ $25/mo) is more than sufficient for this workload.
- Serverless pricing is justified for unpredictable spikes or very large datasets.
  This system has neither. The cost premium is not justified.

### Alternative: Single node, no HA (Phase 3 continuation)
**Strengths:** Minimal cost, zero failover complexity
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- In Phase 3 on ECS with SLAs, a Redis node failure takes ~15-30 minutes to detect and replace
  if managed manually. That is 15-30 minutes of either degraded pricing performance or
  rate limiting bypass.
- A Replication Group with Sentinel failover reduces that to ~30-60 seconds automatically.
- The cost delta (one extra replica node ≈ $25/mo) is low relative to Phase 3 operational maturity.

---

## Rationale

### Replication Group Matches the HA Requirement Without Over-Engineering

WRITE THIS YOURSELF
Redis failure is non-fatal for this system (ADR-0005 defines the fallback path).
But in Phase 3 with paying lender partners and SLA expectations, a 15-minute
Redis outage degrading pricing-service response times is visible and unacceptable.
The Replication Group + Sentinel approach provides:
- Automatic primary failover in ~60 seconds
- Multi-AZ replica (primary in us-east-1a, replica in us-east-1b) — AZ-failure tolerant
- Read replicas offload read-heavy cache lookups if needed (not required at this scale)
No application code changes required vs. single node — the failover is transparent to Spring's
RedisTemplate via the ElastiCache configuration endpoint.

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

# ADR 0028: AWS Migration Strategy — Trigger Criteria and Migration Path

## Status
<!-- Fill in when finalized: Proposed | Accepted | Deprecated | Superseded -->

## Date
<!-- Fill in when you write this -->

## Phase
3 — Scale and Operations

---

## Context
<!--
WRITE THIS YOURSELF

Prompts to answer:
- What is the current deployment state? (Phase 1: Hetzner VPS, Docker Compose)
- What signals indicate the VPS is no longer sufficient?
  (Server CPU/memory consistently above 70%, response time degradation under load,
   manual intervention required for more than one incident per week,
   MRR or client count reaching a specific threshold.)
- What are the components that must migrate?
  (Compute: 7 services → ECS Fargate. Database: MySQL → RDS. Cache: Redis → ElastiCache.
   Messaging: RabbitMQ → SQS (transport abstraction makes this a config change).
   Static files: nginx-served → S3 + CloudFront. CI/CD: remains GitHub Actions.)
- What is the migration risk?
  (Data migration, DNS cutover, brief downtime window, environment variable changes,
   service discovery changes from Docker hostnames to AWS service discovery or ALB.)
- What is the recommended migration order?
  (Least risky first: 1. Static frontend to S3/CloudFront (zero app code change)
   2. MySQL to RDS (connection string change, Flyway runs on new DB)
   3. Redis to ElastiCache (connection string change)
   4. Messaging to SQS (swap transport adapter — covered by ADR-0007)
   5. Compute to ECS Fargate (biggest change, done last when all dependencies are AWS-managed))
-->


---

## Decision
<!--
WRITE THIS YOURSELF

What is the decision? Be concrete — a sentence or two that could stand alone.
Describe the *what*, not the *why* (why belongs in Rationale).
-->


---

## Alternatives Considered

### Alternative: Lift-and-shift (move all containers to EC2 at once)
**Strengths:** Identical runtime environment to VPS, fastest migration, Docker Compose runs on EC2
**Rejected because:**
<!--
WRITE THIS YOURSELF
Key points:
- Lift-and-shift preserves all the VPS limitations on EC2: single point of failure,
  no managed HA, no auto-scaling. You pay AWS prices for VPS-level reliability.
- The Phase 3 goal is not "same thing on AWS" — it is managed services, HA, auto-scaling.
- A phased migration to ECS Fargate + RDS + ElastiCache achieves the actual goal.
-->

### Alternative: Kubernetes (EKS) instead of ECS Fargate
**Strengths:** Kubernetes portability, larger ecosystem, Helm charts, GitOps with ArgoCD
**Rejected because:**
<!--
WRITE THIS YOURSELF
Reference ADR-0014.
EKS adds significant operational complexity for a team that is not already running Kubernetes.
ECS Fargate (serverless containers) eliminates node management entirely — no EC2 nodes to patch.
The application does not have requirements that justify EKS over ECS:
no multi-cloud portability requirement, no existing Kubernetes investment,
no specialized scheduling needs.
-->

---

## Rationale

### Dependency-First Migration Minimizes Risk
<!--
WRITE THIS YOURSELF
By migrating databases and cache to managed AWS services BEFORE migrating compute,
the ECS tasks can be tested against the actual production data before the DNS cutover.
Run the ECS tasks pointed at RDS/ElastiCache alongside the VPS.
Run smoke tests against the ECS environment.
DNS cutover only when confidence is high.
This is the blue-green migration pattern applied at the infrastructure level.
-->

### Define the Migration Trigger Now
<!--
WRITE THIS YOURSELF — and actually fill in the numbers.
The migration trigger should be a specific, measurable threshold, not "when we feel like it."
Examples:
- "When monthly recurring revenue exceeds $X"
- "When daily quote volume exceeds Y"
- "When the VPS CPU averages above 70% for more than 3 consecutive days"
- "When the team headcount exceeds N (operational overhead becomes justifiable)"
The purpose of writing this in the ADR is to prevent the migration from being
delayed indefinitely by "we're too busy with product work" when the VPS starts struggling.
-->

---

## Consequences

### Positive
<!--
WRITE THIS YOURSELF
- List the positive consequences of this decision.
-->

### Negative
<!--
WRITE THIS YOURSELF
- List the negative consequences. Be honest — no decision is without trade-offs.
-->

---

## Follow-up
<!--
WRITE THIS YOURSELF
- List follow-up actions, related ADRs to write, or open questions to resolve.
-->

# ADR 0038: Container Image Registry — GHCR vs. ECR vs. Docker Hub

## Status
Deprecated

Container registry selection is deferred. Not in scope for the
current project phase.

## Date
Fill in when you write this

## Phase
3 — Scale and Operations

---

## Context

WRITE THIS YOURSELF

Prompts to answer:
- What happens to Docker images in Phase 1?
  (Built locally or in GitHub Actions, run directly via docker compose up --build.
   No registry required for Phase 1 — images are built on the deployment target.)
- Why does Phase 3 (ECS) require a registry?
  (ECS pulls container images from a registry at task startup.
   The image must be accessible from the VPC — either a public registry or
   a private registry within AWS (ECR).)
- What is GHCR (GitHub Container Registry)?
  (Container registry integrated with GitHub. Free for public packages.
   Images pushed by GitHub Actions, referenced in ECS task definitions.)
- What is ECR (Elastic Container Registry)?
  (AWS-managed container registry. Images stay in the same AWS region as ECS.
   IAM-controlled access — no separate credentials needed with task roles.
   $0.10/GB/month storage + data transfer.)
- What is the pull speed consideration?
  (ECR images in the same AWS region as ECS pull at internal AWS network speed — fast.
   GHCR images pull over the public internet — slower and incur data transfer costs.)


---

## Decision

WRITE THIS YOURSELF

What is the decision? Be concrete — a sentence or two that could stand alone.
Describe the *what*, not the *why* (why belongs in Rationale).


---

## Alternatives Considered

### Alternative: GitHub Container Registry (GHCR)
**Strengths:** Free for public repos, tight GitHub Actions integration, no AWS-specific configuration
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- ECS tasks pulling from GHCR cross the public internet boundary.
  Each task startup pulls the image (~200-500MB) over the internet — latency and cost.
- GHCR requires credentials for private repositories — stored as ECS task secrets,
  adding credential management overhead.
- ECR in the same region as ECS uses AWS internal networking — faster and free data transfer.
- ECR integrates with IAM via task execution role — no stored credentials required.

### Alternative: Docker Hub
**Strengths:** Universal compatibility, simplest to configure
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- Docker Hub has pull rate limits for unauthenticated/free tier pulls.
  ECS scaling events that pull images may hit rate limits.
- Same cross-internet pull latency issue as GHCR.
- Credentials stored in ECS task definitions or Secrets Manager required for private repos.
- ECR is unambiguously the correct choice for ECS workloads on AWS.

---

## Rationale

### ECR Is Purpose-Built for ECS

WRITE THIS YOURSELF
ECR and ECS are designed to work together:
- Same VPC/region = internal network pulls = fast task startup
- IAM task execution role = no stored credentials
- ECR lifecycle policies = automatic cleanup of old image tags (control storage costs)
- Image scanning built in (can replace Trivy in Phase 3 with native ECR scanning)

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

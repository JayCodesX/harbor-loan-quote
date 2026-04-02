# ADR 0014: VPS Deployment — Docker Compose vs. Nomad vs. K3s

## Status
Deprecated

Deployment strategy is deferred until the platform moves beyond
local Docker Compose. See ADR-0028 (AWS Migration Strategy) for
future direction.

## Date
Fill in when you write this

## Phase
1 — Foundation

---

## Context

WRITE THIS YOURSELF

Prompts to answer:
- What is the deployment target for Phase 1?
  (Single VPS — Hetzner CX42 or equivalent: 8 vCPU, 16GB RAM, ~$20/month)
- What does the existing docker-compose.yml prod profile give us?
  (edge-prod nginx with SSL, certbot for Let's Encrypt, all services, volumes for data persistence.
   The prod infrastructure is already partially designed — this ADR documents why Compose was chosen.)
- What services need to run?
  (api, auth-service, borrower-service, pricing-service, lead-service, notification-service,
   web, admin-web, nginx, mysql, redis, rabbitmq — ~12 containers)
- What are the operational requirements?
  (Automatic container restart on crash, persistent data volumes, rolling deploys where possible,
   environment variable management, health check gating)
- What is the team's operational capacity?
  (Solo/small team — complexity of the orchestrator must be proportional to team size)


---

## Decision

WRITE THIS YOURSELF

What is the decision? Be concrete — a sentence or two that could stand alone.
Describe the *what*, not the *why* (why belongs in Rationale).

---

## Alternatives Considered

### Alternative: HashiCorp Nomad
**Strengths:** Lightweight orchestrator, supports rolling deploys natively, multi-node capable (ready for Phase 2 scaling), simpler than Kubernetes, runs on a single node
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- Nomad adds an orchestrator layer that Docker Compose does not need for a single-node deployment.
- For 12 containers on one server, Docker Compose is operationally simpler and
  has zero additional learning curve for Spring Boot developers.
- Rolling deploys at Phase 1 scale (one server, one deployment at a time) are
  a nice-to-have, not a requirement. Brief downtime during a deploy is acceptable.
- Nomad is the right answer if the deployment grows to multiple nodes in Phase 2.
  The migration from Compose to Nomad is straightforward — same containers, new scheduler.

### Alternative: K3s (lightweight Kubernetes)
**Strengths:** Kubernetes API compatibility means tooling (Helm, kubectl, ArgoCD) is available.
Full EKS/GKE parity means the deployment is cloud-portable with minimal changes.
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- K3s on a single VPS adds all of Kubernetes' complexity (control plane, etcd, CNI plugins,
  RBAC, namespaces, resource requests/limits, liveness/readiness probes in YAML)
  with none of the multi-node scaling benefit.
- The project moves to AWS ECS (not EKS) in Phase 3 — K3s experience does not
  translate to ECS. The Phase 3 transition would still require rewriting all
  deployment configuration.
- For a portfolio project, a clean Compose → ECS migration story is simpler
  to explain than a K3s → ECS migration story.
- If the future target is EKS rather than ECS, K3s becomes more justifiable.
  Document that as a follow-up consideration.

### Alternative: Kamal (by DHH / 37signals)
**Strengths:** Docker Compose-level simplicity with rolling deploys and zero-downtime deployment, SSH-based, no orchestrator
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- Kamal is a newer tool with a smaller community and fewer resources than Compose.
- The existing docker-compose.yml prod profile is already built and tested.
  Switching to Kamal requires learning a new tool for equivalent functionality.
- Kamal is worth evaluating in Phase 2 if rolling deploys without downtime
  become a requirement before the AWS migration.

---

## Rationale

### Docker Compose Matches the Project's Operational Reality

WRITE THIS YOURSELF
The argument is proportionality: the deployment tool complexity should match
the deployment complexity.
- One server → no scheduler needed
- 12 containers → manageable with a single compose file
- Solo/small team → minimal ops overhead
- Deploy frequency → likely once per day or less
Compose handles all of this with a syntax every Docker user knows.
The existing prod profile (edge-prod, certbot volumes, service depends_on) shows
the team is already comfortable with Compose as the deployment mechanism.

### The Phase 3 Migration Path Is Clear

WRITE THIS YOURSELF
Compose containers → ECS task definitions is a well-documented migration path.
Each docker-compose service maps to an ECS task definition.
Environment variables map to ECS task environment or Secrets Manager references.
Volumes map to EFS mounts or RDS (for MySQL) and ElastiCache (for Redis).
The migration is an infrastructure translation, not a rearchitecture.
Having a clean Compose deployment in Phase 1 means Phase 3 has a clear source of truth to translate from.

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

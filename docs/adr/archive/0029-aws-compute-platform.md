# ADR 0029: AWS Compute — ECS Fargate vs. EC2 Auto Scaling vs. EKS

## Status
Deprecated

Overlaps with ADR-0028 (AWS Migration Strategy). Compute platform
selection is deferred until production deployment.

## Date
Fill in when you write this

## Phase
3 — Scale and Operations

---

## Context

WRITE THIS YOURSELF

Prompts to answer:
- What are the containerized workloads that need to run on AWS?
  (7 Spring Boot services: harbor-api, auth-service, borrower-service, pricing-service,
   lead-service, notification-service, directory-service)
- What does ECS Fargate provide that EC2 does not?
  (Serverless containers — no EC2 instances to provision, patch, or manage.
   You define CPU/memory per task. AWS handles the underlying infrastructure.)
- What is the cost difference?
  (Fargate: pay per vCPU/GB-hour of task runtime.
   EC2 Auto Scaling: pay for the EC2 instances whether tasks are using all capacity or not.
   At low-to-medium scale, Fargate is more cost-efficient. At high scale, EC2 is cheaper.)
- What is EKS and why is it overkill?
  (Elastic Kubernetes Service — managed Kubernetes control plane.
   Requires knowledge of Kubernetes concepts: pods, deployments, services, ingress,
   RBAC, namespaces, resource requests/limits, ConfigMaps, Secrets.
   Justified only for multi-cloud portability or Kubernetes-specific tooling requirements.)
- What does the existing codebase assume about deployment?
  (deploy-aws.yml in GitHub Actions suggests ECS is the target.
   Docker Compose → ECS task definition is a well-documented migration path.)


---

## Decision

WRITE THIS YOURSELF

What is the decision? Be concrete — a sentence or two that could stand alone.
Describe the *what*, not the *why* (why belongs in Rationale).


---

## Alternatives Considered

### Alternative: EC2 Auto Scaling with ECS EC2 launch type
**Strengths:** Lower per-unit cost at high scale, more control over instance types and storage
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- EC2 launch type requires managing EC2 instances: AMI updates, patching, capacity reservation.
  Fargate eliminates this operational burden.
- At the projected scale for Phase 3 (< 1,000 daily active users initially), Fargate is
  cost-competitive. The per-hour premium over EC2 is justified by zero node management.
- Revisit EC2 launch type only when Fargate cost exceeds the operational savings it provides —
  typically at very high sustained task counts.

### Alternative: AWS EKS (Kubernetes)
**Strengths:** Kubernetes portability, Helm ecosystem, GitOps tooling (ArgoCD, Flux)
**Rejected because:**

WRITE THIS YOURSELF
Reference ADR-0014 (VPS deployment decision).
EKS adds: kubectl configuration, YAML manifests for every resource, RBAC policies,
namespace management, ingress controller setup, cluster add-ons.
For a team migrating from Docker Compose to AWS, ECS Fargate is the natural bridge —
task definitions map closely to compose service definitions.
EKS is the right choice when: multi-cloud is a requirement, the team is already
Kubernetes-proficient, or Kubernetes-native tooling provides specific value.
None of these apply here.

### Alternative: AWS Lambda (serverless functions)
**Strengths:** Zero idle cost, infinite scale, per-invocation pricing
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- Spring Boot applications have cold start times of 2-10 seconds.
  Lambda cold starts make Spring Boot impractical for latency-sensitive API endpoints.
- notification-service holds long-lived SSE connections. Lambda functions have a
  maximum execution time (15 minutes) incompatible with persistent connections.
- Lambda is appropriate for event-driven, short-duration functions — not for
  long-running Spring Boot applications with connection pooling and startup initialization.

---

## Rationale

### ECS Fargate Matches Spring Boot Application Characteristics

WRITE THIS YOURSELF
Spring Boot services are long-running processes that need persistent TCP connections
(to MySQL, Redis, RabbitMQ/SQS). They have significant startup time (10-30 seconds).
ECS Fargate is designed for this workload: persistent containers, definable CPU/memory,
health check-based routing via ALB.
The Docker Compose → ECS Fargate translation is direct:
service definition → task definition, mem_limit → memory, expose → container port.

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

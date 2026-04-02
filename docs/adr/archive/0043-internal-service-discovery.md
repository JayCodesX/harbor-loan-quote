# ADR 0043: Internal Service Discovery — Docker Hostnames vs. ECS Cloud Map vs. Internal ALB

## Status
Deprecated

Service discovery is simplified in the 3-service architecture.
Docker Compose networking handles routing for local development.

## Date
Fill in when you write this

## Phase
1–3 (approach differs per phase)

---

## Context

WRITE THIS YOURSELF

Prompts to answer:
- How do services currently find each other in Phase 1?
  (Docker Compose assigns each service a hostname matching the service name in docker-compose.yml.
   harbor-api calls pricing-service at http://pricing-service:8081.
   This is DNS-based service discovery provided automatically by Docker's internal network.
   Zero configuration — it just works within a Docker Compose deployment.)
- Why does Docker Compose hostname resolution not work on ECS?
  (ECS Fargate tasks run in a VPC with dynamic IP addresses. There is no shared Docker network.
   Each task has its own ENI (Elastic Network Interface) with a private IP that changes on restart.
   Services cannot find each other by hostname without a service registry or load balancer.)
- What is AWS Cloud Map?
  (AWS service registry. ECS tasks register themselves with Cloud Map on startup.
   Cloud Map provides DNS-based discovery: pricing-service.harbor.local resolves to the
   current task IPs. Supports health check integration — unhealthy tasks are deregistered.)
- What is an internal ALB (Application Load Balancer)?
  (An ALB that is internet-facing to the internal VPC only (not publicly accessible).
   Services call http://pricing-service-alb.internal/ and the ALB routes to healthy ECS tasks.
   The ALB handles health checks, target group registration, and load balancing.
   More expensive than Cloud Map but provides load balancing and TLS termination.)
- What is the relationship between service discovery and the circuit breaker (ADR-0006)?
  (Resilience4j circuit breaker wraps the HTTP call from harbor-api to pricing-service.
   The circuit breaker operates at the client level — service discovery determines the URL
   that the RestClient/WebClient calls. Both are needed for resilient inter-service communication.)


---

## Decision

WRITE THIS YOURSELF

What is the decision? Be concrete — a sentence or two that could stand alone.
Describe the *what*, not the *why* (why belongs in Rationale).


---

## Alternatives Considered

### Alternative: Internal ALB per service
**Strengths:** Built-in load balancing across multiple task replicas, TLS termination, health check integration, AWS-native monitoring
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- Each internal ALB costs ~$20-25/month minimum (ALB hourly charge + LCU charges).
  With 3 services, that is $60-75/month for internal routing — significant for Phase 3 budget.
- Cloud Map + ECS service discovery provides DNS-based load balancing across multiple task IPs
  at no additional cost beyond the Cloud Map namespace ($0.10/namespace/month).
- The circuit breaker (ADR-0006) operates at the client level — it does not require an ALB
  to detect failures. RestClient with Resilience4j handles retry and failure detection.
- Internal ALB is appropriate when services need TLS between each other (mTLS) or when
  sophisticated routing rules are required. Neither applies at Phase 3.

### Alternative: Hardcoded internal IP addresses / environment variables
**Strengths:** Zero additional services, explicit configuration
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- ECS Fargate task IPs are dynamic — they change on every task restart or deployment.
  Hardcoded IPs break every time a task is replaced.
- Environment variable URLs (PRICING_SERVICE_URL=http://10.0.1.45:8081) require manual
  updates with every deployment — operationally error-prone.
- Cloud Map DNS is the correct solution: IP registration and deregistration happen automatically
  as tasks start and stop. The service URL never needs to change.

### Alternative: Service mesh (AWS App Mesh / Istio)
**Strengths:** mTLS between services, sophisticated traffic management (weighted routing, retries, timeouts), observability sidecars
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- Service mesh is significant operational overhead: Envoy sidecar per task,
  App Mesh control plane configuration, certificate rotation.
- The resilience requirements for this system are handled by Resilience4j at the application
  level (ADR-0006). Application-level circuit breaking is sufficient for 3 services.
- Service mesh is justified when the team needs mTLS (zero-trust networking) or has
  10+ services where centralized traffic management is preferable to per-service Resilience4j.
  This system has 3 services.

---

## Rationale

### Cloud Map Matches the Phase 3 Scale Without ALB Cost Overhead

WRITE THIS YOURSELF
The service discovery requirements are straightforward:
- harbor-api needs to find pricing-service
- harbor-api and pricing-service need to find notification-service (for message publishing — though this is via SQS in Phase 3, not direct HTTP)
- All services need to find their databases (handled by RDS endpoints, not service discovery)

Cloud Map provides:
- Private DNS namespace (harbor.local) with automatic A record registration/deregistration
- ECS integration: `serviceRegistries` field in ECS service definition registers tasks automatically
- Health check integration: failed health checks trigger automatic DNS deregistration
- Cost: ~$0.10/namespace/month + standard Route 53 query charges — effectively free

Application configuration is simple:
  pricing.service.url: http://pricing-service.harbor.local:8081
This URL never changes regardless of how many tasks are running or what their IPs are.

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

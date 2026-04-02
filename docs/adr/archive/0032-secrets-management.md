# ADR 0032: Secrets Management — Env Files vs. AWS Secrets Manager vs. HashiCorp Vault

## Status
Archived — valid, deferred to production readiness phase

Secrets management remains an important operational concern.
Will be revisited during AWS migration.

## Date
Fill in when you write this

## Phase
1 (env files) → 3 (AWS Secrets Manager)

---

## Context

WRITE THIS YOURSELF

Prompts to answer:
- What secrets exist in this system?
  (Database passwords, JWT signing keys (RSA private key after ADR-0010),
   Redis password, RabbitMQ credentials, API keys for email provider,
   partner webhook authentication tokens, GitHub Actions deploy SSH key)
- How are secrets managed today?
  (.env files on the VPS, GitHub Actions encrypted secrets for CI/CD,
   environment variables injected at container startup)
- What are the risks of .env file secrets?
  (File-based secrets can be accidentally committed to git,
   visible to anyone with SSH access to the server,
   no audit log of who accessed which secret,
   rotation requires SSH access and manual file editing)
- What does AWS Secrets Manager provide?
  (Centralized secret storage, IAM-controlled access per secret,
   audit log via CloudTrail, automatic rotation for supported secret types,
   ECS integration via task role — no environment variables in task definitions)
- What is the Phase 1 vs. Phase 3 approach?
  (Phase 1: .env files on VPS + GitHub Actions encrypted secrets (minimal viable).
   Phase 3: AWS Secrets Manager with ECS task role injection.)


---

## Decision

WRITE THIS YOURSELF

What is the decision? Be concrete — a sentence or two that could stand alone.
Describe the *what*, not the *why* (why belongs in Rationale).


---

## Alternatives Considered

### Alternative: HashiCorp Vault
**Strengths:** Most powerful secret management, dynamic secrets, fine-grained policies, multi-cloud
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- Vault is a significant operational dependency: high availability requires 3+ nodes,
  unsealing after restart requires operator intervention, backup and recovery is complex.
- For a team deploying to AWS, Secrets Manager is the native solution with
  IAM integration, zero infrastructure to manage, and automatic rotation for RDS.
- Vault is the right choice for multi-cloud environments or organizations with
  strict secret governance requirements beyond what Secrets Manager provides.

### Alternative: SSM Parameter Store (AWS)
**Strengths:** Free for standard parameters, similar IAM integration to Secrets Manager
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- SSM Parameter Store is appropriate for configuration (non-sensitive app settings).
  For secrets (credentials, private keys), Secrets Manager's automatic rotation
  and dedicated secret semantics are more appropriate.
- Secrets Manager integrates directly with RDS for automatic password rotation.
  Parameter Store does not have this integration.
- The cost difference ($0.40/secret/month vs. free for Parameter Store) is
  negligible for the number of secrets in this system.

---

## Rationale

### Never Put Secrets in Docker Images or Task Definitions

WRITE THIS YOURSELF
The ECS task role injection pattern is the AWS best practice:
1. Store the secret in Secrets Manager: arn:aws:secretsmanager:us-east-1:...
2. Grant the ECS task role secretsmanager:GetSecretValue on that ARN
3. Reference the ARN in the task definition: {name: DB_PASSWORD, valueFrom: arn:...}
4. At task startup, ECS injects the secret value as an environment variable
This means: no secrets in git, no secrets in task definition JSON, no secrets in logs.
The only place the secret exists is Secrets Manager (encrypted) and the running container's environment.

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

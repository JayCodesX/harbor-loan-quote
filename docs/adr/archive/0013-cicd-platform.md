# ADR 0013: CI/CD Platform — Jenkins vs. GitHub Actions

## Status
Deprecated

CI/CD platform selection is deferred. Not in scope for the current
project phase.

## Date
Fill in when you write this

## Phase
1 — Foundation

---

## Context

WRITE THIS YOURSELF

Prompts to answer:
- What does the current Jenkinsfile do?
  (Backend tests for all 6 services, frontend install + tests + build,
   E2E Playwright tests with docker-compose integration profile, no deploy stage)
- What is the operational cost of Jenkins?
  (~$30/month EC2 t3.medium, requires updates, plugin management, credential management)
- What does the RabbitMQ migration board assume about CI/CD?
  (GitHub Actions — ci.yml and deploy.yml are the target, not Jenkinsfile)
- What does the deploy workflow need to do?
  (SSH to VPS, pull latest code/images, run docker compose up --env-file .env.prod)
- What is the team size and operational preference?
  (Solo or small team — Jenkins overhead is disproportionate to team size)


---

## Decision

WRITE THIS YOURSELF

What is the decision? Be concrete — a sentence or two that could stand alone.
Describe the *what*, not the *why* (why belongs in Rationale).

---

## Alternatives Considered

### Alternative: Retain Jenkins
**Strengths:** Already configured, full control over the build environment, supports any plugin, no SaaS dependency
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- Jenkins requires a dedicated EC2 instance (~$30/month) that must be:
  updated, patched, backed up, and monitored.
- For a solo/small team project, Jenkins maintenance overhead is
  disproportionate to the value it provides over GitHub Actions.
- The Jenkinsfile has no deploy stage — it only runs tests.
  GitHub Actions handles both CI and CD in one platform with less configuration.
- Jenkins plugins for Docker, Maven, Node, and SSH are manageable but
  GitHub Actions has native support for all of these with zero plugin installation.

### Alternative: GitLab CI/CD (with GitLab migration)
**Strengths:** Tightly integrated with GitLab, built-in container registry, mature pipeline syntax
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- The project is already on GitHub. Migrating to GitLab for CI/CD means
  either mirroring or moving the repository — added complexity with no benefit.
- GitHub Actions is native to GitHub — PR status checks, branch protection,
  and deployment environments are first-class integrations.
- For a portfolio project, GitHub Actions is the more visible and
  recognized platform to potential employers.

### Alternative: CircleCI or Travis CI
**Strengths:** Well-established, good Docker support, free tier for open source
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- Both require a separate account and configuration outside of GitHub.
- GitHub Actions is free for public repositories and $4/month for private
  with 3,000 minutes/month — sufficient for this project.
- GitHub Actions is now the industry default for GitHub-hosted projects.
  Employers reviewing the portfolio will recognize it immediately.

---

## Rationale

### GitHub Actions Eliminates the Dedicated CI Server

WRITE THIS YOURSELF
The $30/month Jenkins EC2 instance is the second largest cost item after compute.
GitHub Actions replaces it at $0 (public) or $4/month (private).
No server to maintain, no plugins to update, no Java runtime to keep current.
The cost saving is real and the operational overhead reduction is significant for a small team.

### Co-location With Source Code Simplifies the Developer Workflow

WRITE THIS YOURSELF
PR opened → CI runs → status check on the PR.
Merge to main → deploy runs → VPS updated.
All visible in one GitHub interface. No switching between GitHub and Jenkins.
For a portfolio project, reviewers see green CI badges directly in the repository.

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

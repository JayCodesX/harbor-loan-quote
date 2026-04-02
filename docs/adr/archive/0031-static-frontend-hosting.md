# ADR 0031: Static Frontend Hosting — S3 + CloudFront vs. ECS Nginx vs. Amplify

## Status
Deprecated

Frontend hosting strategy is deferred. Not in scope for the current
project phase.

## Date
Fill in when you write this

## Phase
3 — Scale and Operations

---

## Context

WRITE THIS YOURSELF

Prompts to answer:
- What are the two frontends and what are they built with?
  (web: borrower-facing React app built with Vite.
   admin-web: Mortgage Desk admin React app built with Vite.
   Both are static build outputs: HTML, CSS, JS bundles — no server-side rendering.)
- How are they currently served?
  (Nginx containers (web and admin-web) in Docker Compose, proxied by edge nginx.)
- What is the cost of running Nginx containers for static files on ECS?
  (~$18-36/month in Fargate compute for two nginx containers serving static files.
   S3 + CloudFront serves the same files for ~$5-10/month with global CDN distribution.)
- What is the deployment workflow for static files?
  (CI builds the React app (npm run build), outputs to /dist.
   Push the /dist contents to S3 bucket, invalidate CloudFront cache.
   No container rebuild required for frontend-only changes.)
- What is the benefit of CDN edge delivery?
  (CloudFront serves files from the AWS edge location nearest to the user.
   Lower latency than a single-region ECS container for geographically distributed users.)


---

## Decision

WRITE THIS YOURSELF

What is the decision? Be concrete — a sentence or two that could stand alone.
Describe the *what*, not the *why* (why belongs in Rationale).


---

## Alternatives Considered

### Alternative: Continue serving from Nginx containers in ECS
**Strengths:** Consistent with Phase 1 pattern, familiar, no new AWS services
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- Serving static files from an ECS Fargate container costs ~$18/month per nginx task.
  S3 + CloudFront achieves better performance at ~$5/month.
- Static file serving is a commodity — it should not consume ECS task resources.
- A frontend-only change (CSS fix, copy update) requires building and pushing a Docker image
  then triggering an ECS deployment. With S3, it is `aws s3 sync /dist s3://bucket` — seconds.
- CloudFront global edge network provides better latency than a single-region ECS task.

### Alternative: AWS Amplify Hosting
**Strengths:** Managed CI/CD for frontend, branch previews, custom domains, integrated with GitHub
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- Amplify Hosting is opinionated about the deployment workflow — it wants to manage CI/CD.
  The project already has GitHub Actions CI/CD configured.
  Adding Amplify alongside GHA creates two parallel deployment systems for the frontend.
- Amplify has higher cost than S3 + CloudFront for equivalent traffic at scale.
- S3 + CloudFront provides the same CDN capabilities with direct control over the configuration.
- Amplify is a good choice for teams that want managed frontend CI/CD from scratch.
  For a team with existing GitHub Actions pipelines, it adds complexity without proportionate benefit.

---

## Rationale

### Static Files Do Not Belong in Containers

WRITE THIS YOURSELF
A container is a running process. A static file is data.
Serving static HTML/JS/CSS from a running Nginx process is using a hammer to hang a picture.
S3 is designed for object storage and static website hosting.
CloudFront distributes those objects globally with edge caching.
The combination is purpose-built for this exact use case.
Removing the nginx containers from ECS simplifies the deployment,
reduces cost, and improves performance simultaneously.

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

# ADR 0015: TLS Termination — Nginx + Certbot vs. Cloudflare vs. Cloud Load Balancer

## Status
Archived — valid, deferred to production readiness phase

TLS termination approach remains applicable. Will be revisited
when the platform moves to AWS deployment.

## Date
Fill in when you write this

## Phase
1 (VPS) → 3 (AWS)

---

## Context

WRITE THIS YOURSELF

Prompts to answer:
- What does the current prod setup use?
  (Nginx with certbot/Let's Encrypt — docker-compose.yml has certbot service with volumes
   for certbot_www and letsencrypt, and the nginx prod config references the cert path)
- What does Let's Encrypt require?
  (Domain validation via HTTP-01 or DNS-01 challenge. HTTP-01 requires port 80 accessible.
   Certificates expire every 90 days and must be renewed automatically.)
- What does Cloudflare offer as an alternative?
  (Free CDN + DDoS protection + SSL termination. The origin server gets a Cloudflare-issued cert
   or can use origin certificates. No certbot required.)
- What changes in Phase 3 on AWS?
  (AWS Certificate Manager provides free TLS certificates managed by AWS.
   ALB terminates TLS with an ACM cert. No certbot, no Nginx TLS config needed.)
- What is the difference between "TLS termination at edge" vs. "end-to-end TLS"?
  (Termination at edge: TLS from browser to Cloudflare/ALB, then internal HTTP.
   End-to-end: TLS all the way to the origin server as well. Consider which matters for compliance.)


---

## Decision

WRITE THIS YOURSELF

What is the decision? Be concrete — a sentence or two that could stand alone.
Describe the *what*, not the *why* (why belongs in Rationale).

---

## Alternatives Considered

### Alternative: Nginx + Certbot (current prod setup)
**Strengths:** Already configured, no external dependency, fully self-contained, industry standard
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- Certbot renewal is a cron job that must succeed every 60-80 days.
  If the cron fails silently, the certificate expires and the site goes HTTPS-broken.
  This is a common production incident for self-managed TLS.
- Certbot requires port 80 to be accessible for HTTP-01 challenges during renewal.
  Automating this in a Docker environment with Nginx routing is possible but
  requires specific Nginx location block configuration and certbot hooks.
- Cloudflare Origin Certificates are 15-year validity with no renewal required.
  They eliminate the renewal risk entirely.
- Note: Cloudflare terminates TLS at the edge, so the origin Nginx still needs
  a certificate for the Cloudflare → origin connection (Origin Certificate handles this).

### Alternative: Caddy (automatic HTTPS built in)
**Strengths:** Automatic Let's Encrypt renewal via ACME built into the binary, simpler configuration than Nginx, no separate certbot container
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- The existing Nginx configuration (rate limiting, proxy pass routing to services,
  security headers) is already written and tested.
  Rewriting it in Caddyfile syntax is work with no new capability.
- Caddy is less commonly encountered in enterprise environments — less recognizable
  on a portfolio than Nginx.
- Cloudflare Origin Certificates solve the renewal problem without replacing Nginx.

### Alternative: AWS ACM in Phase 1 (skip VPS TLS complexity)
**Strengths:** Free, managed, no renewal, integrates with ALB perfectly
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- ACM requires an AWS load balancer to terminate TLS. On a VPS without ALB,
  ACM certificates cannot be used — they are not exportable.
- This is the correct solution for Phase 3 but requires the AWS infrastructure
  that does not exist in Phase 1.

---

## Rationale

### Cloudflare Origin Certificates Eliminate Renewal Risk in Phase 1

WRITE THIS YOURSELF
A 15-year Cloudflare Origin Certificate installed once is operationally simpler
than a 90-day Let's Encrypt cert renewed by certbot.
The certbot container and cron job are removed from the production stack.
Cloudflare's CDN also provides DDoS mitigation and edge caching at no cost.

### ACM + ALB Is the Correct AWS-Native Solution for Phase 3

WRITE THIS YOURSELF
AWS Certificate Manager certificates are free, auto-renewed by AWS, and attach
directly to ALB listeners. No certificate management code, no renewal monitoring.
Phase 3 TLS is zero operational overhead.
The migration from Cloudflare Origin to ACM is a DNS change (point domain at ALB)
and an ALB listener configuration — not a code change.

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

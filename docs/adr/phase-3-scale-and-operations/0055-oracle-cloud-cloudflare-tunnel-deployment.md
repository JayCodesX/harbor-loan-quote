# ADR 0055: Deployment Topology — Oracle Cloud Always-Free + Cloudflare Tunnel

## Status
Accepted

## Date
2026-07-09

## Phase
3 — Scale and Operations

## Context
Harbor needed a real, reachable deployment so the agent story is demonstrable end
to end (an LLM calling the live MCP server over the internet), not just local. The
constraints: near-zero cost, the full stack running (three Java services + web/admin
frontends + MySQL/Redis/RabbitMQ/Redpanda + observability), and minimal operational
surface. ADR-0028 records AWS as a *future* managed target with cost triggers; it is
explicitly not the choice for a free personal deployment.

Two things had to be decided: **where it runs** and **how public traffic reaches it**.

## Decision
Deploy the full stack with Docker Compose on a single **Oracle Cloud Always-Free
Ampere A1** instance (ARM, 2 OCPU / 12 GB), and expose it through a **Cloudflare
Tunnel** (`cloudflared`) rather than opening inbound ports.

- **Ingress:** `cloudflared` makes an **outbound** connection from the VM to
  Cloudflare; Cloudflare terminates TLS and forwards over the tunnel. No inbound
  ports are opened, so the OCI security list stays at its default (SSH only).
- **Routing:** `oraroute.com` → an nginx `edge-tunnel` (HTTP :80, internal) that does
  the existing path routing to web/api/admin; `pricing.oraroute.com` → `pricing-service`
  directly for the MCP endpoint. Config lives in `cloudflared/config.yml` (in-repo,
  minus the tunnel id/credentials).
- **Composition:** a `docker-compose.prod.yml` override on the existing compose adds
  `edge-tunnel` + `cloudflared`, sets `restart: unless-stopped`, and enables the MCP
  gateway, Kafka, and OTLP tracing for production. Secrets come from `.env.prod`.

## Alternatives Considered
1. **nginx + Let's Encrypt on open 80/443 (the repo's `edge-prod` path).** Standard
   and already partly built, but on Oracle it requires opening the OCI security list
   *and* the instance firewall, managing certbot renewals, and exposes the host to
   inbound scanning. Rejected for this deployment in favor of the tunnel, which needs
   no inbound rules and gets TLS + a CDN from Cloudflare for free. (`edge-prod` is
   retained for a generic VPS where a tunnel isn't wanted.)
2. **AWS (ECS/EKS/EC2), per ADR-0028.** The documented future path when scale or an
   employer mandate justifies managed services and cost. Rejected now: it is not free,
   and the goal is a zero-cost always-on demo. ADR-0028's triggers still stand.
3. **A small paid VPS (Hetzner/DigitalOcean, ~$5–12/mo).** Simpler capacity story
   than Oracle's ARM lottery, but not free and lower spec at the low tiers. Reasonable
   fallback; rejected while the Oracle free ARM (far more RAM/CPU for $0) is available.
4. **Fly.io / Railway / Render free tiers.** Great DX, but free tiers sleep or cap
   hours/resources and don't comfortably hold this whole multi-service stack. Rejected
   for a full always-on deployment.
5. **Cloudflare *quick* tunnel (`trycloudflare.com`).** Zero setup, but an ephemeral
   URL that changes each restart — not a stable link. Kept as a throwaway test option,
   not the deployment.

## Rationale
- **Free and roomy.** The Always-Free A1 gives far more RAM/CPU than any other free
  tier — enough for the full stack — at no cost.
- **Closed by default.** An outbound tunnel means zero inbound ports and no security-
  list/firewall carve-outs, which is both simpler and safer than managing public
  80/443. It also sidesteps the OCI networking friction (security lists + NSGs +
  instance iptables) that trips up first-time Oracle deployments.
- **TLS + CDN for free**, terminated at Cloudflare, so no certbot on the box.
- **Reuses what exists.** The tunnel forwards to the same nginx routing the repo
  already defines; the prod compose is a thin override, not a parallel definition.
- **Provider-neutral escape hatch.** If Oracle capacity or free-tier terms change,
  the same compose + tunnel move to any Docker host (paid VPS, AWS Lightsail) with
  only the host swapped — the ingress design doesn't change.

## Consequences
- **ARM/ARM64.** Every image must be multi-arch; all of Harbor's are (Temurin JRE,
  MySQL, Redis, Redpanda, nginx, cloudflared, otel-lgtm), so builds run natively.
- **Capacity + reclamation risk.** Always-Free ARM can be capacity-denied and idle
  VMs can be reclaimed; converting to Pay-As-You-Go (still within free limits)
  mitigates both. Documented in the runbook.
- **Single node = single point of failure.** Acceptable for a demo/portfolio
  deployment; there is no HA, and MySQL/Redis/Redpanda are single instances. The
  per-node rate-limit and in-JVM bucket caveats (ADR-0053) apply.
- **Grafana stays private** (SSH tunnel), not on a public hostname, unless placed
  behind Cloudflare Access.
- **Secrets live in `.env.prod` on the box** (gitignored). A secret manager is the
  natural next step if this becomes more than a personal deployment.
- The MCP demo now has a public target: `MCP_BASE_URL=https://pricing.oraroute.com`
  proves an agent calling the engine across the internet, through the same API-key +
  rate-limit boundary (ADR-0053).

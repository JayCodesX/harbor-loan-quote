# ADR 0056: Abuse Protection for the Public Demo

## Status
Accepted

## Date
2026-07-09

## Phase
3 — Scale and Operations

## Context
Once Harbor is deployed publicly (ADR-0055), it presents anonymous, write-capable
endpoints to the open internet: `POST /api/loan-quotes/public` (creates quote rows +
loads the pricing engine) and `POST /api/auth/register` / `login` (creates account
rows). Left unguarded, these are the obvious targets for bot spam and would fill the
database, run up load, and pollute the demo. The agent-facing MCP surface is already
gated (API key + Bucket4j, ADR-0053); the gap is the public web surface.

The deployment is a portfolio/interview demo, not a funded product, so the design
goal is: **stay frictionless for a genuine visitor (no login wall on the borrower
app) while making automated abuse impractical**, at zero cost and minimal
operational surface.

## Decision
Defend in **three layers**, cheapest-and-outermost first, matched to each surface's
purpose:

1. **Cloudflare edge (before traffic reaches the instance):**
   - **Bot Fight Mode** on — challenges/blocks known automated traffic.
   - One **rate-limiting rule** on `/api/*` (free tier allows one), e.g. ~10 req/min
     per IP.
   - **Cloudflare Access** in front of the **admin** app (`/admin`) — email-PIN /
     Google, allow-listed to the owner. The admin panel is never public.
   - **Turnstile** on the public quote + register forms — deferred; add only if spam
     actually appears (it needs a small frontend + verify change).

2. **nginx (`default.tunnel.conf`):**
   - General `/api/` capped at **8 r/s** (burst 16) per IP.
   - `/api/auth/` capped hard at **1 r/s** (burst 5) per IP — the account-creation
     abuse target gets its own strict zone.
   - Over-limit → `429`, shed before the request reaches a service.

3. **Application:**
   - MCP boundary keeps **API-key auth + per-key Bucket4j rate limit** (ADR-0053);
     the key is never published on the site. `HARBOR_MCP_RPM` is tunable per
     deployment (lower it for the demo).
   - The MCP subdomain (`pricing.oraroute.com`) routes straight to pricing-service,
     bypassing nginx, so its protection is Cloudflare edge + Bucket4j (by design).

**Posture summary:** borrower app = public but hardened; admin = Access-gated (owner
only); agent/MCP = key-gated and owner-driven.

## Alternatives Considered
1. **Cloudflare Access over the entire zone (login to view anything).** Maximum
   protection, near-zero effort, no app changes. Rejected as the default because it
   puts a login wall in front of the borrower app — friction that loses a casual
   interviewer clicking a link. Retained as the one-flip option if abuse gets bad or
   for a fully-private demo window.
2. **App-level global rate limiting / Bucket4j on the web endpoints too.** Would work
   but duplicates what the edge + nginx already do and adds code to the hot path.
   Rejected: layer the cheap controls first; revisit only if they prove insufficient.
3. **CAPTCHA/Turnstile on every form up front.** Strong, but adds frontend work and
   friction before it is known to be needed. Deferred to a fast-follow if spam shows.
4. **Do nothing / rely on obscurity.** A public URL is found by scanners quickly;
   the write endpoints would be abused. Rejected.

## Rationale
- **Right control per surface.** The borrower app must stay clickable, so it gets
  invisible edge + nginx limits, not a login wall. The admin panel has no reason to
  be public, so it gets a login wall. The agent surface is inherently credentialed,
  so it keeps its key + bucket.
- **Cheap and outermost first.** Cloudflare absorbs bot floods before they cost the
  instance anything; nginx sheds the rest at the edge of the network; the app only
  sees traffic that passed both.
- **Reversible and tunable.** Every limit is a number in config; the whole thing can
  be escalated to full Cloudflare Access with one dashboard change if needed.

## Consequences
- Legitimate burst traffic (e.g. a quick demo clicking around) must stay under the
  limits; the chosen numbers are generous for a single human but hostile to a script.
- The `/api/auth/` cap slows credential-stuffing and mass registration to a crawl.
- Cloudflare Access on `/admin` means the owner authenticates once to reach it, and
  can grant a reviewer temporary access by email — no app-side user management.
- Edge rules (Bot Fight Mode, the rate-limit rule, Access, Turnstile) are **Cloudflare
  dashboard configuration**, documented in the deploy runbook; they are not in the
  repo and must be set per environment.
- If the demo is ever meant to be fully private, flip to zone-wide Cloudflare Access
  (alternative 1) — one change, no redeploy.

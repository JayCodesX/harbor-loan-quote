# ADR 0053: Secure and Rate-Limit the Agent-Facing MCP Boundary

## Status
Accepted

## Date
2026-07-09

## Phase
4 — AI Agent Integration

## Context
ADR-0051 added an MCP server to `pricing-service` and explicitly deferred one
thing: "The MCP endpoint must be secured and rate-limited before any non-local
exposure (tracked separately)." This ADR is that follow-up.

The MCP surface is different in kind from the rest of `pricing-service`. The
existing endpoints are called by **Harbor's own services**, authenticated with
short-lived RSA-signed service tokens (`ServiceTokenValidator`, ADR-0010). The
MCP endpoints (`GET /sse`, `POST /mcp/message`) are called by **autonomous
agents** — an LLM framework, a copilot, eventually a third party. Two new risks
follow from that:

1. **Unauthenticated access.** Without a credential, anyone who can reach the
   port can invoke the pricing tools. The internal service-token scheme is a poor
   fit for agents: an agent framework holds a static configured credential, not a
   JWT it mints per call against Harbor's issuer/audience/scope.
2. **Runaway call volume.** LLM agents retry, loop, and fan out. A misbehaving or
   hostile agent can issue calls far faster than a human, and each `getRateQuote`
   touches the pricing engine, Redis, and MySQL. Nothing today sheds that load.

The pricing tools also now include catalog reads (`listLoanPrograms`,
`getLoanProgramDetails`, ADR-0051 follow-on), so the surface an unauthenticated
caller could enumerate has grown.

## Decision
Add a single scoped servlet filter, `McpGatewayFilter`
(`OncePerRequestFilter`), that guards **only** the MCP transport paths and
enforces two things in order:

1. **API-key authentication.** The caller must present a key in the
   `X-API-Key` header that is in the configured accepted set
   (`harbor.mcp.api-keys`, injected from a secret, never committed). Missing or
   unknown key → `401`.
2. **Per-key rate limiting.** Each API key gets its own **Bucket4j** token bucket
   (`harbor.mcp.requests-per-minute`, default 60, refilled continuously). Over
   the limit → `429`, shed before the request reaches any tool, DB, or cache.

The filter's `shouldNotFilter` returns true for every non-MCP path, so the rest
of `pricing-service` — and its existing service-token flow — is untouched. A
`harbor.mcp.enabled` flag (default `false`) keeps local exploration
frictionless; it is set true in any deployed environment.

Bucket state is an in-JVM `ConcurrentHashMap<apiKey, Bucket>` — correct for the
current single-node deployment.

## Alternatives Considered
1. **Reuse the internal RSA service-token (`ServiceTokenValidator`) for agents.**
   Consistent with existing auth, but wrong-shaped: agents would need to mint
   Harbor-issued JWTs with the right issuer/audience/scope, which is not how agent
   frameworks present credentials. Over-engineered for an external caller.
   Rejected; the service-token flow stays for service-to-service calls only.
2. **Full OAuth2 resource server on the MCP endpoints** (the parent starter is
   available). The most "correct" enterprise answer and the likely end state for a
   real third-party program, but heavy for the current goal: it needs an
   authorization server, token issuance, and per-agent client registration. The
   MCP spec's own auth story is also still stabilizing. Deferred — API keys are
   the pragmatic, industry-common credential for a machine/agent boundary today,
   and the filter is a clean seam to swap in OAuth2 later.
3. **A global Spring Security `SecurityFilterChain`.** Would secure the MCP paths
   but also newly constrain every other endpoint in the service (which currently
   authenticates manually in controllers), risking regressions across
   unrelated surface. Rejected in favor of a filter scoped to the MCP paths only —
   minimal blast radius.
4. **Rate limit at the edge (Cloudflare / gateway) only.** Useful as defense in
   depth and likely added later, but leaves the application itself unprotected if
   traffic arrives by any other path, and cannot rate-limit *per API key* without
   the app's knowledge of keys. Rejected as the sole control; complementary.
5. **No rate limiting, auth only.** Auth stops unknown callers but not an
   authenticated agent stuck in a retry loop — the exact failure mode LLM agents
   exhibit. Insufficient.

## Rationale
- **Right credential for the caller.** API keys match how agent frameworks
  authenticate; RSA service tokens match how Harbor's services authenticate. Using
  each where it fits is clearer than forcing one scheme across both.
- **Defense before the expensive work.** Both checks run in the filter, ahead of
  the MCP dispatch, so rejected calls never reach the pricing engine, Redis, or
  MySQL — the protection is cheap exactly when it matters.
- **Minimal blast radius.** Scoping the filter to `/sse` and `/mcp/**` means the
  agent boundary is hardened without re-authenticating the internal surface or
  disturbing the existing service-token path.
- **Bucket4j** is a small, well-understood token-bucket library with no external
  dependency for the single-node case, and a drop-in Redis backend if we scale
  out — so the choice does not paint us into a corner.
- **A flag keeps dev honest.** `enabled=false` locally means the security posture
  is explicit and opt-in per environment, not something developers route around.

## Consequences
- The MCP endpoints now require an `X-API-Key`; every agent client (including the
  Phase-4 Ollama Cloud demo client) must be configured with a key, and keys must
  be provisioned/rotated via secrets, not code.
- Rate-limit state is per-node and in-memory: on a **multi-node** deployment each
  node enforces its own bucket, so the effective limit is `nodes × rpm`, and a
  restart resets buckets. Moving buckets to the existing **Redis** (Bucket4j has a
  Redis backend) is the documented next step when the service scales past one node.
- `429`/`401` responses are now part of the agent contract; the MCP client demo
  and any consumer must handle back-off on 429.
- The observability work (ADR-0030) should surface auth-reject and rate-limit
  counts as metrics so abuse and misconfiguration are visible.
- OAuth2 remains the likely evolution for a public, multi-tenant agent program;
  this ADR deliberately leaves that seam open rather than building it now.

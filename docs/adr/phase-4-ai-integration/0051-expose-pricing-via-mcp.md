# ADR 0051: Expose the Pricing Engine to AI Agents via a Model Context Protocol (MCP) Server

## Status
Accepted

## Date
2026-07-09

## Phase
4 — AI Agent Integration

## Context
Harbor's pricing engine (`pricing-service`) produces public rate estimates and
refined, credit-aware quotes through `QuotePricingService`. These capabilities
are currently reachable only through Harbor's own REST endpoints and internal
messaging. There is growing demand to let AI agents (LLM-driven assistants,
internal copilots) query pricing directly and safely, so that a natural-language
request such as "what rate could I get on a 30-year fixed at 80% LTV?" can be
answered by an agent calling the real pricing logic rather than hallucinating a
number.

The question is how to expose existing business capabilities to autonomous
agents without (a) reimplementing pricing, (b) hand-maintaining a bespoke
integration contract per agent framework, or (c) loosening the security and
correctness guarantees the engine already enforces.

## Decision
Add a **Model Context Protocol (MCP) server** to `pricing-service` using
**Spring AI 1.0.4** (`spring-ai-starter-mcp-server-webmvc`), running on
**Spring Boot 3.4.9** (bumped from 3.3.5 in this service only, as each service
parents `spring-boot-starter-parent` independently).

Business capabilities are exposed as **MCP tools**: annotated Java methods
(`@Tool` / `@ToolParam`) that act as thin adapters over the existing
`QuotePricingService`. The first tool, `getRateQuote`, maps an agent's flat
arguments onto the existing `PricingScenario` record, delegates to
`pricePublicQuote(...)`, and returns a purpose-built `RateQuoteResult`. Tools
are registered via a `ToolCallbackProvider` bean; Spring AI's MCP
auto-configuration publishes their generated JSON schemas over the protocol.

**Transport: SSE over Spring MVC** (`GET /sse`, `POST /mcp/message`), consistent
with the SSE decision already made for borrower notifications in ADR-0049.

## Alternatives Considered
1. **Bespoke REST + hand-written OpenAPI, consumed per agent framework.**
   Works, but every agent integration must be told about the endpoints
   out-of-band, and each framework (function-calling, tool-use) needs its own
   glue. No standard discovery. Rejected: high per-integration cost, no
   standardization.
2. **MCP transport via stdio.** The stdio transport is ideal for local,
   single-process agent tooling (e.g., a desktop assistant spawning the server
   as a subprocess). It does not fit a networked, multi-client, containerized
   service. Rejected for this service.
3. **MCP transport via WebSocket.** Full-duplex, but the agent↔tool interaction
   is request/response plus server-initiated streaming, which SSE serves with
   less operational surface. WebSocket is stateful, needs a distinct handshake,
   and complicates load balancing, auth, and edge (Cloudflare) traversal.
   Rejected: over-engineered for the interaction shape.
4. **MCP transport via streamable-HTTP (newer spec).** A valid forward option;
   deferred to keep transport consistent with ADR-0049's SSE choice and the
   current Spring AI 1.0.x WebMVC starter. Revisit when the toolchain and
   clients standardize on it.
5. **Upgrade to Spring Boot 4.0 + Spring AI 2.0.** The current Spring AI GA
   (2.0) requires Spring Boot 4.x — a major, breaking upgrade across all
   services. Rejected for now: disproportionate risk for the goal; Spring AI
   1.0.4 delivers full MCP server support on a minor 3.3→3.4 bump.

## Rationale
- **Reuse, not reimplementation.** The tool is an adapter; all pricing logic,
  the existing Redis caching, and tests remain in `QuotePricingService`. The agent
  contract is decoupled from internals via a dedicated result record.
- **Standardized discovery.** MCP advertises typed tool schemas that any
  compatible agent discovers automatically, versus spoon-feeding each framework
  a bespoke REST contract.
- **Correctness and safety are preserved** because the same engine, with the
  same guardrails, answers the call. This positions later work (ADR-0053) to
  add auth scoping and rate limiting at the agent boundary.
- **SSE transport** rides existing HTTP infrastructure (load balancers, auth,
  Cloudflare, auto-reconnect) and avoids holding a database connection or
  transaction open across a long-lived socket, keeping DB access request-scoped
  (the accurate reason SSE fits here — it is a connection-pool and
  operational-simplicity benefit, not a deadlock guarantee).
- **Minimal blast radius.** Only `pricing-service` moves to Boot 3.4.9; the
  other services and `harbor-api` remain on 3.3.5.

## Consequences
- `pricing-service` now serves an agent-facing surface. The MCP endpoint must be
  secured and rate-limited before any non-local exposure — done in ADR-0053
  (API-key auth + Bucket4j rate limiting, scoped to the MCP paths).
- A new dependency surface (Spring AI + MCP SDK 0.10.0) is introduced in one
  service; version alignment with Spring Boot must be maintained on upgrades.
- Tool descriptions are effectively part of the public contract for agents and
  should be reviewed like API docs, since the LLM relies on them to decide when
  and how to call a tool.
- Future tools extend the same provider with no new wiring. (Follow-on:
  `listLoanPrograms` and `getLoanProgramDetails` were added over the pricing
  product catalog, registered through the same `ToolCallbackProvider`.)

# ADR 0054: MCP Client Demo — an LLM Agent Calling the Pricing Tools

## Status
Accepted

## Date
2026-07-09

## Phase
4 — AI Agent Integration

## Context
ADR-0051 and ADR-0053 built the server half of the agent story: pricing-service
exposes its engine as secured, rate-limited MCP tools. That proves an agent *can*
call the pricing engine, but nothing in the repo *demonstrates* it — the payoff
("an LLM autonomously calls the real engine instead of hallucinating a rate") was
untold.

This ADR covers the client half: a runnable demo where a real model discovers the
MCP tools and calls them to answer a natural-language mortgage question. Three
choices had to be made: which model runtime, how the client speaks MCP, and how
credentials are handled.

## Decision
Add `mcp-client-demo/` — a standalone client that runs the loop:
discover tools (MCP `tools/list`) → expose them to the model as function-calling
tools → let the model choose and call them → execute via MCP `tools/call` against
the real engine → model composes the answer from real results.

1. **LLM access: the OpenAI Chat Completions standard, provider-agnostic.** The
   client calls `{LLM_BASE_URL}/chat/completions` with `Authorization: Bearer
   <LLM_API_KEY>` and a `model` — the three-part OpenAI contract. The vendor is
   just the base URL, so the same code runs against local Ollama
   (`http://localhost:11434/v1`), Ollama Cloud (`https://ollama.com/v1`), OpenAI,
   Groq, Together, vLLM, OpenRouter, etc. by changing three env vars. This mirrors
   the vendor-neutrality principle already used for observability (ADR-0030):
   emit/consume a standard, make the provider configuration. The default is local
   Ollama; `*-cloud` models work after `ollama signin` (the daemon handles cloud
   auth, so any `LLM_API_KEY` value works locally).
2. **Client transport: the raw MCP SSE protocol in Python standard library.** A
   background thread reads the SSE event stream and correlates JSON-RPC responses
   to requests by id; requests are POSTed to the session message endpoint. No pip
   dependencies.
3. **Credentials: never handled by the client, fully env-driven.** Cloud auth is
   done once by the user via `ollama signin` (browser); the local daemon holds the
   token. All config (base URL, model, and the MCP API key) comes from environment
   variables, loaded from a local `.env` (gitignored) for convenience but always
   overridable by a real env var — so deployment (Oracle Cloud) just sets env vars.
   The `MCP_API_KEY` is **required with no default**: nothing insecure is baked in,
   and the client exits with guidance if it is unset. A committed `.env.example`
   documents every variable.

## Alternatives Considered
1. **Bind to one vendor's native SDK/API (e.g. Ollama's `/api/chat`, or Anthropic's
   Messages API).** Simplest for that one provider, but locks the client to it and
   needs a rewrite to switch. Rejected in favor of the OpenAI Chat Completions
   standard, which every major provider (and Ollama itself, at `/v1`) speaks — the
   client stays vendor-neutral. Local Ollama via `ollama signin` still works through
   `localhost:11434/v1` with a dummy key, so the keyless-local convenience is kept
   without giving up portability.
2. **The official MCP Python/TypeScript SDK.** More idiomatic and less code, but it
   pulls a dependency tree (pydantic-core etc.) that needs a build toolchain, which
   was friction in this environment. Implementing the SSE transport directly also
   *documents* the protocol, which has teaching value here. Revisit if the demo
   grows.
3. **A local small model only (no cloud).** Zero cost and fully offline, but the
   smallest tool-capable local models reason less reliably about multi-tool calls.
   Cloud models make the demo robust; local remains a one-line fallback.
4. **Bake the demo into a service / expose a chat endpoint.** Over-scoped for the
   goal (prove the agent path). A script is the smallest thing that demonstrates it
   and is trivial to read in an interview.

## Rationale
- **Proves the whole loop with the real engine.** The model's knowledge of what it
  can do comes entirely from MCP discovery; the numbers come from the real
  `QuotePricingService` (JDBC + cache), not the model. Adding a tool server-side
  surfaces it in the demo with no client change.
- **Secure by construction.** No secret in the client; the demo is still subject to
  the same API-key + rate-limit boundary (ADR-0053) a third-party agent would hit.
- **Provider-neutral and reproducible.** Swap `OLLAMA_MODEL` between a cloud and a
  local model with one env var; stdlib-only means `python3 harbor_agent_demo.py`
  just runs.

## Consequences
- The repo now has a runnable, screenshot-able demonstration of the agent-facing
  value, closing the loop opened by ADR-0051.
- Verified end to end: `gpt-oss:120b-cloud` discovered the three tools, called
  `listLoanPrograms` and `getRateQuote` (arguments extracted from the
  natural-language prompt), and answered from the engine's real numbers.
- The demo depends on the pricing-service default-config fix (starting with
  `app.kafka.enabled=false`); without it the MCP-only run would not boot.
- Because the transport is hand-rolled, protocol changes (e.g. the newer
  streamable-HTTP MCP transport) would require updating the client; noted as a
  future option alongside adopting the MCP SDK.

# Harbor MCP Client Demo

An LLM agent that calls Harbor's pricing engine through the **Model Context
Protocol (MCP)**. pricing-service exposes its pricing logic as MCP tools
(ADR-0051, ADR-0053); this client lets a real model discover and call them to
answer a natural-language question — the agent reasons, the engine prices.

## What it demonstrates

```
  natural-language question
          │
          ▼
   Ollama model  ──(1) tools/list schemas──►  discovers what it can do
          │
          ├─(2) decides to call getRateQuote / listLoanPrograms
          ▼
   this client ──(3) MCP tools/call──►  pricing-service (REAL engine, JDBC, cache)
          │                                   │
          │◄──────── real numbers ────────────┘
          ▼
   Ollama model ──(4) final answer from real data (no hallucinated rates)
```

The model's knowledge of *what it can do* comes entirely from MCP discovery, not
from hard-coded endpoints. Add a tool to `MortgagePricingTools` and it shows up
here with no client change.

## Prerequisites

1. **pricing-service running with the MCP gateway on**, reachable at
   `MCP_BASE_URL` (default `http://localhost:8084`):
   ```bash
   cd pricing-service
   HARBOR_MCP_ENABLED=true HARBOR_MCP_API_KEYS=test-key-123 HARBOR_MCP_RPM=1000 \
     DB_URL='jdbc:mysql://localhost:3307/mortgage_pricing?...' \
     mvn spring-boot:run
   ```
   (needs MySQL + Redis from `docker compose up -d mysql redis`.)

2. **Ollama** with a tool-capable model. Two options:
   - **Ollama Cloud** (used here): `ollama signin`, then reference a `*-cloud`
     model, e.g. `gpt-oss:120b-cloud`. The local Ollama daemon proxies the cloud
     model on `localhost:11434` after sign-in — this client never handles an API
     key.
   - **Local model**: `ollama pull llama3.1:8b` (or any tool-capable model) and
     set `OLLAMA_MODEL=llama3.1:8b`.

## Configuration (environment variables)

All config is env-driven. Locally, copy the template and edit it; on a deployment
(Oracle Cloud, a container, systemd) set the same names as real environment
variables — a real env var always overrides the file.

```bash
cp .env.example .env    # then edit .env  (.env is gitignored)
```

| Var | Default | Meaning |
|---|---|---|
| `MCP_BASE_URL` | `http://localhost:8084` | pricing-service base URL (its public URL when deployed) |
| `MCP_API_KEY` | **required** | `X-API-Key` for the MCP gateway (ADR-0053); must match a `HARBOR_MCP_API_KEYS` value. No default — it's a credential. |
| `OLLAMA_URL` | `http://localhost:11434` | Ollama server |
| `OLLAMA_MODEL` | `gpt-oss:120b-cloud` | any tool-capable local or `*-cloud` model |

## Run

```bash
python3 harbor_agent_demo.py
# or ask your own question:
python3 harbor_agent_demo.py "What would a $750k home with 20% down cost me on a 15-year FHA loan in 33101?"
```

If `MCP_API_KEY` isn't set (via `.env` or the environment), the client exits with a
message telling you how to set it — nothing insecure is baked in as a fallback.

## Notes

- Pure Python **standard library** — no pip install. The MCP SSE transport is
  implemented directly (background thread reads the event stream, requests are
  correlated to responses by JSON-RPC id), which also documents the raw protocol.
- Security: the MCP gateway requires the API key and rate-limits per key
  (ADR-0053), so this client is subject to the same boundary a third-party agent
  would be.

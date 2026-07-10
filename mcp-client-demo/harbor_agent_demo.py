#!/usr/bin/env python3
"""
Harbor MCP client demo — an LLM agent calls the pricing engine's tools.

This is the other half of the MCP story. pricing-service exposes its pricing
engine as MCP tools (ADR-0051); this script is a client that lets a real model
*discover and call* those tools to answer a natural-language question.

Flow:
  1. Open an MCP session to pricing-service over SSE and run the handshake.
  2. tools/list -> get the typed tool schemas the server advertises.
  3. Hand those schemas to the model as OpenAI function-calling tools.
  4. The model decides which tool(s) to call; we execute each via MCP tools/call
     against the REAL pricing engine and feed the result back.
  5. The model composes a final answer from the real numbers.

No pricing logic lives here — the model reasons, the engine prices. The point is
that the agent's knowledge of "what it can do" comes entirely from MCP discovery,
not hard-coding.

Provider-agnostic: the LLM is reached over the **OpenAI Chat Completions standard**
(`{base_url}/chat/completions`, `Authorization: Bearer <api_key>`, `model`). Any
OpenAI-compatible provider works by changing three env vars — local Ollama, Ollama
Cloud, OpenAI, Groq, Together, vLLM, OpenRouter, etc. The vendor is just the base URL.

Dependencies: Python 3 standard library only (urllib, json, threading). No pip.

Config — all via environment variables (loaded from a local .env if present, but a
real env var always wins, so deployment just sets env vars):
  MCP_BASE_URL   pricing-service base             (default http://localhost:8084)
  MCP_API_KEY    X-API-Key for the MCP gateway     (REQUIRED — no default; credential)
  LLM_BASE_URL   OpenAI-compatible base URL        (default http://localhost:11434/v1)
  LLM_API_KEY    bearer key for the LLM provider   (default "ollama"; real key elsewhere)
  LLM_MODEL      model name                        (default gpt-oss:120b-cloud)

  Examples (base_url / model):
    local Ollama    http://localhost:11434/v1   gpt-oss:120b-cloud   (key: anything; `ollama signin` for cloud models)
    Ollama Cloud    https://ollama.com/v1       gpt-oss:120b-cloud   (key: your Ollama Cloud key)
    OpenAI          https://api.openai.com/v1   gpt-4o               (key: your OpenAI key)
    Groq            https://api.groq.com/openai/v1  llama-3.3-70b-versatile

Setup:
  cp .env.example .env    # then edit; or set the same vars in the environment
Usage:
  python3 harbor_agent_demo.py ["your question about a mortgage"]
"""

import json
import os
import sys
import threading
import time
import urllib.request


def _load_dotenv():
    """
    Load KEY=VALUE lines from a .env file next to this script into the environment,
    without overriding variables that are already set. This keeps config out of the
    code: locally, copy .env.example -> .env; on a deployment (e.g. Oracle Cloud),
    set real environment variables and skip the file entirely (real env wins).
    """
    path = os.path.join(os.path.dirname(os.path.abspath(__file__)), ".env")
    if not os.path.exists(path):
        return
    with open(path) as handle:
        for line in handle:
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, _, value = line.partition("=")
            # setdefault: a real environment variable always takes precedence.
            os.environ.setdefault(key.strip(), value.strip().strip('"').strip("'"))


_load_dotenv()

# MCP server (the pricing engine's agent boundary).
MCP_BASE = os.environ.get("MCP_BASE_URL", "http://localhost:8084")
MCP_KEY = os.environ.get("MCP_API_KEY")  # required; no default — it is a credential

# LLM provider, addressed via the OpenAI Chat Completions standard. Swap providers
# by changing these three — nothing else in the code is provider-specific.
LLM_BASE_URL = os.environ.get("LLM_BASE_URL", "http://localhost:11434/v1")
LLM_API_KEY = os.environ.get("LLM_API_KEY", "ollama")
LLM_MODEL = os.environ.get("LLM_MODEL", "gpt-oss:120b-cloud")

DEFAULT_PROMPT = (
    "I'm buying a $500,000 home with $100,000 down, and I'd take a conventional "
    "30-year loan. The property ZIP is 89101 and it's my primary residence. What "
    "rate and monthly payment might I get? Also, what loan programs do you offer?"
)


class McpSseClient:
    """
    A minimal MCP client over the SSE transport.

    The SSE transport is asymmetric: the client POSTs JSON-RPC requests to the
    message endpoint, but the server's responses arrive asynchronously on the
    long-lived GET /sse event stream. So we read that stream on a background
    thread and correlate responses to requests by their JSON-RPC id.
    """

    def __init__(self, base, api_key):
        self.base = base
        self.api_key = api_key
        self.message_url = None
        self._responses = {}
        self._cv = threading.Condition()
        self._ready = threading.Event()
        self._next_id = 1

    def connect(self, timeout=10):
        threading.Thread(target=self._read_stream, daemon=True).start()
        if not self._ready.wait(timeout):
            raise RuntimeError("MCP: never received the SSE 'endpoint' event "
                               "(is pricing-service up with HARBOR_MCP_ENABLED=true?)")

    def _read_stream(self):
        req = urllib.request.Request(
            self.base + "/sse",
            headers={"X-API-Key": self.api_key, "Accept": "text/event-stream"},
        )
        resp = urllib.request.urlopen(req)
        event = None
        for raw in resp:
            line = raw.decode("utf-8").rstrip("\n")
            if line.startswith("event:"):
                event = line[len("event:"):].strip()
            elif line.startswith("data:"):
                data = line[len("data:"):].strip()
                if event == "endpoint":
                    # The server tells us where to POST for this session.
                    self.message_url = self.base + data
                    self._ready.set()
                elif event == "message":
                    try:
                        msg = json.loads(data)
                    except json.JSONDecodeError:
                        continue
                    if "id" in msg:
                        with self._cv:
                            self._responses[msg["id"]] = msg
                            self._cv.notify_all()

    def _post(self, payload):
        req = urllib.request.Request(
            self.message_url,
            data=json.dumps(payload).encode(),
            headers={"X-API-Key": self.api_key, "Content-Type": "application/json"},
        )
        urllib.request.urlopen(req).read()

    def request(self, method, params=None, timeout=30):
        rid = self._next_id
        self._next_id += 1
        self._post({"jsonrpc": "2.0", "id": rid, "method": method, "params": params or {}})
        deadline = time.time() + timeout
        with self._cv:
            while rid not in self._responses:
                remaining = deadline - time.time()
                if remaining <= 0 or not self._cv.wait(timeout=remaining):
                    raise TimeoutError(f"MCP: no response for '{method}'")
            return self._responses.pop(rid)

    def notify(self, method, params=None):
        self._post({"jsonrpc": "2.0", "method": method, "params": params or {}})

    def initialize(self):
        self.request("initialize", {
            "protocolVersion": "2024-11-05",
            "capabilities": {},
            "clientInfo": {"name": "harbor-agent-demo", "version": "1.0"},
        })
        self.notify("notifications/initialized")

    def list_tools(self):
        return self.request("tools/list")["result"]["tools"]

    def call_tool(self, name, arguments):
        result = self.request("tools/call", {"name": name, "arguments": arguments})["result"]
        # MCP returns content parts; our tools return a single text part of JSON.
        if result.get("content"):
            return result["content"][0].get("text", json.dumps(result))
        return json.dumps(result)


def mcp_tools_to_openai(tools):
    """
    Translate MCP tool descriptors into OpenAI function-calling tool specs. MCP's
    inputSchema is already JSON Schema, which is exactly what the OpenAI 'parameters'
    field expects — so this is a straight remap, no schema translation.
    """
    return [
        {
            "type": "function",
            "function": {
                "name": t["name"],
                "description": t.get("description", ""),
                "parameters": t.get("inputSchema") or {"type": "object", "properties": {}},
            },
        }
        for t in tools
    ]


def llm_chat(messages, tools):
    """One turn against any OpenAI-compatible Chat Completions endpoint."""
    payload = {"model": LLM_MODEL, "messages": messages, "tools": tools, "stream": False}
    req = urllib.request.Request(
        LLM_BASE_URL.rstrip("/") + "/chat/completions",
        data=json.dumps(payload).encode(),
        headers={
            "Content-Type": "application/json",
            "Authorization": f"Bearer {LLM_API_KEY}",
        },
    )
    response = json.loads(urllib.request.urlopen(req, timeout=180).read())
    return response["choices"][0]["message"]


def main():
    if not MCP_KEY:
        sys.exit(
            "MCP_API_KEY is not set.\n"
            "  Local: copy .env.example to .env and fill it in.\n"
            "  Deploy: set MCP_API_KEY (and MCP_BASE_URL, LLM_BASE_URL, LLM_API_KEY, "
            "LLM_MODEL) as environment variables.\n"
            "It must match one of pricing-service's HARBOR_MCP_API_KEYS."
        )

    prompt = " ".join(sys.argv[1:]).strip() or DEFAULT_PROMPT
    print(f"\n{'='*70}\nLLM: {LLM_MODEL} @ {LLM_BASE_URL}\nMCP: {MCP_BASE}\n{'='*70}")
    print(f"\n[USER]\n{prompt}\n")

    mcp = McpSseClient(MCP_BASE, MCP_KEY)
    mcp.connect()
    mcp.initialize()
    tools = mcp.list_tools()
    print(f"[MCP] discovered tools: {[t['name'] for t in tools]}\n")

    openai_tools = mcp_tools_to_openai(tools)
    messages = [{"role": "user", "content": prompt}]

    for _turn in range(6):
        message = llm_chat(messages, openai_tools)
        messages.append(message)  # keep the assistant turn (incl. any tool_calls) in context

        tool_calls = message.get("tool_calls") or []
        if not tool_calls:
            print(f"[AGENT — final answer]\n{(message.get('content') or '').strip()}\n")
            return

        for call in tool_calls:
            fn = call["function"]
            name = fn["name"]
            # OpenAI sends tool arguments as a JSON string.
            args = fn.get("arguments") or "{}"
            if isinstance(args, str):
                args = json.loads(args)
            print(f"  --> agent calls  {name}({json.dumps(args)})")
            result = mcp.call_tool(name, args)
            print(f"  <-- engine returns  {result}\n")
            # OpenAI tool results must carry the tool_call_id they answer.
            messages.append({"role": "tool", "tool_call_id": call["id"], "content": result})

    print("[demo] hit the turn limit without a final answer")


if __name__ == "__main__":
    main()

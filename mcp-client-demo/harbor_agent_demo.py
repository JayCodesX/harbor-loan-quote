#!/usr/bin/env python3
"""
Harbor MCP client demo — an LLM agent calls the pricing engine's tools.

This is the other half of the MCP story. pricing-service exposes its pricing
engine as MCP tools (ADR-0051); this script is a client that lets a real model
*discover and call* those tools to answer a natural-language question.

Flow:
  1. Open an MCP session to pricing-service over SSE and run the handshake.
  2. tools/list -> get the typed tool schemas the server advertises.
  3. Hand those schemas to an Ollama model as function-calling tools.
  4. The model decides which tool(s) to call; we execute each via MCP tools/call
     against the REAL pricing engine and feed the result back.
  5. The model composes a final answer from the real numbers.

No pricing logic lives here — the model reasons, the engine prices. The point is
that the agent's knowledge of "what it can do" comes entirely from MCP discovery,
not hard-coding.

Dependencies: Python 3 standard library only (urllib, json, threading). No pip.

Config (all via env, with sane local defaults):
  MCP_BASE_URL   pricing-service base            (default http://localhost:8084)
  MCP_API_KEY    X-API-Key for the MCP gateway    (default test-key-123)
  OLLAMA_URL     local Ollama server              (default http://localhost:11434)
  OLLAMA_MODEL   model name; a *-cloud tag works  (default gpt-oss:120b-cloud)
                 after `ollama signin`. Secrets are handled by the local Ollama
                 daemon after signin — this script never sees an API key.

Usage:
  python3 harbor_agent_demo.py ["your question about a mortgage"]
"""

import json
import os
import sys
import threading
import time
import urllib.request

MCP_BASE = os.environ.get("MCP_BASE_URL", "http://localhost:8084")
MCP_KEY = os.environ.get("MCP_API_KEY", "test-key-123")
OLLAMA_URL = os.environ.get("OLLAMA_URL", "http://localhost:11434")
OLLAMA_MODEL = os.environ.get("OLLAMA_MODEL", "gpt-oss:120b-cloud")

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


def mcp_tools_to_ollama(tools):
    """Translate MCP tool descriptors into Ollama function-calling tool specs."""
    specs = []
    for t in tools:
        specs.append({
            "type": "function",
            "function": {
                "name": t["name"],
                "description": t.get("description", ""),
                "parameters": t.get("inputSchema") or {"type": "object", "properties": {}},
            },
        })
    return specs


def ollama_chat(messages, tools):
    payload = {"model": OLLAMA_MODEL, "messages": messages, "tools": tools, "stream": False}
    req = urllib.request.Request(
        OLLAMA_URL + "/api/chat",
        data=json.dumps(payload).encode(),
        headers={"Content-Type": "application/json"},
    )
    return json.loads(urllib.request.urlopen(req, timeout=180).read())


def main():
    prompt = " ".join(sys.argv[1:]).strip() or DEFAULT_PROMPT
    print(f"\n{'='*70}\nMODEL: {OLLAMA_MODEL}   MCP: {MCP_BASE}\n{'='*70}")
    print(f"\n[USER]\n{prompt}\n")

    mcp = McpSseClient(MCP_BASE, MCP_KEY)
    mcp.connect()
    mcp.initialize()
    tools = mcp.list_tools()
    print(f"[MCP] discovered tools: {[t['name'] for t in tools]}\n")

    ollama_tools = mcp_tools_to_ollama(tools)
    messages = [{"role": "user", "content": prompt}]

    for _turn in range(6):
        response = ollama_chat(messages, ollama_tools)
        message = response["message"]
        messages.append(message)

        tool_calls = message.get("tool_calls") or []
        if not tool_calls:
            print(f"[AGENT — final answer]\n{message.get('content', '').strip()}\n")
            return

        for call in tool_calls:
            fn = call["function"]
            name = fn["name"]
            args = fn.get("arguments", {})
            if isinstance(args, str):
                args = json.loads(args)
            print(f"  --> agent calls  {name}({json.dumps(args)})")
            result = mcp.call_tool(name, args)
            print(f"  <-- engine returns  {result}\n")
            messages.append({"role": "tool", "content": result, "tool_name": name})

    print("[demo] hit the turn limit without a final answer")


if __name__ == "__main__":
    main()

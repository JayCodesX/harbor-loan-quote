# ADR 0049: Real-Time Borrower Notification via Server-Sent Events (SSE)

## Status
Proposed

## Date
2026-04-01

## Phase
2 — Pricing Engine

---

## Context

The platform operates as a lead aggregator with real-time pricing as the draw. A borrower submits an initial quote request and receives a fast response — an early estimate of what a loan will cost, with no commitment required. That response is delivered synchronously and requires no persistent channel.

If the borrower continues to refine their quote, the system provides accurate pricing, matched lenders, and real estate agents to work with. At this stage, ongoing notifications become important: if the rate is not locked, the borrower needs to know when the quoted rate has changed, when it is no longer active, or when it is approaching expiry — all of which affect their ability to lock at the right moment.

These notifications happen after the initial response, at unpredictable times driven by investor rate sheet updates and market conditions. They require a persistent channel back to the browser — a connection that stays open so the server can push events without the borrower having to poll. notification-service owns this channel: it holds open SSE connections from borrower browsers, receives internal events from pricing-service via message broker, and pushes them to connected borrowers. If a borrower has no active connection when an event fires, the fallback is email.

---

## Decision

notification-service uses Server-Sent Events (SSE) to push real-time updates to connected borrowers — rate changes, lock status, and expiry alerts. notification-service owns all open browser connections. If a borrower has no active SSE connection when an event fires, the system falls back to email delivery. Quote result delivery is not SSE — that is a synchronous HTTP response from harbor-api.

---

## Alternatives Considered

### Alternative: WebSockets
**Strengths:** Full-duplex bidirectional channel, lower latency for high-frequency updates, widely supported
**Rejected because:**
Borrower notifications are unidirectional — the server pushes rate status and updates to the borrower, and the borrower has nothing to send back. Full-duplex is unnecessary complexity for a read-only stream. SSE is HTTP/1.1 and HTTP/2 compatible, works through standard proxies and load balancers without special configuration, and has built-in reconnection and event ID tracking. WebSocket requires an upgrade handshake that some proxies block and client-side reconnection logic that must be implemented manually. Spring WebFlux has first-class SSE support via `Flux<ServerSentEvent>` — WebSocket in WebFlux is lower-level and requires significantly more boilerplate. SSE is the right tool for this use case.

### Alternative: Short polling (client polls an endpoint every N seconds)
**Strengths:** Simplest to implement, stateless server, works everywhere, no persistent connection
**Rejected because:**
Polling consistently hits an endpoint regardless of whether anything has changed — most requests return nothing, creating unnecessary load on the system. Under concurrent borrower sessions this compounds quickly. And even with frequent polling there is still a staleness window: a rate change published between poll cycles is not surfaced until the next request fires. SSE eliminates both problems — the connection is open, the update arrives the moment it is published, and no wasted requests are made in between.

### Alternative: Push notifications (Web Push API / FCM)
**Strengths:** Works when browser tab is closed, reaches users who are not actively on the page
**Rejected because:**
The target use case is in-session — the borrower is actively on the platform comparing rates. Web Push adds significant implementation complexity (service worker registration, VAPID keys, push subscription management per device, browser permission prompts) for a capability that is not required in Phase 1. Out-of-browser notification is a Phase 3+ enhancement. For closed-session alerts, email is the fallback and is already sufficient. Web Push is deferred per ADR-0027.

### Alternative: Email only (no real-time channel)
**Strengths:** Zero infrastructure complexity, works for all users, persistent record of notification
**Rejected because:**
Rates change in real-time. An email can take minutes to deliver and the borrower may not see it for an hour — by then the rate window may have closed and the opportunity to lock is gone. The core value proposition of the platform is real-time pricing; email latency eliminates that value for in-session borrowers. Email is the right fallback when a borrower has no active SSE connection. It is not a replacement for real-time delivery.

---

## Rationale

### SSE Matches the Notification Domain's Unidirectional, Low-Frequency Requirements

Rate alerts fire 1-4 times per day — this is not a high-frequency trading feed. SSE is right-sized for this pattern: it doesn't tax the system with polling, it doesn't over-complicate delivery the way Web Push does, and idle open connections are cheap on WebFlux's non-blocking event loop. The connection sits open and quiet until a notification arrives, at which point it pushes and goes quiet again. No wasted requests, no unnecessary infrastructure, no complexity the use case doesn't justify.

### notification-service's WebFlux Stack Is Built for Persistent Connections

Rate changes originate in pricing-service, not harbor-api. pricing-service publishes the `RateSheetActivated` event, notification-service consumes it and pushes to connected borrowers. harbor-api is not in that flow.

More importantly, harbor-api runs Spring MVC with thread-per-request. Holding thousands of idle SSE connections in Spring MVC would exhaust the thread pool — each open connection holds a thread even when nothing is being sent. notification-service runs WebFlux on Netty's non-blocking event loop, where idle SSE connections are cheap `Flux` subscriptions. No thread is blocked waiting for an event to arrive. The connection count ceiling is memory, not threads. This is the core reason notification-service exists as a separate service. See ADR-0001 (service decomposition) and ADR-0004 (Spring MVC vs WebFlux).

### Graceful Fallback to Email Preserves Notification Reliability

Not every borrower will have an active SSE connection when a rate change fires — they may have closed the tab, lost their internet connection, or stepped away. Email is the safety net: notification-service attempts SSE delivery first, and if no active connection exists for that borrower, an email is queued. The borrower still gets notified regardless of their connection state.

Email is not a fallback in the sense of being inferior — it is the right channel for closed-session delivery. A borrower who reconnects later will pick up the SSE stream again via built-in SSE reconnection; email ensures they are not left uninformed in the gap.

---

## Consequences

### Positive

- The system has a real-time notification framework that delivers rate changes, lock status, and expiry alerts to borrowers the moment they occur — as close to real-time as the browser allows. Email as a fallback ensures no borrower is left uninformed if their connection drops.

### Negative

- notification-service is a third service to build, deploy, and maintain. It runs a different programming model (WebFlux) from the other two services, which adds team complexity.
- The email fallback has inherent latency limitations. If a borrower loses connection and rates expire before they see the email, the pricing window has closed and they cannot lock at the quoted rate. The system cannot fully protect against this — it can only notify as fast as the channel allows.

---

## Follow-up
- **Borrower-to-connection mapping.** notification-service must maintain an in-memory mapping of `borrower_id` to active `Flux` subscriptions to know which connections to push to when an event arrives.
- **SSE reconnection on service restart.** When notification-service restarts all SSE connections drop. Clients reconnect automatically via SSE's built-in reconnection using `Last-Event-ID` — define what event IDs are issued and how missed events are replayed or handled.
- **Event schema.** Define the JSON payload structure for SSE messages: event type field (rate change, lock status, expiry alert), rate sheet ID, and relevant pricing data.
- **ADR-0026 (rate alert subscription model).** Defines which borrowers are subscribed to which alert types — notification-service uses this to filter who receives which events.
- **ADR-0048 (dynamic rate sheet update strategy).** The upstream source of `RateSheetActivated` events that notification-service consumes.
- **ADR-0027 (notification delivery channels).** Defines the email fallback behavior when no active SSE connection exists.

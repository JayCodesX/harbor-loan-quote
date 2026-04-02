# ADR 0027: Notification Delivery Channels — SSE vs. Email vs. Web Push

## Status
Archived — valid, pending notification service buildout

Notification channel selection (SSE, polling, etc.) will be
finalized as the notification service is built out.

## Date
Fill in when you write this

## Phase
3 — Scale and Operations

---

## Context

WRITE THIS YOURSELF

Prompts to answer:
- What types of notifications does the system need to send?
  (1. Quote status updates: real-time while borrower has the page open
   2. Rate alerts: "your saved rate threshold has been crossed"
   3. Lead confirmation: "your inquiry has been sent to [lender]"
   4. Admin alerts: "rate sheet is stale" or "DLQ has unprocessed messages")
- What is SSE and when does it apply?
  (Server-Sent Events: push to an open browser session. Only works while the tab is open.)
- What is Web Push?
  (Browser Push API: delivers notifications even when the browser is closed,
   via a service worker. Requires VAPID keys, service worker registration, user permission.)
- What is the existing notification-service architecture?
  (WebFlux SSE for quote status updates and rate alert live display. Redis for snapshots.
   Email via subscription management for marketing communications.)
- When does email make sense vs. SSE?
  (SSE: immediate, real-time, in-session notifications.
   Email: asynchronous, persistent record, works when the browser is closed.
   A rate alert should use SSE if the browser is open AND email as a fallback
   when the browser is closed.)


---

## Decision

WRITE THIS YOURSELF

What is the decision? Be concrete — a sentence or two that could stand alone.
Describe the *what*, not the *why* (why belongs in Rationale).

---

## Alternatives Considered

### Alternative: Web Push for rate alerts
**Strengths:** Delivers to closed browsers, native mobile feel, works without email
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- Web Push requires service worker registration and user permission prompts.
  Permission prompt conversion rates are typically 20-40% — most users decline.
  Email has near-100% reachability (they already provided their email).
- Service worker implementation adds frontend complexity.
- The use case (rate alert) is not time-critical enough to require push notification UX.
  Email arriving within minutes of a rate change is sufficient for the "alert me when rates drop" use case.
- Web Push is a Phase 4+ consideration when there is a mobile app and a proven user base
  that values push notification delivery.

### Alternative: WebSocket instead of SSE
**Strengths:** Bidirectional, standard protocol, better browser support for some edge cases
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- Quote status updates and rate alerts are server-to-client only — there is no
  client-to-server message needed over the same channel.
- SSE is simpler for unidirectional server push and has better HTTP/2 multiplexing behavior.
- WebSocket requires a custom protocol layer; SSE is plain HTTP text/event-stream.
- The existing notification-service already implements SSE. Replacing it with WebSocket
  is additional work for no functional benefit given the unidirectional use case.

### Alternative: SMS delivery for rate alerts
**Strengths:** High open rate, works on any phone without app or browser
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- SMS delivery (Twilio or similar) has a per-message cost that accumulates at scale.
- TCPA compliance for SMS marketing/alerts requires explicit written consent,
  a compliant opt-in flow, and an opt-out mechanism.
  This is non-trivial to implement correctly and has legal exposure if done wrong.
- For Phase 3, email is sufficient. SMS can be added as a user preference option
  once the compliance requirements are properly scoped.

---

## Rationale

### Channel Selection Matches Notification Urgency and Delivery Guarantee Requirements

WRITE THIS YOURSELF
SSE + Email as fallback is the right model because:
- When the browser is open, SSE provides instant delivery (sub-second)
- When the browser is closed, email provides reliable delivery with a persistent record
- The combination covers all user states without requiring user permission prompts
  (they already have an account with an email address)
This is the same model used by Google Alerts, GitHub notifications, and similar services.

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

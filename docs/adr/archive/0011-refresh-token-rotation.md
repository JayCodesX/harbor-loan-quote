# ADR 0011: Refresh Token Rotation and Reuse Detection

## Status
Superseded

Auth functionality has been folded into the Loan API service as part
of the 3-service consolidation. Token handling is now internal to
the Loan API.

## Date
Fill in when you write this

## Phase
1 — Foundation

---

## Context

WRITE THIS YOURSELF

Prompts to answer:
- What does auth-service currently store for refresh tokens?
  (AuthRefreshToken entity persisted to mortgage_auth via AuthRefreshTokenRepository)
- What is a refresh token's purpose?
  (Long-lived credential that allows issuing new access tokens without re-entering credentials.
   Access tokens expire in 15 minutes (APP_USER_TOKEN_TTL_SECONDS=900).
   Refresh tokens allow silent renewal.)
- What is the risk of a non-rotating refresh token?
  (A stolen refresh token remains valid indefinitely until the user explicitly logs out.
   The attacker has permanent silent access.)
- What is token family tracking?
  (All refresh tokens issued from one login event share a family ID.
   If any token in the family is presented after being revoked,
   the entire family is invalidated — forcing a re-login.
   This detects token theft: legitimate user uses token A → token B issued.
   Attacker uses stolen token A → reuse detected → both A and B revoked.)
- Why does this matter specifically for a mortgage platform?
  (Quote history, borrower profiles, and lead data are financial PII.
   A stolen refresh token gives persistent silent access to this data.)


---

## Decision

WRITE THIS YOURSELF

What is the decision? Be concrete — a sentence or two that could stand alone.
Describe the *what*, not the *why* (why belongs in Rationale).

---

## Alternatives Considered

### Alternative: Non-rotating refresh tokens (current implied state)
**Strengths:** Simpler implementation — one token per session, no family tracking, no revocation chain
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- A refresh token that does not rotate is a permanent credential for the life of the session.
- Stolen from localStorage, a network trace, or a compromised device = permanent silent access.
- There is no detection mechanism. The legitimate user and the attacker both have valid tokens.
- For a mortgage platform handling financial PII, this is not an acceptable risk posture.

### Alternative: Short-lived refresh tokens without rotation
**Strengths:** Limits the exposure window — a stolen token expires quickly without rotation logic
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- Short-lived refresh tokens without rotation force frequent re-authentication.
  A 24-hour refresh token means borrowers re-login daily — poor UX.
- There is still a window of exposure equal to the token lifetime.
- Rotation with reuse detection provides stronger security AND better UX
  (longer token lifetime is safe because stolen tokens trigger detection).
- Short lifetime is not a substitute for rotation — it is a degraded version of it.

### Alternative: Stateless refresh tokens (no server-side storage)
**Strengths:** No database table, horizontally scalable without shared state, tokens are self-contained JWTs
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- Stateless refresh tokens cannot be revoked before expiry.
  If a user logs out or a token is compromised, the token remains valid until expiry.
- Reuse detection requires server-side state — you must know which tokens have been used.
  Stateless tokens make this impossible.
- The current auth-service already has AuthRefreshToken in the database.
  Stateful refresh tokens are already the design. This decision is about making them rotate.

---

## Rationale

### Rotation Limits the Theft Window to a Single Use

WRITE THIS YOURSELF
Without rotation: stolen token is valid forever (or until session max lifetime).
With rotation: stolen token can only be used once before it is superseded.
If the attacker uses the stolen token before the legitimate user:
  - Attacker gets a new token pair.
  - Legitimate user presents the original (now revoked) token.
  - Reuse detected → entire family revoked → both sessions terminated.
If the legitimate user uses their token first:
  - Token rotates. Attacker's copy is now revoked.
  - If attacker tries to use it: reuse detected → family revoked.
  - Legitimate user is forced to re-login — a minor inconvenience that signals compromise.

### Token Family Reuse Detection Is the Key Security Property

WRITE THIS YOURSELF
Without reuse detection, rotation alone is not sufficient.
An attacker who steals a refresh token can race to use it before the legitimate user.
If they win the race, they get a valid new token pair and the legitimate user is silently locked out.
With reuse detection:
  - Using a revoked token is detectable.
  - The response is total family revocation — both attacker and legitimate user are logged out.
  - The legitimate user re-authenticates and discovers their session was compromised.
  - The attacker loses access immediately.
This is the RFC 6819 refresh token rotation pattern.

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

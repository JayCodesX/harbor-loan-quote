# ADR 0010: Service-to-Service JWT Signing — HMAC vs. RSA Asymmetric

## Status
Accepted

## Date
2026-04-01

## Phase
1 — Foundation

---

## Context

Service-to-service JWTs are the internal authentication layer between services. When harbor-api calls pricing-service, the request carries a JWT that pricing-service verifies to confirm the call is genuinely from harbor-api. This prevents compromised services from hijacking internal calls or impersonating other services.

The current implementation uses HMAC-SHA256 — a shared secret where the same key is used to both sign and verify tokens. The security problem with this model is that any service holding the secret can not only verify tokens but also forge them. A compromised pricing-service or notification-service gives an attacker the full signing secret, which can then be used to mint tokens claiming any identity or scope — effectively unlocking every service in the system.

The correct trust model is: pricing-service should be able to verify that a JWT came from harbor-api, but it should not be able to sign new tokens or change their scope. Verify only, never sign. HMAC cannot enforce this boundary because signing and verifying use the same key. Asymmetric signing (RSA) can — the private key signs and stays with the issuer, the public key verifies and can be safely distributed to all services.


---

## Decision

Service-to-service JWTs are signed with RSA-2048 asymmetric keys. The issuing service holds the private key and signs tokens. Receiving services hold only the public key and can verify tokens but cannot forge them. No service other than the issuer holds signing capability.

---

## Alternatives Considered

### Alternative: Retain HMAC-SHA256 with secret rotation
**Strengths:** No migration required, simple implementation, fast signature verification
**Rejected because:**
Secret rotation doesn't fix the fundamental problem — every service still holds a key that can both sign and verify. A compromised service still gives an attacker full forgery capability until the next rotation window.

Rotation itself is operationally dangerous with HMAC. It requires all services to receive the new secret simultaneously. In a rolling deploy, services with the new secret sign tokens that services still on the old secret reject — a self-inflicted denial of service. Zero-downtime rotation is not achievable with a shared secret model.

### Alternative: ECDSA P-256 (instead of RSA-2048)
**Strengths:** Smaller key size (256-bit vs. 2048-bit), faster signature generation, equivalent security to RSA-2048, smaller JWT
**Rejected because:**
ECDSA P-256 is a legitimate alternative with equivalent security and better performance characteristics — smaller keys, faster signature generation, smaller JWT payload. The reason RSA-2048 was chosen over it is familiarity: Spring Security's `spring-security-oauth2-jose` has well-documented RSA key configuration examples, and RSA is more familiar from prior experience. If a team is comfortable with ECDSA P-256 configuration, it is a valid choice and arguably the better one.

### Alternative: Mutual TLS (mTLS) instead of JWTs
**Strengths:** Authentication is at the transport layer, no token expiry management, industry-standard service mesh authentication
**Rejected because:**
Certificate management in a Docker Compose environment is significant operational overhead. Certificates expire — missing a renewal takes a service down. Scaling pricing-service or notification-service to multiple instances means managing certificates per instance. Spring JWT-based service authentication is already implemented and working; replacing it with mTLS is a much larger migration than switching from HMAC to RSA asymmetric signing. mTLS is the right choice for a service mesh (Istio, Linkerd) at scale — not for a 3-service VPS deployment. It can be revisited in Phase 3 if the platform moves to ECS with a service mesh.

---

## Rationale

### Asymmetric Signing Enforces Correct Trust Boundaries

RSA produces two keys with different capabilities. The issuing service (harbor-api) holds the private key and uses it to sign tokens. Receiving services (pricing-service, notification-service) hold only the public key and use it to verify that a token was genuinely signed by the issuer — but they cannot forge a new one because they don't have the private key.

With HMAC, the same secret is used to sign and verify. Any service holding the secret can forge tokens. A compromised pricing-service gives an attacker the full signing secret and the ability to mint tokens claiming any identity or scope. With RSA, a compromised pricing-service gives an attacker only the public key — which every service already has and which is useless for forging. The security property is fundamentally different.

### JWKS Endpoint Enables Zero-Downtime Key Rotation

HMAC rotation causes broken auth during a rolling deploy — services still on the old secret reject tokens signed with the new one, creating a downtime window that requires a hard coordinated restart to resolve.

RSA with a JWKS endpoint solves this. The issuer publishes its public keys at `/.well-known/jwks.json`. Receiving services cache that list and refresh periodically. To rotate: generate a new key pair, publish both old and new public keys in the JWKS, and start signing new tokens with the new private key. Services update at their own pace on their next cache refresh. Old tokens continue to verify against the old public key — which stays in JWKS until all outstanding tokens have expired. Once all services are using the new tokens, the old public key is removed from JWKS. No hard restarts, no downtime, no coordinated deploy required.

---

## Consequences

### Positive

- A compromised receiving service (pricing-service, notification-service) gives an attacker only the public key — which is already distributed everywhere and useless for forgery. The attacker cannot mint new tokens, escalate scope, or impersonate harbor-api. The correct trust boundary holds even under breach.
- Zero-downtime key rotation via JWKS. New service instances (horizontal scaling) pick up the current public key on startup from the JWKS endpoint — no secret distribution, no coordinated deploy, no downtime window.

### Negative

- Key lifecycle management is a real operational responsibility. The team must maintain policy for: when to rotate, how long expired public keys stay in JWKS (until all outstanding tokens against them have expired), and when to remove them. Getting this wrong — removing an old public key too early — invalidates tokens that are still in flight.
- The key hierarchy must be maintained correctly. The private key must stay exclusively with the issuing service. If it is ever distributed to a receiving service — even accidentally — the forgery protection collapses. This requires deliberate operational discipline that HMAC, with its single shared secret, does not.

---

## Follow-up

- **HMAC → RSA migration plan.** Existing services use HMAC-SHA256 today. The migration does not require a full system takedown — JWKS enables a rolling cutover: generate the RSA key pair, stand up the JWKS endpoint on the issuer, update receiving services to verify against JWKS, switch the issuer to sign with RSA, and retire HMAC config once all services are verifying RSA. This plan needs to be formally defined and sequenced before the migration begins.
- **JWKS endpoint implementation.** The issuing service must expose `/.well-known/jwks.json` with its current public keys. This is a prerequisite for the migration and for zero-downtime rotation going forward.
- **Key storage policy.** Define where the RSA private key is stored and how it is injected at runtime (environment variable, secret manager, mounted volume). The private key must never appear in source control or be accessible to receiving services.

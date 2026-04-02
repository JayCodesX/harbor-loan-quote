# ADR 0034: Consent and Privacy Audit Log Durability

## Status
Accepted

## Date
2026-04-01

## Phase
1 — Foundation

---

## Context

The system captures consent from borrowers at the point of lead submission: TCPA consent for marketing contact, email opt-in/out, and consent to share their information with lenders.

Consent records are legal evidence. TCPA is federal law and states carry additional statutes on top of it. Violations carry per-violation fines ($500–$1,500 per call or text for willful violations). If a borrower disputes that they ever gave consent and the system cannot produce an unmodified record proving they did — with the IP address, timestamp, and consent language captured at the moment of consent — the company is liable. The inability to prove consent is not a technical failure; it is a legal one with direct financial and regulatory consequences.

Standard CRUD database tables allow UPDATE and DELETE. A consent record stored in a mutable table can be altered by application code, a bug, or anyone with database access. That is not a defensible evidence store. Consent records require an append-only model: created once, never modified or deleted, retained for the duration of the TCPA statute of limitations (4 years from date of consent or last contact, whichever is later).


---

## Decision

Consent records are stored in an append-only audit log table. Records are created once and cannot be modified or deleted. Revocations are recorded as new entries — the full history of consent granted and revoked is preserved. IP address, timestamp, and consent language are captured at the moment of consent.

---

## Alternatives Considered

### Alternative: Standard mutable JPA entity (current state)
**Strengths:** Simple, consistent with rest of data model, Spring Data JPA handles it automatically
**Rejected because:**
A mutable record loses its value as legal evidence. In a TCPA dispute, opposing counsel does not need to prove the record was changed — they only need to establish that it could have been. A standard JPA entity with no write protections can be updated or deleted by application code, a bug, or anyone with database access. That mutability, even if never exercised, is enough to call the record into question in court. The defense fails not because the record is wrong but because it cannot be proven unmodified. The cost of append-only is low; the legal exposure of staying mutable is not.

### Alternative: Immutable S3 bucket with Object Lock
**Strengths:** True WORM storage, cannot be deleted for the retention period, legally defensible
**Rejected because:**
S3 Object Lock is the gold standard for regulatory WORM storage and is the right choice at scale — but it is Phase 3 work, not Phase 1. At current volume an append-only database table provides adequate tamper-evidence. Adding an S3 dependency, bucket lifecycle policies, and dual-write logic now is premature. In Phase 3, when the system handles significant volume and each service has its own RDS instance, the right migration is to add an event that writes consent records to S3 with Object Lock alongside the database record — database for querying, S3 for compliance archival. This is deferred, not dismissed.

### Alternative: Blockchain / distributed ledger
**Strengths:** Maximum tamper-evidence, immutable by construction
**Rejected because:**
A public ledger means anyone can view it — writing borrower PII, IP addresses, and consent records to a public blockchain is itself a privacy violation. It trades one compliance problem for a worse one. An append-only database table with access controls achieves the same tamper-evidence property without exposing sensitive data. Blockchain is not an industry practice for TCPA compliance and would be correctly identified as over-engineering in any architecture review.

---

## Rationale

### Append-Only Matches the Legal Nature of Consent

Consent is a point-in-time event — "I consented on Date X at Time Y from IP Z." That event happened and cannot be un-happened. The data model should reflect this reality: consent records are created once and never modified or deleted.

When a borrower revokes consent, that revocation is recorded as a new entry. The history remains complete and unbroken: consent granted at T1, revoked at T2. Both records exist, neither is touched. This is the legally complete picture — a full chain of custody that can be produced in a dispute — and the technically correct model for data that is inherently an immutable event log.

---

## Consequences

### Positive

- The system can prove it gathered consent in an immutable, tamper-evident way. In a TCPA dispute, the append-only log is a defensible evidence record — no entry has been modified, no history has been erased.

### Negative

- The consent log must be actively maintained. Records cannot be cleaned up on a normal data lifecycle — they must be retained for 4 years minimum. Table size grows over time and requires a retention policy to manage it.
- The database is not the permanent home for this data. Phase 3 requires migrating to a more durable compliance store (S3 Object Lock). That migration must be planned and executed before volume makes it painful.

---

## Follow-up

- **Enforce append-only in code.** The consent entity must have no update or delete methods exposed. JPA must be configured to prevent modifications — no `save()` calls on existing records, no delete repository methods. The constraint should be enforced at the application layer, not just by convention.
- **Define the 4-year retention policy.** Records must be retained for 4 years from date of consent or last contact. A policy for how this is tracked and how aged-out records are handled (archived, not deleted) needs to be defined.
- **Phase 3: migrate to S3 Object Lock.** When the system moves to Phase 3, consent records should be dual-written to an S3 bucket with Object Lock enabled for compliance archival. The database record remains for querying; S3 is the long-term tamper-evident store.

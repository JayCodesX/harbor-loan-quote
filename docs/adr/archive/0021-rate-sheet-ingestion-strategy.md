# ADR 0021: Rate Sheet Ingestion — Manual Upload vs. Scheduled Poll vs. Webhook

## Status
Superseded by ADR-0019 (Rate Sheet Data Model)

Rate sheet ingestion approach evolved as the data model matured.
See ADR-0019 for the current design.

## Date
Fill in when you write this

## Phase
2 — Pricing Engine

---

## Context

WRITE THIS YOURSELF

Prompts to answer:
- How do real wholesale lenders/investors publish rate sheets?
  (Range: email with spreadsheet attachment, FTP/SFTP drop, proprietary portal download,
   API endpoint, or webhook push. Most small-to-mid wholesale lenders use email or portal.
   Large investors like Fannie/Freddie have structured data APIs.)
- How often do rate sheets change?
  (1-4 times per day. During volatile markets (Fed announcements) sometimes more.
   Mid-day reprices are common — a rate sheet published at 9:00 AM may be superseded at 1:00 PM.)
- What is the operational impact of a stale rate sheet?
  (Borrowers are quoted rates that are no longer available. The lender rejects the rate lock.
   This is a business-critical correctness issue, not just a UX issue.)
- What format are rate sheets in?
  (CSV, Excel (.xlsx), PDF, proprietary XML, or structured JSON. Varies by investor.)
- What is realistic for Phase 2 of a portfolio project?
  (Manual upload via admin UI with CSV format — realistic, demonstrable, and correct.)


---

## Decision

WRITE THIS YOURSELF

What is the decision? Be concrete — a sentence or two that could stand alone.
Describe the *what*, not the *why* (why belongs in Rationale).

---

## Alternatives Considered

### Alternative: Scheduled API polling (fully automated)
**Strengths:** No human intervention, rates update within minutes of investor publication, scales to many investors
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- Phase 2 is building the core engine. Investor API integrations require
  individual agreements with each investor/wholesale lender — a business development
  activity, not an engineering one.
- APIs differ per investor: some have REST APIs, some have SFTP, some have proprietary protocols.
  Building a generic polling framework before having actual investor API access is speculative.
- Manual upload is the correct MVP approach: validate the data model, the calculation engine,
  and the ingestion pipeline with controlled data before automating the intake.
- Scheduled polling is the correct Phase 3 approach once investor relationships exist.

### Alternative: Webhook/push from investor systems
**Strengths:** Real-time — rate update arrives within seconds of publication, no polling overhead
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- Webhook delivery requires the investor to push to Harbor's endpoint.
  This requires investor-side configuration — a business arrangement, not a technical decision.
- Webhooks are unreliable without retry and acknowledgment mechanisms.
  A missed webhook = stale rates until the next push or manual discovery.
- For Phase 2, the ingestion mechanism must be under Harbor's control to ensure
  rates can be updated on demand. Webhook dependency on external systems removes that control.
- Webhooks are a valid Phase 3 addition alongside scheduled polling for investors that support it.

### Alternative: Parse PDF rate sheets automatically
**Strengths:** Many investors publish rate sheets as PDFs — automating PDF parsing
would reduce manual data entry
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- PDF parsing is brittle. Rate sheet PDFs have variable formats that change without notice.
  A format change breaks the parser and rates stop updating — a silent, high-impact failure.
- The correct solution is a CSV format contract between Harbor and each investor.
  This requires a business conversation, not a PDF parser.
- PDF parsing is a separate engineering effort with significant edge cases.
  Do not build it until there is a specific investor who will only provide PDFs.

---

## Rationale

### Manual Upload Validates the Pipeline Before Automating It

WRITE THIS YOURSELF
The Phase 2 goal is building a working pricing engine with correct data modeling.
Manual upload lets the team:
1. Define and test the CSV schema against real rate sheet data
2. Validate the parsing and LLPA calculation pipeline
3. Test the Redis cache warm-up and effective window transitions
4. Build admin UI familiarity with the ingestion workflow

Automating ingestion before the pipeline is validated with real data adds
automation complexity on top of an untested foundation.
Build the pipeline. Validate it manually. Then automate the intake.

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

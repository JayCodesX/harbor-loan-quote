# ADR 0045: Admin Pricing Product Management UI — In-App Editor vs. Spreadsheet Import vs. API-Only

## Status
Deprecated

Admin app was removed in the 3-service consolidation. Pricing
product management is handled directly through the Pricing Engine
service.

## Date
Fill in when you write this

## Phase
2 — Pricing Engine

---

## Context

WRITE THIS YOURSELF

Prompts to answer:
- Who manages pricing products in this system?
  (Admins at the mortgage company. They configure: investors, loan products per investor,
   product eligibility rules, rate sheets, LLPA adjustments. These are not frequent changes
   — an investor's product catalog might change monthly; rate sheets change daily.)
- What is the "admin" surface area for pricing data?
  (Investors: name, NMLS ID, active/inactive.
   Products per investor: loan type (Conventional/FHA/VA), product terms (30yr fixed, 15yr ARM).
   Rate sheets: CSV upload or manual entry of rate/price pairs per lock period.
   LLPAs: condition + price adjustment rules per product.
   Eligibility rules: FICO minimums, LTV limits, loan amount bands.)
- What is the spreadsheet import approach?
  (Admins download a CSV template, fill in rates and prices in their spreadsheet,
   upload the CSV to the admin UI. The system parses and imports the rate sheet.
   This matches how investors actually deliver rate sheets — as Excel/CSV files.)
- What is an in-app editor?
  (A web-based UI with forms for creating rate/price pairs, LLPA rows, and product terms.
   Good for low-volume, infrequent changes (product setup, LLPA configuration).
   Not practical for bulk rate sheet entry — rate sheets can have 50-100 rate/price rows.)
- What is the API-only approach?
  (No UI. Admins use curl, Postman, or a script to call the admin API directly.
   Appropriate for a developer-internal tool, not for operational staff who manage rate sheets daily.)
- What is the relationship to ADR-0021 (rate sheet ingestion)?
  (ADR-0021 establishes that Phase 2 uses manual CSV upload; Phase 3 adds automated polling
   from investor portals. This ADR defines the admin UI that drives that manual upload workflow.)


---

## Decision

WRITE THIS YOURSELF

What is the decision? Be concrete — a sentence or two that could stand alone.
Describe the *what*, not the *why* (why belongs in Rationale).

---

## Alternatives Considered

### Alternative: API-only admin interface (no UI)
**Strengths:** Fastest to build, no frontend work required, flexible for automation
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- The operational staff managing rate sheets and investor products are not necessarily developers.
  An API-only interface requires curl, Postman, or a custom script — impractical for daily rate sheet uploads.
- The admin UI is an observable part of the system for a portfolio project.
  A clean admin interface demonstrates full-stack design thinking.
- An API-only approach is appropriate as a supplement (the REST API should exist for automation),
  not as the primary operator interface.

### Alternative: Full spreadsheet-only management (all configuration via CSV import)
**Strengths:** Consistent interface for all admin tasks, admins who are comfortable with spreadsheets handle everything in one tool
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- Configuring an investor (name, NMLS ID, contact info) via CSV template is awkward.
  Forms are the natural interface for structured record creation.
- LLPA rules have conditional logic (if FICO < 700 AND LTV > 80 then +0.500 points) —
  expressing this in a CSV format requires a rigid template that is error-prone to hand-edit.
- CSV upload is the right tool for bulk tabular data (rate sheets).
  Form editors are the right tool for structured configuration with validation feedback.

### Alternative: Embed an admin spreadsheet editor (AG Grid, Handsontable)
**Strengths:** Spreadsheet-like experience in the browser, familiar to users, avoids CSV file round-trip
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- AG Grid and Handsontable are heavy dependencies with complex licensing (AG Grid Enterprise is paid).
- For Phase 2, the rate sheet workflow is: download template → fill in Excel → upload CSV.
  This matches exactly how investors deliver rate sheets (they email or post an Excel file).
  A browser spreadsheet editor would be a different workflow, not better.
- Phase 3 automated ingestion (ADR-0021) eliminates manual rate sheet entry — the in-browser
  spreadsheet editor would be deprecated almost immediately after being built.

---

## Rationale

### Match the Workflow to the Data Type

WRITE THIS YOURSELF
Rate sheets are inherently tabular bulk data. Investors deliver them as CSV or Excel.
The admin workflow should be: receive file from investor → upload → validate → activate.
The CSV upload endpoint transforms the investor's file format directly into RateSheetEntry records.

Product configuration (investors, LLPAs, eligibility rules) is structured record management.
Form editors with real-time validation are the natural fit.
An LLPA form with fields for (condition field, operator, value, price adjustment) is cleaner
and safer than hand-editing a CSV row with the same information.

The hybrid approach — forms for configuration, CSV for bulk data — correctly models the
two distinct admin workflows. Neither is a compromise of the other.

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

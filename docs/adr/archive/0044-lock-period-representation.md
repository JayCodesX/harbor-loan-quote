# ADR 0044: Rate Lock Period Representation — Enum vs. Integer vs. Investor-Specific Configuration

## Status
Archived — valid, pending pricing engine buildout

Lock period representation is directly relevant to the pricing
engine. Will be revisited as rate lock functionality is built.

## Date
Fill in when you write this

## Phase
2 — Pricing Engine

---

## Context

WRITE THIS YOURSELF

Prompts to answer:
- What is a rate lock period in mortgage lending?
  (A rate lock is a lender's commitment to hold a specific interest rate for a borrower
   for a defined number of days while the loan is processed. Common lock periods:
   15, 30, 45, 60, 90 days. Longer lock periods carry a price premium — the lender
   bears more interest rate risk for a longer commitment.)
- How does the lock period affect the rate or price?
  (Lock period adjustments are a type of LLPA (ADR-0020). A 30-day lock might be
   par (0.000 adjustment). A 60-day lock might add 0.250 points. A 15-day lock
   might subtract 0.125 points. These adjustments are investor-specific and product-specific.)
- How are lock periods represented in rate sheets from investors?
  (Investors publish rate sheets with separate columns or rows per lock period.
   A rate sheet might have: Rate | 15-Day | 30-Day | 45-Day | 60-Day columns
   where each value is the price at that lock period for that rate.)
- What is the enum approach?
  (Define a Java enum: LOCK_15(15), LOCK_30(30), LOCK_45(45), LOCK_60(60), LOCK_90(90).
   Strongly typed, compile-time safety. Cannot represent a new lock period (e.g., 25-day)
   without a code change and deployment.)
- What is the integer approach?
  (Store lock period as an integer (number of days). Flexible — any lock period an investor
   offers can be stored without code changes. The pricing engine queries:
   SELECT price FROM rate_sheet_entries WHERE lock_period_days = 30.
   Validation applied at the application layer.)
- What are the trade-offs between investor-specific valid values vs. a universal set?
  (Investor A might offer 15/30/45/60-day locks. Investor B might offer 10/20/30/45/60-day locks.
   If lock periods are an enum, the enum must be the superset of all investors' lock periods.
   An integer with investor-specific valid values stored in the database is more flexible.)


---

## Decision

WRITE THIS YOURSELF

What is the decision? Be concrete — a sentence or two that could stand alone.
Describe the *what*, not the *why* (why belongs in Rationale).

---

## Alternatives Considered

### Alternative: Java enum (LockPeriod)
**Strengths:** Compile-time type safety, impossible to pass an invalid lock period to a method, self-documenting in method signatures
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- Different investors offer different lock period sets. An enum representing the universal superset
  (15, 20, 25, 30, 45, 60, 90) may include lock periods that no investor actually offers.
- When a new investor is onboarded with a non-standard lock period (e.g., 10-day express lock),
  a code change and deployment is required to add it to the enum.
- An integer with database-driven validation is more appropriate for investor-specific configuration.
  The set of valid lock periods is data, not code.
- Enums are appropriate for truly fixed value sets (e.g., LoanType: CONVENTIONAL, FHA, VA, USDA).
  Lock periods are investor-configurable and belong in the database.

### Alternative: Investor-specific enum per investor
**Strengths:** Type-safe per investor, impossible to use Investor A's lock periods for Investor B's rates
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- This is over-engineering at Phase 2. Creating a new Java enum file for each onboarded investor
  is operationally impractical.
- The investor-specific valid lock period configuration in the database achieves the same validation
  goal: when pricing a loan for Investor A, only validate against Investor A's configured lock periods.
- Database-driven validation scales to N investors without code changes.

---

## Rationale

### Integer + Database-Driven Validation Matches the Domain

WRITE THIS YOURSELF
The lock period is fundamentally a number: "lock this rate for 30 days."
The valid set of lock periods is investor-specific configuration data:
  investor_lock_periods (investor_id, lock_period_days, is_active)
  -- Investor A: 15, 30, 45, 60
  -- Investor B: 10, 30, 60, 90

The pricing engine query is clean:
  SELECT price_adjustment
  FROM rate_sheet_entries
  WHERE rate_sheet_id = ? AND lock_period_days = ?

Validation in PricingService:
  List<Integer> validLocks = investorLockPeriodRepository.findByInvestorId(investorId);
  if (!validLocks.contains(requestedLockPeriod)) throw new InvalidLockPeriodException(...)

This approach:
- Requires no code changes when a new investor is onboarded with a different lock period set
- Correctly models that lock periods are investor-specific data, not a universal enum
- Stores cleanly in the database as an integer column
- Integrates naturally with the LLPA table (ADR-0020) which uses JSON conditions on lockPeriodDays

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

# ADR 0047: End-to-End Test Framework — Playwright vs. Selenium vs. Cypress

## Status
Archived — valid, deferred

E2E test framework selection will be revisited as the 3-service
architecture stabilizes.

## Date
Fill in when you write this

## Phase
1 — Foundation

---

## Context

WRITE THIS YOURSELF

Prompts to answer:
- What E2E scenarios does this system need to test?
  (Full quote flow: borrower enters loan parameters → receives rate quotes → captures lead (email + consent).
   Authentication flow: register → login → view quote history.
   Admin flow: upload rate sheet → verify rates appear in quote results.
   These tests exercise the full stack: React frontend → harbor-api → pricing-service → MySQL.)
- What is Playwright?
  (Microsoft's modern E2E browser testing framework. Supports Chromium, Firefox, WebKit.
   TypeScript-native API. Auto-waits for elements before interacting (no explicit waits needed).
   Parallel test execution across browsers. Built-in screenshot, video, and trace recording.
   The current Jenkinsfile already references Playwright in the E2E test stage.)
- What is Selenium?
  (The original browser automation framework. Wide language support (Java, Python, JavaScript).
   WebDriver protocol. More setup required vs. Playwright; explicit waits often needed.
   No built-in auto-wait; no built-in network interception. Slower due to WebDriver architecture.)
- What is Cypress?
  (JavaScript E2E framework. Runs inside the browser (not WebDriver). Excellent DX for web apps.
   Time-travel debugging, automatic retry, cy.intercept for network stubbing.
   Limited multi-tab/multi-domain support. Electron-based test runner.
   Commercial Cypress Cloud for parallel execution and test analytics.)
- Why does the Jenkinsfile already use Playwright?
  (The project already has a Playwright E2E test suite running in CI. This ADR documents
   the rationale for the existing choice and confirms it as the correct decision.)


---

## Decision

WRITE THIS YOURSELF

What is the decision? Be concrete — a sentence or two that could stand alone.
Describe the *what*, not the *why* (why belongs in Rationale).

---

## Alternatives Considered

### Alternative: Selenium WebDriver
**Strengths:** Broad language support, industry longevity, compatible with any CI system, Java-native (aligns with backend language)
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- Selenium's WebDriver protocol is slower and more verbose than Playwright's direct browser API.
  Playwright auto-waits for elements to be actionable — Selenium requires explicit WebDriverWait calls
  for dynamic React components, leading to either flaky tests or excessive boilerplate.
- Playwright's network interception (page.route()) is a first-class feature — useful for mocking
  pricing-service responses in frontend-only E2E tests. Selenium has no equivalent.
- Playwright runs multi-browser tests (Chromium, Firefox, WebKit) with the same test code.
  Selenium requires browser-specific driver binaries (ChromeDriver, GeckoDriver) to be maintained.
- The React frontend is JavaScript — Playwright's TypeScript-native API is a better fit
  for writing and maintaining frontend tests than Java Selenium.

### Alternative: Cypress
**Strengths:** Excellent DX, time-travel debugging, auto-retry, strong documentation, large ecosystem
**Rejected because:**

WRITE THIS YOURSELF
Key points:
- Cypress has limited multi-origin support — testing flows that cross domains (e.g., Cloudflare
  auth redirect → app domain) is complex with Cypress restrictions on cross-origin iframes.
- Playwright supports multi-tab and multi-context scenarios natively, which is relevant for
  testing the borrower and admin flows in the same test run.
- Playwright's trace viewer is comparable to Cypress's time-travel debugging and does not
  require a paid cloud service for parallel execution.
- Cypress Cloud (for parallel execution and test analytics) is a paid tier. Playwright's
  parallel execution is built-in and free.
- The existing CI pipeline (Jenkinsfile) already uses Playwright — switching to Cypress
  would require rewriting the existing test suite.

---

## Rationale

### Playwright Is the Modern Standard for Full-Stack React + API E2E Testing

WRITE THIS YOURSELF
The harbor-loan-quote E2E test suite tests:
1. React frontend interactions (form fills, button clicks, quote result display)
2. Full API calls (harbor-api, pricing-service, auth-service)
3. Database state assertions (quote created, lead captured)

Playwright supports all three:
- Browser automation with auto-wait (no flaky explicit waits)
- Network interception (mock pricing-service for isolated frontend tests)
- API testing via `request` fixture (call harbor-api directly without a browser for setup/teardown)

The CI integration is straightforward:
  npx playwright test --reporter=html
with the docker-compose integration profile starting all services before the test run.

Playwright is the correct choice and is already implemented. This ADR documents why.

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

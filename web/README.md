# Harbor Loan Quotes — Borrower App

React + Vite borrower-facing web application. Served by Nginx, routed through the edge proxy.

## Features
- Public mortgage quote request
- Multi-step quote refinement (authenticated)
- My Quotes history
- Mortgage and amortization calculators
- Sign in / register

## Local Development

```bash
npm install
npm run dev
```

## Build

```bash
npm run build
```

## Test

```bash
npm run test:ci
```

## Environment

The app expects requests to be proxied through the edge at `https://localhost:8443`. See the root `docker-compose.yml` for the full stack setup.

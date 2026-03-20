import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'

function mockSummary() {
  return {
    auth: { totalUsers: 3, adminUsers: 1, standardUsers: 2 },
    borrowers: { totalBorrowers: 4, averageCreditScore: 716.5, creditScoreBands: [] },
    pricing: { totalProducts: 4, activeProducts: 4, activeRateSheets: 5, activeAdjustmentRules: 16, programDistribution: [] },
    leads: { totalLeads: 3, leadStatusDistribution: [], leadSourceDistribution: [] },
    notifications: { cachedQuoteSnapshots: 1 },
    quotes: { quotesStarted: 8, quotesCompleted: 11, quotesFailed: 0, quotesDeduped: 0, sessionsWithQuotes: 1, authenticatedSessions: 0, sessionsWithRefinements: 0, leadConvertedSessions: 0, averagePricingDurationMs: 1987, averageLeadDurationMs: 2636, quoteRefinementsRequested: 0 },
  }
}

function mockJsonResponse(payload, overrides = {}) {
  return { ok: true, status: 200, json: async () => payload, text: async () => '', ...overrides }
}

describe('Admin console', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
    vi.stubGlobal('crypto', { randomUUID: () => 'admin-session-id' })
    window.localStorage.clear()
  })

  afterEach(() => {
    cleanup()
    vi.unstubAllGlobals()
  })

  it('shows a direct admin sign-in form when there is no session', () => {
    render(<App />)
    expect(screen.getByText(/Sign in to the admin console/i)).toBeInTheDocument()
  })

  it('signs in directly and loads the dashboard', async () => {
    fetch.mockResolvedValueOnce(mockJsonResponse({ accessToken: 'admin-token', tokenType: 'Bearer', expiresIn: 900, email: 'admin@example.com', role: 'ADMIN' }))
      .mockResolvedValueOnce(mockJsonResponse(mockSummary()))

    render(<App />)
    fireEvent.click(screen.getByRole('button', { name: /sign in as admin/i }))
    await waitFor(() => expect(screen.getByText(/Run the complete borrower quote demo from the admin console/i)).toBeInTheDocument())
  })

  it('rejects non-admin sessions', () => {
    window.localStorage.setItem('mortgage-loan-api-auth', JSON.stringify({ accessToken: 'user-token', role: 'USER', email: 'user@example.com' }))
    render(<App />)
    expect(screen.getByText(/does not have the `ADMIN` role/i)).toBeInTheDocument()
  })

  it('shows pricing tab and can load products', async () => {
    window.localStorage.setItem('mortgage-loan-api-auth', JSON.stringify({ accessToken: 'admin-token', role: 'ADMIN', email: 'admin@example.com' }))
    fetch.mockResolvedValueOnce(mockJsonResponse(mockSummary()))
      .mockResolvedValueOnce(mockJsonResponse([{ id: 2, programCode: 'FHA', productName: 'FHA Streamline', baseRate: 5.875, active: true }]))

    render(<App />)
    await waitFor(() => expect(screen.getByText(/Run the complete borrower quote demo from the admin console/i)).toBeInTheDocument())
    fireEvent.click(screen.getByRole('button', { name: /pricing/i }))
    await waitFor(() => expect(screen.getByText(/FHA Streamline/i)).toBeInTheDocument())
  })

  it('shows reports tab and can run a report', async () => {
    window.localStorage.setItem('mortgage-loan-api-auth', JSON.stringify({ accessToken: 'admin-token', role: 'ADMIN', email: 'admin@example.com' }))
    fetch.mockResolvedValueOnce(mockJsonResponse(mockSummary()))
      .mockResolvedValueOnce(mockJsonResponse({ title: 'Pricing products', columns: ['id', 'programCode'], rows: [{ id: 1, programCode: 'CONVENTIONAL' }], totalRows: 1 }))

    render(<App />)
    await waitFor(() => expect(screen.getByText(/Run the complete borrower quote demo from the admin console/i)).toBeInTheDocument())
    fireEvent.click(screen.getByRole('button', { name: /reports/i }))
    fireEvent.click(screen.getByRole('button', { name: /run report/i }))
    await waitFor(() => expect(screen.getByText(/Pricing products/i)).toBeInTheDocument())
    expect(screen.getByText('CONVENTIONAL')).toBeInTheDocument()
  })

  it('shows workspace and docs tabs for internal tooling', async () => {
    window.localStorage.setItem('mortgage-loan-api-auth', JSON.stringify({ accessToken: 'admin-token', role: 'ADMIN', email: 'admin@example.com' }))
    fetch.mockResolvedValueOnce(mockJsonResponse(mockSummary()))
      .mockResolvedValueOnce(mockJsonResponse([{ id: 1, firstName: 'Jay', lastName: 'Carter', email: 'user@example.com', creditScore: 741 }]))

    render(<App />)
    await waitFor(() => expect(screen.getByText(/Run the complete borrower quote demo from the admin console/i)).toBeInTheDocument())
    fireEvent.click(screen.getByRole('button', { name: /workspace/i }))
    fireEvent.click(screen.getByRole('button', { name: /load borrowers/i }))
    await waitFor(() => expect(screen.getByText(/Jay Carter/i)).toBeInTheDocument())
    fireEvent.click(screen.getByRole('button', { name: /docs/i }))
    expect(screen.getByText(/\/api\/admin\/products/)).toBeInTheDocument()
  })
})

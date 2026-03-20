import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'

const AUTH_KEY = 'harbor-loan-quote-auth'

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

// URL-based mock — never gets "consumed", avoids ordering issues with concurrent fetches.
function mockFetch(routes = {}) {
  return vi.fn((url) => {
    for (const [pattern, payload] of Object.entries(routes)) {
      if (url.includes(pattern)) {
        return Promise.resolve({
          ok: true,
          status: 200,
          json: async () => payload,
          text: async () => '',
        })
      }
    }
    return Promise.resolve({ ok: false, status: 404, json: async () => null, text: async () => '' })
  })
}

function seedAdminSession() {
  window.sessionStorage.setItem(AUTH_KEY, JSON.stringify({
    accessToken: 'admin-token',
    role: 'ADMIN',
    email: 'admin@example.com',
  }))
}

describe('Admin console', () => {
  beforeEach(() => {
    vi.stubGlobal('crypto', { randomUUID: () => 'admin-session-id' })
    window.localStorage.clear()
    window.sessionStorage.clear()
  })

  afterEach(() => {
    cleanup()
    vi.unstubAllGlobals()
  })

  it('shows a direct admin sign-in form when there is no session', () => {
    vi.stubGlobal('fetch', mockFetch())
    render(<App />)
    expect(screen.getByText(/Sign in to Mortgage Desk/i)).toBeInTheDocument()
  })

  it('signs in directly and loads the dashboard', async () => {
    vi.stubGlobal('fetch', mockFetch({
      '/auth/login': { accessToken: 'admin-token', tokenType: 'Bearer', expiresIn: 900, email: 'admin@example.com', role: 'ADMIN' },
      '/metrics/admin/summary': mockSummary(),
    }))

    render(<App />)

    // Fill in required fields so HTML5 validation does not block submission in jsdom.
    fireEvent.change(screen.getByPlaceholderText('admin@harborloanquotes.com'), {
      target: { name: 'email', value: 'admin@example.com' },
    })
    fireEvent.change(screen.getByPlaceholderText('Password'), {
      target: { name: 'password', value: 'secret' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Sign in' }))

    await waitFor(() => expect(screen.getByText('Quote funnel')).toBeInTheDocument())
    expect(screen.getByText('Operational highlights')).toBeInTheDocument()
  })

  it('rejects non-admin sessions', () => {
    window.sessionStorage.setItem(AUTH_KEY, JSON.stringify({ accessToken: 'user-token', role: 'USER', email: 'user@example.com' }))
    vi.stubGlobal('fetch', mockFetch())
    render(<App />)
    expect(screen.getByText(/does not have the ADMIN role/i)).toBeInTheDocument()
  })

  it('shows pricing tab and can load products', async () => {
    seedAdminSession()
    vi.stubGlobal('fetch', mockFetch({
      '/metrics/admin/summary': mockSummary(),
      '/admin/products': [{ id: 2, programCode: 'FHA', productName: 'FHA Streamline', baseRate: 5.875, active: true }],
    }))

    render(<App />)
    await waitFor(() => expect(screen.getByText('Quote funnel')).toBeInTheDocument())
    fireEvent.click(screen.getByRole('button', { name: /pricing/i }))
    await waitFor(() => expect(screen.getByText(/FHA Streamline/i)).toBeInTheDocument())
  })

  it('shows reports tab and can run a report', async () => {
    seedAdminSession()
    vi.stubGlobal('fetch', mockFetch({
      '/metrics/admin/summary': mockSummary(),
      '/admin/reports/query': { totalRecords: 5, quotesStarted: 3, refined: 1 },
    }))

    render(<App />)
    await waitFor(() => expect(screen.getByText('Quote funnel')).toBeInTheDocument())
    fireEvent.click(screen.getByRole('button', { name: /reports/i }))
    fireEvent.click(screen.getByRole('button', { name: /run report/i }))
    await waitFor(() => expect(screen.getByText('Report output')).toBeInTheDocument())
    expect(screen.getByRole('button', { name: /Export CSV/i })).toBeInTheDocument()
  })

  it('shows the workspace tab with borrower and loan sections', async () => {
    seedAdminSession()
    vi.stubGlobal('fetch', mockFetch({
      '/metrics/admin/summary': mockSummary(),
    }))

    render(<App />)
    await waitFor(() => expect(screen.getByText('Quote funnel')).toBeInTheDocument())
    fireEvent.click(screen.getByRole('button', { name: /workspace/i }))
    expect(screen.getByText('Borrower workspace')).toBeInTheDocument()
    expect(screen.getByText('Loan workspace')).toBeInTheDocument()
    expect(screen.getByText('Loan lookup')).toBeInTheDocument()
  })
})

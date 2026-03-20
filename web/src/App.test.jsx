import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'

class EventSourceMock {
  close() {}
}

// URL-based mock so initial useEffect fetches (legalPolicies, locationOptions, etc.)
// don't consume mocks intended for specific API operations.
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

describe('Borrower demo app', () => {
  beforeEach(() => {
    vi.stubGlobal('EventSource', EventSourceMock)
    vi.stubGlobal('crypto', { randomUUID: () => 'test-session-id' })
    window.localStorage.clear()
    window.sessionStorage.clear()
  })

  afterEach(() => {
    cleanup()
    vi.unstubAllGlobals()
  })

  it('loads the borrower-facing quote home by default', () => {
    vi.stubGlobal('fetch', mockFetch())
    render(<App />)

    expect(screen.getByText('Get a credible home loan quote before filling out a long mortgage form.')).toBeInTheDocument()
    expect(screen.getByText('See an estimate in under a minute')).toBeInTheDocument()
    expect(screen.queryByText('Quote funnel')).not.toBeInTheDocument()
    expect(screen.queryByText('Workspace')).not.toBeInTheDocument()
    expect(screen.queryByText('API Docs')).not.toBeInTheDocument()
  })

  it('submits a public quote from the home experience', async () => {
    vi.stubGlobal('fetch', mockFetch({
      '/loan-quotes/public': {
        id: 17,
        sessionId: 'test-session-id',
        processingStatus: 'COMPLETED',
        quoteStage: 'PUBLIC',
        quoteStatus: 'PENDING_PRICING',
        estimatedMonthlyPayment: 2841,
        duplicate: false,
      },
    }))

    render(<App />)

    fireEvent.click(screen.getByRole('button', { name: 'See result' }))

    await waitFor(() => expect(screen.getByText('Initial quote result')).toBeInTheDocument())
    expect(screen.getByText('Confidence is moderate. Add borrower details to tighten lender + agent fit.')).toBeInTheDocument()
  })

  it('routes unauthenticated users to sign in when they try to refine a quote', async () => {
    vi.stubGlobal('fetch', mockFetch({
      '/loan-quotes/public': {
        id: 17,
        processingStatus: 'COMPLETED',
        quoteStatus: 'PENDING_PRICING',
        estimatedMonthlyPayment: 2841,
      },
    }))

    render(<App />)

    fireEvent.click(screen.getByRole('button', { name: 'See result' }))
    await waitFor(() => expect(screen.getByText('Initial quote result')).toBeInTheDocument())

    fireEvent.click(screen.getByRole('button', { name: 'Refine quote' }))
    expect(screen.getByText('Save and refine your quote')).toBeInTheDocument()
  })

  it('signs in and reflects authenticated state in the navigation', async () => {
    vi.stubGlobal('fetch', mockFetch({
      '/auth/login': {
        accessToken: 'token-123',
        tokenType: 'Bearer',
        expiresIn: 900,
        email: 'borrower@example.com',
        role: 'BORROWER',
      },
    }))

    render(<App />)

    fireEvent.click(screen.getByRole('button', { name: 'Sign In' }))

    fireEvent.change(screen.getByPlaceholderText('Email'), {
      target: { name: 'email', value: 'borrower@example.com' },
    })
    fireEvent.change(screen.getByPlaceholderText('Password'), {
      target: { name: 'password', value: 'secret' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Continue' }))

    await waitFor(() => expect(screen.getByRole('button', { name: 'Sign Out' })).toBeInTheDocument())
  })

  it('opens the calculators view and runs the mortgage calculator', async () => {
    vi.stubGlobal('fetch', mockFetch({
      '/loans/mortgage-payment': {
        monthlyPayment: 2275.45,
        financedPrincipal: 360000,
        numberOfPayments: 360,
      },
    }))

    render(<App />)

    fireEvent.click(screen.getByRole('button', { name: 'Calculators' }))
    fireEvent.click(screen.getByRole('button', { name: 'Calculate payment' }))

    await waitFor(() => expect(screen.getByText('$2,275.45')).toBeInTheDocument())
    expect(screen.getByText('$360,000.00')).toBeInTheDocument()
  })

  it('runs the amortization calculator from the calculators view', async () => {
    vi.stubGlobal('fetch', mockFetch({
      '/loans/amortization': {
        monthlyPayment: 2212.11,
        principal: 350000,
        numberOfPayments: 360,
      },
    }))

    render(<App />)

    fireEvent.click(screen.getByRole('button', { name: 'Calculators' }))
    fireEvent.click(screen.getByRole('button', { name: 'Amortization' }))
    fireEvent.click(screen.getByRole('button', { name: 'Calculate amortization' }))

    await waitFor(() => expect(screen.getByText('$2,212.11')).toBeInTheDocument())
    expect(screen.getByText('$350,000.00')).toBeInTheDocument()
  })
})

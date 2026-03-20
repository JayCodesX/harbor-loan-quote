import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'

class EventSourceMock {
  close() {}
}

function mockJsonResponse(payload, overrides = {}) {
  return {
    ok: true,
    status: 200,
    json: async () => payload,
    text: async () => '',
    ...overrides,
  }
}

describe('Borrower demo app', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
    vi.stubGlobal('EventSource', EventSourceMock)
    vi.stubGlobal('crypto', { randomUUID: () => 'test-session-id' })
    window.localStorage.clear()
  })

  afterEach(() => {
    cleanup()
    vi.unstubAllGlobals()
  })

  it('loads the borrower-facing quote home by default', () => {
    render(<App />)

    expect(screen.getAllByText('Get a loan quote').length).toBeGreaterThan(0)
    expect(screen.getByText('See an estimated mortgage quote in minutes.')).toBeInTheDocument()
    expect(screen.queryByText('Quote Funnel')).not.toBeInTheDocument()
    expect(screen.queryByText('Workspace')).not.toBeInTheDocument()
    expect(screen.queryByText('API Docs')).not.toBeInTheDocument()
    expect(screen.queryByText('Ops')).not.toBeInTheDocument()
  })

  it('submits a public quote from the home experience', async () => {
    fetch.mockResolvedValueOnce(mockJsonResponse({
      id: 17,
      sessionId: 'test-session-id',
      processingStatus: 'QUEUED',
      quoteStage: 'PUBLIC',
      quoteStatus: 'PENDING_PRICING',
      qualificationTier: 'Prime',
      duplicate: false,
      nextStep: 'Wait for pricing',
    }))

    render(<App />)

    fireEvent.click(screen.getByRole('button', { name: 'See my quote' }))

    await waitFor(() => expect(screen.getByText('Quote #17')).toBeInTheDocument())
    expect(fetch).toHaveBeenCalledWith('http://localhost:8080/loan-quotes/public', expect.any(Object))
    expect(fetch.mock.calls[0][1]).toMatchObject({
      method: 'POST',
      headers: { 'X-Session-Id': 'test-session-id' },
    })
    expect(screen.getByText('Your quote is queued')).toBeInTheDocument()
  })

  it('routes users to sign in before personalizing a quote', () => {
    render(<App />)

    fireEvent.change(screen.getByDisplayValue(''), { target: { value: '17' } })
    fireEvent.click(screen.getByRole('button', { name: 'Personalize my quote' }))

    expect(screen.getByText('Sign in before personalizing the quote.')).toBeInTheDocument()
    expect(screen.getByText('Continue your quote when you are ready.')).toBeInTheDocument()
  })

  it('signs in and exposes the admin console link for admin users', async () => {
    fetch.mockResolvedValueOnce(mockJsonResponse({
      accessToken: 'token-123',
      tokenType: 'Bearer',
      expiresIn: 900,
      email: 'admin@example.com',
      role: 'ADMIN',
    }))

    render(<App />)

    fireEvent.click(screen.getByRole('button', { name: 'Sign In' }))
    fireEvent.click(screen.getByRole('button', { name: 'Sign in' }))

    await waitFor(() => expect(screen.getAllByText('admin@example.com').length).toBeGreaterThan(0))
    expect(screen.getByRole('link', { name: 'Admin Console' })).toBeInTheDocument()
  })

  it('opens the calculators view and runs the mortgage calculator', async () => {
    fetch.mockResolvedValueOnce(mockJsonResponse({
      monthlyPayment: 2275.45,
      financedPrincipal: 360000,
      numberOfPayments: 360,
    }))

    render(<App />)

    fireEvent.click(screen.getByRole('button', { name: 'Calculators' }))
    fireEvent.click(screen.getByRole('button', { name: 'Calculate payment' }))

    await waitFor(() => expect(screen.getByText('$2,275.45')).toBeInTheDocument())
    expect(screen.getByText('$360,000.00')).toBeInTheDocument()
  })

  it('runs the amortization calculator from the calculators view', async () => {
    fetch.mockResolvedValueOnce(mockJsonResponse({
      monthlyPayment: 2212.11,
      principal: 350000,
      numberOfPayments: 360,
    }))

    render(<App />)

    fireEvent.click(screen.getByRole('button', { name: 'Calculators' }))
    fireEvent.click(screen.getByRole('button', { name: 'Amortization' }))
    fireEvent.click(screen.getByRole('button', { name: 'Calculate amortization' }))

    await waitFor(() => expect(screen.getByText('$2,212.11')).toBeInTheDocument())
    expect(screen.getByText('$350,000.00')).toBeInTheDocument()
  })
})

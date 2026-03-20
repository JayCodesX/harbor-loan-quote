export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
export const AUTH_STORAGE_KEY = 'harbor-loan-quote-auth'
export const SESSION_STORAGE_KEY = 'harbor-loan-quote-session-id'

export const defaultQuoteForm = {
  homePrice: '525000',
  downPayment: '60000',
  zipCode: '98109',
  stateCode: 'WA',
  countyName: 'King',
  loanProgram: 'CONVENTIONAL',
  propertyUse: 'PRIMARY_RESIDENCE',
  termYears: '30',
}

export const defaultRefineForm = {
  firstName: '',
  lastName: '',
  email: '',
  phone: '',
  stateCode: '',
  countyName: '',
  annualIncome: '0',
  monthlyDebts: '0',
  creditScore: '0',
  cashReserves: '0',
  firstTimeBuyer: 'false',
  vaEligible: 'false',
  estimatedFundingDate: '',
}

export const defaultMortgageForm = {
  loanAmount: '450000',
  downPayment: '90000',
  annualInterestRate: '6.5',
  termYears: '30',
}

export const defaultAmortizationForm = {
  principal: '350000',
  annualInterestRate: '6.5',
  termYears: '30',
}

export const defaultAuthForm = {
  email: '',
  password: '',
}

export const defaultSubscriptionForm = {
  email: '',
  productUpdates: 'true',
  rateAlerts: 'true',
  partnerAlerts: 'true',
}

export function getStoredAuth() {
  if (typeof window === 'undefined') {
    return null
  }

  const raw = window.sessionStorage.getItem(AUTH_STORAGE_KEY)
  if (!raw) {
    return null
  }

  try {
    return JSON.parse(raw)
  } catch {
    window.sessionStorage.removeItem(AUTH_STORAGE_KEY)
    return null
  }
}

export function getOrCreateSessionId() {
  if (typeof window === 'undefined') {
    return 'server-session'
  }

  const existing = window.localStorage.getItem(SESSION_STORAGE_KEY)
  if (existing) {
    return existing
  }

  const created = crypto.randomUUID()
  window.localStorage.setItem(SESSION_STORAGE_KEY, created)
  return created
}

export function isQuoteInFlight(quote) {
  return quote && ['PENDING', 'QUEUED', 'PROCESSING'].includes(quote.processingStatus)
}

export function formatCurrency(value) {
  if (value === undefined || value === null || Number.isNaN(Number(value))) {
    return '--'
  }

  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    maximumFractionDigits: 2,
  }).format(Number(value))
}

export function formatPercent(value) {
  if (value === undefined || value === null || Number.isNaN(Number(value))) {
    return '--'
  }

  return `${Number(value).toFixed(3).replace(/\.0+$/, '').replace(/(\.\d*[1-9])0+$/, '$1')}%`
}

export function getQuoteStatusMeta(quote) {
  if (!quote) {
    return {
      tone: 'idle',
      badge: 'Start here',
      headline: 'Get a loan quote',
      detail: 'Enter a few property details to see pricing, payment, and next steps.',
    }
  }

  if (quote.processingStatus === 'FAILED') {
    return {
      tone: 'failed',
      badge: 'Needs attention',
      headline: 'Quote processing failed',
      detail: 'Retry the quote request or refresh status before you continue.',
    }
  }

  if (quote.leadCaptured && quote.quoteStatus === 'LEAD_READY') {
    return {
      tone: 'success',
      badge: 'Lead ready',
      headline: 'Your personalized quote is ready',
      detail: 'The refined scenario is complete and the lead has been captured for follow-up.',
    }
  }

  if (quote.processingStatus === 'COMPLETED') {
    return {
      tone: 'success',
      badge: 'Quote ready',
      headline: 'Your estimate is ready',
      detail: 'Review the pricing and personalize it with borrower details when you are ready.',
    }
  }

  if (quote.processingStatus === 'PROCESSING') {
    return {
      tone: 'processing',
      badge: 'In progress',
      headline: 'We are pricing your quote',
      detail: 'The pricing engine is processing the request right now.',
    }
  }

  if (quote.processingStatus === 'QUEUED') {
    return {
      tone: 'queued',
      badge: quote.duplicate ? 'Already in line' : 'Queued',
      headline: quote.duplicate ? 'We found your in-flight quote' : 'Your quote is queued',
      detail: quote.duplicate
        ? 'A matching request is already running for this session, so we reused it.'
        : 'The quote was accepted and is waiting for the pricing worker.',
    }
  }

  return {
    tone: 'idle',
    badge: 'Ready',
    headline: 'Continue your quote',
    detail: 'Refresh the quote status or personalize it to capture borrower details.',
  }
}

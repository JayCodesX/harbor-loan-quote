export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'
export const AUTH_STORAGE_KEY = 'harbor-loan-quote-auth'
export const SESSION_STORAGE_KEY = 'harbor-loan-quote-session-id'
export const palette = ['#0b5ed7', '#1d7a69', '#f59f28', '#cf4f4f', '#6c757d']

export const defaultAdminLogin = {
  email: '',
  password: '',
}

export const defaultBorrowerForm = {
  firstName: '',
  lastName: '',
  email: '',
  creditScore: '0',
  stateCode: '',
  countyName: '',
}

export const defaultLoanForm = {
  borrowerId: '',
  loanAmount: '385000',
  interestRate: '6.125',
  termYears: '30',
  status: 'PENDING',
}

export const defaultProductForm = {
  programCode: 'CONVENTIONAL',
  productName: '',
  baseRate: '6.1250',
  active: 'true',
}

export const defaultPartnerForm = {
  displayName: '',
  companyName: '',
  email: '',
  phone: '',
  stateCode: 'WA',
  countyName: 'King',
  city: '',
  specialty: '',
  licenseNumber: '',
  nmlsId: '',
  rankingScore: '92',
  responseSlaHours: '4',
  languages: 'English',
  websiteUrl: '',
  active: 'true',
}

export const defaultReportForm = {
  reportType: 'PRODUCTS',
  preset: '',
  search: '',
  activeOnly: 'false',
  status: '',
  minCreditScore: '',
  maxCreditScore: '',
  programCode: '',
  stateCode: '',
  countyName: '',
  dateFrom: '',
  dateTo: '',
  sortBy: 'id',
  sortDirection: 'ASC',
  page: '1',
  pageSize: '25',
}

export function getStoredAuth() {
  if (typeof window === 'undefined') {
    return null
  }

  const raw = window.localStorage.getItem(AUTH_STORAGE_KEY)
  if (!raw) {
    return null
  }

  try {
    return JSON.parse(raw)
  } catch {
    window.localStorage.removeItem(AUTH_STORAGE_KEY)
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

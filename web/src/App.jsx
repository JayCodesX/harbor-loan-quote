import { useEffect, useMemo, useState } from 'react'
import 'bootstrap/dist/css/bootstrap.min.css'
import './App.css'
import BorrowerApp from './components/BorrowerApp'
import {
  API_BASE_URL,
  AUTH_STORAGE_KEY,
  defaultAmortizationForm,
  defaultAuthForm,
  defaultMortgageForm,
  defaultQuoteForm,
  defaultRefineForm,
  defaultSubscriptionForm,
  getOrCreateSessionId,
  getStoredAuth,
  isQuoteInFlight,
} from './lib/demoApp'

function App() {
  const [activeView, setActiveView] = useState('home')
  const [activeCalculator, setActiveCalculator] = useState('mortgage')
  const [publicQuoteForm, setPublicQuoteForm] = useState(defaultQuoteForm)
  const [refineForm, setRefineForm] = useState(defaultRefineForm)
  const [mortgageForm, setMortgageForm] = useState(defaultMortgageForm)
  const [amortizationForm, setAmortizationForm] = useState(defaultAmortizationForm)
  const [loginForm, setLoginForm] = useState(defaultAuthForm)
  const [registerForm, setRegisterForm] = useState(defaultAuthForm)
  const [subscriptionForm, setSubscriptionForm] = useState(defaultSubscriptionForm)
  const [quoteResult, setQuoteResult] = useState(null)
  const [quoteHistory, setQuoteHistory] = useState([])
  const [locationOptions, setLocationOptions] = useState([])
  const [matchedAgents, setMatchedAgents] = useState([])
  const [matchedLenders, setMatchedLenders] = useState([])
  const [mortgageResult, setMortgageResult] = useState(null)
  const [amortizationResult, setAmortizationResult] = useState(null)
  const [errorMessage, setErrorMessage] = useState('')
  const [loadingTarget, setLoadingTarget] = useState('')
  const [subscriptionMessage, setSubscriptionMessage] = useState('')
  const [subscriptionReceipt, setSubscriptionReceipt] = useState(null)
  const [unsubscribeToken, setUnsubscribeToken] = useState('')
  const [unsubscribeMessage, setUnsubscribeMessage] = useState('')
  const [policyVersions, setPolicyVersions] = useState(null)
  const [authState, setAuthState] = useState(() => getStoredAuth())
  const [sessionId] = useState(() => getOrCreateSessionId())
  const endpoints = useMemo(() => ({
    register: `${API_BASE_URL}/auth/register`,
    login: `${API_BASE_URL}/auth/login`,
    refresh: `${API_BASE_URL}/auth/refresh`,
    logout: `${API_BASE_URL}/auth/logout`,
    publicQuote: `${API_BASE_URL}/loan-quotes/public`,
    refineQuoteBase: `${API_BASE_URL}/loan-quotes`,
    quoteBase: `${API_BASE_URL}/loan-quotes`,
    borrowerQuotes: `${API_BASE_URL}/borrower/quotes`,
    borrowerCurrentQuote: `${API_BASE_URL}/borrower/quotes/current`,
    borrowerRefineLatest: `${API_BASE_URL}/borrower/quotes/refine-latest`,
    borrowerAttachSession: `${API_BASE_URL}/borrower/quotes/attach-session`,
    borrowerProfile: `${API_BASE_URL}/borrower/profile`,
    quoteEventsBase: `${API_BASE_URL}/notifications/quotes`,
    authSessionMetric: `${API_BASE_URL}/metrics/quotes/sessions/authenticated`,
    subscriptions: `${API_BASE_URL}/subscriptions`,
    unsubscribeSubscription: `${API_BASE_URL}/subscriptions/unsubscribe`,
    consents: `${API_BASE_URL}/consents`,
    legalPolicies: `${API_BASE_URL}/legal/policies`,
    locationOptions: `${API_BASE_URL}/directory/locations`,
    agents: `${API_BASE_URL}/directory/agents`,
    lenders: `${API_BASE_URL}/directory/lenders`,
    mortgage: `${API_BASE_URL}/loans/mortgage-payment/calculate`,
    amortization: `${API_BASE_URL}/loans/amortization/calculate`,
  }), [])

const saveAuthState = (nextAuthState) => {
    setAuthState(nextAuthState)

    if (typeof window === 'undefined') {
      return
    }

    if (nextAuthState) {
      window.sessionStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(nextAuthState))
    } else {
      window.sessionStorage.removeItem(AUTH_STORAGE_KEY)
    }
  }

  const handleInput = (setter) => (event) => {
    const { name, value } = event.target
    setter((current) => ({ ...current, [name]: value }))
  }

  const refreshAuthSession = async (currentAuthState = authState) => {
    if (!currentAuthState?.refreshToken) {
      saveAuthState(null)
      setActiveView('auth')
      return null
    }

    try {
      const response = await fetch(endpoints.refresh, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken: currentAuthState.refreshToken }),
      })

      if (!response.ok) {
        saveAuthState(null)
        setActiveView('auth')
        return null
      }

      const result = await response.json()
      saveAuthState(result)
      return result
    } catch {
      saveAuthState(null)
      setActiveView('auth')
      return null
    }
  }

  const captureAuthConsents = async (accessToken, sourceSurface) => {
    if (!accessToken) {
      return null
    }

    return callApi('borrower-consents', endpoints.consents, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${accessToken}`,
        'Content-Type': 'application/json',
        'X-Session-Id': sessionId,
      },
      body: JSON.stringify({
        sourceSurface,
        termsOfService: true,
        privacyNotice: true,
        marketingEmail: false,
      }),
    })
  }

  const handleSubscriptionSubmit = async (event) => {
    event.preventDefault()
    const selectedTopics = [
      subscriptionForm.productUpdates === 'true' ? 'Product updates' : null,
      subscriptionForm.rateAlerts === 'true' ? 'Rate alerts' : null,
      subscriptionForm.partnerAlerts === 'true' ? 'Lender and agent updates' : null,
    ].filter(Boolean)
    const topicCount = selectedTopics.length
    if (!subscriptionForm.email.trim()) {
      setSubscriptionMessage('Enter an email address to subscribe.')
      return
    }
    if (topicCount === 0) {
      setSubscriptionMessage(`We saved ${subscriptionForm.email.trim()}, but choose at least one alert type before this goes live.`)
      return
    }

    const sourceSurface = event.currentTarget.dataset.sourceSurface || 'borrower_landing_subscription'
    const result = await callApi('subscription', endpoints.subscriptions, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Session-Id': sessionId,
        ...(authState?.accessToken ? { Authorization: `Bearer ${authState.accessToken}` } : {}),
      },
      body: JSON.stringify({
        email: subscriptionForm.email.trim(),
        productUpdates: subscriptionForm.productUpdates === 'true',
        rateAlerts: subscriptionForm.rateAlerts === 'true',
        partnerAlerts: subscriptionForm.partnerAlerts === 'true',
        sourceSurface,
      }),
    })

    if (result) {
      setSubscriptionReceipt({
        email: result.email,
        topics: selectedTopics,
        unsubscribeToken: result.unsubscribeToken,
        sourceSurface: result.sourceSurface,
      })
      setUnsubscribeToken(result.unsubscribeToken || '')
      setUnsubscribeMessage('')
      setSubscriptionMessage(`Subscribed ${result.email} to ${selectedTopics.join(', ')}.`)
    }
  }

  const handleUnsubscribeSubmit = async (event) => {
    event.preventDefault()
    if (!unsubscribeToken.trim()) {
      setUnsubscribeMessage('Enter an unsubscribe token to stop email updates.')
      return
    }

    const result = await callApi('unsubscribe', endpoints.unsubscribeSubscription, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ unsubscribeToken: unsubscribeToken.trim() }),
    })

    if (result) {
      setSubscriptionReceipt((current) => current ? { ...current, unsubscribeToken: unsubscribeToken.trim() } : current)
      setUnsubscribeMessage(`Unsubscribed ${result.email} from optional Harbor email updates.`)
    }
  }

  const callApi = async (target, endpoint, options = {}) => {
    setErrorMessage('')
    setLoadingTarget(target)

    try {
      const initialHeaders = { ...(options.headers || {}) }
      let response = await fetch(endpoint, {
        headers: initialHeaders,
        ...options,
      })

      if (response.status === 401 && String(initialHeaders.Authorization || '').startsWith('Bearer ') && authState?.refreshToken) {
        const refreshed = await refreshAuthSession(authState)
        if (refreshed?.accessToken) {
          response = await fetch(endpoint, {
            ...options,
            headers: {
              ...initialHeaders,
              Authorization: `Bearer ${refreshed.accessToken}`,
            },
          })
        }
      }

      if (response.status === 401) {
        saveAuthState(null)
        setActiveView('auth')
        setErrorMessage('Your session has expired. Please sign in to continue.')
        return null
      }

      if (!response.ok) {
        const errorBody = await response.text()
        throw new Error(errorBody || 'Unable to complete this request.')
      }

      if (response.status === 204) {
        return null
      }
      return await response.json()
    } catch (error) {
      setErrorMessage(error.message || 'Something went wrong while calling the API.')
      return null
    } finally {
      setLoadingTarget('')
    }
  }

  const callApiOptional = async (target, endpoint, options = {}) => {
    setLoadingTarget(target)
    try {
      const initialHeaders = { ...(options.headers || {}) }
      let response = await fetch(endpoint, {
        headers: initialHeaders,
        ...options,
      })

      if (response.status === 401 && String(initialHeaders.Authorization || '').startsWith('Bearer ') && authState?.refreshToken) {
        const refreshed = await refreshAuthSession(authState)
        if (refreshed?.accessToken) {
          response = await fetch(endpoint, {
            ...options,
            headers: {
              ...initialHeaders,
              Authorization: `Bearer ${refreshed.accessToken}`,
            },
          })
        }
      }

      if (response.status === 401) {
        saveAuthState(null)
        setActiveView('auth')
        setErrorMessage('Your session has expired. Please sign in to continue.')
        return null
      }

      if (response.status === 404) {
        return null
      }

      if (!response.ok) {
        const errorBody = await response.text()
        throw new Error(errorBody || 'Unable to complete this request.')
      }

      if (response.status === 204) {
        return null
      }
      return await response.json()
    } catch (error) {
      setErrorMessage(error.message || 'Something went wrong while calling the API.')
      return null
    } finally {
      setLoadingTarget('')
    }
  }

  const callCalculator = async (target, formValues, endpoint) => {
    const params = new URLSearchParams(formValues)
    return callApi(target, `${endpoint}?${params.toString()}`, { method: 'GET', headers: {} })
  }

  const postAuthSessionMetric = async (authEventType) => {
    try {
      await fetch(`${endpoints.authSessionMetric}?authEventType=${encodeURIComponent(authEventType)}`, {
        method: 'POST',
        headers: { 'X-Session-Id': sessionId },
      })
    } catch {
    }
  }

  const hydrateBorrowerContext = async (accessToken = authState?.accessToken) => {
    if (!accessToken) {
      return { currentQuote: null, borrowerProfile: null }
    }

    const [currentQuote, borrowerProfile, borrowerQuotes] = await Promise.all([
      callApiOptional('borrower-current-quote', endpoints.borrowerCurrentQuote, {
        method: 'GET',
        headers: {
          Authorization: `Bearer ${accessToken}`,
          'X-Session-Id': sessionId,
        },
      }),
      callApiOptional('borrower-profile', endpoints.borrowerProfile, {
        method: 'GET',
        headers: { Authorization: `Bearer ${accessToken}` },
      }),
      callApiOptional('borrower-quote-history', endpoints.borrowerQuotes, {
        method: 'GET',
        headers: {
          Authorization: `Bearer ${accessToken}`,
          'X-Session-Id': sessionId,
        },
      }),
    ])

    if (currentQuote) {
      setQuoteResult(currentQuote)
    }

    if (borrowerProfile) {
      setRefineForm((current) => ({
        ...current,
        firstName: borrowerProfile.firstName ?? current.firstName,
        lastName: borrowerProfile.lastName ?? current.lastName,
        email: borrowerProfile.email ?? borrowerProfile.accountEmail ?? current.email,
        phone: borrowerProfile.phone ?? current.phone,
        stateCode: borrowerProfile.stateCode ?? current.stateCode,
        countyName: borrowerProfile.countyName ?? current.countyName,
        annualIncome: borrowerProfile.annualIncome != null ? String(borrowerProfile.annualIncome) : current.annualIncome,
        monthlyDebts: borrowerProfile.monthlyDebts != null ? String(borrowerProfile.monthlyDebts) : current.monthlyDebts,
        creditScore: borrowerProfile.creditScore != null ? String(borrowerProfile.creditScore) : current.creditScore,
        cashReserves: borrowerProfile.cashReserves != null ? String(borrowerProfile.cashReserves) : current.cashReserves,
        firstTimeBuyer: borrowerProfile.firstTimeBuyer != null ? String(borrowerProfile.firstTimeBuyer) : current.firstTimeBuyer,
        vaEligible: borrowerProfile.vaEligible != null ? String(borrowerProfile.vaEligible) : current.vaEligible,
        estimatedFundingDate: borrowerProfile.estimatedFundingDate ?? current.estimatedFundingDate,
      }))
    }

    setQuoteHistory(Array.isArray(borrowerQuotes) ? borrowerQuotes : [])

    return { currentQuote, borrowerProfile }
  }

  const attachSessionQuotes = async (accessToken = authState?.accessToken) => {
    if (!accessToken) {
      return null
    }

    try {
      const response = await fetch(endpoints.borrowerAttachSession, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${accessToken}`,
          'X-Session-Id': sessionId,
        },
      })
      if (!response.ok || response.status === 204) {
        return null
      }
      return await response.json()
    } catch {
      return null
    }
  }

  const fetchBorrowerQuoteHistory = async (accessToken = authState?.accessToken) => {
    if (!accessToken) {
      setQuoteHistory([])
      return []
    }
    const result = await callApiOptional('borrower-quote-history', endpoints.borrowerQuotes, {
      method: 'GET',
      headers: {
        Authorization: `Bearer ${accessToken}`,
        'X-Session-Id': sessionId,
      },
    })
    const history = Array.isArray(result) ? result : []
    setQuoteHistory(history)
    return history
  }

  const loadBorrowerQuote = async (quoteId, accessToken = authState?.accessToken) => {
    if (!accessToken) {
      return
    }
    const selected = await callApiOptional('borrower-quote-selected', `${endpoints.borrowerQuotes}/${quoteId}`, {
      method: 'GET',
      headers: {
        Authorization: `Bearer ${accessToken}`,
        'X-Session-Id': sessionId,
      },
    })
    if (selected) {
      setQuoteResult(selected)
      setActiveView('quote-results')
    }
  }

  const handleDeleteBorrowerQuote = async (quoteId) => {
    if (!authState?.accessToken) {
      setActiveView('auth')
      return
    }
    if (typeof window !== 'undefined' && !window.confirm(`Delete quote #${quoteId}? This cannot be undone.`)) {
      return
    }
    await callApi('delete-quote', `${endpoints.borrowerQuotes}/${quoteId}`, {
      method: 'DELETE',
      headers: {
        Authorization: `Bearer ${authState.accessToken}`,
        'X-Session-Id': sessionId,
      },
    })
    const history = await fetchBorrowerQuoteHistory(authState.accessToken)
    if (quoteResult?.id === quoteId) {
      if (history.length > 0) {
        setQuoteResult(history[0])
        setActiveView('quote-results')
      } else {
        setQuoteResult(null)
        setActiveView('home')
      }
    }
  }

  const fetchDirectoryMatches = async (stateCode, countyName) => {
    if (!stateCode || !countyName) {
      setMatchedAgents([])
      setMatchedLenders([])
      return
    }

    try {
      const params = new URLSearchParams({ stateCode, countyName })
      const [agentsResponse, lendersResponse] = await Promise.all([
        fetch(`${endpoints.agents}?${params.toString()}`),
        fetch(`${endpoints.lenders}?${params.toString()}`),
      ])
      const [agents, lenders] = await Promise.all([
        agentsResponse.ok ? agentsResponse.json() : [],
        lendersResponse.ok ? lendersResponse.json() : [],
      ])
      setMatchedAgents(Array.isArray(agents) ? agents : [])
      setMatchedLenders(Array.isArray(lenders) ? lenders : [])
    } catch {
      setMatchedAgents([])
      setMatchedLenders([])
    }
  }

  const handlePublicQuoteSubmit = async (event) => {
    if (event) {
      event.preventDefault()
    }

    const result = await callApi('public-quote', endpoints.publicQuote, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Session-Id': sessionId,
      },
      body: JSON.stringify({
        ...publicQuoteForm,
        homePrice: Number(publicQuoteForm.homePrice),
        downPayment: Number(publicQuoteForm.downPayment),
        termYears: Number(publicQuoteForm.termYears),
      }),
    })

    if (result) {
      setQuoteResult(result)
      setActiveView('quote-results')
      if (authState?.accessToken) {
        await attachSessionQuotes(authState.accessToken)
        await fetchBorrowerQuoteHistory(authState.accessToken)
      }
    }
  }

  const buildRefinePayload = () => ({
    ...refineForm,
    annualIncome: Number(refineForm.annualIncome),
    monthlyDebts: Number(refineForm.monthlyDebts),
    creditScore: Number(refineForm.creditScore),
    cashReserves: Number(refineForm.cashReserves),
    firstTimeBuyer: refineForm.firstTimeBuyer === 'true',
    vaEligible: refineForm.vaEligible === 'true',
  })

  const handleRefineProgressSave = async () => {
    if (!authState?.accessToken) {
      return false
    }

    await attachSessionQuotes(authState.accessToken)
    const refineEndpoint = quoteResult?.id
      ? `${endpoints.borrowerQuotes}/${quoteResult.id}/refine`
      : endpoints.borrowerRefineLatest

    setLoadingTarget('refine-progress')
    try {
      const response = await fetch(refineEndpoint, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${authState.accessToken}`,
          'Content-Type': 'application/json',
          'X-Session-Id': sessionId,
        },
        body: JSON.stringify(buildRefinePayload()),
      })
      if (!response.ok) {
        return false
      }
      const result = await response.json()
      setQuoteResult(result)
      await fetchBorrowerQuoteHistory(authState.accessToken)
      return true
    } catch {
      return false
    } finally {
      setLoadingTarget('')
    }
  }

  const handleRefineQuoteSubmit = async (event) => {
    event.preventDefault()

    if (!authState?.accessToken) {
      setErrorMessage('Sign in before personalizing the quote.')
      setActiveView('auth')
      return
    }

    await attachSessionQuotes(authState.accessToken)
    const refineEndpoint = quoteResult?.id
      ? `${endpoints.borrowerQuotes}/${quoteResult.id}/refine`
      : endpoints.borrowerRefineLatest

    const result = await callApi('refine-quote', refineEndpoint, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${authState.accessToken}`,
        'Content-Type': 'application/json',
        'X-Session-Id': sessionId,
      },
      body: JSON.stringify(buildRefinePayload()),
    })

    if (result) {
      setQuoteResult(result)
      await fetchBorrowerQuoteHistory(authState.accessToken)
      setActiveView('quote-matches')
    }
  }

  const handleLoginSubmit = async (event) => {
    event.preventDefault()
    const result = await callApi('login', endpoints.login, {
      method: 'POST',
      body: JSON.stringify(loginForm),
      headers: { 'Content-Type': 'application/json' },
    })

    if (result) {
      saveAuthState(result)
      setRegisterForm((current) => ({ ...current, email: result.email }))
      await postAuthSessionMetric('LOGIN')
      await captureAuthConsents(result.accessToken, 'borrower_auth_signin')
      await attachSessionQuotes(result.accessToken)
      const hydrated = await hydrateBorrowerContext(result.accessToken)
      setActiveView((quoteResult || hydrated?.currentQuote) ? 'quote-refine' : 'home')
    }
  }

  const handleRegisterSubmit = async (event) => {
    event.preventDefault()
    const result = await callApi('register', endpoints.register, {
      method: 'POST',
      body: JSON.stringify(registerForm),
      headers: { 'Content-Type': 'application/json' },
    })

    if (result) {
      saveAuthState(result)
      setLoginForm((current) => ({ ...current, email: result.email }))
      await postAuthSessionMetric('REGISTER')
      await captureAuthConsents(result.accessToken, 'borrower_auth_register')
      await attachSessionQuotes(result.accessToken)
      const hydrated = await hydrateBorrowerContext(result.accessToken)
      setActiveView((quoteResult || hydrated?.currentQuote) ? 'quote-refine' : 'home')
    }
  }

  const handleMortgageSubmit = async (event) => {
    event.preventDefault()
    const result = await callCalculator('mortgage', mortgageForm, endpoints.mortgage)
    if (result) {
      setMortgageResult(result)
    }
  }

  const handleAmortizationSubmit = async (event) => {
    event.preventDefault()
    const result = await callCalculator('amortization', amortizationForm, endpoints.amortization)
    if (result) {
      setAmortizationResult(result)
    }
  }

  const handleSignOut = () => {
    if (authState?.refreshToken) {
      fetch(endpoints.logout, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken: authState.refreshToken }),
      }).catch(() => {})
    }
    saveAuthState(null)
    setErrorMessage('')
  }

  useEffect(() => {
    let ignore = false

    const loadPolicyVersions = async () => {
      try {
        const response = await fetch(endpoints.legalPolicies)
        if (!response.ok) {
          return
        }
        const result = await response.json()
        if (!ignore) {
          setPolicyVersions(result)
        }
      } catch {
      }
    }

    loadPolicyVersions()

    const loadLocations = async () => {
      try {
        const response = await fetch(endpoints.locationOptions)
        if (!response.ok) {
          return
        }
        const result = await response.json()
        if (!ignore) {
          setLocationOptions(Array.isArray(result) ? result : [])
        }
      } catch {
      }
    }

    loadLocations()
    return () => {
      ignore = true
    }
  }, [endpoints.legalPolicies, endpoints.locationOptions])

  useEffect(() => {
    if (locationOptions.length === 0) {
      return
    }
    const selectedState = locationOptions.find((location) => location.stateCode === publicQuoteForm.stateCode)
    if (!selectedState) {
      setPublicQuoteForm((current) => ({
        ...current,
        stateCode: locationOptions[0].stateCode,
        countyName: locationOptions[0].counties[0] ?? '',
      }))
      return
    }
    if (!selectedState.counties.includes(publicQuoteForm.countyName)) {
      setPublicQuoteForm((current) => ({
        ...current,
        countyName: selectedState.counties[0] ?? '',
      }))
    }
  }, [locationOptions, publicQuoteForm.stateCode, publicQuoteForm.countyName])

  useEffect(() => {
    if (locationOptions.length === 0) {
      return
    }
    const selectedState = locationOptions.find((location) => location.stateCode === refineForm.stateCode)
    if (!selectedState) {
      setRefineForm((current) => ({
        ...current,
        stateCode: locationOptions[0].stateCode,
        countyName: locationOptions[0].counties[0] ?? '',
      }))
      return
    }
    if (!selectedState.counties.includes(refineForm.countyName)) {
      setRefineForm((current) => ({
        ...current,
        countyName: selectedState.counties[0] ?? '',
      }))
    }
  }, [locationOptions, refineForm.stateCode, refineForm.countyName])

  useEffect(() => {
    fetchDirectoryMatches(publicQuoteForm.stateCode, publicQuoteForm.countyName)
  }, [publicQuoteForm.stateCode, publicQuoteForm.countyName])

  useEffect(() => {
    if (!quoteResult) {
      return
    }

    setPublicQuoteForm((current) => ({
      ...current,
      homePrice: quoteResult.homePrice != null ? String(quoteResult.homePrice) : current.homePrice,
      downPayment: quoteResult.downPayment != null ? String(quoteResult.downPayment) : current.downPayment,
      zipCode: quoteResult.zipCode ?? current.zipCode,
      stateCode: quoteResult.stateCode ?? current.stateCode,
      countyName: quoteResult.countyName ?? current.countyName,
      loanProgram: quoteResult.loanProgram ?? current.loanProgram,
      propertyUse: quoteResult.propertyUse ?? current.propertyUse,
      termYears: quoteResult.termYears != null ? String(quoteResult.termYears) : current.termYears,
    }))
  }, [quoteResult?.id])

  useEffect(() => {
    if (!authState?.accessToken) {
      return
    }
    const hydrate = async () => {
      const hydrated = await hydrateBorrowerContext(authState.accessToken)
      if (activeView === 'home' && hydrated?.currentQuote) {
        setActiveView('quote-results')
      }
    }
    hydrate()
  }, [authState?.accessToken])

  useEffect(() => {
    if (typeof window === 'undefined' || !quoteResult?.id || !isQuoteInFlight(quoteResult)) {
      return undefined
    }

    const stream = new EventSource(`${endpoints.quoteEventsBase}/${quoteResult.id}/events`)
    stream.onmessage = (event) => {
      try {
        const payload = JSON.parse(event.data)
        setQuoteResult(payload)
        if (!isQuoteInFlight(payload)) {
          stream.close()
        }
      } catch {
      }
    }
    stream.onerror = () => stream.close()

    return () => stream.close()
  }, [endpoints.quoteEventsBase, quoteResult?.id, quoteResult?.processingStatus])

  useEffect(() => {
    if (typeof document === 'undefined') {
      return undefined
    }
    const className = `demo-view-${activeView}`
    document.body.classList.add(className)
    return () => {
      document.body.classList.remove(className)
    }
  }, [activeView])

  return (
    <BorrowerApp
      activeView={activeView}
      setActiveView={setActiveView}
      publicQuoteForm={publicQuoteForm}
      setPublicQuoteForm={setPublicQuoteForm}
      handlePublicQuoteSubmit={handlePublicQuoteSubmit}
      locationOptions={locationOptions}
      quoteResult={quoteResult}
      loadingTarget={loadingTarget}
      handleInput={handleInput}
      loginForm={loginForm}
      setLoginForm={setLoginForm}
      registerForm={registerForm}
      setRegisterForm={setRegisterForm}
      handleLoginSubmit={handleLoginSubmit}
      handleRegisterSubmit={handleRegisterSubmit}
      authState={authState}
      onSignOut={handleSignOut}
      refineForm={refineForm}
      setRefineForm={setRefineForm}
      handleRefineQuoteSubmit={handleRefineQuoteSubmit}
      handleRefineProgressSave={handleRefineProgressSave}
      matchedLenders={matchedLenders}
      matchedAgents={matchedAgents}
      errorMessage={errorMessage}
      activeCalculator={activeCalculator}
      setActiveCalculator={setActiveCalculator}
      mortgageForm={mortgageForm}
      setMortgageForm={setMortgageForm}
      mortgageResult={mortgageResult}
      amortizationForm={amortizationForm}
      setAmortizationForm={setAmortizationForm}
      amortizationResult={amortizationResult}
      handleMortgageSubmit={handleMortgageSubmit}
      handleAmortizationSubmit={handleAmortizationSubmit}
      quoteHistory={quoteHistory}
      onSelectBorrowerQuote={loadBorrowerQuote}
      onDeleteBorrowerQuote={handleDeleteBorrowerQuote}
    />
  )
}

export default App

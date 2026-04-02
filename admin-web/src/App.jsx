import { useEffect, useMemo, useState } from 'react'
import './App.css'
import AdminApp from './components/AdminApp'
import {
  API_BASE_URL,
  AUTH_STORAGE_KEY,
  defaultAdminLogin,
  defaultBorrowerForm,
  defaultLoanForm,
  defaultPartnerForm,
  defaultProductForm,
  defaultRateSheetForm,
  defaultReportForm,
  formatCurrency,
  getOrCreateSessionId,
  getStoredAuth,
} from './lib/adminApp'


function App() {
  const [activeTab, setActiveTab] = useState('dashboard')
  const [authState, setAuthState] = useState(() => getStoredAuth())
  const [loginForm, setLoginForm] = useState(defaultAdminLogin)
  const [summary, setSummary] = useState(null)
  const [borrowerForm, setBorrowerForm] = useState(defaultBorrowerForm)
  const [borrowers, setBorrowers] = useState([])
  const [createdBorrower, setCreatedBorrower] = useState(null)
  const [loanForm, setLoanForm] = useState(defaultLoanForm)
  const [loanLookupId, setLoanLookupId] = useState('')
  const [loanResult, setLoanResult] = useState(null)
  const [productForm, setProductForm] = useState(defaultProductForm)
  const [products, setProducts] = useState([])
  const [editingProductId, setEditingProductId] = useState(null)
  const [lenderForm, setLenderForm] = useState(defaultPartnerForm)
  const [agentForm, setAgentForm] = useState(defaultPartnerForm)
  const [lenders, setLenders] = useState([])
  const [agents, setAgents] = useState([])
  const [editingLenderId, setEditingLenderId] = useState(null)
  const [editingAgentId, setEditingAgentId] = useState(null)
  const [reportForm, setReportForm] = useState(defaultReportForm)
  const [reportResult, setReportResult] = useState(null)
  const [rateSheetForm, setRateSheetForm] = useState(defaultRateSheetForm)
  const [rateSheetResult, setRateSheetResult] = useState(null)
  const [errorMessage, setErrorMessage] = useState('')
  const [loadingTarget, setLoadingTarget] = useState('')
  const [sessionId] = useState(() => getOrCreateSessionId())

  const isAdmin = authState?.role === 'ADMIN'

  const endpointPreview = useMemo(() => ({
    login: `${API_BASE_URL}/auth/login`,
    refresh: `${API_BASE_URL}/auth/refresh`,
    logout: `${API_BASE_URL}/auth/logout`,
    adminSummary: `${API_BASE_URL}/metrics/admin/summary`,
    borrowers: `${API_BASE_URL}/borrowers`,
    loans: `${API_BASE_URL}/loans`,
    products: `${API_BASE_URL}/admin/products`,
    lenders: `${API_BASE_URL}/admin/lenders`,
    agents: `${API_BASE_URL}/admin/agents`,
    lenderSync: `${API_BASE_URL}/admin/lenders/sync`,
    agentSync: `${API_BASE_URL}/admin/agents/sync`,
    reports: `${API_BASE_URL}/admin/reports/query`,
    reportExport: `${API_BASE_URL}/admin/reports/export`,
    rateSheets: `${API_BASE_URL}/admin/rate-sheets`,
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
      return null
    }

    try {
      const response = await fetch(endpointPreview.refresh, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken: currentAuthState.refreshToken }),
      })

      if (!response.ok) {
        saveAuthState(null)
        return null
      }

      const result = await response.json()
      saveAuthState(result)
      return result
    } catch {
      saveAuthState(null)
      return null
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
      }

      if (!response.ok) {
        const errorBody = await response.text()
        throw new Error(errorBody || 'Unable to complete this admin request.')
      }

      if (response.status === 204) {
        return {}
      }

      return await response.json()
    } catch (error) {
      setErrorMessage(error.message || 'Unable to complete this admin request.')
      return null
    } finally {
      setLoadingTarget('')
    }
  }

  const confirmAction = (message) => {
    if (typeof window === 'undefined' || typeof window.confirm !== 'function') {
      return true
    }
    return window.confirm(message)
  }

  const fetchSummary = async () => {
    if (!authState?.accessToken) {
      return
    }

    const result = await callApi('summary', endpointPreview.adminSummary, {
      method: 'GET',
      headers: { Authorization: `Bearer ${authState.accessToken}` },
    })

    if (result) {
      setSummary(result)
    }
  }

  const handleLoginSubmit = async (event) => {
    event.preventDefault()
    const result = await callApi('login', endpointPreview.login, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(loginForm),
    })

    if (result) {
      saveAuthState(result)
    }
  }

  const fetchBorrowers = async () => {
    if (!authState?.accessToken) {
      setErrorMessage('Sign in as an admin before loading borrowers.')
      return
    }

    const result = await callApi('borrowers', endpointPreview.borrowers, {
      method: 'GET',
      headers: { Authorization: `Bearer ${authState.accessToken}` },
    })

    if (result) {
      setBorrowers(result)
    }
  }

  const createBorrower = async (event) => {
    event.preventDefault()
    if (!authState?.accessToken) {
      setErrorMessage('Sign in as an admin before creating a borrower.')
      return
    }

    const result = await callApi('create-borrower', endpointPreview.borrowers, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${authState.accessToken}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        ...borrowerForm,
        creditScore: Number(borrowerForm.creditScore),
      }),
    })

    if (result) {
      setCreatedBorrower(result)
      setLoanForm((current) => ({ ...current, borrowerId: String(result.id) }))
      fetchBorrowers()
    }
  }

  const createLoan = async (event) => {
    event.preventDefault()
    if (!authState?.accessToken) {
      setErrorMessage('Sign in as an admin before creating a loan.')
      return
    }

    const result = await callApi('create-loan', endpointPreview.loans, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${authState.accessToken}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        ...loanForm,
        borrowerId: Number(loanForm.borrowerId),
        loanAmount: Number(loanForm.loanAmount),
        interestRate: Number(loanForm.interestRate),
        termYears: Number(loanForm.termYears),
      }),
    })

    if (result) {
      setLoanResult(result)
      setLoanLookupId(String(result.id))
    }
  }

  const fetchLoan = async () => {
    if (!loanLookupId.trim()) {
      setErrorMessage('Enter a loan ID to load the loan snapshot.')
      return
    }

    const result = await callApi('loan-monitor', `${endpointPreview.loans}/${loanLookupId.trim()}`, {
      method: 'GET',
      headers: {},
    })

    if (result) {
      setLoanResult(result)
    }
  }

  const loadProducts = async () => {
    const result = await callApi('products', endpointPreview.products, {
      method: 'GET',
      headers: { Authorization: `Bearer ${authState.accessToken}` },
    })

    if (result) {
      setProducts(result)
    }
  }

  const startEditProduct = (product) => {
    setEditingProductId(product.id)
    setProductForm({
      programCode: product.programCode,
      productName: product.productName,
      baseRate: String(product.baseRate),
      active: String(product.active),
    })
  }

  const resetProductForm = () => {
    setEditingProductId(null)
    setProductForm(defaultProductForm)
  }

  const parseRateSheetEntries = (csv) => {
    if (!csv.trim()) return []
    return csv.trim().split('\n').map((line) => {
      const parts = line.split(',').map((s) => s.trim())
      return {
        productTermId: parts[0] || '',
        rate: parts[1] ? Number(parts[1]) : 0,
        price: parts[2] ? Number(parts[2]) : 0,
      }
    }).filter((e) => e.productTermId)
  }

  const publishRateSheet = async (event) => {
    event.preventDefault()
    if (!authState?.accessToken) {
      setErrorMessage('Sign in as an admin before publishing a rate sheet.')
      return
    }
    const entries = parseRateSheetEntries(rateSheetForm.entriesCsv)
    const result = await callApi('publish-rate-sheet', endpointPreview.rateSheets, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${authState.accessToken}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        investorId: rateSheetForm.investorId,
        effectiveAt: rateSheetForm.effectiveAt || null,
        expiresAt: rateSheetForm.expiresAt || null,
        source: rateSheetForm.source,
        entries,
      }),
    })
    if (result) {
      setRateSheetResult(result)
      setRateSheetForm(defaultRateSheetForm)
    }
  }

  const loadPartners = async (partnerType) => {
    const endpoint = partnerType === 'lenders' ? endpointPreview.lenders : endpointPreview.agents
    const setter = partnerType === 'lenders' ? setLenders : setAgents
    const result = await callApi(`${partnerType}-load`, endpoint, {
      method: 'GET',
      headers: { Authorization: `Bearer ${authState.accessToken}` },
    })

    if (result) {
      setter(result)
    }
  }

  const startEditPartner = (partnerType, partner) => {
    const editingSetter = partnerType === 'lenders' ? setEditingLenderId : setEditingAgentId
    const setter = partnerType === 'lenders' ? setLenderForm : setAgentForm
    editingSetter(partner.id)
    setter({
      displayName: partner.displayName ?? '',
      companyName: partner.companyName ?? '',
      email: partner.email ?? '',
      phone: partner.phone ?? '',
      stateCode: partner.stateCode ?? '',
      countyName: partner.countyName ?? '',
      city: partner.city ?? '',
      specialty: partner.specialty ?? '',
      licenseNumber: partner.licenseNumber ?? '',
      nmlsId: partner.nmlsId ?? '',
      rankingScore: partner.rankingScore ? String(partner.rankingScore) : '',
      responseSlaHours: partner.responseSlaHours ? String(partner.responseSlaHours) : '',
      languages: partner.languages ?? '',
      websiteUrl: partner.websiteUrl ?? '',
      active: String(partner.active),
    })
  }

  const resetPartnerForm = (partnerType) => {
    if (partnerType === 'lenders') {
      setEditingLenderId(null)
      setLenderForm(defaultPartnerForm)
      return
    }
    setEditingAgentId(null)
    setAgentForm(defaultPartnerForm)
  }

  const savePartner = async (event, partnerType) => {
    event.preventDefault()
    const endpointBase = partnerType === 'lenders' ? endpointPreview.lenders : endpointPreview.agents
    const form = partnerType === 'lenders' ? lenderForm : agentForm
    const editingId = partnerType === 'lenders' ? editingLenderId : editingAgentId
    const endpoint = editingId ? `${endpointBase}/${editingId}` : endpointBase
    const method = editingId ? 'PUT' : 'POST'
    const result = await callApi(`${partnerType}-save`, endpoint, {
      method,
      headers: {
        Authorization: `Bearer ${authState.accessToken}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        ...form,
        rankingScore: form.rankingScore ? Number(form.rankingScore) : null,
        responseSlaHours: form.responseSlaHours ? Number(form.responseSlaHours) : null,
        active: form.active === 'true',
      }),
    })

    if (result) {
      resetPartnerForm(partnerType)
      loadPartners(partnerType)
    }
  }

  const deletePartner = async (partnerType, id) => {
    if (!confirmAction(`Remove this ${partnerType === 'lenders' ? 'lender' : 'agent'} entry?`)) {
      return
    }
    const endpointBase = partnerType === 'lenders' ? endpointPreview.lenders : endpointPreview.agents
    const result = await callApi(`${partnerType}-delete-${id}`, `${endpointBase}/${id}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${authState.accessToken}` },
    })

    if (result !== null) {
      if (partnerType === 'lenders' && editingLenderId === id) {
        resetPartnerForm('lenders')
      }
      if (partnerType === 'agents' && editingAgentId === id) {
        resetPartnerForm('agents')
      }
      loadPartners(partnerType)
    }
  }

  const syncPartners = async (partnerType) => {
    const endpoint = partnerType === 'lenders' ? endpointPreview.lenderSync : endpointPreview.agentSync
    const result = await callApi(`${partnerType}-sync`, `${endpoint}?provider=external-csv`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${authState.accessToken}` },
    })

    if (result) {
      loadPartners(partnerType)
      fetchSummary()
    }
  }

  const saveProduct = async (event) => {
    event.preventDefault()
    const endpoint = editingProductId ? `${endpointPreview.products}/${editingProductId}` : endpointPreview.products
    const method = editingProductId ? 'PUT' : 'POST'
    const result = await callApi('save-product', endpoint, {
      method,
      headers: {
        Authorization: `Bearer ${authState.accessToken}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        ...productForm,
        baseRate: Number(productForm.baseRate),
        active: productForm.active === 'true',
      }),
    })

    if (result) {
      resetProductForm()
      loadProducts()
      fetchSummary()
    }
  }

  const deleteProduct = async (id) => {
    if (!confirmAction('Remove this product from the pricing catalog?')) {
      return
    }
    const result = await callApi(`delete-product-${id}`, `${endpointPreview.products}/${id}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${authState.accessToken}` },
    })

    if (result !== null) {
      if (editingProductId === id) {
        resetProductForm()
      }
      setProducts((current) => current.filter((product) => product.id !== id))
      fetchSummary()
    }
  }

  const runReport = async (event) => {
    event.preventDefault()
    const result = await callApi('run-report', endpointPreview.reports, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${authState.accessToken}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        ...reportForm,
        activeOnly: reportForm.activeOnly === 'true',
        minCreditScore: reportForm.minCreditScore ? Number(reportForm.minCreditScore) : null,
        maxCreditScore: reportForm.maxCreditScore ? Number(reportForm.maxCreditScore) : null,
        dateFrom: reportForm.dateFrom || null,
        dateTo: reportForm.dateTo || null,
        page: Number(reportForm.page || '1'),
        pageSize: Number(reportForm.pageSize || '25'),
      }),
    })

    if (result) {
      setReportResult(result)
    }
  }

  const exportReport = async () => {
    setErrorMessage('')
    setLoadingTarget('export-report')
    try {
      const response = await fetch(endpointPreview.reportExport, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${authState.accessToken}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          ...reportForm,
          activeOnly: reportForm.activeOnly === 'true',
          minCreditScore: reportForm.minCreditScore ? Number(reportForm.minCreditScore) : null,
          maxCreditScore: reportForm.maxCreditScore ? Number(reportForm.maxCreditScore) : null,
          dateFrom: reportForm.dateFrom || null,
          dateTo: reportForm.dateTo || null,
          page: 1,
          pageSize: Number(reportForm.pageSize || '25'),
        }),
      })

      if (!response.ok) {
        throw new Error(await response.text() || 'Unable to export this report.')
      }

      const csv = await response.text()
      const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = 'mortgage-desk-report.csv'
      a.click()
      URL.revokeObjectURL(url)
    } catch (error) {
      setErrorMessage(error.message || 'Unable to export this report.')
    } finally {
      setLoadingTarget('')
    }
  }

  const handleSignOut = () => {
    if (authState?.refreshToken) {
      fetch(endpointPreview.logout, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken: authState.refreshToken }),
      }).catch(() => {})
    }
    saveAuthState(null)
    setSummary(null)
    setBorrowers([])
    setCreatedBorrower(null)
    setLoanResult(null)
    setProducts([])
    setReportResult(null)
    setErrorMessage('')
  }

  useEffect(() => {
    if (isAdmin) {
      fetchSummary()
    }
  }, [isAdmin])

  useEffect(() => {
    if (isAdmin && activeTab === 'pricing') {
      loadProducts()
    }
  }, [activeTab, isAdmin])

  useEffect(() => {
    if (isAdmin && activeTab === 'lenders') {
      loadPartners('lenders')
    }
  }, [activeTab, isAdmin])

  useEffect(() => {
    if (isAdmin && activeTab === 'agents') {
      loadPartners('agents')
    }
  }, [activeTab, isAdmin])

  useEffect(() => {
    if (typeof document === 'undefined') {
      return undefined
    }
    document.body.classList.add('v3-parity-mode')
    return () => {
      document.body.classList.remove('v3-parity-mode')
    }
  }, [])

  return (
    <AdminApp
      activeTab={activeTab}
      setActiveTab={setActiveTab}
      authState={authState}
      onSignOut={handleSignOut}
      loginForm={loginForm}
      setLoginForm={setLoginForm}
      handleLoginSubmit={handleLoginSubmit}
      handleInput={handleInput}
      loadingTarget={loadingTarget}
      errorMessage={errorMessage}
      summary={summary}
      fetchSummary={fetchSummary}
      borrowerForm={borrowerForm}
      setBorrowerForm={setBorrowerForm}
      createBorrower={createBorrower}
      fetchBorrowers={fetchBorrowers}
      borrowers={borrowers}
      createdBorrower={createdBorrower}
      loanForm={loanForm}
      setLoanForm={setLoanForm}
      createLoan={createLoan}
      fetchLoan={fetchLoan}
      loanLookupId={loanLookupId}
      setLoanLookupId={setLoanLookupId}
      loanResult={loanResult}
      productForm={productForm}
      setProductForm={setProductForm}
      products={products}
      editingProductId={editingProductId}
      saveProduct={saveProduct}
      startEditProduct={startEditProduct}
      resetProductForm={resetProductForm}
      deleteProduct={deleteProduct}
      loadProducts={loadProducts}
      lenderForm={lenderForm}
      setLenderForm={setLenderForm}
      lenders={lenders}
      editingLenderId={editingLenderId}
      agentForm={agentForm}
      setAgentForm={setAgentForm}
      agents={agents}
      editingAgentId={editingAgentId}
      loadPartners={loadPartners}
      savePartner={savePartner}
      startEditPartner={startEditPartner}
      resetPartnerForm={resetPartnerForm}
      deletePartner={deletePartner}
      syncPartners={syncPartners}
      reportForm={reportForm}
      setReportForm={setReportForm}
      reportResult={reportResult}
      runReport={runReport}
      exportReport={exportReport}
      rateSheetForm={rateSheetForm}
      setRateSheetForm={setRateSheetForm}
      rateSheetResult={rateSheetResult}
      publishRateSheet={publishRateSheet}
      endpointPreview={endpointPreview}
      formatCurrency={formatCurrency}
      isAdmin={isAdmin}
    />
  )
}

export default App

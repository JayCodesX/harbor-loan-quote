import { useMemo } from 'react'
import AdminLogin from './AdminLogin'
import AdminDashboard from './AdminDashboard'
import AdminWorkspace from './AdminWorkspace'
import AdminPricing from './AdminPricing'
import AdminPartners from './AdminPartners'
import AdminReports from './AdminReports'
import AdminDocs from './AdminDocs'

const pageMap = {
  login: 'login',
  dashboard: 'dashboard',
  workspace: 'workspace',
  pricing: 'pricing',
  partners: 'partners',
  lenders: 'lenders',
  agents: 'agents',
  reports: 'reports',
  docs: 'docs',
}

export default function AdminApp({
  activeTab,
  setActiveTab,
  authState,
  onSignOut,
  loginForm,
  setLoginForm,
  handleLoginSubmit,
  handleInput,
  loadingTarget,
  errorMessage,
  summary,
  fetchSummary,
  borrowerForm,
  setBorrowerForm,
  createBorrower,
  fetchBorrowers,
  borrowers,
  createdBorrower,
  loanForm,
  setLoanForm,
  createLoan,
  fetchLoan,
  loanLookupId,
  setLoanLookupId,
  loanResult,
  productForm,
  setProductForm,
  products,
  editingProductId,
  saveProduct,
  startEditProduct,
  resetProductForm,
  deleteProduct,
  loadProducts,
  lenderForm,
  setLenderForm,
  lenders,
  editingLenderId,
  agentForm,
  setAgentForm,
  agents,
  editingAgentId,
  loadPartners,
  savePartner,
  startEditPartner,
  resetPartnerForm,
  deletePartner,
  syncPartners,
  reportForm,
  setReportForm,
  reportResult,
  runReport,
  exportReport,
  rateSheetForm,
  setRateSheetForm,
  rateSheetResult,
  publishRateSheet,
  endpointPreview,
  formatCurrency,
  isAdmin,
}) {
  const resolvedTab = useMemo(() => {
    if (typeof window === 'undefined') {
      return activeTab
    }
    const page = new URLSearchParams(window.location.search).get('page')
    return page && pageMap[page] ? pageMap[page] : activeTab
  }, [activeTab])

  const isLoggedIn = authState?.accessToken && isAdmin

  if (!isLoggedIn) {
    return (
      <AdminLogin
        loginForm={loginForm}
        setLoginForm={setLoginForm}
        handleLoginSubmit={handleLoginSubmit}
        handleInput={handleInput}
        loadingTarget={loadingTarget}
        errorMessage={errorMessage}
        authState={authState}
      />
    )
  }

  if (resolvedTab === 'dashboard') {
    return (
      <AdminDashboard
        activeTab={resolvedTab}
        setActiveTab={setActiveTab}
        summary={summary}
        fetchSummary={fetchSummary}
        loadingTarget={loadingTarget}
        formatCurrency={formatCurrency}
        authState={authState}
        onSignOut={onSignOut}
      />
    )
  }

  if (resolvedTab === 'workspace') {
    return (
      <AdminWorkspace
        activeTab={resolvedTab}
        setActiveTab={setActiveTab}
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
        handleInput={handleInput}
        loadingTarget={loadingTarget}
        formatCurrency={formatCurrency}
        authState={authState}
        onSignOut={onSignOut}
      />
    )
  }

  if (resolvedTab === 'pricing') {
    return (
      <AdminPricing
        activeTab={resolvedTab}
        setActiveTab={setActiveTab}
        productForm={productForm}
        setProductForm={setProductForm}
        products={products}
        editingProductId={editingProductId}
        saveProduct={saveProduct}
        startEditProduct={startEditProduct}
        resetProductForm={resetProductForm}
        deleteProduct={deleteProduct}
        loadProducts={loadProducts}
        handleInput={handleInput}
        loadingTarget={loadingTarget}
        authState={authState}
        onSignOut={onSignOut}
        rateSheetForm={rateSheetForm}
        setRateSheetForm={setRateSheetForm}
        rateSheetResult={rateSheetResult}
        publishRateSheet={publishRateSheet}
      />
    )
  }

  if (resolvedTab === 'lenders' || resolvedTab === 'agents' || resolvedTab === 'partners') {
    const mode = resolvedTab === 'agents' ? 'agents' : 'lenders'
    return (
      <AdminPartners
        mode={mode}
        activeTab={resolvedTab}
        setActiveTab={setActiveTab}
        loadingTarget={loadingTarget}
        authState={authState}
        onSignOut={onSignOut}
      />
    )
  }

  if (resolvedTab === 'reports') {
    return (
      <AdminReports
        activeTab={resolvedTab}
        setActiveTab={setActiveTab}
        reportForm={reportForm}
        setReportForm={setReportForm}
        reportResult={reportResult}
        runReport={runReport}
        exportReport={exportReport}
        handleInput={handleInput}
        loadingTarget={loadingTarget}
        authState={authState}
        onSignOut={onSignOut}
      />
    )
  }

  if (resolvedTab === 'docs') {
    return (
      <AdminDocs
        activeTab={resolvedTab}
        setActiveTab={setActiveTab}
        endpointPreview={endpointPreview}
        onSignOut={onSignOut}
      />
    )
  }

  return (
    <AdminDashboard
      activeTab={resolvedTab}
      setActiveTab={setActiveTab}
      summary={summary}
      fetchSummary={fetchSummary}
      loadingTarget={loadingTarget}
      formatCurrency={formatCurrency}
      authState={authState}
      onSignOut={onSignOut}
    />
  )
}

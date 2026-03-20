import AdminShell from './AdminShell'
import './AdminStyles.css'

export default function AdminDocs({
  activeTab = 'docs',
  setActiveTab,
  authState,
  onSignOut,
  endpointPreview,
}) {
  const endpoints = endpointPreview || {}

  return (
    <AdminShell activeTab={activeTab} setActiveTab={setActiveTab} authState={authState} onSignOut={onSignOut}>
      <div className="admin-v3-layout">
        <section className="admin-v3-card admin-v3-report-output">
          <h2>Internal API docs</h2>
          <p className="admin-v3-doc-intro">Reference endpoints for Mortgage Desk admin operations</p>
          <div className="admin-v3-doc-grid">
            <article className="admin-v3-doc-card">
              <h3>Auth</h3>
              <p>{endpoints.login || '/api/auth/login'}</p>
              <p>{endpoints.refresh || '/api/auth/refresh'}</p>
            </article>
            <article className="admin-v3-doc-card">
              <h3>Borrower & Loans</h3>
              <p>{endpoints.borrowers || '/api/borrowers'}</p>
              <p>{endpoints.loans || '/api/loans'}</p>
            </article>
            <article className="admin-v3-doc-card">
              <h3>Lenders</h3>
              <p>{endpoints.lenders || '/api/admin/lenders'}</p>
              <p>{endpoints.lenderSync || '/api/admin/lenders/sync'}</p>
            </article>
            <article className="admin-v3-doc-card">
              <h3>Agents</h3>
              <p>{endpoints.agents || '/api/admin/agents'}</p>
              <p>{endpoints.agentSync || '/api/admin/agents/sync'}</p>
            </article>
            <article className="admin-v3-doc-card">
              <h3>Products</h3>
              <p>{endpoints.products || '/api/admin/products'}</p>
            </article>
            <article className="admin-v3-doc-card">
              <h3>Reports</h3>
              <p>{endpoints.reports || '/api/admin/reports/query'}</p>
              <p>{endpoints.reportExport || '/api/admin/reports/export'}</p>
            </article>
          </div>
        </section>
      </div>
    </AdminShell>
  )
}

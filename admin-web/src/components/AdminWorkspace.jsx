import AdminShell from './AdminShell'
import './AdminStyles.css'

export default function AdminWorkspace({
  activeTab,
  setActiveTab,
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
  handleInput,
  loadingTarget,
  formatCurrency,
  authState,
  onSignOut,
}) {
  return (
    <AdminShell activeTab={activeTab} setActiveTab={setActiveTab} authState={authState} onSignOut={onSignOut}>
      <div className="admin-v3-layout">
        <section className="admin-v3-grid-2">
          <article className="admin-v3-card admin-v3-panel">
            <h2>Borrower workspace</h2>
            <div className="admin-v3-stack">
              <input
                className="admin-v3-input admin-v3-input-compact"
                placeholder="First name"
                name="firstName"
                value={borrowerForm.firstName}
                onChange={handleInput(setBorrowerForm)}
              />
              <input
                className="admin-v3-input admin-v3-input-compact"
                placeholder="Last name"
                name="lastName"
                value={borrowerForm.lastName}
                onChange={handleInput(setBorrowerForm)}
              />
              <input
                className="admin-v3-input admin-v3-input-compact"
                placeholder="Email"
                name="email"
                type="email"
                value={borrowerForm.email}
                onChange={handleInput(setBorrowerForm)}
              />
              <input
                className="admin-v3-input admin-v3-input-compact"
                placeholder="Credit score"
                name="creditScore"
                type="number"
                value={borrowerForm.creditScore}
                onChange={handleInput(setBorrowerForm)}
              />
              <button
                className="admin-v3-btn admin-v3-btn-blue"
                onClick={createBorrower}
                disabled={loadingTarget === 'create-borrower'}
              >
                {loadingTarget === 'create-borrower' ? 'Creating...' : 'Create borrower'}
              </button>
            </div>
            {createdBorrower ? (
              <div className="admin-v3-success-message">
                Created: {createdBorrower.firstName} {createdBorrower.lastName} (ID: {createdBorrower.id})
              </div>
            ) : null}
          </article>

          <article className="admin-v3-card admin-v3-panel">
            <h2>Loan workspace</h2>
            <div className="admin-v3-stack">
              <select
                className="admin-v3-input admin-v3-input-compact"
                name="borrowerId"
                value={loanForm.borrowerId}
                onChange={handleInput(setLoanForm)}
              >
                <option value="">Select borrower</option>
                {borrowers.map((b) => (
                  <option key={b.id} value={String(b.id)}>
                    {b.firstName} {b.lastName}
                  </option>
                ))}
              </select>
              <input
                className="admin-v3-input admin-v3-input-compact"
                placeholder="Loan amount"
                name="loanAmount"
                type="number"
                value={loanForm.loanAmount}
                onChange={handleInput(setLoanForm)}
              />
              <input
                className="admin-v3-input admin-v3-input-compact"
                placeholder="Interest rate (%)"
                name="interestRate"
                type="number"
                step="0.01"
                value={loanForm.interestRate}
                onChange={handleInput(setLoanForm)}
              />
              <input
                className="admin-v3-input admin-v3-input-compact"
                placeholder="Term (years)"
                name="termYears"
                type="number"
                value={loanForm.termYears}
                onChange={handleInput(setLoanForm)}
              />
              <button
                className="admin-v3-btn admin-v3-btn-blue"
                onClick={createLoan}
                disabled={loadingTarget === 'create-loan'}
              >
                {loadingTarget === 'create-loan' ? 'Creating...' : 'Create loan'}
              </button>
            </div>
            {loanResult ? (
              <div className="admin-v3-success-message">
                Loan created: {formatCurrency(loanResult.loanAmount)} at {loanResult.interestRate}% (ID: {loanResult.id})
              </div>
            ) : null}
          </article>
        </section>

        <section className="admin-v3-card admin-v3-header-card" style={{ marginTop: '30px' }}>
          <h2>Loan lookup</h2>
          <p>Search and monitor existing loans</p>
          <div className="admin-v3-lookup-row">
            <input
              className="admin-v3-input admin-v3-input-compact"
              placeholder="Enter loan ID"
              value={loanLookupId}
              onChange={(e) => setLoanLookupId(e.target.value)}
            />
            <button
              className="admin-v3-btn admin-v3-btn-blue"
              onClick={fetchLoan}
              disabled={loadingTarget === 'loan-monitor'}
            >
              {loadingTarget === 'loan-monitor' ? 'Loading...' : 'Load loan'}
            </button>
          </div>
        </section>

        {loanResult ? (
          <section className="admin-v3-card admin-v3-report-output" style={{ marginTop: '30px' }}>
            <h2>Loan snapshot</h2>
            <div className="admin-v3-row">ID: {loanResult.id} • Amount: {formatCurrency(loanResult.loanAmount)}</div>
            <div className="admin-v3-row">Rate: {loanResult.interestRate}% • Term: {loanResult.termYears} years</div>
            <div className="admin-v3-row">Status: {loanResult.status} • Borrower: {loanResult.borrowerId}</div>
          </section>
        ) : null}
      </div>
    </AdminShell>
  )
}

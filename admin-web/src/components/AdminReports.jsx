import AdminShell from './AdminShell'
import './AdminStyles.css'

export default function AdminReports({
  activeTab,
  setActiveTab,
  reportForm,
  setReportForm,
  reportResult,
  runReport,
  exportReport,
  handleInput,
  loadingTarget,
  authState,
  onSignOut,
}) {
  return (
    <AdminShell activeTab={activeTab} setActiveTab={setActiveTab} authState={authState} onSignOut={onSignOut}>
      <div className="admin-v3-layout">
        <section className="admin-v3-card admin-v3-header-card">
          <h2>Query report metrics</h2>
          <p>Filter by date, state, program, partner type, and export CSV for analysis.</p>
        </section>

        <section className="admin-v3-card admin-v3-report-form">
          <h3>Report filters</h3>
          <form onSubmit={runReport}>
            <div className="admin-v3-form-grid">
              <select
                className="admin-v3-input admin-v3-input-compact"
                name="reportType"
                value={reportForm.reportType}
                onChange={handleInput(setReportForm)}
              >
                <option value="PRODUCTS">Pricing products</option>
                <option value="BORROWERS">Borrowers</option>
                <option value="LOANS">Loans &amp; quotes</option>
              </select>
              <input
                className="admin-v3-input admin-v3-input-compact"
                placeholder="From date"
                name="dateFrom"
                type="date"
                value={reportForm.dateFrom}
                onChange={handleInput(setReportForm)}
              />
              <input
                className="admin-v3-input admin-v3-input-compact"
                placeholder="To date"
                name="dateTo"
                type="date"
                value={reportForm.dateTo}
                onChange={handleInput(setReportForm)}
              />
              <input
                className="admin-v3-input admin-v3-input-compact"
                placeholder="State code"
                name="stateCode"
                value={reportForm.stateCode}
                onChange={handleInput(setReportForm)}
              />
              <input
                className="admin-v3-input admin-v3-input-compact"
                placeholder="Program code"
                name="programCode"
                value={reportForm.programCode}
                onChange={handleInput(setReportForm)}
              />
              <select
                className="admin-v3-input admin-v3-input-compact"
                name="activeOnly"
                value={reportForm.activeOnly}
                onChange={handleInput(setReportForm)}
              >
                <option value="false">All partners</option>
                <option value="true">Active only</option>
              </select>
              <input
                className="admin-v3-input admin-v3-input-compact"
                placeholder="Min score"
                name="minCreditScore"
                type="number"
                value={reportForm.minCreditScore}
                onChange={handleInput(setReportForm)}
              />
            </div>
            <button
              type="submit"
              className="admin-v3-btn admin-v3-btn-blue"
              disabled={loadingTarget === 'run-report'}
            >
              {loadingTarget === 'run-report' ? 'Running...' : 'Run report'}
            </button>
          </form>
        </section>

        {reportResult ? (
          <section className="admin-v3-card admin-v3-report-output">
            <h2>Report output</h2>
            <div className="admin-v3-row">
              Date range: {reportForm.dateFrom ? new Date(reportForm.dateFrom).toLocaleDateString() : 'All'} to{' '}
              {reportForm.dateTo ? new Date(reportForm.dateTo).toLocaleDateString() : 'All'}
            </div>
            <div className="admin-v3-row">State: {reportForm.stateCode || 'All'} • Program: {reportForm.programCode || 'All'}</div>
            <div className="admin-v3-row">{reportResult.title} — Total records: {reportResult.totalRows ?? 0}</div>
            {reportResult.rows && reportResult.rows.length > 0 ? (
              <table className="admin-v3-report-table">
                <thead>
                  <tr>{reportResult.columns.map((c) => <th key={c} style={{ textAlign: 'left', padding: '6px 10px', textTransform: 'capitalize' }}>{c}</th>)}</tr>
                </thead>
                <tbody>
                  {reportResult.rows.map((row, i) => (
                    <tr key={i}>{reportResult.columns.map((c) => <td key={c} style={{ padding: '6px 10px' }}>{String(row[c] ?? '')}</td>)}</tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <div className="admin-v3-row">No records match these filters.</div>
            )}
            <button type="button" className="admin-v3-btn admin-v3-btn-orange" onClick={exportReport} disabled={loadingTarget === 'export-report'}>
              {loadingTarget === 'export-report' ? 'Exporting...' : 'Export CSV'}
            </button>
          </section>
        ) : null}
      </div>
    </AdminShell>
  )
}

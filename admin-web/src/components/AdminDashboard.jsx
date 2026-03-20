import { useEffect } from 'react'
import AdminShell from './AdminShell'
import './AdminStyles.css'

export default function AdminDashboard({
  activeTab,
  setActiveTab,
  summary,
  fetchSummary,
  loadingTarget,
  authState,
  onSignOut,
}) {
  useEffect(() => {
    if (!summary) {
      fetchSummary()
    }
  }, [summary, fetchSummary])

  const dailyQuotes = summary?.quotes?.dailyQuotes ?? 0
  const refineConversion = summary?.quotes?.refineConversion ?? 0
  const matchedToLender = summary?.quotes?.matchedToLender ?? 0
  const activeSubscriptions = summary?.subscriptions?.activeSubscriptions ?? 0

  const quotesStarted = summary?.quotes?.quotesStarted ?? 0
  const quotesCompleted = summary?.quotes?.quotesCompleted ?? 0
  const sessionsWithRefinements = summary?.quotes?.sessionsWithRefinements ?? 0
  const leadConvertedSessions = summary?.quotes?.leadConvertedSessions ?? 0

  const formatPercent = (value) => {
    if (typeof value !== 'number' || Number.isNaN(value)) return '0%'
    return `${Math.round(value)}%`
  }

  const highlights = summary?.operationalHighlights ?? [
    'Top lender market: King County, WA',
    'Highest borrower drop-off: auth step',
    'Agent coverage gap: Spokane, WA',
    'Product needing update: FHA 30 fixed',
  ]

  return (
    <AdminShell activeTab={activeTab} setActiveTab={setActiveTab} authState={authState} onSignOut={onSignOut}>
      <div className="admin-v3-layout">
        <section className="admin-v3-stat-grid">
          <article className="admin-v3-card admin-v3-stat-card">
            <h3>Daily quotes</h3>
            <p>{dailyQuotes}</p>
          </article>
          <article className="admin-v3-card admin-v3-stat-card">
            <h3>Refine conversion</h3>
            <p>{formatPercent(refineConversion)}</p>
          </article>
          <article className="admin-v3-card admin-v3-stat-card">
            <h3>Matched to lender</h3>
            <p>{matchedToLender}</p>
          </article>
          <article className="admin-v3-card admin-v3-stat-card admin-v3-stat-warm">
            <h3>Rate alert subs</h3>
            <p>{activeSubscriptions}</p>
          </article>
        </section>

        <section className="admin-v3-grid-2">
          <article className="admin-v3-card admin-v3-panel">
            <h2>Quote funnel</h2>
            <div className="admin-v3-funnel">
              <div className="admin-v3-funnel-row">Started: {quotesStarted}</div>
              <div className="admin-v3-funnel-row">Quoted: {quotesCompleted}</div>
              <div className="admin-v3-funnel-row">Refined: {sessionsWithRefinements}</div>
              <div className="admin-v3-funnel-row">Matched: {leadConvertedSessions}</div>
            </div>
          </article>
          <article className="admin-v3-card admin-v3-panel">
            <h2>Operational highlights</h2>
            <ul className="admin-v3-bullets">
              {highlights.map((highlight, idx) => (
                <li key={idx}>{highlight}</li>
              ))}
            </ul>
          </article>
        </section>
      </div>
    </AdminShell>
  )
}

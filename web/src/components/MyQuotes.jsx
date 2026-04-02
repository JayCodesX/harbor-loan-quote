import BorrowerShell from './BorrowerShell'
import './BorrowerStyles.css'

const fmtDollars = (n) => n != null ? `$${Math.round(n).toLocaleString('en-US')}` : '—'
const fmtRate = (n) => n != null ? `${(parseFloat(n) * 100).toFixed(3)}%` : '—'
const fmtDate = (s) => {
  if (!s) return '—'
  const d = new Date(s)
  return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
}

const statusBadgeClass = (status) => {
  if (!status) return 'mq-badge mq-badge-neutral'
  const s = status.toUpperCase()
  if (s === 'LEAD_CAPTURED') return 'mq-badge mq-badge-success'
  if (s === 'LEAD_READY') return 'mq-badge mq-badge-success'
  if (s === 'REFINEMENT_REQUESTED') return 'mq-badge mq-badge-active'
  if (s === 'ESTIMATED') return 'mq-badge mq-badge-neutral'
  if (s === 'REQUESTED') return 'mq-badge mq-badge-neutral'
  return 'mq-badge mq-badge-neutral'
}

const statusLabel = (stage, status) => {
  if (!status) return 'Pending'
  const s = status.toUpperCase()
  if (s === 'LEAD_CAPTURED') return 'Matched'
  if (s === 'LEAD_READY') return 'Ready to Match'
  if (s === 'REFINEMENT_REQUESTED') return 'Refining'
  if (stage?.toUpperCase() === 'REFINED') return 'Refined'
  return 'Initial Quote'
}

export default function MyQuotes({
  setActiveView,
  authState,
  onSignOut,
  quoteHistory,
  onSelectQuote,
  onDeleteQuote,
}) {
  const hasQuotes = Array.isArray(quoteHistory) && quoteHistory.length > 0

  return (
    <BorrowerShell setActiveView={setActiveView} authState={authState} onSignOut={onSignOut} activeView="my-quotes">
      <div className="borrower-v3-layout">

        <section className="borrower-v3-card mq-hero">
          <div>
            <h1 className="borrower-v3-hero-title">My Quotes</h1>
            <p className="borrower-v3-hero-copy">All your saved loan scenarios, sorted by most recently updated.</p>
          </div>
          <button
            type="button"
            className="borrower-v3-btn borrower-v3-btn-primary"
            onClick={() => setActiveView?.('home')}
          >
            New quote
          </button>
        </section>

        {!hasQuotes ? (
          <section className="borrower-v3-card mq-empty">
            <div className="mq-empty-icon">📋</div>
            <h2 className="mq-empty-title">No quotes yet</h2>
            <p className="mq-empty-copy">Get your first quote in under a minute — no account info required to start.</p>
            <button
              type="button"
              className="borrower-v3-btn borrower-v3-btn-primary"
              onClick={() => setActiveView?.('home')}
            >
              Get a quote
            </button>
          </section>
        ) : (
          <div className="mq-grid">
            {quoteHistory.map((quote) => {
              const monthly = quote.estimatedMonthlyPayment
              const rate = quote.estimatedRate
              const stage = quote.quoteStage
              const status = quote.quoteStatus

              return (
                <article key={quote.id} className="borrower-v3-card mq-card">
                  <div className="mq-card-header">
                    <div className="mq-card-meta">
                      <span className="mq-card-id">Quote #{quote.id}</span>
                      <span className={statusBadgeClass(status)}>{statusLabel(stage, status)}</span>
                    </div>
                    <p className="mq-card-date">Updated {fmtDate(quote.updatedAt)}</p>
                  </div>

                  <div className="mq-card-body">
                    <div className="mq-stat-row">
                      <div className="mq-stat">
                        <span className="mq-stat-label">Monthly payment</span>
                        <span className="mq-stat-value">{monthly ? fmtDollars(monthly) : '—'}</span>
                      </div>
                      <div className="mq-stat">
                        <span className="mq-stat-label">Est. rate</span>
                        <span className="mq-stat-value">{fmtRate(rate)}</span>
                      </div>
                      <div className="mq-stat">
                        <span className="mq-stat-label">Home price</span>
                        <span className="mq-stat-value">{fmtDollars(quote.homePrice)}</span>
                      </div>
                    </div>

                    <div className="mq-details">
                      <span>{quote.loanProgram?.replace('_', ' ')}</span>
                      <span className="mq-dot">·</span>
                      <span>{quote.termYears}yr</span>
                      <span className="mq-dot">·</span>
                      <span>{quote.propertyUse?.replace(/_/g, ' ')}</span>
                      {quote.zipCode && (
                        <>
                          <span className="mq-dot">·</span>
                          <span>{quote.zipCode}</span>
                        </>
                      )}
                    </div>
                  </div>

                  <div className="mq-card-actions">
                    <button
                      type="button"
                      className="borrower-v3-btn mq-btn-open"
                      onClick={() => onSelectQuote?.(quote.id)}
                    >
                      Open
                    </button>
                    {onDeleteQuote && (
                      <button
                        type="button"
                        className="mq-btn-delete"
                        onClick={() => onDeleteQuote?.(quote.id)}
                      >
                        Delete
                      </button>
                    )}
                  </div>
                </article>
              )
            })}
          </div>
        )}
      </div>
    </BorrowerShell>
  )
}

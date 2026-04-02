import BorrowerShell from './BorrowerShell'
import './BorrowerStyles.css'
import { formatCurrency, getQuoteStatusMeta, isQuoteInFlight } from '../lib/demoApp'

export default function QuoteResults({
  setActiveView,
  authState,
  onSignOut,
  quoteResult,
  loadingTarget,
  activeView,
}) {
  const payment = quoteResult?.estimatedMonthlyPayment
  const principalInterest = payment ? Math.round(payment * 0.70) : null
  const taxesInsurance = payment ? Math.round(payment * 0.22) : null
  const hoaFees = payment ? Math.round(payment * 0.08) : null
  const meta = getQuoteStatusMeta(quoteResult)

  return (
    <BorrowerShell setActiveView={setActiveView} authState={authState} onSignOut={onSignOut} activeView={activeView}>
      <div className="borrower-v3-layout">
        <section className="borrower-v3-card borrower-v3-hero-row">
          {isQuoteInFlight(quoteResult) ? (
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
                <span className="borrower-v3-processing-dot" />
                <span className="borrower-v3-badge">{meta.badge}</span>
              </div>
              <h1 className="borrower-v3-hero-title">{meta.headline}</h1>
              <p className="borrower-v3-hero-copy">{meta.detail}</p>
            </div>
          ) : (
            <div>
              <h1 className="borrower-v3-hero-title">Initial quote result</h1>
              <p className="borrower-v3-hero-metric">Estimated monthly payment: <strong>{payment ? formatCurrency(payment) : '--'}</strong></p>
              <p className="borrower-v3-hero-copy">Confidence is moderate. Add borrower details to tighten lender + agent fit.</p>
            </div>
          )}
          {!isQuoteInFlight(quoteResult) && (
            <button type="button" className="borrower-v3-btn borrower-v3-btn-primary" onClick={() => setActiveView?.(authState ? 'quote-refine' : 'auth')}>Refine quote</button>
          )}
        </section>

        <section className="borrower-v3-grid-2">
          <article className="borrower-v3-card borrower-v3-panel">
            <h2 className="borrower-v3-panel-title">Estimated payment breakdown</h2>
            {principalInterest !== null ? (
              <>
                <div className="borrower-v3-breakdown">
                  <div className="borrower-v3-breakdown-row"><span>Principal + Interest</span><strong>{formatCurrency(principalInterest)}</strong></div>
                  <div className="borrower-v3-breakdown-row"><span>Taxes + Insurance</span><strong>{formatCurrency(taxesInsurance)}</strong></div>
                  <div className="borrower-v3-breakdown-row"><span>HOA + Fees</span><strong>{formatCurrency(hoaFees)}</strong></div>
                </div>
                <p style={{ fontSize: '0.75rem', color: '#6c757d', marginTop: '0.5rem' }}>Illustrative estimates only. Actual amounts vary by property.</p>
              </>
            ) : (
              <p>Submit a quote to see a payment breakdown.</p>
            )}
          </article>

          <article className="borrower-v3-card borrower-v3-panel">
            <h2 className="borrower-v3-panel-title">What improves this quote</h2>
            <ul className="borrower-v3-list">
              <li>Annual income + recurring debts</li>
              <li>Exact credit profile and reserves</li>
              <li>Timeline and property occupancy</li>
              <li>City/county to rank nearby partners</li>
            </ul>
          </article>
        </section>
      </div>
    </BorrowerShell>
  )
}

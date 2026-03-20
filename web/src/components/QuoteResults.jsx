import BorrowerShell from './BorrowerShell'
import './BorrowerStyles.css'
import { formatCurrency } from '../lib/demoApp'

const fmtDollars = (n) => `$${Math.round(n).toLocaleString('en-US')}`

export default function QuoteResults({
  setActiveView,
  authState,
  onSignOut,
  quoteResult,
}) {
  const principalInterest = quoteResult?.estimatedMonthlyPayment ? Math.round(quoteResult.estimatedMonthlyPayment * 0.7) : 1992
  const taxesInsurance = quoteResult?.estimatedMonthlyPayment ? Math.round(quoteResult.estimatedMonthlyPayment * 0.22) : 624
  const hoaFees = quoteResult?.estimatedMonthlyPayment ? Math.round(quoteResult.estimatedMonthlyPayment * 0.08) : 225

  return (
    <BorrowerShell setActiveView={setActiveView} authState={authState} onSignOut={onSignOut}>
      <div className="borrower-v3-layout">
        <section className="borrower-v3-card borrower-v3-hero-row">
          <div>
            <h1 className="borrower-v3-hero-title">Initial quote result</h1>
            <p className="borrower-v3-hero-metric">
              Estimated monthly payment: <strong>{quoteResult?.estimatedMonthlyPayment ? fmtDollars(quoteResult.estimatedMonthlyPayment) : '$2,841'}</strong>
            </p>
            <p className="borrower-v3-hero-copy">Confidence is moderate. Add borrower details to tighten lender + agent fit.</p>
          </div>
          <button type="button" className="borrower-v3-btn borrower-v3-btn-primary" onClick={() => setActiveView?.(authState ? 'quote-refine' : 'auth')}>Refine quote</button>
        </section>

        <section className="borrower-v3-grid-2">
          <article className="borrower-v3-card borrower-v3-panel">
            <h2 className="borrower-v3-panel-title">Payment breakdown</h2>
            <div className="borrower-v3-breakdown">
              <div className="borrower-v3-breakdown-row"><span>Principal + Interest</span><strong>{fmtDollars(principalInterest)}</strong></div>
              <div className="borrower-v3-breakdown-row"><span>Taxes + Insurance</span><strong>{fmtDollars(taxesInsurance)}</strong></div>
              <div className="borrower-v3-breakdown-row"><span>HOA + Fees</span><strong>{fmtDollars(hoaFees)}</strong></div>
            </div>
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

import './BorrowerStyles.css'
import './QuoteLanding.css'
import { formatCurrency } from '../lib/demoApp'

export default function CalculatorsView({
  activeCalculator,
  setActiveCalculator,
  mortgageForm,
  setMortgageForm,
  mortgageResult,
  amortizationForm,
  setAmortizationForm,
  amortizationResult,
  loadingTarget,
  handleMortgageSubmit,
  handleAmortizationSubmit,
  handleInput,
}) {
  return (
    <div className="borrower-v3-layout">
      <div className="v3-calc-header">
        <div className="v3-landing-pill">FREE TOOLS</div>
        <h1 className="v3-calc-title">Loan calculators</h1>
        <p className="v3-calc-copy">Estimate your monthly payment or preview a full amortization schedule.</p>
      </div>

      <div className="v3-landing-program v3-calc-tabs">
        <button
          type="button"
          className={`v3-landing-program-btn${activeCalculator === 'mortgage' ? ' v3-landing-program-btn-active' : ''}`}
          onClick={() => setActiveCalculator('mortgage')}
        >
          Mortgage payment
        </button>
        <button
          type="button"
          className={`v3-landing-program-btn${activeCalculator === 'amortization' ? ' v3-landing-program-btn-active' : ''}`}
          onClick={() => setActiveCalculator('amortization')}
        >
          Amortization
        </button>
      </div>

      {activeCalculator === 'mortgage' ? (
        <div className="v3-calc-grid">
          <div className="borrower-v3-card v3-calc-form-card">
            <p className="v3-landing-kicker">Mortgage payment</p>
            <h2 className="borrower-v3-panel-title" style={{ marginTop: 8 }}>Monthly payment calculator</h2>
            <p className="v3-calc-form-copy">Estimate your monthly payment from home price, down payment, rate, and term.</p>
            <form onSubmit={handleMortgageSubmit}>
              <div className="v3-calc-fields">
                <article className="v3-landing-field">
                  <p>Home price</p>
                  <input className="v3-landing-field-input" name="loanAmount" inputMode="decimal" value={mortgageForm.loanAmount} onChange={handleInput(setMortgageForm)} required />
                </article>
                <article className="v3-landing-field">
                  <p>Down payment</p>
                  <input className="v3-landing-field-input" name="downPayment" inputMode="decimal" value={mortgageForm.downPayment} onChange={handleInput(setMortgageForm)} required />
                </article>
                <article className="v3-landing-field">
                  <p>Interest rate (%)</p>
                  <input className="v3-landing-field-input" name="annualInterestRate" inputMode="decimal" value={mortgageForm.annualInterestRate} onChange={handleInput(setMortgageForm)} required />
                </article>
                <article className="v3-landing-field">
                  <p>Loan term (years)</p>
                  <input className="v3-landing-field-input" name="termYears" inputMode="numeric" value={mortgageForm.termYears} onChange={handleInput(setMortgageForm)} required />
                </article>
              </div>
              <div className="v3-calc-submit-row">
                <button type="submit" className="v3-landing-result" disabled={loadingTarget === 'mortgage'}>
                  {loadingTarget === 'mortgage' ? 'Calculating...' : 'Calculate payment'}
                </button>
              </div>
            </form>
          </div>

          <div className="borrower-v3-card v3-calc-result-card">
            <p className="v3-landing-kicker">Payment result</p>
            <div className="v3-landing-estimate v3-calc-estimate">
              <p>ESTIMATED MONTHLY PAYMENT</p>
              <strong>{mortgageResult?.monthlyPayment ? formatCurrency(mortgageResult.monthlyPayment) : '—'}</strong>
            </div>
            <div className="v3-calc-breakdown">
              <div className="borrower-v3-breakdown-row">
                <span>Financed principal</span>
                <strong>{mortgageResult?.financedPrincipal ? formatCurrency(mortgageResult.financedPrincipal) : '—'}</strong>
              </div>
              <div className="borrower-v3-breakdown-row">
                <span>Number of payments</span>
                <strong>{mortgageResult?.numberOfPayments ?? '—'}</strong>
              </div>
            </div>
          </div>
        </div>
      ) : (
        <div className="v3-calc-grid">
          <div className="borrower-v3-card v3-calc-form-card">
            <p className="v3-landing-kicker">Amortization</p>
            <h2 className="borrower-v3-panel-title" style={{ marginTop: 8 }}>Amortization preview</h2>
            <p className="v3-calc-form-copy">Use principal, rate, and term to preview your payment and schedule.</p>
            <form onSubmit={handleAmortizationSubmit}>
              <div className="v3-calc-fields v3-calc-fields-3">
                <article className="v3-landing-field">
                  <p>Principal</p>
                  <input className="v3-landing-field-input" name="principal" inputMode="decimal" value={amortizationForm.principal} onChange={handleInput(setAmortizationForm)} required />
                </article>
                <article className="v3-landing-field">
                  <p>Interest rate (%)</p>
                  <input className="v3-landing-field-input" name="annualInterestRate" inputMode="decimal" value={amortizationForm.annualInterestRate} onChange={handleInput(setAmortizationForm)} required />
                </article>
                <article className="v3-landing-field">
                  <p>Loan term (years)</p>
                  <input className="v3-landing-field-input" name="termYears" inputMode="numeric" value={amortizationForm.termYears} onChange={handleInput(setAmortizationForm)} required />
                </article>
              </div>
              <div className="v3-calc-submit-row">
                <button type="submit" className="v3-landing-result" disabled={loadingTarget === 'amortization'}>
                  {loadingTarget === 'amortization' ? 'Calculating...' : 'Calculate amortization'}
                </button>
              </div>
            </form>
          </div>

          <div className="borrower-v3-card v3-calc-result-card">
            <p className="v3-landing-kicker">Amortization result</p>
            <div className="v3-landing-estimate v3-calc-estimate">
              <p>MONTHLY PAYMENT</p>
              <strong>{amortizationResult?.monthlyPayment ? formatCurrency(amortizationResult.monthlyPayment) : '—'}</strong>
            </div>
            <div className="v3-calc-breakdown">
              <div className="borrower-v3-breakdown-row">
                <span>Principal</span>
                <strong>{amortizationResult?.principal ? formatCurrency(amortizationResult.principal) : '—'}</strong>
              </div>
              <div className="borrower-v3-breakdown-row">
                <span>Number of payments</span>
                <strong>{amortizationResult?.numberOfPayments ?? '—'}</strong>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

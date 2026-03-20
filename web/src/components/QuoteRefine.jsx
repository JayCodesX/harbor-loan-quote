import { useState } from 'react'
import BorrowerShell from './BorrowerShell'
import './BorrowerStyles.css'
import './QuoteLanding.css'
import { formatCurrency } from '../lib/demoApp'

export default function QuoteRefine({
  setActiveView,
  authState,
  onSignOut,
  refineForm,
  setRefineForm,
  handleRefineQuoteSubmit,
  handleRefineProgressSave,
  quoteResult,
  locationOptions,
  loadingTarget,
  handleInput,
  errorMessage,
}) {
  const [currentStep, setCurrentStep] = useState(1)
  const [submitError, setSubmitError] = useState('')

  const selectedState = locationOptions?.find((loc) => loc.stateCode === refineForm?.stateCode)
  const counties = selectedState?.counties || []

  const handleNext = async () => {
    await handleRefineProgressSave?.()
    if (currentStep < 3) {
      setCurrentStep(currentStep + 1)
    }
  }

  const handleBack = () => {
    if (currentStep > 1) {
      setCurrentStep(currentStep - 1)
    }
  }

  const handleFinalSubmit = (e) => {
    e.preventDefault()
    setSubmitError('')
    handleRefineQuoteSubmit?.(e)
  }

  const getConfidenceLevel = () => {
    if (currentStep === 1) return 'Building'
    if (currentStep === 2) return 'Growing'
    return 'Strong'
  }

  return (
    <BorrowerShell setActiveView={setActiveView} authState={authState} onSignOut={onSignOut}>
      <div className="borrower-v3-layout">
        <section className="borrower-v3-card borrower-v3-steps">
          <div className={`borrower-v3-step ${currentStep >= 1 ? 'borrower-v3-step-active' : ''}`}>
            <span className="borrower-v3-step-dot">1</span>Borrower profile
          </div>
          <div className={`borrower-v3-step ${currentStep >= 2 ? 'borrower-v3-step-active' : ''}`}>
            <span className="borrower-v3-step-dot">2</span>Financials
          </div>
          <div className={`borrower-v3-step ${currentStep >= 3 ? 'borrower-v3-step-active' : ''}`}>
            <span className="borrower-v3-step-dot">3</span>Property details
          </div>
        </section>

        <section className="borrower-v3-stepper-grid">
          <article className="borrower-v3-card borrower-v3-stepper-main">
            <form onSubmit={currentStep === 3 ? handleFinalSubmit : (e) => e.preventDefault()}>
              {currentStep === 1 && (
                <>
                  <h2>Step 1: Borrower profile</h2>
                  <div className="borrower-v3-stepper-fields">
                    <article className="v3-landing-field">
                      <p>Annual income</p>
                      <input type="number" name="annualIncome" value={refineForm?.annualIncome || ''} onChange={handleInput?.(setRefineForm)} className="v3-landing-field-input" placeholder="0" />
                    </article>
                    <article className="v3-landing-field">
                      <p>Cash reserves</p>
                      <input type="number" name="cashReserves" value={refineForm?.cashReserves || ''} onChange={handleInput?.(setRefineForm)} className="v3-landing-field-input" placeholder="0" />
                    </article>
                    <article className="v3-landing-field">
                      <p>Monthly debts</p>
                      <input type="number" name="monthlyDebts" value={refineForm?.monthlyDebts || ''} onChange={handleInput?.(setRefineForm)} className="v3-landing-field-input" placeholder="0" />
                    </article>
                    <article className="v3-landing-field">
                      <p>First time buyer</p>
                      <select name="firstTimeBuyer" value={refineForm?.firstTimeBuyer || ''} onChange={handleInput?.(setRefineForm)} className="v3-landing-field-input">
                        <option value="">Select</option>
                        <option value="true">Yes</option>
                        <option value="false">No</option>
                      </select>
                    </article>
                  </div>
                </>
              )}

              {currentStep === 2 && (
                <>
                  <h2>Step 2: Financials</h2>
                  <div className="borrower-v3-stepper-fields">
                    <article className="v3-landing-field">
                      <p>Credit score</p>
                      <input type="number" name="creditScore" value={refineForm?.creditScore || ''} onChange={handleInput?.(setRefineForm)} className="v3-landing-field-input" placeholder="300–850" min="300" max="850" />
                    </article>
                    <article className="v3-landing-field">
                      <p>VA eligible</p>
                      <select name="vaEligible" value={refineForm?.vaEligible || ''} onChange={handleInput?.(setRefineForm)} className="v3-landing-field-input">
                        <option value="">Select</option>
                        <option value="true">Yes</option>
                        <option value="false">No</option>
                      </select>
                    </article>
                    <article className="v3-landing-field">
                      <p>Est. funding date</p>
                      <input type="date" name="estimatedFundingDate" value={refineForm?.estimatedFundingDate || ''} onChange={handleInput?.(setRefineForm)} className="v3-landing-field-input" />
                    </article>
                  </div>
                </>
              )}

              {currentStep === 3 && (
                <>
                  <h2>Step 3: Property details</h2>
                  <div className="borrower-v3-stepper-fields">
                    <article className="v3-landing-field">
                      <p>State</p>
                      <select name="stateCode" value={refineForm?.stateCode || ''} onChange={handleInput?.(setRefineForm)} className="v3-landing-field-input">
                        <option value="">Select</option>
                        {locationOptions?.map((loc) => (
                          <option key={loc.stateCode} value={loc.stateCode}>{loc.stateCode}</option>
                        ))}
                      </select>
                    </article>
                    <article className="v3-landing-field">
                      <p>County</p>
                      <select name="countyName" value={refineForm?.countyName || ''} onChange={handleInput?.(setRefineForm)} className="v3-landing-field-input" disabled={!selectedState}>
                        <option value="">Select</option>
                        {counties.map((county) => (
                          <option key={county} value={county}>{county}</option>
                        ))}
                      </select>
                    </article>
                  </div>
                </>
              )}

              {submitError ? (
                <div className="borrower-v3-auth-error">{submitError}</div>
              ) : errorMessage && currentStep === 3 ? (
                <div className="borrower-v3-auth-error">{errorMessage}</div>
              ) : null}

              <div className="borrower-v3-stepper-actions">
                <button
                  type="button"
                  className="borrower-v3-btn-soft"
                  onClick={handleBack}
                  disabled={currentStep === 1}
                >
                  Back
                </button>
                {currentStep < 3 ? (
                  <button
                    type="button"
                    className="borrower-v3-btn borrower-v3-btn-primary"
                    onClick={handleNext}
                    disabled={loadingTarget === 'refine-progress'}
                  >
                    {loadingTarget === 'refine-progress' ? 'Saving...' : 'Next'}
                  </button>
                ) : (
                  <button
                    type="submit"
                    className="borrower-v3-btn borrower-v3-btn-primary"
                    disabled={loadingTarget === 'refine-quote'}
                  >
                    {loadingTarget === 'refine-quote' ? 'Submitting...' : 'Submit'}
                  </button>
                )}
              </div>
            </form>
          </article>

          <aside className="borrower-v3-card borrower-v3-stepper-preview">
            <h3>Live quote preview</h3>

            <div className="borrower-v3-preview-block">
              <p>Monthly payment</p>
              <strong>{quoteResult?.estimatedMonthlyPayment ? formatCurrency(quoteResult.estimatedMonthlyPayment) : '$2,764'}</strong>
            </div>

            <div className="borrower-v3-preview-block">
              <p>Confidence</p>
              <strong>{getConfidenceLevel()}</strong>
            </div>

            <p className="borrower-v3-preview-note">Changes are saved to your active quote.</p>
          </aside>
        </section>
      </div>
    </BorrowerShell>
  )
}

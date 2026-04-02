import './QuoteLanding.css'
import { formatCurrency } from '../lib/demoApp'

export default function QuoteLanding({
  setActiveView,
  authState,
  onSignOut,
  publicQuoteForm,
  setPublicQuoteForm,
  handlePublicQuoteSubmit,
  locationOptions,
  quoteResult,
  loadingTarget,
  handleInput,
  onSubscribeClick,
}) {
  const handleSignInClick = () => {
    if (authState) {
      onSignOut?.()
    } else {
      setActiveView?.('auth')
    }
  }

  const handleProgramClick = (program) => {
    setPublicQuoteForm?.((current) => ({ ...current, loanProgram: program }))
  }

  const handleSubmit = (e) => {
    e.preventDefault()
    handlePublicQuoteSubmit?.(e)
  }

  return (
    <div className="v3-landing-page">
      <header className="v3-landing-topbar" aria-label="Primary">
        <div className="v3-landing-brand">Harbor Loan Quotes</div>
        <nav className="v3-landing-nav" aria-label="Main navigation">
          <button type="button" className="v3-landing-nav-link v3-landing-nav-link-active">Get a Quote</button>
          <button type="button" className="v3-landing-nav-link" onClick={() => setActiveView?.('tools')}>Calculators</button>
          <button type="button" className="v3-landing-nav-link" onClick={handleSignInClick}>
            {authState ? 'Sign Out' : 'Sign In'}
          </button>
          <button type="button" className="v3-landing-subscribe" onClick={onSubscribeClick}>Subscribe</button>
        </nav>
      </header>

      <main className="v3-landing-shell">
        <section className="v3-landing-left">
          <div className="v3-landing-pill">QUOTE FIRST. DETAILS LATER.</div>
          <h1 className="v3-landing-title">Get a credible home loan quote before filling out a long mortgage form.</h1>
          <p className="v3-landing-copy">
            Start with lightweight inputs. Then refine to compare lenders and meet a local real estate agent.
          </p>
          <button type="button" className="v3-landing-start" onClick={() => document.querySelector('.v3-landing-quick-card')?.scrollIntoView({ behavior: 'smooth' })}>Start quote</button>
        </section>

        <aside className="v3-landing-quick-card" aria-label="Quick quote">
          <div className="v3-landing-quick-header">
            <div>
              <p className="v3-landing-kicker">QUICK QUOTE</p>
              <h2 className="v3-landing-quick-title">See an estimate in under a minute</h2>
            </div>
            <span className="v3-landing-step">1/2</span>
          </div>

          <form onSubmit={handleSubmit}>
            <div className="v3-landing-program">
              {['CONVENTIONAL', 'FHA', 'VA', 'JUMBO'].map((program) => (
                <button
                  key={program}
                  type="button"
                  className={`v3-landing-program-btn ${publicQuoteForm?.loanProgram === program ? 'v3-landing-program-btn-active' : ''}`}
                  onClick={() => handleProgramClick(program)}
                >
                  {program}
                </button>
              ))}
            </div>

            <div className="v3-landing-fields">
              <article className="v3-landing-field">
                <label htmlFor="homePrice">Home price</label>
                <input
                  id="homePrice"
                  type="number"
                  name="homePrice"
                  value={publicQuoteForm?.homePrice || ''}
                  onChange={handleInput?.(setPublicQuoteForm)}
                  placeholder="$0"
                  className="v3-landing-field-input"
                />
              </article>
              <article className="v3-landing-field">
                <label htmlFor="downPayment">Down payment</label>
                <input
                  id="downPayment"
                  type="number"
                  name="downPayment"
                  value={publicQuoteForm?.downPayment || ''}
                  onChange={handleInput?.(setPublicQuoteForm)}
                  placeholder="$0"
                  className="v3-landing-field-input"
                />
              </article>
              <article className="v3-landing-field">
                <label htmlFor="zipCode">ZIP code</label>
                <input
                  id="zipCode"
                  type="text"
                  name="zipCode"
                  value={publicQuoteForm?.zipCode || ''}
                  onChange={handleInput?.(setPublicQuoteForm)}
                  placeholder="00000"
                  className="v3-landing-field-input"
                />
              </article>
              <article className="v3-landing-field">
                <label htmlFor="creditProfile">Credit profile</label>
                <select
                  id="creditProfile"
                  name="creditProfile"
                  value={publicQuoteForm?.creditProfile || 'Good'}
                  onChange={handleInput?.(setPublicQuoteForm)}
                  className="v3-landing-field-input"
                >
                  <option>Excellent</option>
                  <option>Good</option>
                  <option>Fair</option>
                </select>
              </article>
            </div>

            <section className="v3-landing-estimate">
              <p>ESTIMATED MONTHLY PAYMENT</p>
              <strong>{quoteResult?.estimatedMonthlyPayment ? formatCurrency(quoteResult.estimatedMonthlyPayment) : '$2,841'}</strong>
            </section>

            <div className="v3-landing-cta-row">
              <button
                type="submit"
                className="v3-landing-result"
                disabled={loadingTarget === 'public-quote'}
              >
                {loadingTarget === 'public-quote' ? 'Loading...' : 'See result'}
              </button>
            </div>
          </form>
        </aside>
      </main>

      <footer className="v3-landing-footer">
        <p>&copy; 2026 Harbor Loan Quotes &bull; Terms &bull; Privacy &bull; Licensing &bull; Equal Housing</p>
        <p>NMLS + State Notices</p>
      </footer>
    </div>
  )
}

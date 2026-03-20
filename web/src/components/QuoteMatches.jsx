import BorrowerShell from './BorrowerShell'
import './BorrowerStyles.css'

export default function QuoteMatches({
  setActiveView,
  authState,
  onSignOut,
  matchedLenders,
  matchedAgents,
  quoteResult,
}) {
  return (
    <BorrowerShell setActiveView={setActiveView} authState={authState} onSignOut={onSignOut}>
      <div className="borrower-v3-layout">
        <section className="borrower-v3-card borrower-v3-matches-hero">
          <h1>Your refined matches are ready</h1>
          <p>Based on your full profile, here are lender and agent options likely to close smoothly.</p>
        </section>

        <section className="borrower-v3-grid-2">
          <article className="borrower-v3-card borrower-v3-panel">
            <h2 className="borrower-v3-panel-title">Matched lenders</h2>
            <div className="borrower-v3-match-stack">
              {matchedLenders && matchedLenders.length > 0 ? (
                matchedLenders.map((lender) => (
                  <div key={lender.id} className="borrower-v3-match-card">
                    <h3>{lender.institutionName || 'Lender'}</h3>
                    <p className="borrower-v3-match-meta">
                      {lender.nmlsId ? <span>NMLS# {lender.nmlsId}</span> : null}
                      {lender.loanTypes ? <span>{lender.loanTypes}</span> : null}
                    </p>
                  </div>
                ))
              ) : (
                <p>No matched lenders found. Refine your profile to see options.</p>
              )}
            </div>
          </article>

          <article className="borrower-v3-card borrower-v3-panel">
            <h2 className="borrower-v3-panel-title">Matched real estate agents</h2>
            <div className="borrower-v3-match-stack">
              {matchedAgents && matchedAgents.length > 0 ? (
                matchedAgents.map((agent) => (
                  <div key={agent.id} className="borrower-v3-match-card borrower-v3-match-card-warm">
                    <h3>{[agent.firstName, agent.lastName].filter(Boolean).join(' ') || 'Agent'}</h3>
                    <p className="borrower-v3-match-meta">
                      {agent.nmlsId ? <span>NMLS# {agent.nmlsId}</span> : null}
                      {agent.specialty ? <span>{agent.specialty}</span> : null}
                    </p>
                  </div>
                ))
              ) : (
                <p>No matched agents found. Refine your profile to see options.</p>
              )}
            </div>
          </article>
        </section>

        <div style={{ textAlign: 'center', marginTop: '2rem' }}>
          <button
            type="button"
            className="borrower-v3-btn borrower-v3-btn-primary"
            onClick={() => setActiveView?.('home')}
          >
            Start new quote
          </button>
        </div>
      </div>
    </BorrowerShell>
  )
}

export default function BorrowerShell({ children, setActiveView, authState, onSignOut, activeView }) {
  return (
    <div className="borrower-v3-page">
      <header className="borrower-v3-topbar">
        <div className="borrower-v3-brand">Harbor Loan Quotes</div>
        <nav className="borrower-v3-nav" aria-label="Borrower navigation">
          <button type="button" className={`borrower-v3-nav-link${activeView === 'home' ? ' borrower-v3-nav-link-active' : ''}`} onClick={() => setActiveView?.('home')}>Get a Quote</button>
          <button type="button" className={`borrower-v3-nav-link${activeView === 'tools' ? ' borrower-v3-nav-link-active' : ''}`} onClick={() => setActiveView?.('tools')}>Calculators</button>
          {authState ? (
            <>
              <button type="button" className={`borrower-v3-nav-link${activeView === 'my-quotes' ? ' borrower-v3-nav-link-active' : ''}`} onClick={() => setActiveView?.('my-quotes')}>My Quotes</button>
              <button type="button" className="borrower-v3-nav-link" onClick={onSignOut}>Sign Out</button>
            </>
          ) : (
            <button type="button" className="borrower-v3-nav-link" onClick={() => setActiveView?.('auth')}>Sign In</button>
          )}
          <button type="button" className="borrower-v3-nav-pill">Subscribe</button>
        </nav>
      </header>
      {children}
    </div>
  )
}

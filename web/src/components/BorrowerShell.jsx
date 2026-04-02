export default function BorrowerShell({
  children,
  setActiveView,
  authState,
  onSignOut,
  activeView,
  onSubscribeClick,
  showSubscribePanel,
  onCloseSubscribePanel,
  subscriptionForm,
  onSubscriptionInput,
  handleSubscriptionSubmit,
  subscriptionMessage,
  subscriptionReceipt,
  rateAlertBanner,
  onDismissRateAlert,
}) {
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
          <button type="button" className="borrower-v3-nav-pill" onClick={onSubscribeClick}>Subscribe</button>
        </nav>
      </header>
      {rateAlertBanner && (
        <div className="bv3-rate-alert-banner">
          <span>📊 {rateAlertBanner}</span>
          <button type="button" className="bv3-rate-alert-dismiss" onClick={onDismissRateAlert}>✕</button>
        </div>
      )}
      {children}
      {showSubscribePanel && (
        <div className="bv3-modal-backdrop" onClick={onCloseSubscribePanel}>
          <div className="bv3-modal-card" onClick={(e) => e.stopPropagation()}>
            <div className="bv3-modal-header">
              <h2 className="bv3-modal-title">Stay informed on rate changes</h2>
              <button type="button" className="bv3-modal-close" onClick={onCloseSubscribePanel}>✕</button>
            </div>
            {subscriptionReceipt ? (
              <div className="bv3-modal-success">
                <p>✓ Subscribed <strong>{subscriptionReceipt.email}</strong></p>
                <p style={{fontSize: '0.85rem', color: '#6b7280', marginTop: '0.25rem'}}>You'll receive alerts for: {subscriptionReceipt.topics?.join(', ')}</p>
              </div>
            ) : (
              <form onSubmit={handleSubscriptionSubmit} data-source-surface="borrower_shell_subscription">
                <div className="bv3-modal-field">
                  <label htmlFor="sub-email">Email address</label>
                  <input
                    id="sub-email"
                    type="email"
                    name="email"
                    value={subscriptionForm?.email || ''}
                    onChange={onSubscriptionInput}
                    placeholder="you@example.com"
                    className="v3-landing-field-input"
                    required
                  />
                </div>
                <div className="bv3-modal-toggles">
                  <label className="bv3-toggle-row">
                    <input type="checkbox" name="productUpdates" checked={subscriptionForm?.productUpdates === 'true'} onChange={onSubscriptionInput} />
                    <span>Product updates</span>
                  </label>
                  <label className="bv3-toggle-row">
                    <input type="checkbox" name="rateAlerts" checked={subscriptionForm?.rateAlerts === 'true'} onChange={onSubscriptionInput} />
                    <span>Rate alerts</span>
                  </label>
                  <label className="bv3-toggle-row">
                    <input type="checkbox" name="partnerAlerts" checked={subscriptionForm?.partnerAlerts === 'true'} onChange={onSubscriptionInput} />
                    <span>Lender &amp; agent updates</span>
                  </label>
                </div>
                {subscriptionMessage && <p className="bv3-modal-msg">{subscriptionMessage}</p>}
                <button type="submit" className="borrower-v3-btn borrower-v3-btn-primary" style={{width: '100%', marginTop: '1rem'}}>
                  Subscribe
                </button>
              </form>
            )}
          </div>
        </div>
      )}
    </div>
  )
}

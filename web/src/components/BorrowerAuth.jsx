import { useState } from 'react'
import BorrowerShell from './BorrowerShell'
import './BorrowerStyles.css'

export default function BorrowerAuth({
  setActiveView,
  authState,
  onSignOut,
  loginForm,
  setLoginForm,
  registerForm,
  setRegisterForm,
  handleLoginSubmit,
  handleRegisterSubmit,
  loadingTarget,
  handleInput,
  errorMessage,
}) {
  const [showRegisterForm, setShowRegisterForm] = useState(false)

  return (
    <BorrowerShell setActiveView={setActiveView} authState={authState} onSignOut={onSignOut}>
      <section className="borrower-v3-card borrower-v3-auth-wrap">
        <h1 className="borrower-v3-auth-title">Save and refine your quote</h1>
        <p className="borrower-v3-auth-subtitle">
          Create an account to track quotes over time and unlock lender/agent matches.
        </p>

        {errorMessage ? (
          <div className="borrower-v3-auth-error">{errorMessage}</div>
        ) : null}

        <div className="borrower-v3-auth-grid">
          <article className="borrower-v3-auth-panel">
            <h3>Sign in</h3>
            <form onSubmit={handleLoginSubmit}>
              <input
                type="email"
                name="email"
                value={loginForm?.email || ''}
                onChange={handleInput?.(setLoginForm)}
                className="borrower-v3-input"
                placeholder="Email"
                required
              />
              <input
                type="password"
                name="password"
                value={loginForm?.password || ''}
                onChange={handleInput?.(setLoginForm)}
                className="borrower-v3-input"
                placeholder="Password"
                required
              />
              <button
                type="submit"
                className="borrower-v3-btn-blue"
                disabled={loadingTarget === 'login'}
              >
                {loadingTarget === 'login' ? 'Loading...' : 'Continue'}
              </button>
            </form>
          </article>

          <article className="borrower-v3-auth-panel borrower-v3-auth-panel-warm">
            <h3>Why create an account</h3>
            <ul className="borrower-v3-auth-list">
              <li>Save multiple quote scenarios</li>
              <li>Re-run pricing as rates shift</li>
              <li>Compare matched lenders + agents</li>
              <li>Continue on any device</li>
            </ul>
            {showRegisterForm ? (
              <form onSubmit={handleRegisterSubmit}>
                <input
                  type="email"
                  name="email"
                  value={registerForm?.email || ''}
                  onChange={handleInput?.(setRegisterForm)}
                  className="borrower-v3-input"
                  placeholder="Email"
                  required
                />
                <input
                  type="password"
                  name="password"
                  value={registerForm?.password || ''}
                  onChange={handleInput?.(setRegisterForm)}
                  className="borrower-v3-input"
                  placeholder="Password"
                  required
                />
                <button
                  type="submit"
                  className="borrower-v3-btn-orange"
                  disabled={loadingTarget === 'register'}
                >
                  {loadingTarget === 'register' ? 'Creating...' : 'Create account'}
                </button>
              </form>
            ) : (
              <button
                type="button"
                className="borrower-v3-btn-orange"
                onClick={() => setShowRegisterForm(true)}
              >
                Create account
              </button>
            )}
          </article>
        </div>
      </section>
    </BorrowerShell>
  )
}

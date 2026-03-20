import './AdminStyles.css'

export default function AdminLogin({
  loginForm,
  setLoginForm,
  handleLoginSubmit,
  handleInput,
  loadingTarget,
  errorMessage,
  authState,
}) {
  const isNonAdminSession = authState?.accessToken && authState?.role !== 'ADMIN'
  const isLoading = loadingTarget === 'login'

  return (
    <div className="admin-v3-page">
      <header className="admin-v3-topbar">
        <div className="admin-v3-brand">Mortgage Desk</div>
      </header>

      <section className="admin-v3-card admin-v3-login-wrap">
        <h1 className="admin-v3-login-title">Sign in to Mortgage Desk</h1>
        <p className="admin-v3-login-copy">Monitor borrower funnels, pricing, and partner coverage.</p>

        {isNonAdminSession ? (
          <div className="admin-v3-error-message">This session is valid but does not have the ADMIN role.</div>
        ) : null}
        {errorMessage ? <div className="admin-v3-error-message">{errorMessage}</div> : null}

        <form onSubmit={handleLoginSubmit}>
          <input
            className="admin-v3-input"
            placeholder="admin@harborloanquotes.com"
            type="email"
            name="email"
            value={loginForm.email}
            onChange={handleInput(setLoginForm)}
            required
          />
          <input
            className="admin-v3-input"
            placeholder="Password"
            type="password"
            name="password"
            value={loginForm.password}
            onChange={handleInput(setLoginForm)}
            required
          />
          <button type="submit" className="admin-v3-btn admin-v3-btn-blue" disabled={isLoading}>
            {isLoading ? 'Signing in...' : 'Sign in'}
          </button>
        </form>

      </section>
    </div>
  )
}

const tabs = ['Dashboard', 'Workspace', 'Pricing', 'Lenders', 'Agents', 'Reports']

export default function AdminShell({
  activeTab,
  setActiveTab,
  authState,
  onSignOut,
  children,
}) {
  const handleTabClick = (tabName) => {
    setActiveTab(tabName.toLowerCase())
  }

  return (
    <div className="admin-v3-page">
      <header className="admin-v3-topbar">
        <div className="admin-v3-brand">Mortgage Desk</div>
        <nav className="admin-v3-nav" aria-label="Mortgage Desk navigation">
          {tabs.map((tab) => (
            <button
              key={tab}
              type="button"
              className={`admin-v3-tab ${activeTab === tab.toLowerCase() ? 'admin-v3-tab-active' : ''}`}
              onClick={() => handleTabClick(tab)}
            >
              {tab}
            </button>
          ))}
          <button
            type="button"
            className="admin-v3-tab"
            onClick={onSignOut}
          >
            Sign out
          </button>
        </nav>
      </header>
      {children}
    </div>
  )
}

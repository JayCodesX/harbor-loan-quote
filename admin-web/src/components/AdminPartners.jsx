import { useState, useEffect, useMemo } from 'react'
import AdminShell from './AdminShell'
import './AdminStyles.css'

const API_BASE = '/api'
const PAGE_SIZE = 25

const truncate = (str, max = 128) =>
  str && str.length > max ? str.substring(0, max) + '…' : (str || '—')

const emptyForm = {
  displayName: '',
  companyName: '',
  email: '',
  phone: '',
  stateCode: '',
  countyName: '',
  city: '',
  specialty: '',
  licenseNumber: '',
  nmlsId: '',
  rankingScore: '',
  responseSlaHours: '',
  languages: 'English',
  websiteUrl: '',
  active: 'true',
}

export default function AdminPartners({
  mode = 'lenders',
  activeTab,
  setActiveTab,
  authState,
  onSignOut,
  loadingTarget: globalLoadingTarget,
}) {
  const isAgents = mode === 'agents'
  const label = isAgents ? 'Agent' : 'Lender'
  const plural = isAgents ? 'Agents' : 'Lenders'

  // Location state
  const [locationOptions, setLocationOptions] = useState([])
  const [selectedState, setSelectedState] = useState('')
  const [selectedCounty, setSelectedCounty] = useState('')

  // Table state
  const [partners, setPartners] = useState([])
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [currentPage, setCurrentPage] = useState(0)
  const [loading, setLoading] = useState(false)

  // Add/Edit form state
  const [showForm, setShowForm] = useState(false)
  const [editingId, setEditingId] = useState(null)
  const [form, setForm] = useState(emptyForm)
  const [saving, setSaving] = useState(false)

  // Counties for selected state
  const counties = useMemo(() => {
    const found = locationOptions.find((l) => l.stateCode === selectedState)
    return found ? found.counties : []
  }, [locationOptions, selectedState])

  const states = useMemo(
    () => locationOptions.map((l) => l.stateCode).sort(),
    [locationOptions]
  )

  // Fetch location options on mount
  useEffect(() => {
    fetch(`${API_BASE}/directory/locations`)
      .then((r) => (r.ok ? r.json() : []))
      .then((data) => {
        if (Array.isArray(data)) {
          const sorted = [...data].sort((a, b) => a.stateCode.localeCompare(b.stateCode))
          setLocationOptions(sorted)
          // Default to first state + first county alphabetically
          if (sorted.length > 0) {
            const firstState = sorted[0]
            setSelectedState(firstState.stateCode)
            const sortedCounties = [...(firstState.counties || [])].sort()
            if (sortedCounties.length > 0) {
              setSelectedCounty(sortedCounties[0])
            }
          }
        }
      })
      .catch(() => {})
  }, [])

  // Auto-reset county when state changes
  const handleStateChange = (e) => {
    const st = e.target.value
    setSelectedState(st)
    const found = locationOptions.find((l) => l.stateCode === st)
    const sortedCounties = found ? [...found.counties].sort() : []
    setSelectedCounty(sortedCounties[0] || '')
    setCurrentPage(0)
  }

  const handleCountyChange = (e) => {
    setSelectedCounty(e.target.value)
    setCurrentPage(0)
  }

  // Fetch partners when state/county/page changes
  useEffect(() => {
    if (!selectedState || !selectedCounty || !authState?.accessToken) return
    fetchPartners(currentPage)
  }, [selectedState, selectedCounty, currentPage, authState?.accessToken, mode])

  const fetchPartners = async (page = 0) => {
    setLoading(true)
    try {
      const params = new URLSearchParams({
        stateCode: selectedState,
        countyName: selectedCounty,
        page,
        size: PAGE_SIZE,
      })
      const res = await fetch(`${API_BASE}/admin/${mode}?${params}`, {
        headers: { Authorization: `Bearer ${authState.accessToken}` },
      })
      if (res.ok) {
        const data = await res.json()
        setPartners(data.content || [])
        setTotalElements(data.totalElements || 0)
        setTotalPages(data.totalPages || 0)
      }
    } finally {
      setLoading(false)
    }
  }

  // Form helpers
  const openAddForm = () => {
    setForm({ ...emptyForm, stateCode: selectedState, countyName: selectedCounty })
    setEditingId(null)
    setShowForm(true)
  }

  const openEditForm = (partner) => {
    setForm({
      displayName: partner.displayName ?? '',
      companyName: partner.companyName ?? '',
      email: partner.email ?? '',
      phone: partner.phone ?? '',
      stateCode: partner.stateCode ?? '',
      countyName: partner.countyName ?? '',
      city: partner.city ?? '',
      specialty: partner.specialty ?? '',
      licenseNumber: partner.licenseNumber ?? '',
      nmlsId: partner.nmlsId ?? '',
      rankingScore: partner.rankingScore != null ? String(partner.rankingScore) : '',
      responseSlaHours: partner.responseSlaHours != null ? String(partner.responseSlaHours) : '',
      languages: partner.languages ?? 'English',
      websiteUrl: partner.websiteUrl ?? '',
      active: String(partner.active ?? true),
    })
    setEditingId(partner.id)
    setShowForm(true)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const closeForm = () => {
    setShowForm(false)
    setEditingId(null)
    setForm(emptyForm)
  }

  const handleFormInput = (e) => {
    const { name, value } = e.target
    setForm((f) => ({ ...f, [name]: value }))
  }

  const handleSave = async (e) => {
    e.preventDefault()
    setSaving(true)
    try {
      const url = editingId
        ? `${API_BASE}/admin/${mode}/${editingId}`
        : `${API_BASE}/admin/${mode}`
      const method = editingId ? 'PUT' : 'POST'
      const res = await fetch(url, {
        method,
        headers: {
          Authorization: `Bearer ${authState.accessToken}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          ...form,
          rankingScore: form.rankingScore ? Number(form.rankingScore) : null,
          responseSlaHours: form.responseSlaHours ? Number(form.responseSlaHours) : null,
          active: form.active === 'true',
        }),
      })
      if (res.ok) {
        closeForm()
        fetchPartners(currentPage)
      }
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (id) => {
    if (!window.confirm(`Remove this ${label.toLowerCase()}?`)) return
    await fetch(`${API_BASE}/admin/${mode}/${id}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${authState.accessToken}` },
    })
    fetchPartners(currentPage)
  }

  const handleImport = async () => {
    await fetch(`${API_BASE}/admin/${mode}/sync?provider=external-csv`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${authState.accessToken}` },
    })
    fetchPartners(currentPage)
  }

  // Sorted counties for dropdown
  const sortedCounties = useMemo(() => [...counties].sort(), [counties])

  const activeCount = partners.filter((p) => p.active).length
  const avgSla =
    partners.length > 0
      ? Math.round(partners.reduce((s, p) => s + (p.responseSlaHours || 0), 0) / partners.length)
      : 0

  return (
    <AdminShell activeTab={activeTab} setActiveTab={setActiveTab} authState={authState} onSignOut={onSignOut}>
      <div className="admin-v3-layout">
        <section className="admin-v3-page-header">
          <h1>{plural}</h1>
          <p>
            {isAgents
              ? 'Built for high volume recruiting, routing, and territory balancing.'
              : 'Built for high volume onboarding, search, bulk edit, and routing quality controls.'}
          </p>
        </section>

        {/* Filter bar */}
        <section className="ap-filter-bar admin-v3-card">
          <div className="ap-filter-group">
            <label className="ap-filter-label">State</label>
            <select className="admin-v3-input ap-filter-select" value={selectedState} onChange={handleStateChange}>
              {states.map((s) => (
                <option key={s} value={s}>{s}</option>
              ))}
            </select>
          </div>
          <div className="ap-filter-group">
            <label className="ap-filter-label">County</label>
            <select className="admin-v3-input ap-filter-select" value={selectedCounty} onChange={handleCountyChange}>
              {sortedCounties.map((c) => (
                <option key={c} value={c}>{c}</option>
              ))}
            </select>
          </div>
          <button className="admin-v3-btn admin-v3-btn-blue ap-add-btn" onClick={openAddForm} disabled={showForm}>
            + Add {label}
          </button>
        </section>

        {/* Inline add/edit form */}
        {showForm && (
          <section className="admin-v3-card ap-form-panel">
            <div className="ap-form-header">
              <h2>{editingId ? `Edit ${label}` : `New ${label}`}</h2>
              <button type="button" className="ap-form-close" onClick={closeForm}>✕</button>
            </div>
            <form onSubmit={handleSave} className="ap-form-grid">
              <div className="ap-field">
                <label>Display name</label>
                <input className="admin-v3-input admin-v3-input-compact" name="displayName" value={form.displayName} onChange={handleFormInput} placeholder="Full name or institution" required />
              </div>
              <div className="ap-field">
                <label>Company</label>
                <input className="admin-v3-input admin-v3-input-compact" name="companyName" value={form.companyName} onChange={handleFormInput} placeholder="Company name" />
              </div>
              <div className="ap-field">
                <label>Email</label>
                <input className="admin-v3-input admin-v3-input-compact" name="email" type="email" value={form.email} onChange={handleFormInput} placeholder="Email" required />
              </div>
              <div className="ap-field">
                <label>Phone</label>
                <input className="admin-v3-input admin-v3-input-compact" name="phone" value={form.phone} onChange={handleFormInput} placeholder="(555) 000-0000" />
              </div>
              <div className="ap-field">
                <label>State</label>
                <select className="admin-v3-input admin-v3-input-compact" name="stateCode" value={form.stateCode} onChange={handleFormInput}>
                  {states.map((s) => <option key={s} value={s}>{s}</option>)}
                </select>
              </div>
              <div className="ap-field">
                <label>County</label>
                <input className="admin-v3-input admin-v3-input-compact" name="countyName" value={form.countyName} onChange={handleFormInput} placeholder="County" />
              </div>
              <div className="ap-field">
                <label>License #</label>
                <input className="admin-v3-input admin-v3-input-compact" name="licenseNumber" value={form.licenseNumber} onChange={handleFormInput} placeholder="License number" />
              </div>
              <div className="ap-field">
                <label>NMLS ID</label>
                <input className="admin-v3-input admin-v3-input-compact" name="nmlsId" value={form.nmlsId} onChange={handleFormInput} placeholder="NMLS ID" />
              </div>
              <div className="ap-field">
                <label>{isAgents ? 'Specialty' : 'Loan types'}</label>
                <input className="admin-v3-input admin-v3-input-compact" name="specialty" value={form.specialty} onChange={handleFormInput} placeholder={isAgents ? 'e.g. Purchase' : 'e.g. Conventional, FHA'} />
              </div>
              <div className="ap-field">
                <label>Ranking score</label>
                <input className="admin-v3-input admin-v3-input-compact" name="rankingScore" type="number" min="0" max="100" value={form.rankingScore} onChange={handleFormInput} placeholder="0–100" />
              </div>
              <div className="ap-field">
                <label>{isAgents ? 'Avg response (hrs)' : 'Avg SLA (hrs)'}</label>
                <input className="admin-v3-input admin-v3-input-compact" name="responseSlaHours" type="number" min="1" value={form.responseSlaHours} onChange={handleFormInput} placeholder="Hours" />
              </div>
              <div className="ap-field">
                <label>Status</label>
                <select className="admin-v3-input admin-v3-input-compact" name="active" value={form.active} onChange={handleFormInput}>
                  <option value="true">Active</option>
                  <option value="false">Inactive</option>
                </select>
              </div>
              <div className="ap-form-actions">
                <button type="submit" className="admin-v3-btn admin-v3-btn-blue" disabled={saving}>
                  {saving ? 'Saving…' : 'Save'}
                </button>
                <button type="button" className="admin-v3-btn ap-btn-cancel" onClick={closeForm}>Cancel</button>
              </div>
            </form>
          </section>
        )}

        {/* Stats */}
        <section className="admin-v3-stat-grid-partners">
          <article className="admin-v3-card admin-v3-stat-card">
            <h3>Total in county</h3>
            <p>{totalElements}</p>
          </article>
          <article className="admin-v3-card admin-v3-stat-card">
            <h3>{isAgents ? 'Active agents' : 'Active lenders'}</h3>
            <p>{activeCount}</p>
          </article>
          <article className="admin-v3-card admin-v3-stat-card">
            <h3>{isAgents ? 'Avg response (hrs)' : 'Avg SLA (hrs)'}</h3>
            <p>{avgSla || '—'}</p>
          </article>
          <article className="admin-v3-card admin-v3-stat-card admin-v3-stat-warm">
            <h3>{isAgents ? 'Territory gaps' : 'Coverage gaps'}</h3>
            <p>{totalElements === 0 ? '!' : '0'}</p>
          </article>
        </section>

        {/* Table */}
        <section className="admin-v3-card admin-v3-data-table">
          {loading ? (
            <div className="ap-loading">Loading {plural.toLowerCase()}…</div>
          ) : (
            <table className="admin-v3-table">
              <thead>
                <tr className="admin-v3-table-header">
                  <th className="ap-col-name">NAME</th>
                  <th>EMAIL</th>
                  <th>PHONE</th>
                  <th>{isAgents ? 'SPECIALTY' : 'LOAN TYPES'}</th>
                  <th>SCORE</th>
                  <th>STATUS</th>
                  <th>ACTIONS</th>
                </tr>
              </thead>
              <tbody>
                {partners.length === 0 ? (
                  <tr>
                    <td colSpan="7" className="admin-v3-empty-state">
                      {selectedState && selectedCounty
                        ? `No ${plural.toLowerCase()} found in ${selectedCounty}, ${selectedState}`
                        : 'Select a state and county to load results'}
                    </td>
                  </tr>
                ) : (
                  partners.map((p) => (
                    <tr key={p.id} className="admin-v3-table-row">
                      <td className="ap-col-name" title={p.displayName}>
                        {truncate(p.displayName, 128)}
                      </td>
                      <td>{p.email || '—'}</td>
                      <td>{p.phone || '—'}</td>
                      <td>{truncate(p.specialty, 40)}</td>
                      <td>{p.rankingScore ?? '—'}</td>
                      <td>
                        <span className={`admin-v3-status-badge ${p.active ? 'admin-v3-status-active' : 'admin-v3-status-inactive'}`}>
                          {p.active ? 'Active' : 'Inactive'}
                        </span>
                      </td>
                      <td>
                        <button className="admin-v3-btn-action" onClick={() => openEditForm(p)}>Edit</button>
                        <button className="admin-v3-btn-action admin-v3-btn-action-danger" onClick={() => handleDelete(p.id)}>Delete</button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          )}

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="admin-v3-pagination">
              <span className="admin-v3-pagination-info">
                Showing {currentPage * PAGE_SIZE + 1}–{Math.min((currentPage + 1) * PAGE_SIZE, totalElements)} of {totalElements}
              </span>
              <div className="admin-v3-pagination-controls">
                <button
                  className="admin-v3-pagination-btn"
                  onClick={() => setCurrentPage((p) => p - 1)}
                  disabled={currentPage === 0}
                >
                  ‹
                </button>
                {Array.from({ length: Math.min(totalPages, 7) }, (_, i) => {
                  const p = totalPages <= 7 ? i : Math.max(0, Math.min(currentPage - 3, totalPages - 7)) + i
                  return (
                    <button
                      key={p}
                      className={`admin-v3-pagination-btn${currentPage === p ? ' admin-v3-pagination-btn-active' : ''}`}
                      onClick={() => setCurrentPage(p)}
                      disabled={currentPage === p}
                    >
                      {p + 1}
                    </button>
                  )
                })}
                <button
                  className="admin-v3-pagination-btn"
                  onClick={() => setCurrentPage((p) => p + 1)}
                  disabled={currentPage >= totalPages - 1}
                >
                  ›
                </button>
              </div>
            </div>
          )}
        </section>

        {/* Import button — bottom of page */}
        <div className="ap-import-row">
          <button className="admin-v3-btn admin-v3-btn-orange" onClick={handleImport}>
            Import {plural} CSV
          </button>
          <span className="ap-import-hint">Upload a CSV to bulk-sync {plural.toLowerCase()} for the selected county.</span>
        </div>
      </div>
    </AdminShell>
  )
}

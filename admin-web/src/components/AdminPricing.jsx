import { useEffect, useRef } from 'react'
import AdminShell from './AdminShell'
import './AdminStyles.css'

export default function AdminPricing({
  activeTab,
  setActiveTab,
  productForm,
  setProductForm,
  products,
  editingProductId,
  saveProduct,
  startEditProduct,
  resetProductForm,
  deleteProduct,
  loadProducts,
  handleInput,
  loadingTarget,
  authState,
  onSignOut,
  rateSheetForm,
  setRateSheetForm,
  rateSheetResult,
  publishRateSheet,
}) {
  useEffect(() => {
    if (!products.length) {
      loadProducts()
    }
  }, [products, loadProducts])

  const csvFileInputRef = useRef(null)

  const handleCsvFileUpload = (e) => {
    const file = e.target.files[0]
    if (!file) return
    const reader = new FileReader()
    reader.onload = (evt) => {
      setRateSheetForm((prev) => ({ ...prev, entriesCsv: evt.target.result }))
    }
    reader.readAsText(file)
    e.target.value = ''
  }

  const handleProductClick = (product) => {
    startEditProduct(product)
  }

  const handleDelete = (id) => {
    deleteProduct(id)
  }

  return (
    <AdminShell activeTab={activeTab} setActiveTab={setActiveTab} authState={authState} onSignOut={onSignOut}>
      <div className="admin-v3-layout">
        <section className="admin-v3-grid-2">
          <article className="admin-v3-card admin-v3-panel">
            <h2>{editingProductId ? 'Edit' : 'New'} product</h2>
            <form onSubmit={saveProduct}>
              <div className="admin-v3-stack">
                <input
                  className="admin-v3-input admin-v3-input-compact"
                  placeholder="Program code"
                  name="programCode"
                  value={productForm.programCode}
                  onChange={handleInput(setProductForm)}
                  required
                />
                <input
                  className="admin-v3-input admin-v3-input-compact"
                  placeholder="Product name"
                  name="productName"
                  value={productForm.productName}
                  onChange={handleInput(setProductForm)}
                  required
                />
                <input
                  className="admin-v3-input admin-v3-input-compact"
                  placeholder="Base rate (%)"
                  name="baseRate"
                  type="number"
                  step="0.01"
                  value={productForm.baseRate}
                  onChange={handleInput(setProductForm)}
                  required
                />
                <select
                  className="admin-v3-input admin-v3-input-compact"
                  name="active"
                  value={productForm.active}
                  onChange={handleInput(setProductForm)}
                >
                  <option value="true">Active</option>
                  <option value="false">Inactive</option>
                </select>
                <button
                  type="submit"
                  className="admin-v3-btn admin-v3-btn-blue"
                  disabled={loadingTarget === 'save-product'}
                >
                  {loadingTarget === 'save-product' ? 'Saving...' : 'Save product'}
                </button>
                {editingProductId ? (
                  <button
                    type="button"
                    className="admin-v3-btn admin-v3-btn-blue"
                    onClick={resetProductForm}
                    style={{ background: '#627b98', marginTop: '10px' }}
                  >
                    Cancel
                  </button>
                ) : null}
              </div>
            </form>
          </article>

          <article className="admin-v3-card admin-v3-panel">
            <h2>Active pricing products</h2>
            <div className="admin-v3-stack">
              {products.length === 0 ? (
                <p className="admin-v3-empty-state">No products loaded</p>
              ) : (
                products.map((product) => (
                  <div
                    key={product.id}
                    className={`admin-v3-action-card ${editingProductId === product.id ? 'admin-v3-product-editing' : ''}`}
                    onClick={() => handleProductClick(product)}
                    role="button"
                    tabIndex={0}
                  >
                    <div>
                      {product.programCode} • {product.productName} • {product.baseRate}%
                      {!product.active ? ' (inactive)' : ''}
                    </div>
                    <button
                      type="button"
                      className="admin-v3-btn-delete"
                      onClick={(e) => {
                        e.stopPropagation()
                        handleDelete(product.id)
                      }}
                      disabled={loadingTarget === `delete-product-${product.id}`}
                    >
                      Delete
                    </button>
                  </div>
                ))
              )}
            </div>
          </article>
        </section>

        <section className="admin-v3-card admin-v3-panel" style={{ marginTop: '20px' }}>
          <h2>Publish rate sheet</h2>
          <form onSubmit={publishRateSheet}>
            <div className="admin-v3-stack">
              <input
                className="admin-v3-input admin-v3-input-compact"
                placeholder="Investor ID (e.g. FANNIE_MAE)"
                name="investorId"
                value={rateSheetForm?.investorId || ''}
                onChange={handleInput(setRateSheetForm)}
                required
              />
              <input
                className="admin-v3-input admin-v3-input-compact"
                type="datetime-local"
                name="effectiveAt"
                value={rateSheetForm?.effectiveAt || ''}
                onChange={handleInput(setRateSheetForm)}
              />
              <input
                className="admin-v3-input admin-v3-input-compact"
                type="datetime-local"
                name="expiresAt"
                value={rateSheetForm?.expiresAt || ''}
                onChange={handleInput(setRateSheetForm)}
              />
              <input
                className="admin-v3-input admin-v3-input-compact"
                placeholder="Source (e.g. fannie-mae-morning-2026-04-02)"
                name="source"
                value={rateSheetForm?.source || ''}
                onChange={handleInput(setRateSheetForm)}
              />
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#374151' }}>
                  Entries (one per line: productTermId,rate,price)
                </label>
                <button
                  type="button"
                  className="admin-v3-btn admin-v3-btn-blue"
                  style={{ padding: '4px 10px', fontSize: '0.75rem' }}
                  onClick={() => csvFileInputRef.current?.click()}
                >
                  Upload CSV
                </button>
                <input
                  ref={csvFileInputRef}
                  type="file"
                  accept=".csv,text/csv"
                  style={{ display: 'none' }}
                  onChange={handleCsvFileUpload}
                />
              </div>
              <textarea
                className="admin-v3-input"
                name="entriesCsv"
                rows={6}
                style={{ fontFamily: 'monospace', fontSize: '0.8rem' }}
                placeholder="CONVENTIONAL_30,0.0675,-0.5000"
                value={rateSheetForm?.entriesCsv || ''}
                onChange={handleInput(setRateSheetForm)}
              />
              <button
                type="submit"
                className="admin-v3-btn admin-v3-btn-blue"
                disabled={loadingTarget === 'publish-rate-sheet'}
              >
                {loadingTarget === 'publish-rate-sheet' ? 'Publishing...' : 'Publish rate sheet'}
              </button>
            </div>
          </form>
          {rateSheetResult && (
            <div className="admin-v3-card admin-v3-panel" style={{ marginTop: '16px', background: '#f0fdf4', borderColor: '#bbf7d0' }}>
              <h3 style={{ margin: '0 0 10px', color: '#15803d' }}>Rate sheet published</h3>
              <div className="admin-v3-stack" style={{ fontSize: '0.875rem', color: '#374151' }}>
                <div><strong>ID:</strong> {rateSheetResult.id}</div>
                <div><strong>Investor:</strong> {rateSheetResult.investorId}</div>
                <div><strong>Status:</strong> {rateSheetResult.status}</div>
                <div><strong>Effective at:</strong> {rateSheetResult.effectiveAt}</div>
                <div><strong>Expires at:</strong> {rateSheetResult.expiresAt}</div>
              </div>
            </div>
          )}
        </section>
      </div>
    </AdminShell>
  )
}

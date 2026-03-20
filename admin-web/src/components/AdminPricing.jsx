import { useEffect } from 'react'
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
}) {
  useEffect(() => {
    if (!products.length) {
      loadProducts()
    }
  }, [products, loadProducts])

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
      </div>
    </AdminShell>
  )
}

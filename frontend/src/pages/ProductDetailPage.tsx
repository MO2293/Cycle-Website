import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ApiError, api, formatPrice, mediaUrl } from '../api/client'
import type { Item } from '../api/types'
import { CATEGORY_LABELS } from '../api/types'
import { useCart } from '../context/CartContext'
import { Badge, ErrorNotice, Spinner, buttonClass, secondaryButtonClass } from '../components/ui'

export default function ProductDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { addItem, lines } = useCart()

  const [item, setItem] = useState<Item | null>(null)
  const [quantity, setQuantity] = useState(1)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [cartError, setCartError] = useState<string | null>(null)
  const [isAdding, setIsAdding] = useState(false)
  const [justAdded, setJustAdded] = useState(false)

  useEffect(() => {
    setIsLoading(true)
    api
      .get<Item>(`/api/items/${id}`, false)
      .then(setItem)
      .catch((err: Error) => setError(err.message))
      .finally(() => setIsLoading(false))
  }, [id])

  const inCart = lines.find((line) => line.itemId === Number(id))?.quantity ?? 0

  const handleAdd = async () => {
    if (!item) return
    setCartError(null)
    setIsAdding(true)
    try {
      await addItem(item.id, quantity)
      setJustAdded(true)
      setTimeout(() => setJustAdded(false), 2500)
    } catch (err) {
      setCartError(err instanceof ApiError ? err.message : 'Could not add that to your cart.')
    } finally {
      setIsAdding(false)
    }
  }

  if (isLoading) return <Spinner label="Loading bike…" />

  if (error || !item) {
    return (
      <div className="mx-auto max-w-2xl px-4 py-16">
        <ErrorNotice message={error ?? 'That bike could not be found.'} />
        <Link to="/" className="mt-4 inline-block text-sm font-medium text-moss-600">
          ← Back to the shop
        </Link>
      </div>
    )
  }

  const maxSelectable = Math.max(item.quantity - inCart, 0)

  return (
    <div className="mx-auto max-w-5xl px-4 py-10">
      <Link to="/" className="text-sm font-medium text-moss-600 hover:text-moss-700">
        ← Back to the shop
      </Link>

      <div className="mt-6 grid gap-10 md:grid-cols-2">
        <div className="overflow-hidden rounded-xl border border-ink-100 bg-white">
          <img src={mediaUrl(item.imageUrl)} alt={item.name} className="h-full w-full object-contain" />
        </div>

        <div className="flex flex-col gap-4">
          <div className="flex items-center gap-2">
            <Badge>{CATEGORY_LABELS[item.category]}</Badge>
            {item.inStock ? (
              <Badge tone="good">{item.quantity} in stock</Badge>
            ) : (
              <Badge tone="warn">Out of stock</Badge>
            )}
          </div>

          <h1 className="text-3xl font-semibold tracking-tight text-ink-900">{item.name}</h1>
          <p className="text-2xl font-semibold text-ink-900">{formatPrice(item.price)}</p>
          <p className="leading-relaxed text-ink-600">{item.description}</p>

          <dl className="grid grid-cols-2 gap-y-3 border-t border-ink-100 pt-5 text-sm">
            <dt className="text-ink-400">Model</dt>
            <dd className="text-ink-800">{item.model}</dd>
            <dt className="text-ink-400">Colour</dt>
            <dd className="text-ink-800">{item.colour}</dd>
          </dl>

          {cartError && <ErrorNotice message={cartError} />}

          {inCart > 0 && (
            <p className="text-sm text-moss-700">
              {inCart} already in your cart.
            </p>
          )}

          {item.inStock ? (
            <div className="mt-2 flex flex-col gap-3">
              <label className="flex items-center gap-3 text-sm text-ink-600">
                Quantity
                <select
                  value={quantity}
                  onChange={(event) => setQuantity(Number(event.target.value))}
                  disabled={maxSelectable === 0}
                  className="rounded-lg border border-ink-200 bg-white px-3 py-2 text-sm"
                >
                  {Array.from({ length: Math.min(maxSelectable, 10) }, (_, index) => (
                    <option key={index + 1} value={index + 1}>
                      {index + 1}
                    </option>
                  ))}
                </select>
              </label>

              <div className="flex flex-wrap gap-3">
                <button
                  type="button"
                  onClick={handleAdd}
                  disabled={isAdding || maxSelectable === 0}
                  className={buttonClass}
                >
                  {isAdding ? 'Adding…' : justAdded ? 'Added ✓' : 'Add to cart'}
                </button>

                {inCart > 0 && (
                  <button
                    type="button"
                    onClick={() => navigate('/cart')}
                    className={secondaryButtonClass}
                  >
                    View cart
                  </button>
                )}
              </div>

              {maxSelectable === 0 && (
                <p className="text-sm text-clay-600">
                  You already have all available stock in your cart.
                </p>
              )}
            </div>
          ) : (
            <p className="mt-2 rounded-lg border border-clay-500/30 bg-clay-100 px-4 py-3 text-sm text-clay-600">
              This bike is currently out of stock.
            </p>
          )}
        </div>
      </div>
    </div>
  )
}

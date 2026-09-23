import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ApiError, formatPrice, mediaUrl } from '../api/client'
import { useCart } from '../context/CartContext'
import {
  EmptyState,
  ErrorNotice,
  Spinner,
  buttonClass,
  secondaryButtonClass,
} from '../components/ui'

export default function CartPage() {
  const { lines, subtotal, totalUnits, notices, isLoading, setQuantity, removeItem, clear } =
    useCart()
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)
  const [busyItemId, setBusyItemId] = useState<number | null>(null)

  const run = async (itemId: number, action: () => Promise<void>) => {
    setError(null)
    setBusyItemId(itemId)
    try {
      await action()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not update your cart.')
    } finally {
      setBusyItemId(null)
    }
  }

  if (isLoading && lines.length === 0) return <Spinner label="Loading your cart…" />

  return (
    <div className="mx-auto max-w-4xl px-4 py-10">
      <h1 className="text-2xl font-semibold tracking-tight text-ink-900">Your cart</h1>

      {notices.length > 0 && (
        <div className="mt-4 rounded-lg border border-clay-500/30 bg-clay-100 px-4 py-3 text-sm text-clay-600">
          {/* Stock adjustments the server made, shown rather than applied
              silently the way the original did. */}
          <ul className="list-inside list-disc space-y-1">
            {notices.map((notice) => (
              <li key={notice}>{notice}</li>
            ))}
          </ul>
        </div>
      )}

      {error && (
        <div className="mt-4">
          <ErrorNotice message={error} />
        </div>
      )}

      {lines.length === 0 ? (
        <div className="mt-6">
          <EmptyState title="Your cart is empty" body="Browse the shop to find your next bike." />
          <div className="mt-4 text-center">
            <Link to="/" className="font-medium text-moss-600 hover:text-moss-700">
              ← Back to the shop
            </Link>
          </div>
        </div>
      ) : (
        <>
          <ul className="mt-6 divide-y divide-ink-100 overflow-hidden rounded-xl border border-ink-100 bg-white">
            {lines.map((line) => (
              <li key={line.itemId} className="flex flex-wrap items-center gap-4 p-4">
                <Link to={`/items/${line.itemId}`} className="shrink-0">
                  <img
                    src={mediaUrl(line.imageUrl)}
                    alt={line.name}
                    className="h-20 w-28 rounded-lg bg-ink-50 object-contain"
                  />
                </Link>

                <div className="min-w-40 flex-1">
                  <Link
                    to={`/items/${line.itemId}`}
                    className="font-medium text-ink-900 hover:text-moss-700"
                  >
                    {line.name}
                  </Link>
                  <p className="text-sm text-ink-400">
                    {line.model} · {line.colour}
                  </p>
                  <p className="mt-1 text-sm text-ink-600">{formatPrice(line.unitPrice)} each</p>
                </div>

                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    aria-label={`Decrease quantity of ${line.name}`}
                    disabled={busyItemId === line.itemId}
                    onClick={() => run(line.itemId, () => setQuantity(line.itemId, line.quantity - 1))}
                    className="h-8 w-8 rounded-lg border border-ink-200 text-ink-600 hover:bg-ink-50 disabled:opacity-40"
                  >
                    −
                  </button>

                  <span className="w-8 text-center text-sm font-medium">{line.quantity}</span>

                  <button
                    type="button"
                    aria-label={`Increase quantity of ${line.name}`}
                    // Capped at available stock, so the control cannot ask for
                    // something the server will only reject.
                    disabled={busyItemId === line.itemId || line.quantity >= line.availableStock}
                    onClick={() => run(line.itemId, () => setQuantity(line.itemId, line.quantity + 1))}
                    className="h-8 w-8 rounded-lg border border-ink-200 text-ink-600 hover:bg-ink-50 disabled:opacity-40"
                  >
                    +
                  </button>
                </div>

                <div className="w-24 text-right font-semibold text-ink-900">
                  {formatPrice(line.lineTotal)}
                </div>

                <button
                  type="button"
                  onClick={() => run(line.itemId, () => removeItem(line.itemId))}
                  disabled={busyItemId === line.itemId}
                  className="text-sm text-ink-400 underline-offset-2 hover:text-clay-600 hover:underline"
                >
                  Remove
                </button>
              </li>
            ))}
          </ul>

          <div className="mt-6 flex flex-wrap items-center justify-between gap-4 rounded-xl border border-ink-100 bg-white p-5">
            <div>
              <p className="text-sm text-ink-400">
                {totalUnits} item{totalUnits === 1 ? '' : 's'}
              </p>
              <p className="text-2xl font-semibold text-ink-900">{formatPrice(subtotal)}</p>
              <p className="mt-1 text-xs text-ink-400">
                Final total is confirmed by the server at checkout.
              </p>
            </div>

            <div className="flex gap-3">
              <button type="button" onClick={() => void clear()} className={secondaryButtonClass}>
                Clear cart
              </button>
              <button type="button" onClick={() => navigate('/checkout')} className={buttonClass}>
                Checkout
              </button>
            </div>
          </div>
        </>
      )}
    </div>
  )
}

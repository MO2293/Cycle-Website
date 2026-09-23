import { useEffect, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import { api, formatPrice } from '../api/client'
import type { Order } from '../api/types'
import { useAuth } from '../context/AuthContext'
import { Badge, ErrorNotice, Spinner } from '../components/ui'

export default function OrderConfirmationPage() {
  const { orderRef } = useParams<{ orderRef: string }>()
  const location = useLocation()
  const { isAuthenticated } = useAuth()

  // Checkout navigates here with the order already in hand, so the confirmation
  // renders instantly instead of round-tripping for data we were just given.
  const passedOrder = (location.state as { order?: Order } | null)?.order ?? null

  const [order, setOrder] = useState<Order | null>(passedOrder)
  const [isLoading, setIsLoading] = useState(!passedOrder)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (passedOrder || !orderRef) return

    // Reached by refresh or a direct link. Guest orders cannot be re-fetched,
    // since there is no account to prove ownership against.
    if (!isAuthenticated) {
      setError('Sign in to view this order.')
      setIsLoading(false)
      return
    }

    api
      .get<Order>(`/api/orders/${orderRef}`)
      .then(setOrder)
      .catch((err: Error) => setError(err.message))
      .finally(() => setIsLoading(false))
  }, [orderRef, passedOrder, isAuthenticated])

  if (isLoading) return <Spinner label="Loading your order…" />

  if (error || !order) {
    return (
      <div className="mx-auto max-w-2xl px-4 py-16">
        <ErrorNotice message={error ?? 'That order could not be found.'} />
        <Link to="/" className="mt-4 inline-block font-medium text-moss-600">
          ← Back to the shop
        </Link>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-3xl px-4 py-12">
      <div className="rounded-xl border border-moss-500/25 bg-moss-50 p-6">
        <div className="flex items-center gap-3">
          <span className="flex h-10 w-10 items-center justify-center rounded-full bg-moss-600 text-lg text-white">
            ✓
          </span>
          <div>
            <h1 className="text-xl font-semibold text-ink-900">Order confirmed</h1>
            <p className="text-sm text-ink-600">
              A confirmation would be sent to {order.customerEmail}.
            </p>
          </div>
        </div>
      </div>

      <div className="mt-6 rounded-xl border border-ink-100 bg-white p-6">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <p className="text-xs uppercase tracking-wide text-ink-400">Order reference</p>
            <p className="font-mono text-sm text-ink-900">{order.orderRef}</p>
          </div>
          <Badge tone="good">{order.status}</Badge>
        </div>

        <ul className="mt-6 divide-y divide-ink-100">
          {order.lines.map((line) => (
            <li key={line.itemId} className="flex items-center gap-4 py-4">
              <img
                src={line.imageUrl}
                alt=""
                className="h-16 w-20 rounded-lg bg-ink-50 object-contain"
              />
              <div className="flex-1">
                <p className="font-medium text-ink-900">{line.name}</p>
                <p className="text-sm text-ink-400">
                  {line.model} · Qty {line.quantity} · {formatPrice(line.priceAtPurchase)} each
                </p>
              </div>
              <span className="font-medium text-ink-900">{formatPrice(line.lineTotal)}</span>
            </li>
          ))}
        </ul>

        <dl className="mt-4 flex flex-col gap-2 border-t border-ink-100 pt-4 text-sm">
          <div className="flex justify-between">
            <dt className="text-ink-400">Shipping to</dt>
            <dd className="text-right text-ink-800">{order.shippingAddress}</dd>
          </div>
          <div className="flex justify-between">
            <dt className="text-ink-400">Paid with</dt>
            {/* All that was ever stored of the card. */}
            <dd className="text-ink-800">
              {order.cardBrand} ending in {order.cardLast4}
            </dd>
          </div>
          <div className="mt-2 flex justify-between border-t border-ink-100 pt-3">
            <dt className="font-medium text-ink-900">Total</dt>
            <dd className="text-lg font-semibold text-ink-900">
              {formatPrice(order.totalAmount)}
            </dd>
          </div>
        </dl>
      </div>

      <div className="mt-6 flex gap-4">
        <Link to="/" className="font-medium text-moss-600 hover:text-moss-700">
          ← Continue shopping
        </Link>
        {isAuthenticated && (
          <Link to="/orders" className="font-medium text-moss-600 hover:text-moss-700">
            View all orders
          </Link>
        )}
      </div>
    </div>
  )
}

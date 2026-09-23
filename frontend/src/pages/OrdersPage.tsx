import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, formatPrice } from '../api/client'
import type { Order } from '../api/types'
import { Badge, EmptyState, ErrorNotice, Spinner } from '../components/ui'

export default function OrdersPage() {
  const [orders, setOrders] = useState<Order[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api
      .get<Order[]>('/api/orders/me')
      .then(setOrders)
      .catch((err: Error) => setError(err.message))
      .finally(() => setIsLoading(false))
  }, [])

  if (isLoading) return <Spinner label="Loading your orders…" />

  return (
    <div className="mx-auto max-w-3xl px-4 py-10">
      <h1 className="text-2xl font-semibold tracking-tight text-ink-900">Your orders</h1>

      {error && (
        <div className="mt-4">
          <ErrorNotice message={error} />
        </div>
      )}

      {!error && orders.length === 0 && (
        <div className="mt-6">
          <EmptyState title="No orders yet" body="Your completed orders will appear here." />
          <div className="mt-4 text-center">
            <Link to="/" className="font-medium text-moss-600">
              ← Start shopping
            </Link>
          </div>
        </div>
      )}

      <div className="mt-6 flex flex-col gap-4">
        {orders.map((order) => (
          <article key={order.orderRef} className="rounded-xl border border-ink-100 bg-white p-5">
            <div className="flex flex-wrap items-start justify-between gap-3 border-b border-ink-100 pb-4">
              <div>
                <p className="text-xs uppercase tracking-wide text-ink-400">Order</p>
                <Link
                  to={`/orders/${order.orderRef}`}
                  className="font-mono text-sm text-ink-900 hover:text-moss-700"
                >
                  {order.orderRef.slice(0, 8)}…
                </Link>
                <p className="mt-1 text-sm text-ink-400">
                  {new Date(order.placedAt).toLocaleDateString('en-CA', {
                    year: 'numeric',
                    month: 'long',
                    day: 'numeric',
                  })}
                </p>
              </div>

              <div className="text-right">
                <Badge tone={order.status === 'PAID' ? 'good' : 'warn'}>{order.status}</Badge>
                <p className="mt-1 text-lg font-semibold text-ink-900">
                  {formatPrice(order.totalAmount)}
                </p>
                <p className="text-xs text-ink-400">
                  {order.cardBrand} ···· {order.cardLast4}
                </p>
              </div>
            </div>

            <ul className="mt-4 flex flex-col gap-3">
              {order.lines.map((line) => (
                <li key={line.itemId} className="flex items-center gap-3">
                  <img
                    src={line.imageUrl}
                    alt=""
                    className="h-12 w-16 rounded bg-ink-50 object-contain"
                  />
                  <div className="flex-1 text-sm">
                    <Link
                      to={`/items/${line.itemId}`}
                      className="font-medium text-ink-800 hover:text-moss-700"
                    >
                      {line.name}
                    </Link>
                    <p className="text-ink-400">
                      Qty {line.quantity} · {formatPrice(line.priceAtPurchase)} each
                    </p>
                  </div>
                  <span className="text-sm text-ink-800">{formatPrice(line.lineTotal)}</span>
                </li>
              ))}
            </ul>
          </article>
        ))}
      </div>
    </div>
  )
}

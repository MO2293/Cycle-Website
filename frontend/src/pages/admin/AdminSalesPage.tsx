import { useEffect, useState } from 'react'
import { api, formatPrice } from '../../api/client'
import type { Order, PageResponse } from '../../api/types'
import { Badge, EmptyState, ErrorNotice, Spinner, inputClass } from '../../components/ui'

export default function AdminSalesPage() {
  const [orders, setOrders] = useState<Order[]>([])
  const [emailFilter, setEmailFilter] = useState('')
  const [submittedFilter, setSubmittedFilter] = useState('')
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    setIsLoading(true)
    const query = submittedFilter ? `?email=${encodeURIComponent(submittedFilter)}&size=100` : '?size=100'
    api
      .get<PageResponse<Order>>(`/api/admin/orders${query}`)
      .then((page) => setOrders(page.content))
      .catch((err: Error) => setError(err.message))
      .finally(() => setIsLoading(false))
  }, [submittedFilter])

  const total = orders.reduce((sum, order) => sum + order.totalAmount, 0)

  return (
    <div className="flex flex-col gap-5">
      <form
        onSubmit={(event) => {
          event.preventDefault()
          setSubmittedFilter(emailFilter.trim())
        }}
        className="flex flex-wrap items-end gap-3"
      >
        <label className="flex flex-1 flex-col gap-1.5">
          <span className="text-sm font-medium text-ink-600">Filter by customer email</span>
          <input
            value={emailFilter}
            onChange={(event) => setEmailFilter(event.target.value)}
            placeholder="customer@example.com"
            className={inputClass}
          />
        </label>
        <button
          type="submit"
          className="rounded-lg bg-moss-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-moss-700"
        >
          Search
        </button>
        {submittedFilter && (
          <button
            type="button"
            onClick={() => {
              setEmailFilter('')
              setSubmittedFilter('')
            }}
            className="rounded-lg border border-ink-200 bg-white px-4 py-2.5 text-sm text-ink-800"
          >
            Clear
          </button>
        )}
        <button
          type="button"
          onClick={() => window.print()}
          className="rounded-lg border border-ink-200 bg-white px-4 py-2.5 text-sm text-ink-800"
        >
          Print / save PDF
        </button>
      </form>

      {error && <ErrorNotice message={error} />}

      {isLoading ? (
        <Spinner label="Loading sales…" />
      ) : orders.length === 0 ? (
        <EmptyState
          title="No sales found"
          body={submittedFilter ? 'No orders match that email.' : 'No orders have been placed yet.'}
        />
      ) : (
        <>
          <p className="text-sm text-ink-400">
            {orders.length} order{orders.length === 1 ? '' : 's'} · {formatPrice(total)} total
          </p>

          <div className="overflow-x-auto rounded-xl border border-ink-100 bg-white">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-ink-100 text-xs uppercase tracking-wide text-ink-400">
                <tr>
                  <th className="px-4 py-3 font-medium">Order</th>
                  <th className="px-4 py-3 font-medium">Customer</th>
                  <th className="px-4 py-3 font-medium">Items</th>
                  <th className="px-4 py-3 font-medium">Placed</th>
                  <th className="px-4 py-3 font-medium">Payment</th>
                  <th className="px-4 py-3 text-right font-medium">Total</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-ink-100">
                {orders.map((order) => (
                  <tr key={order.orderRef}>
                    <td className="px-4 py-3">
                      <span className="font-mono text-xs text-ink-600">
                        {order.orderRef.slice(0, 8)}…
                      </span>
                      <div className="mt-1">
                        <Badge tone={order.status === 'PAID' ? 'good' : 'warn'}>
                          {order.status}
                        </Badge>
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <p className="text-ink-800">{order.customerEmail}</p>
                      <p className="text-xs text-ink-400">{order.shippingAddress}</p>
                    </td>
                    <td className="px-4 py-3 text-ink-600">
                      {order.lines.map((line) => (
                        <div key={line.itemId} className="whitespace-nowrap">
                          {line.quantity} × {line.name}
                        </div>
                      ))}
                    </td>
                    <td className="px-4 py-3 whitespace-nowrap text-ink-600">
                      {new Date(order.placedAt).toLocaleDateString('en-CA')}
                    </td>
                    <td className="px-4 py-3 whitespace-nowrap text-ink-600">
                      {/* All that was ever stored. */}
                      {order.cardBrand} ···· {order.cardLast4}
                    </td>
                    <td className="px-4 py-3 text-right font-medium tabular-nums text-ink-900">
                      {formatPrice(order.totalAmount)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </div>
  )
}

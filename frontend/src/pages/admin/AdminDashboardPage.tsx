import { useEffect, useState } from 'react'
import { api, formatPrice } from '../../api/client'
import type { Item, Order, PageResponse } from '../../api/types'
import { CATEGORY_LABELS } from '../../api/types'
import { ErrorNotice, Spinner } from '../../components/ui'
import RevenueByCategoryChart from './RevenueByCategoryChart'

interface SalesSummary {
  totalRevenue: number
  paidOrderCount: number
}

/**
 * A stat tile: one number, its label, and optional context.
 *
 * <p>Four headline numbers are a KPI row, not a chart — a bar chart of four
 * unrelated measures (dollars, counts, units) would put different units on one
 * scale, which is meaningless.
 */
function StatTile({ label, value, context }: { label: string; value: string; context?: string }) {
  return (
    <div className="rounded-xl border border-ink-100 bg-white p-5">
      <p className="text-sm text-ink-400">{label}</p>
      <p className="mt-1 text-3xl font-semibold text-ink-900">{value}</p>
      {context && <p className="mt-1 text-xs text-ink-400">{context}</p>}
    </div>
  )
}

export default function AdminDashboardPage() {
  const [summary, setSummary] = useState<SalesSummary | null>(null)
  const [orders, setOrders] = useState<Order[]>([])
  const [items, setItems] = useState<Item[]>([])
  const [customerCount, setCustomerCount] = useState(0)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // allSettled, not all: `Promise.all` rejects as soon as any one call fails,
  // which would blank the whole dashboard because a single panel could not load.
  // Each result is applied independently and the failures are named, so four
  // working panels survive one broken endpoint.
  useEffect(() => {
    Promise.allSettled([
      api.get<SalesSummary>('/api/admin/orders/summary'),
      api.get<PageResponse<Order>>('/api/admin/orders?size=100'),
      api.get<PageResponse<Item>>('/api/items?size=100'),
      api.get<PageResponse<unknown>>('/api/admin/users?size=1'),
    ])
      .then(([summaryResult, ordersResult, itemsResult, usersResult]) => {
        const failed: string[] = []

        if (summaryResult.status === 'fulfilled') setSummary(summaryResult.value)
        else failed.push('sales summary')

        if (ordersResult.status === 'fulfilled') setOrders(ordersResult.value.content)
        else failed.push('orders')

        if (itemsResult.status === 'fulfilled') setItems(itemsResult.value.content)
        else failed.push('catalogue')

        if (usersResult.status === 'fulfilled') setCustomerCount(usersResult.value.totalElements)
        else failed.push('customers')

        if (failed.length > 0) {
          setError(`Could not load: ${failed.join(', ')}. Everything else is current.`)
        }
      })
      .finally(() => setIsLoading(false))
  }, [])

  if (isLoading) return <Spinner label="Loading dashboard…" />

  // Revenue per category, derived by joining order lines back to the catalogue.
  // Uses priceAtPurchase, so past orders keep the price they were actually sold
  // at rather than being re-valued at today's price.
  const categoryById = new Map(items.map((item) => [item.id, item.category]))
  const revenueByCategory = new Map<string, number>()

  for (const order of orders) {
    for (const line of order.lines) {
      const category = categoryById.get(line.itemId)
      if (!category) continue
      const label = CATEGORY_LABELS[category]
      revenueByCategory.set(label, (revenueByCategory.get(label) ?? 0) + line.lineTotal)
    }
  }

  const lowStock = items.filter((item) => item.quantity > 0 && item.quantity <= 3)
  const outOfStock = items.filter((item) => item.quantity === 0)

  return (
    <div className="flex flex-col gap-6">
      {/* A partial failure is a notice above live data, not a replacement for it. */}
      {error && <ErrorNotice message={error} />}

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatTile
          label="Revenue"
          value={formatPrice(summary?.totalRevenue ?? 0)}
          context="All paid orders"
        />
        <StatTile
          label="Orders"
          value={String(summary?.paidOrderCount ?? 0)}
          context="Paid"
        />
        <StatTile label="Products" value={String(items.length)} context="In the catalogue" />
        <StatTile label="Customers" value={String(customerCount)} context="Registered accounts" />
      </div>

      <section className="rounded-xl border border-ink-100 bg-white p-6">
        <h2 className="font-semibold text-ink-900">Revenue by category</h2>
        <p className="mb-5 mt-1 text-sm text-ink-400">
          Based on the price each item sold for at the time of purchase.
        </p>
        <RevenueByCategoryChart
          data={[...revenueByCategory.entries()].map(([category, revenue]) => ({
            category,
            revenue,
          }))}
        />
      </section>

      {(lowStock.length > 0 || outOfStock.length > 0) && (
        <section className="rounded-xl border border-ink-100 bg-white p-6">
          <h2 className="font-semibold text-ink-900">Stock needing attention</h2>

          <ul className="mt-4 divide-y divide-ink-100">
            {outOfStock.map((item) => (
              <li key={item.id} className="flex items-center justify-between py-2.5 text-sm">
                <span className="text-ink-800">{item.name}</span>
                {/* Status colour ships with a label, never colour alone. */}
                <span className="font-medium text-clay-600">Out of stock</span>
              </li>
            ))}
            {lowStock.map((item) => (
              <li key={item.id} className="flex items-center justify-between py-2.5 text-sm">
                <span className="text-ink-800">{item.name}</span>
                <span className="text-ink-600">{item.quantity} left</span>
              </li>
            ))}
          </ul>
        </section>
      )}
    </div>
  )
}

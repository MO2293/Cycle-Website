import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api, formatPrice } from '../api/client'
import type { Item } from '../api/types'
import { CATEGORY_LABELS } from '../api/types'
import { Badge, ErrorNotice, Spinner } from '../components/ui'

export default function ProductDetailPage() {
  const { id } = useParams<{ id: string }>()
  const [item, setItem] = useState<Item | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    setIsLoading(true)
    api
      .get<Item>(`/api/items/${id}`, false)
      .then(setItem)
      .catch((err: Error) => setError(err.message))
      .finally(() => setIsLoading(false))
  }, [id])

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

  return (
    <div className="mx-auto max-w-5xl px-4 py-10">
      <Link to="/" className="text-sm font-medium text-moss-600 hover:text-moss-700">
        ← Back to the shop
      </Link>

      <div className="mt-6 grid gap-10 md:grid-cols-2">
        <div className="overflow-hidden rounded-xl border border-ink-100 bg-white">
          <img src={item.imageUrl} alt={item.name} className="h-full w-full object-contain" />
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

          <dl className="mt-2 grid grid-cols-2 gap-y-3 border-t border-ink-100 pt-5 text-sm">
            <dt className="text-ink-400">Model</dt>
            <dd className="text-ink-800">{item.model}</dd>
            <dt className="text-ink-400">Colour</dt>
            <dd className="text-ink-800">{item.colour}</dd>
            <dt className="text-ink-400">Category</dt>
            <dd className="text-ink-800">{CATEGORY_LABELS[item.category]}</dd>
          </dl>

          {/* The add-to-cart control arrives with the cart in the next commit. */}
          <p className="mt-4 rounded-lg border border-dashed border-ink-200 px-4 py-3 text-sm text-ink-400">
            Cart coming in the next stage.
          </p>
        </div>
      </div>
    </div>
  )
}

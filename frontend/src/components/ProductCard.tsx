import { Link } from 'react-router-dom'
import { formatPrice, mediaUrl } from '../api/client'
import type { Item } from '../api/types'
import { CATEGORY_LABELS } from '../api/types'
import { Badge } from './ui'

export default function ProductCard({ item }: { item: Item }) {
  return (
    <Link
      to={`/items/${item.id}`}
      className="group flex flex-col overflow-hidden rounded-xl border border-ink-100 bg-white transition-shadow hover:shadow-lg"
    >
      <div className="aspect-4/3 overflow-hidden bg-ink-50">
        <img
          src={mediaUrl(item.imageUrl)}
          alt={item.name}
          /* Lazy loading means a long catalogue does not fetch every image
             before the page becomes usable. */
          loading="lazy"
          className="h-full w-full object-contain transition-transform duration-300 group-hover:scale-105"
        />
      </div>

      <div className="flex flex-1 flex-col gap-2 p-4">
        <div className="flex items-start justify-between gap-2">
          <h3 className="font-medium text-ink-900">{item.name}</h3>
          <Badge>{CATEGORY_LABELS[item.category]}</Badge>
        </div>

        <p className="text-sm text-ink-400">
          {item.model} · {item.colour}
        </p>

        <div className="mt-auto flex items-center justify-between pt-2">
          <span className="text-lg font-semibold text-ink-900">{formatPrice(item.price)}</span>
          {item.inStock ? (
            <Badge tone="good">{item.quantity} in stock</Badge>
          ) : (
            <Badge tone="warn">Out of stock</Badge>
          )}
        </div>
      </div>
    </Link>
  )
}

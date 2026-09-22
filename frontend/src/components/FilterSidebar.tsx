import type { Category } from '../api/types'
import { CATEGORY_LABELS } from '../api/types'

const CATEGORIES = Object.keys(CATEGORY_LABELS) as Category[]

interface Props {
  categories: Category[]
  colours: string[]
  availableColours: string[]
  inStockOnly: boolean
  onToggleCategory: (category: Category) => void
  onToggleColour: (colour: string) => void
  onToggleInStock: (value: boolean) => void
  onClear: () => void
}

export default function FilterSidebar({
  categories,
  colours,
  availableColours,
  inStockOnly,
  onToggleCategory,
  onToggleColour,
  onToggleInStock,
  onClear,
}: Props) {
  const hasFilters = categories.length > 0 || colours.length > 0 || inStockOnly

  return (
    <aside className="flex flex-col gap-6 rounded-xl border border-ink-100 bg-white p-5">
      <div className="flex items-center justify-between">
        <h2 className="font-semibold text-ink-900">Filter</h2>
        {hasFilters && (
          <button
            type="button"
            onClick={onClear}
            className="text-xs font-medium text-moss-600 hover:text-moss-700"
          >
            Clear all
          </button>
        )}
      </div>

      <fieldset className="flex flex-col gap-2">
        <legend className="mb-2 text-sm font-medium text-ink-600">Category</legend>
        {CATEGORIES.map((category) => (
          <label key={category} className="flex cursor-pointer items-center gap-2.5 text-sm">
            <input
              type="checkbox"
              checked={categories.includes(category)}
              onChange={() => onToggleCategory(category)}
              className="h-4 w-4 rounded border-ink-200 text-moss-600 focus:ring-moss-500"
            />
            <span className="text-ink-800">{CATEGORY_LABELS[category]}</span>
          </label>
        ))}
      </fieldset>

      {availableColours.length > 0 && (
        <fieldset className="flex flex-col gap-2">
          <legend className="mb-2 text-sm font-medium text-ink-600">Colour</legend>
          {availableColours.map((colour) => (
            <label key={colour} className="flex cursor-pointer items-center gap-2.5 text-sm">
              <input
                type="checkbox"
                checked={colours.includes(colour)}
                onChange={() => onToggleColour(colour)}
                className="h-4 w-4 rounded border-ink-200 text-moss-600 focus:ring-moss-500"
              />
              <span className="text-ink-800">{colour}</span>
            </label>
          ))}
        </fieldset>
      )}

      <label className="flex cursor-pointer items-center gap-2.5 border-t border-ink-100 pt-4 text-sm">
        <input
          type="checkbox"
          checked={inStockOnly}
          onChange={(event) => onToggleInStock(event.target.checked)}
          className="h-4 w-4 rounded border-ink-200 text-moss-600 focus:ring-moss-500"
        />
        <span className="text-ink-800">In stock only</span>
      </label>
    </aside>
  )
}

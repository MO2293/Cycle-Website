import { useCallback, useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { api } from '../api/client'
import type { Category, Item, PageResponse } from '../api/types'
import { SORT_OPTIONS } from '../api/types'
import FilterSidebar from '../components/FilterSidebar'
import Pagination from '../components/Pagination'
import ProductCard from '../components/ProductCard'
import { EmptyState, ErrorNotice, Spinner, inputClass } from '../components/ui'

const PAGE_SIZE = 9

export default function ShopPage() {
  // Filters live in the URL rather than component state, so a filtered view can
  // be bookmarked, shared, and survives the back button — none of which worked
  // in the original, where filters were POSTed into a JSP.
  const [searchParams, setSearchParams] = useSearchParams()

  const [data, setData] = useState<PageResponse<Item> | null>(null)
  const [availableColours, setAvailableColours] = useState<string[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [searchInput, setSearchInput] = useState(searchParams.get('search') ?? '')

  const categories = searchParams.getAll('category') as Category[]
  const colours = searchParams.getAll('colour')
  const search = searchParams.get('search') ?? ''
  const sort = searchParams.get('sort') ?? 'name_asc'
  const inStockOnly = searchParams.get('inStockOnly') === 'true'
  const page = Number(searchParams.get('page') ?? '0')

  useEffect(() => {
    api
      .get<string[]>('/api/items/colours', false)
      .then(setAvailableColours)
      .catch(() => setAvailableColours([]))
  }, [])

  const load = useCallback(() => {
    setIsLoading(true)
    setError(null)

    const query = new URLSearchParams()
    categories.forEach((category) => query.append('category', category))
    colours.forEach((colour) => query.append('colour', colour))
    if (search) query.set('search', search)
    if (sort) query.set('sort', sort)
    if (inStockOnly) query.set('inStockOnly', 'true')
    query.set('page', String(page))
    query.set('size', String(PAGE_SIZE))

    api
      .get<PageResponse<Item>>(`/api/items?${query.toString()}`, false)
      .then(setData)
      .catch((err: Error) => setError(err.message))
      .finally(() => setIsLoading(false))
    // searchParams is the single source of truth; re-run whenever it changes.
  }, [searchParams.toString()])

  useEffect(load, [load])

  const update = (mutate: (params: URLSearchParams) => void) => {
    const next = new URLSearchParams(searchParams)
    mutate(next)
    // Any filter change resets to the first page — staying on page 4 of a
    // result set that now has 2 pages would show an empty grid.
    next.set('page', '0')
    setSearchParams(next)
  }

  const toggleMulti = (key: string, value: string) =>
    update((params) => {
      const current = params.getAll(key)
      params.delete(key)
      const next = current.includes(value)
        ? current.filter((entry) => entry !== value)
        : [...current, value]
      next.forEach((entry) => params.append(key, entry))
    })

  const submitSearch = (event: React.FormEvent) => {
    event.preventDefault()
    update((params) => {
      if (searchInput.trim()) {
        params.set('search', searchInput.trim())
      } else {
        params.delete('search')
      }
    })
  }

  return (
    <>
      <section className="border-b border-ink-100 bg-white">
        <div className="mx-auto max-w-6xl px-4 py-14">
          <h1 className="max-w-2xl text-4xl font-semibold tracking-tight text-ink-900">
            Bikes built for the way you actually ride
          </h1>
          <p className="mt-3 max-w-xl text-ink-600">
            Road, mountain, electric and everything between — chosen for people who put real
            distance on them.
          </p>

          <form onSubmit={submitSearch} className="mt-7 flex max-w-lg gap-2">
            <input
              type="search"
              value={searchInput}
              onChange={(event) => setSearchInput(event.target.value)}
              placeholder="Search bikes, models, descriptions…"
              aria-label="Search the catalogue"
              className={inputClass}
            />
            <button
              type="submit"
              className="rounded-lg bg-moss-600 px-5 py-2 text-sm font-medium text-white transition-colors hover:bg-moss-700"
            >
              Search
            </button>
          </form>
        </div>
      </section>

      <div className="mx-auto grid max-w-6xl gap-8 px-4 py-10 lg:grid-cols-[250px_1fr]">
        <FilterSidebar
          categories={categories}
          colours={colours}
          availableColours={availableColours}
          inStockOnly={inStockOnly}
          onToggleCategory={(category) => toggleMulti('category', category)}
          onToggleColour={(colour) => toggleMulti('colour', colour)}
          onToggleInStock={(value) =>
            update((params) => {
              if (value) params.set('inStockOnly', 'true')
              else params.delete('inStockOnly')
            })
          }
          onClear={() => setSearchParams(new URLSearchParams())}
        />

        <section>
          <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
            <p className="text-sm text-ink-400">
              {data ? `${data.totalElements} bike${data.totalElements === 1 ? '' : 's'}` : ' '}
              {search && data ? ` matching “${search}”` : ''}
            </p>

            <label className="flex items-center gap-2 text-sm text-ink-600">
              Sort
              <select
                value={sort}
                onChange={(event) =>
                  update((params) => params.set('sort', event.target.value))
                }
                className="rounded-lg border border-ink-200 bg-white px-3 py-1.5 text-sm"
              >
                {SORT_OPTIONS.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>
          </div>

          {isLoading && <Spinner label="Loading bikes…" />}

          {error && !isLoading && <ErrorNotice message={error} onRetry={load} />}

          {!isLoading && !error && data && data.content.length === 0 && (
            <EmptyState
              title="No bikes match those filters"
              body="Try removing a filter or searching for something broader."
            />
          )}

          {!isLoading && !error && data && data.content.length > 0 && (
            <>
              <div className="grid gap-5 sm:grid-cols-2 xl:grid-cols-3">
                {data.content.map((item) => (
                  <ProductCard key={item.id} item={item} />
                ))}
              </div>

              <Pagination
                page={data.page}
                totalPages={data.totalPages}
                onChange={(next) => {
                  const params = new URLSearchParams(searchParams)
                  params.set('page', String(next))
                  setSearchParams(params)
                  window.scrollTo({ top: 0, behavior: 'smooth' })
                }}
              />
            </>
          )}
        </section>
      </div>
    </>
  )
}

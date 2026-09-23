import { useCallback, useEffect, useState } from 'react'
import { ApiError, api, formatPrice, uploadFile } from '../../api/client'
import type { Category, Item, PageResponse } from '../../api/types'
import { CATEGORY_LABELS } from '../../api/types'
import {
  Badge,
  ErrorNotice,
  Field,
  Spinner,
  buttonClass,
  inputClass,
  secondaryButtonClass,
} from '../../components/ui'

const CATEGORIES = Object.keys(CATEGORY_LABELS) as Category[]

const EMPTY_FORM = {
  name: '',
  category: 'ROAD' as Category,
  description: '',
  model: '',
  price: '',
  colour: '',
  quantity: '',
}

export default function AdminItemsPage() {
  const [items, setItems] = useState<Item[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [editingId, setEditingId] = useState<number | null>(null)
  const [isFormOpen, setIsFormOpen] = useState(false)
  const [form, setForm] = useState(EMPTY_FORM)
  const [imageFile, setImageFile] = useState<File | null>(null)
  const [isSaving, setIsSaving] = useState(false)

  const load = useCallback(() => {
    setIsLoading(true)
    api
      .get<PageResponse<Item>>('/api/items?size=100&sort=name_asc', false)
      .then((page) => setItems(page.content))
      .catch((err: Error) => setError(err.message))
      .finally(() => setIsLoading(false))
  }, [])

  useEffect(load, [load])

  const openCreate = () => {
    setForm(EMPTY_FORM)
    setEditingId(null)
    setImageFile(null)
    setFieldErrors({})
    setIsFormOpen(true)
  }

  const openEdit = (item: Item) => {
    setForm({
      name: item.name,
      category: item.category,
      description: item.description,
      model: item.model,
      price: String(item.price),
      colour: item.colour,
      quantity: String(item.quantity),
    })
    setEditingId(item.id)
    setImageFile(null)
    setFieldErrors({})
    setIsFormOpen(true)
  }

  const save = async (event: React.FormEvent) => {
    event.preventDefault()
    setError(null)
    setFieldErrors({})
    setIsSaving(true)

    const payload = {
      name: form.name,
      category: form.category,
      description: form.description,
      model: form.model,
      price: Number(form.price),
      colour: form.colour,
      quantity: Number(form.quantity),
    }

    try {
      const saved = editingId
        ? await api.put<Item>(`/api/items/${editingId}`, payload)
        : await api.post<Item>('/api/items', payload)

      // The image is a separate request: product details stay plain JSON, so
      // changing a price never requires building a multipart body.
      if (imageFile) {
        await uploadFile<Item>(`/api/items/${saved.id}/image`, imageFile)
      }

      setIsFormOpen(false)
      load()
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message)
        if (err.fieldErrors) setFieldErrors(err.fieldErrors)
      } else {
        setError('Could not save the product.')
      }
    } finally {
      setIsSaving(false)
    }
  }

  const remove = async (item: Item) => {
    if (!window.confirm(`Delete “${item.name}”? This cannot be undone.`)) return
    setError(null)
    try {
      await api.delete<void>(`/api/items/${item.id}`)
      load()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not delete that product.')
    }
  }

  const set = (key: keyof typeof form) => (
    event: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>,
  ) => setForm((previous) => ({ ...previous, [key]: event.target.value }))

  if (isLoading) return <Spinner label="Loading products…" />

  return (
    <div className="flex flex-col gap-5">
      <div className="flex items-center justify-between">
        <p className="text-sm text-ink-400">{items.length} products</p>
        <button type="button" onClick={openCreate} className={buttonClass}>
          Add product
        </button>
      </div>

      {error && <ErrorNotice message={error} />}

      {isFormOpen && (
        <form onSubmit={save} className="flex flex-col gap-4 rounded-xl border border-ink-100 bg-white p-6">
          <h2 className="font-semibold text-ink-900">
            {editingId ? 'Edit product' : 'New product'}
          </h2>

          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Name" htmlFor="name" error={fieldErrors.name}>
              <input id="name" required value={form.name} onChange={set('name')} className={inputClass} />
            </Field>

            <Field label="Model" htmlFor="model" error={fieldErrors.model}>
              <input id="model" required value={form.model} onChange={set('model')} className={inputClass} />
            </Field>
          </div>

          <Field label="Description" htmlFor="description" error={fieldErrors.description}>
            <textarea
              id="description"
              required
              rows={3}
              value={form.description}
              onChange={set('description')}
              className={inputClass}
            />
          </Field>

          <div className="grid gap-4 sm:grid-cols-4">
            <Field label="Category" htmlFor="category" error={fieldErrors.category}>
              {/* A dropdown of the enum's values — the API rejects anything else,
                  which is why the legacy data's 'dsads' category cannot recur. */}
              <select id="category" value={form.category} onChange={set('category')} className={inputClass}>
                {CATEGORIES.map((category) => (
                  <option key={category} value={category}>
                    {CATEGORY_LABELS[category]}
                  </option>
                ))}
              </select>
            </Field>

            <Field label="Colour" htmlFor="colour" error={fieldErrors.colour}>
              <input id="colour" required value={form.colour} onChange={set('colour')} className={inputClass} />
            </Field>

            <Field label="Price (CAD)" htmlFor="price" error={fieldErrors.price}>
              <input
                id="price"
                required
                type="number"
                step="0.01"
                min="0.01"
                value={form.price}
                onChange={set('price')}
                className={inputClass}
              />
            </Field>

            <Field label="Quantity" htmlFor="quantity" error={fieldErrors.quantity}>
              <input
                id="quantity"
                required
                type="number"
                min="0"
                value={form.quantity}
                onChange={set('quantity')}
                className={inputClass}
              />
            </Field>
          </div>

          <Field label="Image (optional)" htmlFor="image">
            <input
              id="image"
              type="file"
              accept="image/png,image/jpeg,image/webp,image/gif"
              onChange={(event) => setImageFile(event.target.files?.[0] ?? null)}
              className="text-sm text-ink-600 file:mr-3 file:rounded-lg file:border-0 file:bg-ink-100 file:px-3 file:py-2 file:text-sm file:font-medium"
            />
          </Field>

          <div className="flex gap-3">
            <button type="submit" disabled={isSaving} className={buttonClass}>
              {isSaving ? 'Saving…' : editingId ? 'Save changes' : 'Create product'}
            </button>
            <button type="button" onClick={() => setIsFormOpen(false)} className={secondaryButtonClass}>
              Cancel
            </button>
          </div>
        </form>
      )}

      <div className="overflow-x-auto rounded-xl border border-ink-100 bg-white">
        <table className="w-full text-left text-sm">
          <thead className="border-b border-ink-100 text-xs uppercase tracking-wide text-ink-400">
            <tr>
              <th className="px-4 py-3 font-medium">Product</th>
              <th className="px-4 py-3 font-medium">Category</th>
              <th className="px-4 py-3 font-medium">Price</th>
              <th className="px-4 py-3 font-medium">Stock</th>
              <th className="px-4 py-3" />
            </tr>
          </thead>
          <tbody className="divide-y divide-ink-100">
            {items.map((item) => (
              <tr key={item.id}>
                <td className="px-4 py-3">
                  <div className="flex items-center gap-3">
                    <img src={item.imageUrl} alt="" className="h-10 w-14 rounded bg-ink-50 object-contain" />
                    <div>
                      <p className="font-medium text-ink-900">{item.name}</p>
                      <p className="text-xs text-ink-400">
                        {item.model} · {item.colour}
                      </p>
                    </div>
                  </div>
                </td>
                <td className="px-4 py-3 text-ink-600">{CATEGORY_LABELS[item.category]}</td>
                <td className="px-4 py-3 tabular-nums text-ink-800">{formatPrice(item.price)}</td>
                <td className="px-4 py-3">
                  {item.quantity === 0 ? (
                    <Badge tone="warn">Out of stock</Badge>
                  ) : (
                    <span className="tabular-nums text-ink-800">{item.quantity}</span>
                  )}
                </td>
                <td className="px-4 py-3 text-right whitespace-nowrap">
                  <button
                    type="button"
                    onClick={() => openEdit(item)}
                    className="mr-3 font-medium text-moss-600 hover:text-moss-700"
                  >
                    Edit
                  </button>
                  <button
                    type="button"
                    onClick={() => remove(item)}
                    className="font-medium text-ink-400 hover:text-clay-600"
                  >
                    Delete
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}

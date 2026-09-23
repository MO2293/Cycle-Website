import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ApiError, api, formatPrice } from '../api/client'
import type { Order } from '../api/types'
import { useAuth } from '../context/AuthContext'
import { useCart } from '../context/CartContext'
import {
  EmptyState,
  ErrorNotice,
  Field,
  buttonClass,
  inputClass,
} from '../components/ui'

/** Luhn, mirrored from the server purely for instant feedback while typing. */
function passesLuhn(cardNumber: string): boolean {
  const digits = cardNumber.replace(/\D/g, '')
  if (digits.length < 12 || digits.length > 19) return false

  let sum = 0
  let double = false
  for (let i = digits.length - 1; i >= 0; i--) {
    let digit = Number(digits[i])
    if (double) {
      digit *= 2
      if (digit > 9) digit -= 9
    }
    sum += digit
    double = !double
  }
  return sum % 10 === 0
}

export default function CheckoutPage() {
  const { isAuthenticated, user } = useAuth()
  const { lines, subtotal, guestLines, clear } = useCart()
  const navigate = useNavigate()

  const [form, setForm] = useState({
    email: '',
    shippingAddress: user?.address
      ? [user.address, user.city, user.province, user.country].filter(Boolean).join(', ')
      : '',
    number: '',
    cvv: '',
    expiryMonth: '',
    expiryYear: '',
  })
  const [error, setError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [isSubmitting, setIsSubmitting] = useState(false)

  const set = (key: keyof typeof form) => (event: React.ChangeEvent<HTMLInputElement>) =>
    setForm((previous) => ({ ...previous, [key]: event.target.value }))

  if (lines.length === 0) {
    return (
      <div className="mx-auto max-w-2xl px-4 py-16">
        <EmptyState title="Nothing to check out" body="Your cart is empty." />
        <div className="mt-4 text-center">
          <Link to="/" className="font-medium text-moss-600">
            ← Back to the shop
          </Link>
        </div>
      </div>
    )
  }

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault()
    setError(null)
    setFieldErrors({})

    // Local checks for fast feedback only. The server validates independently,
    // because anything checked in the browser can be bypassed.
    const localErrors: Record<string, string> = {}
    if (!passesLuhn(form.number)) localErrors.number = 'That card number is not valid'
    if (!/^\d{3,4}$/.test(form.cvv)) localErrors.cvv = 'CVV must be 3 or 4 digits'
    if (Object.keys(localErrors).length > 0) {
      setFieldErrors(localErrors)
      return
    }

    const card = {
      number: form.number,
      cvv: form.cvv,
      expiryMonth: Number(form.expiryMonth),
      expiryYear: Number(form.expiryYear),
    }

    setIsSubmitting(true)
    try {
      // Note what is not sent in either case: a price or a total. The server
      // computes those from its own data.
      const order = isAuthenticated
        ? await api.post<Order>('/api/orders', {
            shippingAddress: form.shippingAddress,
            card,
          })
        : await api.post<Order>(
            '/api/orders/guest',
            {
              email: form.email,
              shippingAddress: form.shippingAddress,
              items: guestLines,
              card,
            },
            false,
          )

      await clear()
      navigate(`/orders/${order.orderRef}`, { state: { order }, replace: true })
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message)
        if (err.fieldErrors) setFieldErrors(err.fieldErrors)
      } else {
        setError('Checkout failed. Please try again.')
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="mx-auto grid max-w-5xl gap-10 px-4 py-10 lg:grid-cols-[1fr_320px]">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight text-ink-900">Checkout</h1>

        <div className="mt-3 rounded-lg border border-moss-500/25 bg-moss-50 px-4 py-3 text-sm text-moss-700">
          <strong className="font-medium">Demonstration only.</strong> No payment is processed and
          no card is charged. Use <code className="font-mono">4242 4242 4242 4242</code> to
          succeed, or <code className="font-mono">4000 0000 0000 0002</code> to see a decline.
        </div>

        <form onSubmit={handleSubmit} className="mt-6 flex flex-col gap-5">
          {error && <ErrorNotice message={error} />}

          {!isAuthenticated && (
            <>
              <Field label="Email" htmlFor="email" error={fieldErrors.email}>
                <input
                  id="email"
                  type="email"
                  required
                  value={form.email}
                  onChange={set('email')}
                  placeholder="you@example.com"
                  className={inputClass}
                />
              </Field>
              <p className="-mt-3 text-xs text-ink-400">
                Checking out as a guest.{' '}
                <Link to="/login" className="font-medium text-moss-600">
                  Sign in
                </Link>{' '}
                to save this order to an account.
              </p>
            </>
          )}

          <Field
            label="Shipping address"
            htmlFor="shippingAddress"
            error={fieldErrors.shippingAddress}
          >
            <input
              id="shippingAddress"
              required
              value={form.shippingAddress}
              onChange={set('shippingAddress')}
              placeholder="123 Bloor St W, Toronto, ON"
              className={inputClass}
            />
          </Field>

          <fieldset className="flex flex-col gap-4 rounded-xl border border-ink-100 bg-white p-5">
            <legend className="px-2 text-sm font-medium text-ink-600">Payment</legend>

            <Field label="Card number" htmlFor="number" error={fieldErrors.number}>
              <input
                id="number"
                required
                inputMode="numeric"
                autoComplete="cc-number"
                value={form.number}
                onChange={set('number')}
                placeholder="4242 4242 4242 4242"
                className={inputClass}
              />
            </Field>

            <div className="grid grid-cols-3 gap-3">
              <Field label="Month" htmlFor="expiryMonth" error={fieldErrors.expiryMonth}>
                <input
                  id="expiryMonth"
                  required
                  inputMode="numeric"
                  value={form.expiryMonth}
                  onChange={set('expiryMonth')}
                  placeholder="12"
                  className={inputClass}
                />
              </Field>

              <Field label="Year" htmlFor="expiryYear" error={fieldErrors.expiryYear}>
                <input
                  id="expiryYear"
                  required
                  inputMode="numeric"
                  value={form.expiryYear}
                  onChange={set('expiryYear')}
                  placeholder="2030"
                  className={inputClass}
                />
              </Field>

              <Field label="CVV" htmlFor="cvv" error={fieldErrors.cvv}>
                <input
                  id="cvv"
                  required
                  inputMode="numeric"
                  autoComplete="cc-csc"
                  value={form.cvv}
                  onChange={set('cvv')}
                  placeholder="123"
                  className={inputClass}
                />
              </Field>
            </div>
          </fieldset>

          <button type="submit" disabled={isSubmitting} className={buttonClass}>
            {isSubmitting ? 'Placing order…' : `Place order · ${formatPrice(subtotal)}`}
          </button>
        </form>
      </div>

      <aside className="h-fit rounded-xl border border-ink-100 bg-white p-5">
        <h2 className="font-semibold text-ink-900">Order summary</h2>

        <ul className="mt-4 flex flex-col gap-3">
          {lines.map((line) => (
            <li key={line.itemId} className="flex items-center gap-3 text-sm">
              <img
                src={line.imageUrl}
                alt=""
                className="h-12 w-16 rounded bg-ink-50 object-contain"
              />
              <div className="flex-1">
                <p className="font-medium text-ink-800">{line.name}</p>
                <p className="text-ink-400">Qty {line.quantity}</p>
              </div>
              <span className="text-ink-800">{formatPrice(line.lineTotal)}</span>
            </li>
          ))}
        </ul>

        <div className="mt-5 flex items-center justify-between border-t border-ink-100 pt-4">
          <span className="text-sm text-ink-600">Subtotal</span>
          <span className="text-lg font-semibold text-ink-900">{formatPrice(subtotal)}</span>
        </div>
      </aside>
    </div>
  )
}

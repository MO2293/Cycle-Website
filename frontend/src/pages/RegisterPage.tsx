import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import { useAuth } from '../context/AuthContext'
import { ErrorNotice, Field, buttonClass, inputClass } from '../components/ui'

export default function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()

  const [form, setForm] = useState({
    name: '',
    email: '',
    password: '',
    confirmPassword: '',
    phone: '',
    address: '',
    city: '',
    province: '',
    country: 'Canada',
  })
  const [error, setError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [isSubmitting, setIsSubmitting] = useState(false)

  const set = (key: keyof typeof form) => (event: React.ChangeEvent<HTMLInputElement>) =>
    setForm((previous) => ({ ...previous, [key]: event.target.value }))

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault()
    setError(null)
    setFieldErrors({})

    // Checked here purely for a fast response; the server does its own
    // validation, because client-side checks can always be bypassed.
    if (form.password !== form.confirmPassword) {
      setFieldErrors({ confirmPassword: 'Passwords do not match' })
      return
    }

    setIsSubmitting(true)
    try {
      const { confirmPassword, ...payload } = form
      void confirmPassword
      await register(payload)
      navigate('/', { replace: true })
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message)
        // The API returns per-field messages, so each one can be shown against
        // the input it belongs to instead of as one generic banner.
        if (err.fieldErrors) setFieldErrors(err.fieldErrors)
      } else {
        setError('Could not create your account. Please try again.')
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="mx-auto max-w-xl px-4 py-16">
      <h1 className="text-2xl font-semibold tracking-tight text-ink-900">Create your account</h1>
      <p className="mt-1 text-sm text-ink-400">
        Save your details for faster checkout and keep your order history.
      </p>

      <form onSubmit={handleSubmit} className="mt-8 flex flex-col gap-4">
        {error && <ErrorNotice message={error} />}

        <Field label="Full name" htmlFor="name" error={fieldErrors.name}>
          <input id="name" required value={form.name} onChange={set('name')} className={inputClass} />
        </Field>

        <Field label="Email" htmlFor="email" error={fieldErrors.email}>
          <input
            id="email"
            type="email"
            required
            autoComplete="email"
            value={form.email}
            onChange={set('email')}
            className={inputClass}
          />
        </Field>

        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Password" htmlFor="password" error={fieldErrors.password}>
            <input
              id="password"
              type="password"
              required
              autoComplete="new-password"
              value={form.password}
              onChange={set('password')}
              className={inputClass}
            />
          </Field>

          <Field
            label="Confirm password"
            htmlFor="confirmPassword"
            error={fieldErrors.confirmPassword}
          >
            <input
              id="confirmPassword"
              type="password"
              required
              autoComplete="new-password"
              value={form.confirmPassword}
              onChange={set('confirmPassword')}
              className={inputClass}
            />
          </Field>
        </div>

        <p className="-mt-1 text-xs text-ink-400">
          At least 8 characters, including a letter and a number.
        </p>

        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Phone (optional)" htmlFor="phone" error={fieldErrors.phone}>
            <input id="phone" value={form.phone} onChange={set('phone')} className={inputClass} />
          </Field>

          <Field label="City (optional)" htmlFor="city">
            <input id="city" value={form.city} onChange={set('city')} className={inputClass} />
          </Field>
        </div>

        <Field label="Address (optional)" htmlFor="address">
          <input id="address" value={form.address} onChange={set('address')} className={inputClass} />
        </Field>

        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Province (optional)" htmlFor="province">
            <input
              id="province"
              value={form.province}
              onChange={set('province')}
              className={inputClass}
            />
          </Field>

          <Field label="Country (optional)" htmlFor="country">
            <input
              id="country"
              value={form.country}
              onChange={set('country')}
              className={inputClass}
            />
          </Field>
        </div>

        <button type="submit" disabled={isSubmitting} className={`${buttonClass} mt-2`}>
          {isSubmitting ? 'Creating account…' : 'Create account'}
        </button>
      </form>

      <p className="mt-6 text-sm text-ink-600">
        Already have an account?{' '}
        <Link to="/login" className="font-medium text-moss-600 hover:text-moss-700">
          Sign in
        </Link>
      </p>
    </div>
  )
}

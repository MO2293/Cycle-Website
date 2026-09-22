import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import { useAuth } from '../context/AuthContext'
import { ErrorNotice, Field, buttonClass, inputClass } from '../components/ui'

export default function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  // Where to go after signing in: back to whatever page sent us here, or home.
  const redirectTo = (location.state as { from?: string } | null)?.from ?? '/'

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault()
    setError(null)
    setIsSubmitting(true)

    try {
      await login(email, password)
      navigate(redirectTo, { replace: true })
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not sign in. Please try again.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="mx-auto max-w-md px-4 py-16">
      <h1 className="text-2xl font-semibold tracking-tight text-ink-900">Sign in</h1>
      <p className="mt-1 text-sm text-ink-400">Welcome back to Cycle Haven.</p>

      <form onSubmit={handleSubmit} className="mt-8 flex flex-col gap-4">
        {error && <ErrorNotice message={error} />}

        <Field label="Email" htmlFor="email">
          <input
            id="email"
            type="email"
            required
            autoComplete="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            className={inputClass}
          />
        </Field>

        <Field label="Password" htmlFor="password">
          <input
            id="password"
            type="password"
            required
            autoComplete="current-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            className={inputClass}
          />
        </Field>

        <button type="submit" disabled={isSubmitting} className={`${buttonClass} mt-2`}>
          {isSubmitting ? 'Signing in…' : 'Sign in'}
        </button>
      </form>

      <p className="mt-6 text-sm text-ink-600">
        No account yet?{' '}
        <Link to="/register" className="font-medium text-moss-600 hover:text-moss-700">
          Create one
        </Link>
      </p>
    </div>
  )
}

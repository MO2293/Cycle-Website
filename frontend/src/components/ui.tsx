import type { ReactNode } from 'react'

/** Small shared building blocks, kept together so pages stay readable. */

export function Spinner({ label = 'Loading…' }: { label?: string }) {
  return (
    <div className="flex items-center justify-center gap-3 py-16 text-ink-400" role="status">
      <span className="h-5 w-5 animate-spin rounded-full border-2 border-ink-200 border-t-moss-500" />
      <span className="text-sm">{label}</span>
    </div>
  )
}

export function ErrorNotice({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div className="rounded-lg border border-clay-500/30 bg-clay-100 px-4 py-3 text-sm text-clay-600">
      <p>{message}</p>
      {onRetry && (
        <button
          type="button"
          onClick={onRetry}
          className="mt-2 font-medium underline underline-offset-2"
        >
          Try again
        </button>
      )}
    </div>
  )
}

export function EmptyState({ title, body }: { title: string; body?: string }) {
  return (
    <div className="rounded-xl border border-dashed border-ink-200 bg-white px-6 py-16 text-center">
      <p className="font-medium text-ink-800">{title}</p>
      {body && <p className="mt-1 text-sm text-ink-400">{body}</p>}
    </div>
  )
}

export function Badge({ children, tone = 'neutral' }: { children: ReactNode; tone?: 'neutral' | 'good' | 'warn' }) {
  const tones = {
    neutral: 'bg-ink-100 text-ink-600',
    good: 'bg-moss-100 text-moss-700',
    warn: 'bg-clay-100 text-clay-600',
  }
  return (
    <span className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-medium ${tones[tone]}`}>
      {children}
    </span>
  )
}

export function Field({
  label,
  htmlFor,
  error,
  children,
}: {
  label: string
  htmlFor: string
  error?: string
  children: ReactNode
}) {
  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={htmlFor} className="text-sm font-medium text-ink-600">
        {label}
      </label>
      {children}
      {error && <p className="text-xs text-clay-600">{error}</p>}
    </div>
  )
}

export const inputClass =
  'w-full rounded-lg border border-ink-200 bg-white px-3 py-2 text-sm text-ink-900 ' +
  'placeholder:text-ink-400 focus:border-moss-500 focus:outline-none focus:ring-2 focus:ring-moss-500/20'

export const buttonClass =
  'inline-flex items-center justify-center rounded-lg bg-moss-600 px-4 py-2.5 text-sm font-medium ' +
  'text-white transition-colors hover:bg-moss-700 disabled:cursor-not-allowed disabled:opacity-50'

export const secondaryButtonClass =
  'inline-flex items-center justify-center rounded-lg border border-ink-200 bg-white px-4 py-2.5 ' +
  'text-sm font-medium text-ink-800 transition-colors hover:bg-ink-50 disabled:opacity-50'

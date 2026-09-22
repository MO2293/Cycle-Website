import { Link } from 'react-router-dom'

export default function NotFoundPage() {
  return (
    <div className="mx-auto max-w-md px-4 py-24 text-center">
      <p className="text-sm font-medium text-moss-600">404</p>
      <h1 className="mt-2 text-2xl font-semibold tracking-tight text-ink-900">Page not found</h1>
      <p className="mt-2 text-ink-600">That page does not exist, or it has moved.</p>
      <Link
        to="/"
        className="mt-6 inline-flex rounded-lg bg-moss-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-moss-700"
      >
        Back to the shop
      </Link>
    </div>
  )
}

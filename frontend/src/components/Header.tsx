import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

function BikeMark() {
  return (
    <svg viewBox="0 0 48 28" className="h-7 w-12" aria-hidden="true">
      <circle cx="10" cy="19" r="7.5" fill="none" stroke="currentColor" strokeWidth="2.2" />
      <circle cx="38" cy="19" r="7.5" fill="none" stroke="currentColor" strokeWidth="2.2" />
      <path
        d="M10 19 L20 19 L27 7 L34 19 M20 19 L27 7 M27 7 L23 7"
        fill="none"
        stroke="currentColor"
        strokeWidth="2.2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  )
}

export default function Header() {
  const { isAuthenticated, isAdmin, user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/')
  }

  const linkClass = ({ isActive }: { isActive: boolean }) =>
    [
      'rounded-md px-3 py-2 text-sm font-medium transition-colors',
      isActive ? 'bg-moss-100 text-moss-700' : 'text-ink-600 hover:bg-ink-100 hover:text-ink-900',
    ].join(' ')

  return (
    <header className="sticky top-0 z-40 border-b border-ink-100 bg-white/90 backdrop-blur">
      <div className="mx-auto flex max-w-6xl items-center gap-4 px-4 py-3">
        <Link
          to="/"
          className="flex items-center gap-2 text-moss-600 transition-colors hover:text-moss-700"
        >
          <BikeMark />
          <span className="text-lg font-semibold tracking-tight text-ink-900">Cycle Haven</span>
        </Link>

        <nav className="ml-auto flex items-center gap-1">
          <NavLink to="/" end className={linkClass}>
            Shop
          </NavLink>

          {isAuthenticated && (
            <NavLink to="/orders" className={linkClass}>
              Orders
            </NavLink>
          )}

          {isAdmin && (
            <NavLink to="/admin" className={linkClass}>
              Admin
            </NavLink>
          )}

          <NavLink to="/cart" className={linkClass}>
            Cart
          </NavLink>

          {isAuthenticated ? (
            <div className="ml-2 flex items-center gap-2 border-l border-ink-100 pl-3">
              <Link
                to="/profile"
                className="text-sm font-medium text-ink-600 hover:text-ink-900"
                title={user?.email}
              >
                {user?.name.split(' ')[0]}
              </Link>
              <button
                type="button"
                onClick={handleLogout}
                className="rounded-md px-2 py-1 text-sm text-ink-400 transition-colors hover:text-ink-800"
              >
                Sign out
              </button>
            </div>
          ) : (
            <div className="ml-2 flex items-center gap-2 border-l border-ink-100 pl-3">
              <Link to="/login" className="text-sm font-medium text-ink-600 hover:text-ink-900">
                Sign in
              </Link>
              <Link
                to="/register"
                className="rounded-md bg-moss-600 px-3 py-2 text-sm font-medium text-white transition-colors hover:bg-moss-700"
              >
                Create account
              </Link>
            </div>
          )}
        </nav>
      </div>
    </header>
  )
}

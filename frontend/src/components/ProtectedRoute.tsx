import { Navigate, useLocation } from 'react-router-dom'
import type { ReactNode } from 'react'
import { useAuth } from '../context/AuthContext'
import { Spinner } from './ui'

/**
 * Gates a route behind sign-in, and optionally behind the ADMIN role.
 *
 * <p>Important framing: this is **convenience, not security**. Anyone can edit
 * their own JavaScript, so a client-side check only decides what to render. The
 * real enforcement is in SecurityConfig — every admin endpoint returns 403
 * regardless of what the browser decides to show. Hiding a page that the API
 * still protects is good UX; hiding a page the API *doesn't* protect is exactly
 * the mistake the original made, where admin JSPs were reachable by URL.
 */
export default function ProtectedRoute({
  children,
  requireAdmin = false,
}: {
  children: ReactNode
  requireAdmin?: boolean
}) {
  const { isAuthenticated, isAdmin, isLoading } = useAuth()
  const location = useLocation()

  // Wait for the stored token to be checked, or a refresh on a protected page
  // would bounce a signed-in user to the login screen.
  if (isLoading) return <Spinner />

  if (!isAuthenticated) {
    // Remember where they were headed so login can send them back.
    return <Navigate to="/login" state={{ from: location.pathname }} replace />
  }

  if (requireAdmin && !isAdmin) {
    return <Navigate to="/" replace />
  }

  return <>{children}</>
}

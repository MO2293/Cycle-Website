import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { api, clearToken, getToken, setToken } from '../api/client'
import type { AuthResponse, User } from '../api/types'

/**
 * Who is signed in, available anywhere in the app.
 *
 * <p>React context rather than prop-drilling: the header, the cart and every
 * protected page all need this, and threading it through every component in
 * between would be unmaintainable.
 */

interface AuthContextValue {
  user: User | null
  isLoading: boolean
  isAuthenticated: boolean
  isAdmin: boolean
  login: (email: string, password: string) => Promise<User>
  register: (payload: RegisterPayload) => Promise<User>
  logout: () => void
}

export interface RegisterPayload {
  name: string
  email: string
  password: string
  phone?: string
  address?: string
  city?: string
  province?: string
  country?: string
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  // A token in localStorage survives a page refresh, but it may have expired
  // while the tab was closed. Asking the server who it belongs to is the only
  // way to know it is still good.
  useEffect(() => {
    const token = getToken()
    if (!token) {
      setIsLoading(false)
      return
    }

    api
      .get<User>('/api/auth/me')
      .then(setUser)
      .catch(() => {
        clearToken()
        setUser(null)
      })
      .finally(() => setIsLoading(false))
  }, [])

  const login = useCallback(async (email: string, password: string) => {
    const response = await api.post<AuthResponse>('/api/auth/login', { email, password }, false)
    setToken(response.token)
    setUser(response.user)
    return response.user
  }, [])

  const register = useCallback(async (payload: RegisterPayload) => {
    const response = await api.post<AuthResponse>('/api/auth/register', payload, false)
    setToken(response.token)
    setUser(response.user)
    return response.user
  }, [])

  const logout = useCallback(() => {
    // Logging out is purely client-side, which is the trade-off of stateless
    // tokens: there is no server session to destroy, so a token stays
    // technically valid until it expires. Short expiry limits that window.
    clearToken()
    setUser(null)
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isLoading,
      isAuthenticated: user !== null,
      isAdmin: user?.role === 'ADMIN',
      login,
      register,
      logout,
    }),
    [user, isLoading, login, register, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used inside an AuthProvider')
  }
  return context
}

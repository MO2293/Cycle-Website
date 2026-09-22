import type { ApiErrorBody } from './types'

/**
 * A single place where every HTTP call to the API goes through.
 *
 * <p>Centralising this means the token is attached in exactly one spot, errors
 * are shaped consistently, and no component ever writes a raw `fetch`. If auth
 * moves to cookies later, only this file changes.
 */

/**
 * Empty by default so requests go to the same origin. In development Vite
 * proxies /api to :8080; in production the frontend and API are deployed
 * separately, so VITE_API_BASE_URL supplies the API's URL at build time.
 */
const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''

const TOKEN_KEY = 'cyclehaven.token'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY)
}

/**
 * An API call that failed, carrying the server's structured message so the UI
 * can show something specific instead of "something went wrong".
 */
export class ApiError extends Error {
  readonly status: number
  readonly fieldErrors?: Record<string, string>

  constructor(status: number, message: string, fieldErrors?: Record<string, string>) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.fieldErrors = fieldErrors
  }

  /** True when the caller needs to sign in (or sign in again). */
  get isUnauthorized(): boolean {
    return this.status === 401
  }
}

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  body?: unknown
  /** Set false for endpoints that work without a token, e.g. the catalogue. */
  auth?: boolean
}

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body, auth = true } = options

  const headers: Record<string, string> = {}
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }

  const token = getToken()
  if (auth && token) {
    headers.Authorization = `Bearer ${token}`
  }

  const response = await fetch(`${BASE_URL}${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  })

  if (response.status === 204) {
    return undefined as T
  }

  const text = await response.text()
  const payload = text ? JSON.parse(text) : null

  if (!response.ok) {
    const error = payload as ApiErrorBody | null
    throw new ApiError(
      response.status,
      error?.message ?? `Request failed (${response.status})`,
      error?.fieldErrors,
    )
  }

  return payload as T
}

export const api = {
  get: <T>(path: string, auth = true) => request<T>(path, { method: 'GET', auth }),
  post: <T>(path: string, body?: unknown, auth = true) =>
    request<T>(path, { method: 'POST', body, auth }),
  put: <T>(path: string, body?: unknown, auth = true) =>
    request<T>(path, { method: 'PUT', body, auth }),
  delete: <T>(path: string, auth = true) => request<T>(path, { method: 'DELETE', auth }),
}

/** Formats a number as Canadian dollars. */
export function formatPrice(value: number): string {
  return new Intl.NumberFormat('en-CA', {
    style: 'currency',
    currency: 'CAD',
  }).format(value)
}

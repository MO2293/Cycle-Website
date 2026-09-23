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

/**
 * Uploads a file as multipart/form-data.
 *
 * <p>Separate from `request` because the Content-Type header must be omitted
 * entirely here: the browser generates it itself, including the multipart
 * boundary. Setting it manually produces a request the server cannot parse — a
 * genuinely confusing failure, since the header looks correct.
 */
export async function uploadFile<T>(path: string, file: File): Promise<T> {
  const formData = new FormData()
  formData.append('file', file)

  const headers: Record<string, string> = {}
  const token = getToken()
  if (token) headers.Authorization = `Bearer ${token}`

  const response = await fetch(`${BASE_URL}${path}`, {
    method: 'PUT',
    headers,
    body: formData,
  })

  const text = await response.text()
  const payload = text ? JSON.parse(text) : null

  if (!response.ok) {
    const error = payload as ApiErrorBody | null
    throw new ApiError(response.status, error?.message ?? `Upload failed (${response.status})`)
  }

  return payload as T
}

/**
 * Resolves a server-relative media path (an item's `imageUrl`) against the API's
 * origin.
 *
 * <p>The API returns `imageUrl` as a path, not an absolute URL. In development
 * that works untouched: Vite proxies /api to the backend, so the browser's own
 * origin serves the image. In production the frontend and the API are deployed
 * to different hosts, and a bare path resolves against the frontend's origin —
 * where nothing answers it, and the SPA fallback returns index.html, so the
 * image silently renders broken. Every <img> fed by the API goes through here.
 */
export function mediaUrl(path: string): string {
  return path.startsWith('http') ? path : `${BASE_URL}${path}`
}

/** Formats a number as Canadian dollars. */
export function formatPrice(value: number): string {
  return new Intl.NumberFormat('en-CA', {
    style: 'currency',
    currency: 'CAD',
  }).format(value)
}

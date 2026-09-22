/**
 * Types mirroring the API's response shapes.
 *
 * These are hand-written to match the Java DTOs. Keeping them in one file means
 * a backend change that alters a response surfaces as a TypeScript error at the
 * point of use, rather than as `undefined` appearing somewhere in the UI at
 * runtime.
 */

export type Role = 'CUSTOMER' | 'ADMIN'

export type Category = 'TANDEM' | 'BMX' | 'MOUNTAIN' | 'ELECTRIC' | 'ROAD' | 'KIDS'

export type OrderStatus = 'PENDING' | 'PAID' | 'FAILED'

export interface User {
  id: number
  name: string
  email: string
  role: Role
  phone?: string
  address?: string
  city?: string
  province?: string
  country?: string
  cardLast4?: string
}

export interface AuthResponse {
  token: string
  tokenType: string
  expiresInSeconds: number
  user: User
}

export interface Item {
  id: number
  name: string
  category: Category
  description: string
  model: string
  price: number
  colour: string
  quantity: number
  inStock: boolean
  hasImage: boolean
  imageUrl: string
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export interface CartLine {
  itemId: number
  name: string
  model: string
  colour: string
  unitPrice: number
  quantity: number
  lineTotal: number
  availableStock: number
  imageUrl: string
}

export interface Cart {
  lines: CartLine[]
  distinctItems: number
  totalUnits: number
  subtotal: number
  notices: string[]
}

export interface OrderLine {
  itemId: number
  name: string
  model: string
  quantity: number
  priceAtPurchase: number
  lineTotal: number
  imageUrl: string
}

export interface Order {
  orderRef: string
  status: OrderStatus
  customerEmail: string
  shippingAddress: string
  totalAmount: number
  cardBrand: string
  cardLast4: string
  placedAt: string
  lines: OrderLine[]
}

/** The error shape returned by the API's GlobalExceptionHandler. */
export interface ApiErrorBody {
  timestamp: string
  status: number
  error: string
  message: string
  path: string
  fieldErrors?: Record<string, string>
}

export const CATEGORY_LABELS: Record<Category, string> = {
  TANDEM: 'Tandem',
  BMX: 'BMX',
  MOUNTAIN: 'Mountain',
  ELECTRIC: 'Electric',
  ROAD: 'Road',
  KIDS: 'Kids',
}

export const SORT_OPTIONS = [
  { value: 'name_asc', label: 'Name (A–Z)' },
  { value: 'name_desc', label: 'Name (Z–A)' },
  { value: 'price_asc', label: 'Price (low to high)' },
  { value: 'price_desc', label: 'Price (high to low)' },
  { value: 'newest', label: 'Newest first' },
] as const

import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react'
import type { ReactNode } from 'react'
import { api } from '../api/client'
import type { Cart, CartLine, Item } from '../api/types'
import { useAuth } from './AuthContext'

/**
 * The shopping cart, for both signed-in customers and guests.
 *
 * <p>Two storage strategies behind one interface:
 *
 * <p><b>Signed in</b> — the cart lives on the server, so it follows the customer
 * between devices and survives clearing their browser.
 *
 * <p><b>Guest</b> — the cart lives in this browser's localStorage. This is what
 * replaces the original's shared {@code guest@cyclehaven.com} cart row, where
 * every anonymous visitor on the site read and wrote the same database rows and
 * saw each other's items.
 *
 * <p>When a guest signs in, their local cart is POSTed to /api/cart/merge and
 * folded into their account, so nothing is lost at the moment of login.
 */

const GUEST_CART_KEY = 'cyclehaven.guestCart'

interface GuestLine {
  itemId: number
  quantity: number
}

interface CartContextValue {
  lines: CartLine[]
  subtotal: number
  totalUnits: number
  notices: string[]
  isLoading: boolean
  addItem: (itemId: number, quantity: number) => Promise<void>
  setQuantity: (itemId: number, quantity: number) => Promise<void>
  removeItem: (itemId: number) => Promise<void>
  clear: () => Promise<void>
  dismissNotices: () => void
  /** Guest lines, needed by checkout since a guest has no server-side cart. */
  guestLines: GuestLine[]
}

const CartContext = createContext<CartContextValue | undefined>(undefined)

function readGuestCart(): GuestLine[] {
  try {
    const raw = localStorage.getItem(GUEST_CART_KEY)
    return raw ? (JSON.parse(raw) as GuestLine[]) : []
  } catch {
    // Corrupt or unreadable storage should never break the shop.
    return []
  }
}

function writeGuestCart(lines: GuestLine[]): void {
  localStorage.setItem(GUEST_CART_KEY, JSON.stringify(lines))
}

export function CartProvider({ children }: { children: ReactNode }) {
  const { isAuthenticated, isLoading: authLoading } = useAuth()

  const [lines, setLines] = useState<CartLine[]>([])
  const [notices, setNotices] = useState<string[]>([])
  const [isLoading, setIsLoading] = useState(false)
  const [guestLines, setGuestLines] = useState<GuestLine[]>(() => readGuestCart())

  // Ensures the guest cart is merged exactly once per sign-in, not on every
  // re-render that happens to see isAuthenticated as true.
  const hasMerged = useRef(false)

  const applyServerCart = useCallback((cart: Cart) => {
    setLines(cart.lines)
    if (cart.notices.length > 0) setNotices(cart.notices)
  }, [])

  /** Builds display lines for a guest cart by fetching current item details. */
  const hydrateGuestCart = useCallback(async (stored: GuestLine[]) => {
    if (stored.length === 0) {
      setLines([])
      return
    }

    const items = await Promise.all(
      stored.map((line) =>
        api
          .get<Item>(`/api/items/${line.itemId}`, false)
          .then((item) => ({ item, quantity: line.quantity }))
          .catch(() => null),
      ),
    )

    const messages: string[] = []
    const hydrated: CartLine[] = []

    for (const entry of items) {
      // An item can disappear from the catalogue while it sits in a guest's
      // browser for weeks. Drop it and say so rather than failing outright.
      if (!entry) continue

      const { item, quantity } = entry
      const allowed = Math.min(quantity, item.quantity)
      if (allowed <= 0) {
        messages.push(`${item.name} is out of stock and was removed from your cart`)
        continue
      }
      if (allowed < quantity) {
        messages.push(`Only ${item.quantity} of ${item.name} left — quantity reduced`)
      }

      hydrated.push({
        itemId: item.id,
        name: item.name,
        model: item.model,
        colour: item.colour,
        unitPrice: item.price,
        quantity: allowed,
        lineTotal: item.price * allowed,
        availableStock: item.quantity,
        imageUrl: item.imageUrl,
      })
    }

    setLines(hydrated)
    if (messages.length > 0) setNotices(messages)
  }, [])

  const refresh = useCallback(async () => {
    setIsLoading(true)
    try {
      if (isAuthenticated) {
        applyServerCart(await api.get<Cart>('/api/cart'))
      } else {
        await hydrateGuestCart(readGuestCart())
      }
    } catch {
      setLines([])
    } finally {
      setIsLoading(false)
    }
  }, [isAuthenticated, applyServerCart, hydrateGuestCart])

  // On sign-in, hand any guest cart to the server, then load the merged result.
  useEffect(() => {
    if (authLoading) return

    if (!isAuthenticated) {
      hasMerged.current = false
      void refresh()
      return
    }

    if (hasMerged.current) {
      void refresh()
      return
    }

    hasMerged.current = true
    const stored = readGuestCart()

    const run = async () => {
      setIsLoading(true)
      try {
        if (stored.length > 0) {
          const merged = await api.post<Cart>('/api/cart/merge', { lines: stored })
          localStorage.removeItem(GUEST_CART_KEY)
          setGuestLines([])
          applyServerCart(merged)
        } else {
          applyServerCart(await api.get<Cart>('/api/cart'))
        }
      } catch {
        setLines([])
      } finally {
        setIsLoading(false)
      }
    }

    void run()
  }, [isAuthenticated, authLoading, refresh, applyServerCart])

  const updateGuest = useCallback(
    async (mutate: (current: GuestLine[]) => GuestLine[]) => {
      const next = mutate(readGuestCart()).filter((line) => line.quantity > 0)
      writeGuestCart(next)
      setGuestLines(next)
      await hydrateGuestCart(next)
    },
    [hydrateGuestCart],
  )

  const addItem = useCallback(
    async (itemId: number, quantity: number) => {
      setNotices([])
      if (isAuthenticated) {
        applyServerCart(await api.post<Cart>('/api/cart/items', { itemId, quantity }))
      } else {
        await updateGuest((current) => {
          const existing = current.find((line) => line.itemId === itemId)
          return existing
            ? current.map((line) =>
                line.itemId === itemId ? { ...line, quantity: line.quantity + quantity } : line,
              )
            : [...current, { itemId, quantity }]
        })
      }
    },
    [isAuthenticated, applyServerCart, updateGuest],
  )

  const setQuantity = useCallback(
    async (itemId: number, quantity: number) => {
      setNotices([])
      if (quantity <= 0) {
        // The API refuses quantity 0 on purpose — removal is its own verb.
        if (isAuthenticated) {
          applyServerCart(await api.delete<Cart>(`/api/cart/items/${itemId}`))
        } else {
          await updateGuest((current) => current.filter((line) => line.itemId !== itemId))
        }
        return
      }

      if (isAuthenticated) {
        applyServerCart(await api.put<Cart>(`/api/cart/items/${itemId}`, { quantity }))
      } else {
        await updateGuest((current) =>
          current.map((line) => (line.itemId === itemId ? { ...line, quantity } : line)),
        )
      }
    },
    [isAuthenticated, applyServerCart, updateGuest],
  )

  const removeItem = useCallback(
    async (itemId: number) => {
      setNotices([])
      if (isAuthenticated) {
        applyServerCart(await api.delete<Cart>(`/api/cart/items/${itemId}`))
      } else {
        await updateGuest((current) => current.filter((line) => line.itemId !== itemId))
      }
    },
    [isAuthenticated, applyServerCart, updateGuest],
  )

  const clear = useCallback(async () => {
    setNotices([])
    if (isAuthenticated) {
      applyServerCart(await api.delete<Cart>('/api/cart'))
    } else {
      localStorage.removeItem(GUEST_CART_KEY)
      setGuestLines([])
      setLines([])
    }
  }, [isAuthenticated, applyServerCart])

  const value = useMemo<CartContextValue>(() => {
    // Displayed only. Checkout recomputes the real total from database prices,
    // so nothing the browser calculates here can influence what is charged.
    const subtotal = lines.reduce((sum, line) => sum + line.lineTotal, 0)
    const totalUnits = lines.reduce((sum, line) => sum + line.quantity, 0)

    return {
      lines,
      subtotal,
      totalUnits,
      notices,
      isLoading,
      addItem,
      setQuantity,
      removeItem,
      clear,
      dismissNotices: () => setNotices([]),
      guestLines,
    }
  }, [lines, notices, isLoading, addItem, setQuantity, removeItem, clear, guestLines])

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>
}

export function useCart(): CartContextValue {
  const context = useContext(CartContext)
  if (!context) {
    throw new Error('useCart must be used inside a CartProvider')
  }
  return context
}

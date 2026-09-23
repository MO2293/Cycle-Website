import { BrowserRouter, Route, Routes } from 'react-router-dom'
import Layout from './components/Layout'
import ProtectedRoute from './components/ProtectedRoute'
import { AuthProvider } from './context/AuthContext'
import { CartProvider } from './context/CartContext'
import CartPage from './pages/CartPage'
import CheckoutPage from './pages/CheckoutPage'
import HomePage from './pages/HomePage'
import LoginPage from './pages/LoginPage'
import NotFoundPage from './pages/NotFoundPage'
import OrderConfirmationPage from './pages/OrderConfirmationPage'
import OrdersPage from './pages/OrdersPage'
import ProductDetailPage from './pages/ProductDetailPage'
import ProfilePage from './pages/ProfilePage'
import RegisterPage from './pages/RegisterPage'
import AdminDashboardPage from './pages/admin/AdminDashboardPage'
import AdminItemsPage from './pages/admin/AdminItemsPage'
import AdminLayout from './pages/admin/AdminLayout'
import AdminSalesPage from './pages/admin/AdminSalesPage'
import AdminUsersPage from './pages/admin/AdminUsersPage'

/**
 * Client-side routing: the browser never reloads between pages. React swaps the
 * component and the URL changes, which is the core difference from the original
 * — there, every click was a full round trip that re-rendered a whole JSP.
 *
 * <p>CartProvider sits inside AuthProvider because the cart's behaviour depends
 * on whether anyone is signed in: server-backed for a customer, localStorage for
 * a guest.
 */
export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <CartProvider>
          <Routes>
            <Route element={<Layout />}>
              {/* Public */}
              <Route path="/" element={<HomePage />} />
              <Route path="/items/:id" element={<ProductDetailPage />} />
              <Route path="/cart" element={<CartPage />} />
              <Route path="/checkout" element={<CheckoutPage />} />
              <Route path="/orders/:orderRef" element={<OrderConfirmationPage />} />
              <Route path="/login" element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage />} />

              {/* Signed in */}
              <Route
                path="/orders"
                element={
                  <ProtectedRoute>
                    <OrdersPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/profile"
                element={
                  <ProtectedRoute>
                    <ProfilePage />
                  </ProtectedRoute>
                }
              />

              {/* Admin only. The server enforces this independently — these
                  guards decide what to render, not what is permitted. */}
              <Route
                path="/admin"
                element={
                  <ProtectedRoute requireAdmin>
                    <AdminLayout />
                  </ProtectedRoute>
                }
              >
                <Route index element={<AdminDashboardPage />} />
                <Route path="items" element={<AdminItemsPage />} />
                <Route path="sales" element={<AdminSalesPage />} />
                <Route path="users" element={<AdminUsersPage />} />
              </Route>

              <Route path="*" element={<NotFoundPage />} />
            </Route>
          </Routes>
        </CartProvider>
      </AuthProvider>
    </BrowserRouter>
  )
}

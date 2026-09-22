import { BrowserRouter, Route, Routes } from 'react-router-dom'
import Layout from './components/Layout'
import { AuthProvider } from './context/AuthContext'
import HomePage from './pages/HomePage'
import LoginPage from './pages/LoginPage'
import NotFoundPage from './pages/NotFoundPage'
import ProductDetailPage from './pages/ProductDetailPage'
import RegisterPage from './pages/RegisterPage'

/**
 * Client-side routing: the browser never reloads between pages. React swaps the
 * component and the URL changes, which is the core difference from the original
 * — there, every click was a full round trip that re-rendered a whole JSP.
 */
export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route element={<Layout />}>
            <Route path="/" element={<HomePage />} />
            <Route path="/items/:id" element={<ProductDetailPage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="*" element={<NotFoundPage />} />
          </Route>
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}

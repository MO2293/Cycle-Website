import { Outlet } from 'react-router-dom'
import Header from './Header'

export default function Layout() {
  return (
    <div className="flex min-h-screen flex-col">
      <Header />

      {/* Outlet renders whichever page matches the current route, inside this
          shared shell — so the header and footer are defined once rather than
          repeated on every page the way each JSP had to include header.jsp. */}
      <main className="flex-1">
        <Outlet />
      </main>

      <footer className="mt-16 border-t border-ink-100 bg-white">
        <div className="mx-auto flex max-w-6xl flex-col gap-1 px-4 py-8 text-sm text-ink-400">
          <p className="font-medium text-ink-600">Cycle Haven</p>
          <p>A demonstration storefront. No real orders are placed and no payments are processed.</p>
        </div>
      </footer>
    </div>
  )
}

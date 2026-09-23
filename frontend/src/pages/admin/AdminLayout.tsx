import { NavLink, Outlet } from 'react-router-dom'

export default function AdminLayout() {
  const tabClass = ({ isActive }: { isActive: boolean }) =>
    [
      'rounded-lg px-3 py-2 text-sm font-medium transition-colors',
      isActive ? 'bg-moss-600 text-white' : 'text-ink-600 hover:bg-ink-100',
    ].join(' ')

  return (
    <div className="mx-auto max-w-6xl px-4 py-10">
      <h1 className="text-2xl font-semibold tracking-tight text-ink-900">Admin</h1>

      <nav className="mt-5 flex flex-wrap gap-2 border-b border-ink-100 pb-4">
        <NavLink to="/admin" end className={tabClass}>
          Dashboard
        </NavLink>
        <NavLink to="/admin/items" className={tabClass}>
          Products
        </NavLink>
        <NavLink to="/admin/sales" className={tabClass}>
          Sales
        </NavLink>
        <NavLink to="/admin/users" className={tabClass}>
          Customers
        </NavLink>
      </nav>

      <div className="mt-6">
        <Outlet />
      </div>
    </div>
  )
}

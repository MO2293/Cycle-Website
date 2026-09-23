import { useCallback, useEffect, useState } from 'react'
import { ApiError, api } from '../../api/client'
import type { PageResponse, Role, User } from '../../api/types'
import { useAuth } from '../../context/AuthContext'
import { Badge, ErrorNotice, Spinner, inputClass } from '../../components/ui'

export default function AdminUsersPage() {
  const { user: currentUser } = useAuth()
  const [users, setUsers] = useState<User[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [busyId, setBusyId] = useState<number | null>(null)

  const load = useCallback(() => {
    setIsLoading(true)
    api
      .get<PageResponse<User>>('/api/admin/users?size=100')
      .then((page) => setUsers(page.content))
      .catch((err: Error) => setError(err.message))
      .finally(() => setIsLoading(false))
  }, [])

  useEffect(load, [load])

  const changeRole = async (target: User, role: Role) => {
    setError(null)
    setBusyId(target.id)
    try {
      // The whole profile is sent because the endpoint is a PUT — a full
      // replacement of the resource, not a partial patch.
      await api.put<User>(`/api/admin/users/${target.id}`, {
        name: target.name,
        phone: target.phone ?? null,
        address: target.address ?? null,
        city: target.city ?? null,
        province: target.province ?? null,
        country: target.country ?? null,
        role,
      })
      load()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not update that user.')
    } finally {
      setBusyId(null)
    }
  }

  if (isLoading) return <Spinner label="Loading customers…" />

  return (
    <div className="flex flex-col gap-5">
      {error && <ErrorNotice message={error} />}

      <p className="text-sm text-ink-400">{users.length} accounts</p>

      <div className="overflow-x-auto rounded-xl border border-ink-100 bg-white">
        <table className="w-full text-left text-sm">
          <thead className="border-b border-ink-100 text-xs uppercase tracking-wide text-ink-400">
            <tr>
              <th className="px-4 py-3 font-medium">Name</th>
              <th className="px-4 py-3 font-medium">Email</th>
              <th className="px-4 py-3 font-medium">Location</th>
              <th className="px-4 py-3 font-medium">Role</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-ink-100">
            {users.map((user) => {
              const isSelf = user.id === currentUser?.id

              return (
                <tr key={user.id}>
                  <td className="px-4 py-3">
                    <span className="font-medium text-ink-900">{user.name}</span>
                    {isSelf && <span className="ml-2 text-xs text-ink-400">(you)</span>}
                  </td>
                  <td className="px-4 py-3 text-ink-600">{user.email}</td>
                  <td className="px-4 py-3 text-ink-600">
                    {[user.city, user.province, user.country].filter(Boolean).join(', ') || '—'}
                  </td>
                  <td className="px-4 py-3">
                    {isSelf ? (
                      // Disabled rather than hidden, with the reason stated: the
                      // server refuses this too, so the UI explains rather than
                      // pretending the option does not exist.
                      <span className="flex items-center gap-2">
                        <Badge tone="good">{user.role}</Badge>
                        <span className="text-xs text-ink-400">cannot change your own role</span>
                      </span>
                    ) : (
                      <select
                        value={user.role}
                        disabled={busyId === user.id}
                        onChange={(event) => changeRole(user, event.target.value as Role)}
                        className={`${inputClass} w-36`}
                      >
                        <option value="CUSTOMER">CUSTOMER</option>
                        <option value="ADMIN">ADMIN</option>
                      </select>
                    )}
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
    </div>
  )
}

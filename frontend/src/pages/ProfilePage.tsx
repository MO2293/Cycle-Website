import { useState } from 'react'
import { ApiError, api } from '../api/client'
import type { User } from '../api/types'
import { useAuth } from '../context/AuthContext'
import { ErrorNotice, Field, buttonClass, inputClass } from '../components/ui'

export default function ProfilePage() {
  const { user } = useAuth()

  const [profile, setProfile] = useState({
    name: user?.name ?? '',
    phone: user?.phone ?? '',
    address: user?.address ?? '',
    city: user?.city ?? '',
    province: user?.province ?? '',
    country: user?.country ?? '',
  })
  const [profileState, setProfileState] = useState<{
    error: string | null
    fieldErrors: Record<string, string>
    saved: boolean
    busy: boolean
  }>({ error: null, fieldErrors: {}, saved: false, busy: false })

  const [passwords, setPasswords] = useState({ currentPassword: '', newPassword: '', confirm: '' })
  const [passwordState, setPasswordState] = useState<{
    error: string | null
    saved: boolean
    busy: boolean
  }>({ error: null, saved: false, busy: false })

  const setProfileField =
    (key: keyof typeof profile) => (event: React.ChangeEvent<HTMLInputElement>) =>
      setProfile((previous) => ({ ...previous, [key]: event.target.value }))

  const saveProfile = async (event: React.FormEvent) => {
    event.preventDefault()
    setProfileState({ error: null, fieldErrors: {}, saved: false, busy: true })
    try {
      await api.put<User>('/api/users/me', profile)
      setProfileState({ error: null, fieldErrors: {}, saved: true, busy: false })
    } catch (err) {
      setProfileState({
        error: err instanceof ApiError ? err.message : 'Could not save your profile.',
        fieldErrors: err instanceof ApiError ? (err.fieldErrors ?? {}) : {},
        saved: false,
        busy: false,
      })
    }
  }

  const changePassword = async (event: React.FormEvent) => {
    event.preventDefault()

    if (passwords.newPassword !== passwords.confirm) {
      setPasswordState({ error: 'New passwords do not match', saved: false, busy: false })
      return
    }

    setPasswordState({ error: null, saved: false, busy: true })
    try {
      await api.put<void>('/api/users/me/password', {
        currentPassword: passwords.currentPassword,
        newPassword: passwords.newPassword,
      })
      setPasswords({ currentPassword: '', newPassword: '', confirm: '' })
      setPasswordState({ error: null, saved: true, busy: false })
    } catch (err) {
      setPasswordState({
        error: err instanceof ApiError ? err.message : 'Could not change your password.',
        saved: false,
        busy: false,
      })
    }
  }

  return (
    <div className="mx-auto max-w-2xl px-4 py-10">
      <h1 className="text-2xl font-semibold tracking-tight text-ink-900">Your profile</h1>
      <p className="mt-1 text-sm text-ink-400">{user?.email}</p>

      <form onSubmit={saveProfile} className="mt-8 flex flex-col gap-4 rounded-xl border border-ink-100 bg-white p-6">
        <h2 className="font-semibold text-ink-900">Details</h2>

        {profileState.error && <ErrorNotice message={profileState.error} />}
        {profileState.saved && (
          <p className="rounded-lg bg-moss-50 px-3 py-2 text-sm text-moss-700">Profile saved.</p>
        )}

        <Field label="Full name" htmlFor="name" error={profileState.fieldErrors.name}>
          <input id="name" required value={profile.name} onChange={setProfileField('name')} className={inputClass} />
        </Field>

        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Phone" htmlFor="phone" error={profileState.fieldErrors.phone}>
            <input id="phone" value={profile.phone} onChange={setProfileField('phone')} className={inputClass} />
          </Field>
          <Field label="City" htmlFor="city">
            <input id="city" value={profile.city} onChange={setProfileField('city')} className={inputClass} />
          </Field>
        </div>

        <Field label="Address" htmlFor="address">
          <input id="address" value={profile.address} onChange={setProfileField('address')} className={inputClass} />
        </Field>

        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Province" htmlFor="province">
            <input id="province" value={profile.province} onChange={setProfileField('province')} className={inputClass} />
          </Field>
          <Field label="Country" htmlFor="country">
            <input id="country" value={profile.country} onChange={setProfileField('country')} className={inputClass} />
          </Field>
        </div>

        {/* Email is absent deliberately: it is the login identity, so changing it
            belongs in its own flow with re-verification. */}

        <button type="submit" disabled={profileState.busy} className={`${buttonClass} self-start`}>
          {profileState.busy ? 'Saving…' : 'Save changes'}
        </button>
      </form>

      <form onSubmit={changePassword} className="mt-6 flex flex-col gap-4 rounded-xl border border-ink-100 bg-white p-6">
        <h2 className="font-semibold text-ink-900">Change password</h2>

        {passwordState.error && <ErrorNotice message={passwordState.error} />}
        {passwordState.saved && (
          <p className="rounded-lg bg-moss-50 px-3 py-2 text-sm text-moss-700">
            Password changed. Your next sign-in uses the new one.
          </p>
        )}

        {/* The current password is required even though you are already signed
            in — a token sitting in an unattended browser should not be enough
            to lock the owner out of their own account. */}
        <Field label="Current password" htmlFor="currentPassword">
          <input
            id="currentPassword"
            type="password"
            required
            autoComplete="current-password"
            value={passwords.currentPassword}
            onChange={(event) =>
              setPasswords((previous) => ({ ...previous, currentPassword: event.target.value }))
            }
            className={inputClass}
          />
        </Field>

        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="New password" htmlFor="newPassword">
            <input
              id="newPassword"
              type="password"
              required
              autoComplete="new-password"
              value={passwords.newPassword}
              onChange={(event) =>
                setPasswords((previous) => ({ ...previous, newPassword: event.target.value }))
              }
              className={inputClass}
            />
          </Field>

          <Field label="Confirm new password" htmlFor="confirm">
            <input
              id="confirm"
              type="password"
              required
              autoComplete="new-password"
              value={passwords.confirm}
              onChange={(event) =>
                setPasswords((previous) => ({ ...previous, confirm: event.target.value }))
              }
              className={inputClass}
            />
          </Field>
        </div>

        <button type="submit" disabled={passwordState.busy} className={`${buttonClass} self-start`}>
          {passwordState.busy ? 'Changing…' : 'Change password'}
        </button>
      </form>
    </div>
  )
}

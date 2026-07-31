import { useState, type FormEvent } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { api, ApiRequestError } from '../api/client'
import type { BalanceResponse, UserResponse } from '../api/types'
import { useAuth } from '../auth/AuthContext'

export default function ProfilePage() {
  const { user } = useAuth()
  const [usernameForm, setUsernameForm] = useState({ username: user?.username ?? '', password: '', newUsername: '' })
  const [passwordForm, setPasswordForm] = useState({ name: user?.username ?? '', password: '', newPassword: '' })
  const [usernameMessage, setUsernameMessage] = useState<string | null>(null)
  const [passwordMessage, setPasswordMessage] = useState<string | null>(null)

  const { data: balance } = useQuery({
    queryKey: ['balance'],
    queryFn: () => api.get<BalanceResponse>('/applicants/me/balance'),
    enabled: user?.role === 'APPLICANT',
  })

  const usernameMutation = useMutation({
    mutationFn: () => api.put<UserResponse>('/me/username', usernameForm),
    onSuccess: () => setUsernameMessage('Username updated successfully.'),
    onError: (e) => setUsernameMessage(e instanceof ApiRequestError ? e.message : 'Failed to update username'),
  })

  const passwordMutation = useMutation({
    mutationFn: () => api.put('/me/password', passwordForm),
    onSuccess: () => {
      setPasswordMessage('Password updated successfully.')
      setPasswordForm({ ...passwordForm, password: '', newPassword: '' })
    },
    onError: (e) => setPasswordMessage(e instanceof ApiRequestError ? e.message : 'Failed to update password'),
  })

  function handleUsernameSubmit(e: FormEvent) {
    e.preventDefault()
    setUsernameMessage(null)
    usernameMutation.mutate()
  }

  function handlePasswordSubmit(e: FormEvent) {
    e.preventDefault()
    setPasswordMessage(null)
    passwordMutation.mutate()
  }

  if (!user) return null

  return (
    <div>
      <h1>My Profile</h1>
      <table className="detail-table">
        <tbody>
          <tr><th>Name</th><td>{user.name}</td></tr>
          <tr><th>Username</th><td>{user.username}</td></tr>
          <tr><th>Role</th><td>{user.role}</td></tr>
          {user.role === 'APPLICANT' && <tr><th>Balance</th><td>${balance?.balance.toFixed(2) ?? '...'}</td></tr>}
        </tbody>
      </table>

      <h2>Change Username</h2>
      <form onSubmit={handleUsernameSubmit} className="centered-form">
        <label>
          Current username
          <input value={usernameForm.username} onChange={(e) => setUsernameForm({ ...usernameForm, username: e.target.value })} required />
        </label>
        <label>
          Password
          <input type="password" value={usernameForm.password} onChange={(e) => setUsernameForm({ ...usernameForm, password: e.target.value })} required />
        </label>
        <label>
          New username
          <input value={usernameForm.newUsername} onChange={(e) => setUsernameForm({ ...usernameForm, newUsername: e.target.value })} required />
        </label>
        <button type="submit" disabled={usernameMutation.isPending}>Update username</button>
        {usernameMessage && <p>{usernameMessage}</p>}
      </form>

      <h2>Change Password</h2>
      <form onSubmit={handlePasswordSubmit} className="centered-form">
        <label>
          Username
          <input value={passwordForm.name} onChange={(e) => setPasswordForm({ ...passwordForm, name: e.target.value })} required />
        </label>
        <label>
          Current password
          <input type="password" value={passwordForm.password} onChange={(e) => setPasswordForm({ ...passwordForm, password: e.target.value })} required />
        </label>
        <label>
          New password
          <input type="password" minLength={4} value={passwordForm.newPassword} onChange={(e) => setPasswordForm({ ...passwordForm, newPassword: e.target.value })} required />
        </label>
        <button type="submit" disabled={passwordMutation.isPending}>Update password</button>
        {passwordMessage && <p>{passwordMessage}</p>}
      </form>
    </div>
  )
}

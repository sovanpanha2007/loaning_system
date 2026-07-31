import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api, ApiRequestError } from '../api/client'
import type { StaffResponse } from '../api/types'

export default function NewStaffPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [form, setForm] = useState({
    name: '',
    username: '',
    phoneNumber: '',
    age: 18,
    password: '',
    salary: 0,
    position: 'LOANOFFICER',
  })
  const [error, setError] = useState<string | null>(null)

  const mutation = useMutation({
    mutationFn: () => api.post<StaffResponse>('/staff', form),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['staff'] })
      navigate('/staff')
    },
    onError: (e) => setError(e instanceof ApiRequestError ? e.message : 'Failed to create staff'),
  })

  function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    mutation.mutate()
  }

  return (
    <div className="centered-form">
      <h1>New Staff Member</h1>
      <form onSubmit={handleSubmit}>
        <label>
          Name
          <input pattern="[a-zA-Z ]+" title="Letters only" value={form.name}
                 onChange={(e) => setForm({ ...form, name: e.target.value })} required />
        </label>
        <label>
          Username
          <input value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} required />
        </label>
        <label>
          Phone number
          <input pattern="0[0-9]{8,9}" title="9-10 digits starting with 0" value={form.phoneNumber}
                 onChange={(e) => setForm({ ...form, phoneNumber: e.target.value })} required />
        </label>
        <label>
          Age
          <input type="number" min={18} max={65} value={form.age}
                 onChange={(e) => setForm({ ...form, age: Number(e.target.value) })} required />
        </label>
        <label>
          Password
          <input type="password" minLength={4} value={form.password}
                 onChange={(e) => setForm({ ...form, password: e.target.value })} required />
        </label>
        <label>
          Salary
          <input type="number" min={0} value={form.salary}
                 onChange={(e) => setForm({ ...form, salary: Number(e.target.value) })} required />
        </label>
        <label>
          Position
          <select value={form.position} onChange={(e) => setForm({ ...form, position: e.target.value })}>
            <option value="MANAGER">Manager</option>
            <option value="LOANOFFICER">Loan Officer</option>
            <option value="CREDITCOMMITTEE">Credit Committee</option>
          </select>
        </label>
        {error && <p className="error">{error}</p>}
        <button type="submit" disabled={mutation.isPending}>
          {mutation.isPending ? 'Creating...' : 'Create staff member'}
        </button>
      </form>
    </div>
  )
}

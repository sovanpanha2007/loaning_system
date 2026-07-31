import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api, ApiRequestError } from '../api/client'
import type { ApplicantResponse } from '../api/types'

export default function NewApplicantPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [form, setForm] = useState({
    name: '',
    username: '',
    phoneNumber: '',
    password: '',
    age: 18,
    income: 0,
    gender: 'F',
    existingExternalDebt: 0,
  })
  const [error, setError] = useState<string | null>(null)

  const mutation = useMutation({
    mutationFn: () => api.post<ApplicantResponse>('/applicants', form),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['applicants'] })
      navigate('/applicants')
    },
    onError: (e) => setError(e instanceof ApiRequestError ? e.message : 'Failed to create applicant'),
  })

  function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    mutation.mutate()
  }

  return (
    <div className="centered-form">
      <h1>New Applicant</h1>
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
          Password
          <input type="password" minLength={4} value={form.password}
                 onChange={(e) => setForm({ ...form, password: e.target.value })} required />
        </label>
        <label>
          Age
          <input type="number" min={18} max={65} value={form.age}
                 onChange={(e) => setForm({ ...form, age: Number(e.target.value) })} required />
        </label>
        <label>
          Monthly income
          <input type="number" min={0} value={form.income}
                 onChange={(e) => setForm({ ...form, income: Number(e.target.value) })} required />
        </label>
        <label>
          Existing external debt
          <input type="number" min={0} value={form.existingExternalDebt}
                 onChange={(e) => setForm({ ...form, existingExternalDebt: Number(e.target.value) })} />
        </label>
        <label>
          Gender
          <select value={form.gender} onChange={(e) => setForm({ ...form, gender: e.target.value })}>
            <option value="F">Female</option>
            <option value="M">Male</option>
          </select>
        </label>
        {error && <p className="error">{error}</p>}
        <button type="submit" disabled={mutation.isPending}>
          {mutation.isPending ? 'Creating...' : 'Create applicant'}
        </button>
      </form>
    </div>
  )
}

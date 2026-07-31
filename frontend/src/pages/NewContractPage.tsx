import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { api, ApiRequestError } from '../api/client'
import type { ContractResponse } from '../api/types'
import { useAuth } from '../auth/AuthContext'

export default function NewContractPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [applicantId, setApplicantId] = useState(user?.role === 'APPLICANT' ? user.id : 0)
  const [amount, setAmount] = useState(0)
  const [duration, setDuration] = useState(1)
  const [error, setError] = useState<string | null>(null)

  const mutation = useMutation({
    mutationFn: () => api.post<ContractResponse>('/contracts', { applicantId, amount, duration }),
    onSuccess: (contract) => navigate(user?.role === 'APPLICANT' ? '/contracts/mine' : `/contracts/${contract.id}`),
    onError: (e) => setError(e instanceof ApiRequestError ? e.message : 'Failed to create contract'),
  })

  function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    mutation.mutate()
  }

  return (
    <div className="centered-form">
      <h1>New Loan Contract</h1>
      <form onSubmit={handleSubmit}>
        <label>
          Applicant ID
          <input type="number" min={1} value={applicantId}
                 onChange={(e) => setApplicantId(Number(e.target.value))} required
                 disabled={user?.role === 'APPLICANT'} />
        </label>
        <label>
          Loan amount
          <input type="number" min={1} value={amount} onChange={(e) => setAmount(Number(e.target.value))} required />
        </label>
        <label>
          Duration (years)
          <input type="number" min={1} value={duration} onChange={(e) => setDuration(Number(e.target.value))} required />
        </label>
        {error && <p className="error">{error}</p>}
        <button type="submit" disabled={mutation.isPending}>
          {mutation.isPending ? 'Submitting...' : 'Submit application'}
        </button>
      </form>
    </div>
  )
}

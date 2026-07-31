import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, ApiRequestError } from '../api/client'
import type { ContractResponse } from '../api/types'
import { useAuth } from '../auth/AuthContext'

export default function ContractDetailPage() {
  const { user } = useAuth()
  const { contractId } = useParams()
  const queryClient = useQueryClient()
  const [error, setError] = useState<string | null>(null)
  const [coSignerId, setCoSignerId] = useState('')

  const { data: contract, isLoading } = useQuery({
    queryKey: ['contracts', contractId],
    queryFn: async () => {
      const all = await api.get<ContractResponse[]>('/contracts')
      return all.find((c) => c.id === Number(contractId)) ?? null
    },
  })

  function invalidate() {
    queryClient.invalidateQueries({ queryKey: ['contracts'] })
  }

  const approveMutation = useMutation({
    mutationFn: () => api.post<ContractResponse>(`/contracts/${contractId}/approve`),
    onSuccess: invalidate,
    onError: (e) => setError(e instanceof ApiRequestError ? e.message : 'Failed to approve'),
  })

  const rejectMutation = useMutation({
    mutationFn: () => api.post<ContractResponse>(`/contracts/${contractId}/reject`),
    onSuccess: invalidate,
    onError: (e) => setError(e instanceof ApiRequestError ? e.message : 'Failed to reject'),
  })

  const coSignMutation = useMutation({
    mutationFn: () => api.post<ContractResponse>(`/contracts/${contractId}/cosigners`, { staffId: Number(coSignerId) }),
    onSuccess: () => {
      invalidate()
      setCoSignerId('')
    },
    onError: (e) => setError(e instanceof ApiRequestError ? e.message : 'Failed to add co-signer'),
  })

  if (isLoading) return <p>Loading...</p>
  if (!contract) return <p>Contract not found.</p>

  const canApprove = user?.role === 'LOAN_OFFICER' || user?.role === 'CREDIT_COMMITTEE'
  const canReject = user?.role === 'LOAN_OFFICER'
  const canCoSign = user?.role === 'LOAN_OFFICER' || user?.role === 'CREDIT_COMMITTEE'
  const isDecided = contract.status !== 'PENDING' && contract.status !== 'FORWARDED'

  return (
    <div>
      <h1>Contract #{contract.id}</h1>
      <table className="detail-table">
        <tbody>
          <tr><th>Applicant</th><td>{contract.applicantName}</td></tr>
          <tr><th>Principal</th><td>${contract.principalAmount.toFixed(2)}</td></tr>
          <tr><th>Total</th><td>${contract.totalAmount.toFixed(2)}</td></tr>
          <tr><th>Duration</th><td>{contract.duration} yr(s)</td></tr>
          <tr><th>Nominal rate</th><td>{(contract.interestRate * 100).toFixed(2)}%</td></tr>
          <tr><th>Effective APR</th><td>{(contract.effectiveApr * 100).toFixed(2)}%</td></tr>
          <tr><th>Status</th><td>{contract.status}</td></tr>
          <tr><th>Approving officer</th><td>{contract.approvingOfficerName ?? '-'}</td></tr>
          <tr><th>Drafting officer</th><td>{contract.draftingOfficerName ?? '-'}</td></tr>
          <tr><th>Committee votes</th><td>{contract.committeeVoteCount}</td></tr>
          <tr><th>Co-signers (staff IDs)</th><td>{contract.coSignerIds.join(', ') || '-'}</td></tr>
        </tbody>
      </table>

      {error && <p className="error">{error}</p>}

      {!isDecided && (
        <div className="actions">
          {canApprove && (
            <button onClick={() => approveMutation.mutate()} disabled={approveMutation.isPending}>
              Approve / Vote
            </button>
          )}
          {canReject && (
            <button onClick={() => rejectMutation.mutate()} disabled={rejectMutation.isPending}>
              Reject
            </button>
          )}
        </div>
      )}

      {canCoSign && (
        <div className="actions">
          <input
            type="number"
            placeholder="Committee staff ID"
            value={coSignerId}
            onChange={(e) => setCoSignerId(e.target.value)}
          />
          <button disabled={!coSignerId || coSignMutation.isPending} onClick={() => coSignMutation.mutate()}>
            Add co-signer
          </button>
        </div>
      )}
    </div>
  )
}

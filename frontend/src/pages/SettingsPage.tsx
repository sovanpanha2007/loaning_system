import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, ApiRequestError } from '../api/client'
import type { ConfigResponse, DelinquencyResponse } from '../api/types'

export default function SettingsPage() {
  const queryClient = useQueryClient()
  const [votes, setVotes] = useState('')
  const [ratio, setRatio] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [delinquencyResult, setDelinquencyResult] = useState<DelinquencyResponse | null>(null)

  const { data: config, isLoading } = useQuery({
    queryKey: ['config'],
    queryFn: () => api.get<ConfigResponse>('/config'),
  })

  const votesMutation = useMutation({
    mutationFn: () => api.put<ConfigResponse>('/config/required-votes', { votes: Number(votes) }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['config'] })
      setVotes('')
    },
    onError: (e) => setError(e instanceof ApiRequestError ? e.message : 'Failed to update required votes'),
  })

  const ratioMutation = useMutation({
    mutationFn: () => api.put<ConfigResponse>('/config/max-dti', { ratio: Number(ratio) }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['config'] })
      setRatio('')
    },
    onError: (e) => setError(e instanceof ApiRequestError ? e.message : 'Failed to update max DTI ratio'),
  })

  const delinquencyMutation = useMutation({
    mutationFn: () => api.post<DelinquencyResponse>('/config/delinquency-check'),
    onSuccess: setDelinquencyResult,
    onError: (e) => setError(e instanceof ApiRequestError ? e.message : 'Delinquency check failed'),
  })

  if (isLoading) return <p>Loading...</p>
  if (!config) return null

  return (
    <div>
      <h1>Bank Settings</h1>
      <table className="detail-table">
        <tbody>
          <tr><th>Bank name</th><td>{config.bankName}</td></tr>
          <tr><th>Interest rate</th><td>{(config.interestRate * 100).toFixed(2)}%</td></tr>
          <tr><th>Required committee votes</th><td>{config.requiredCommitteeVotes}</td></tr>
          <tr><th>Max debt-to-income ratio</th><td>{(config.maxDebtToIncomeRatio * 100).toFixed(2)}%</td></tr>
        </tbody>
      </table>

      {error && <p className="error">{error}</p>}

      <div className="actions">
        <input type="number" min={1} placeholder="new required votes" value={votes}
               onChange={(e) => setVotes(e.target.value)} />
        <button disabled={!votes || votesMutation.isPending} onClick={() => votesMutation.mutate()}>
          Set required votes
        </button>
      </div>

      <div className="actions">
        <input type="number" step="0.01" min={0} max={1} placeholder="new max DTI (e.g. 0.40)" value={ratio}
               onChange={(e) => setRatio(e.target.value)} />
        <button disabled={!ratio || ratioMutation.isPending} onClick={() => ratioMutation.mutate()}>
          Set max DTI ratio
        </button>
      </div>

      <div className="actions">
        <button disabled={delinquencyMutation.isPending} onClick={() => delinquencyMutation.mutate()}>
          Run delinquency check
        </button>
        {delinquencyResult && (
          <p>
            {delinquencyResult.flaggedLate} payment(s) newly flagged late, {delinquencyResult.defaulted} contract(s) defaulted.
          </p>
        )}
      </div>
    </div>
  )
}

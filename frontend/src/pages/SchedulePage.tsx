import { useState, type FormEvent } from 'react'
import { useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, ApiRequestError } from '../api/client'
import type { PaymentScheduleResponse } from '../api/types'

export default function SchedulePage() {
  const { contractId } = useParams()
  const queryClient = useQueryClient()
  const [amount, setAmount] = useState(0)
  const [error, setError] = useState<string | null>(null)
  const [lastResult, setLastResult] = useState<string | null>(null)

  const { data: schedule, isLoading } = useQuery({
    queryKey: ['schedule', contractId],
    queryFn: () => api.get<PaymentScheduleResponse>(`/contracts/${contractId}/schedule`),
  })

  const payMutation = useMutation({
    mutationFn: () => api.post<{ amountApplied: number; monthsTouched: number[]; remainingBalance: number }>(
      `/contracts/${contractId}/payments`,
      { amount },
    ),
    onSuccess: (result) => {
      setLastResult(
        `Applied $${result.amountApplied.toFixed(2)} across month(s) ${result.monthsTouched.join(', ')}. ` +
          `Remaining balance: $${result.remainingBalance.toFixed(2)}.`,
      )
      queryClient.invalidateQueries({ queryKey: ['schedule', contractId] })
      queryClient.invalidateQueries({ queryKey: ['contracts', 'mine'] })
      setAmount(0)
    },
    onError: (e) => setError(e instanceof ApiRequestError ? e.message : 'Payment failed'),
  })

  function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setLastResult(null)
    payMutation.mutate()
  }

  if (isLoading) return <p>Loading...</p>
  if (!schedule) return <p>Schedule not found.</p>

  return (
    <div>
      <h1>Payment Schedule for Contract #{schedule.contractId}</h1>
      <p>{schedule.fullyPaid ? 'This loan is fully paid off.' : 'Loan is still active.'}</p>

      <table>
        <thead>
          <tr>
            <th>Month</th>
            <th>Due date</th>
            <th>Interest</th>
            <th>Principal</th>
            <th>Remaining balance</th>
            <th>Owed this month</th>
            <th>Status</th>
          </tr>
        </thead>
        <tbody>
          {schedule.payments.map((p) => (
            <tr key={p.monthNumber}>
              <td>{p.monthNumber}</td>
              <td>{p.dueDate}</td>
              <td>${p.interestPortion.toFixed(2)}</td>
              <td>${p.principalPortion.toFixed(2)}</td>
              <td>${p.remainingBalance.toFixed(2)}</td>
              <td>${p.remainingDue.toFixed(2)}</td>
              <td>
                {p.paid ? 'PAID' : p.late ? `LATE +$${p.lateFee.toFixed(2)}` : p.amountPaid > 0 ? 'PARTIAL' : ''}
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {!schedule.fullyPaid && (
        <form onSubmit={handleSubmit} className="inline-form">
          <label>
            Amount to pay
            <input type="number" min={1} value={amount} onChange={(e) => setAmount(Number(e.target.value))} required />
          </label>
          <button type="submit" disabled={payMutation.isPending}>
            {payMutation.isPending ? 'Paying...' : 'Make payment'}
          </button>
        </form>
      )}
      {lastResult && <p className="success">{lastResult}</p>}
      {error && <p className="error">{error}</p>}
    </div>
  )
}

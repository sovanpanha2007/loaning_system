import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { api, ApiRequestError } from '../api/client'
import type { StaffResponse } from '../api/types'

export default function StaffPage() {
  const queryClient = useQueryClient()
  const [error, setError] = useState<string | null>(null)
  const [limitEdits, setLimitEdits] = useState<Record<number, string>>({})

  const { data, isLoading, error: loadError } = useQuery({
    queryKey: ['staff'],
    queryFn: () => api.get<StaffResponse[]>('/staff'),
  })

  const deactivateMutation = useMutation({
    mutationFn: (staffId: number) => api.put<StaffResponse>(`/staff/${staffId}/deactivate`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['staff'] }),
    onError: (e) => setError(e instanceof ApiRequestError ? e.message : 'Failed to deactivate staff'),
  })

  const limitMutation = useMutation({
    mutationFn: ({ staffId, newAmount }: { staffId: number; newAmount: number }) =>
      api.put<StaffResponse>(`/staff/${staffId}/approval-limit`, { newAmount }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['staff'] }),
    onError: (e) => setError(e instanceof ApiRequestError ? e.message : 'Failed to update approval limit'),
  })

  return (
    <div>
      <h1>Staff</h1>
      <p>
        <Link to="/staff/new">+ New staff member</Link>
      </p>
      {isLoading && <p>Loading...</p>}
      {loadError && <p className="error">Failed to load staff.</p>}
      {error && <p className="error">{error}</p>}
      {data && (
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Name</th>
              <th>Username</th>
              <th>Position</th>
              <th>Salary</th>
              <th>Approval limit</th>
              <th>Active</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {data.map((s) => (
              <tr key={s.id}>
                <td>{s.id}</td>
                <td>{s.name}</td>
                <td>{s.username}</td>
                <td>{s.position}</td>
                <td>${s.salary.toFixed(2)}</td>
                <td>
                  {s.maxApprovalLimit !== null ? (
                    <>
                      ${s.maxApprovalLimit.toFixed(2)}{' '}
                      <input
                        type="number"
                        placeholder="new limit"
                        style={{ width: '6rem' }}
                        value={limitEdits[s.id] ?? ''}
                        onChange={(e) => setLimitEdits({ ...limitEdits, [s.id]: e.target.value })}
                      />
                      <button
                        disabled={!limitEdits[s.id]}
                        onClick={() => limitMutation.mutate({ staffId: s.id, newAmount: Number(limitEdits[s.id]) })}
                      >
                        Set
                      </button>
                    </>
                  ) : (
                    '-'
                  )}
                </td>
                <td>{s.active ? 'Yes' : 'No'}</td>
                <td>
                  {s.active && (
                    <button onClick={() => deactivateMutation.mutate(s.id)}>Deactivate</button>
                  )}
                </td>
              </tr>
            ))}
            {data.length === 0 && (
              <tr>
                <td colSpan={8}>No staff found.</td>
              </tr>
            )}
          </tbody>
        </table>
      )}
    </div>
  )
}

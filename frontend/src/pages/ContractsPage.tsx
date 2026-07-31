import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { api } from '../api/client'
import type { ContractResponse } from '../api/types'

export default function ContractsPage() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['contracts'],
    queryFn: () => api.get<ContractResponse[]>('/contracts'),
  })

  return (
    <div>
      <h1>All Contracts</h1>
      {isLoading && <p>Loading...</p>}
      {error && <p className="error">Failed to load contracts.</p>}
      {data && (
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Applicant</th>
              <th>Principal</th>
              <th>Total</th>
              <th>Status</th>
              <th>Approving officer</th>
              <th>Votes</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {data.map((c) => (
              <tr key={c.id}>
                <td>{c.id}</td>
                <td>{c.applicantName}</td>
                <td>${c.principalAmount.toFixed(2)}</td>
                <td>${c.totalAmount.toFixed(2)}</td>
                <td>{c.status}</td>
                <td>{c.approvingOfficerName ?? '-'}</td>
                <td>{c.committeeVoteCount}</td>
                <td>
                  <Link to={`/contracts/${c.id}`}>Manage</Link>
                </td>
              </tr>
            ))}
            {data.length === 0 && (
              <tr>
                <td colSpan={8}>No contracts found.</td>
              </tr>
            )}
          </tbody>
        </table>
      )}
    </div>
  )
}

import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { api } from '../api/client'
import type { ContractResponse } from '../api/types'

export default function MyContractsPage() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['contracts', 'mine'],
    queryFn: () => api.get<ContractResponse[]>('/contracts/mine'),
  })

  return (
    <div>
      <h1>My Contracts</h1>
      {isLoading && <p>Loading...</p>}
      {error && <p className="error">Failed to load your contracts.</p>}
      {data && (
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Principal</th>
              <th>Total</th>
              <th>Duration</th>
              <th>Effective APR</th>
              <th>Status</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {data.map((c) => (
              <tr key={c.id}>
                <td>{c.id}</td>
                <td>${c.principalAmount.toFixed(2)}</td>
                <td>${c.totalAmount.toFixed(2)}</td>
                <td>{c.duration} yr(s)</td>
                <td>{(c.effectiveApr * 100).toFixed(2)}%</td>
                <td>{c.status}</td>
                <td>
                  <Link to={`/contracts/${c.id}/schedule`}>Schedule & pay</Link>
                </td>
              </tr>
            ))}
            {data.length === 0 && (
              <tr>
                <td colSpan={7}>You have no contracts yet.</td>
              </tr>
            )}
          </tbody>
        </table>
      )}
    </div>
  )
}

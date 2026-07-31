import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { api } from '../api/client'
import type { ApplicantResponse } from '../api/types'

export default function ApplicantsPage() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['applicants'],
    queryFn: () => api.get<ApplicantResponse[]>('/applicants'),
  })

  return (
    <div>
      <h1>Applicants</h1>
      <p>
        <Link to="/applicants/new">+ New applicant</Link>
      </p>
      {isLoading && <p>Loading...</p>}
      {error && <p className="error">Failed to load applicants.</p>}
      {data && (
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Name</th>
              <th>Username</th>
              <th>Phone</th>
              <th>Balance</th>
              <th>Salary</th>
              <th>Active</th>
            </tr>
          </thead>
          <tbody>
            {data.map((a) => (
              <tr key={a.id}>
                <td>{a.id}</td>
                <td>{a.name}</td>
                <td>{a.username}</td>
                <td>{a.phoneNumber}</td>
                <td>${a.accountBalance.toFixed(2)}</td>
                <td>${a.salary.toFixed(2)}</td>
                <td>{a.active ? 'Yes' : 'No'}</td>
              </tr>
            ))}
            {data.length === 0 && (
              <tr>
                <td colSpan={7}>No applicants found.</td>
              </tr>
            )}
          </tbody>
        </table>
      )}
    </div>
  )
}

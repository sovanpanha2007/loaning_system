import { useQuery } from '@tanstack/react-query'
import { api } from '../api/client'
import type { AuditEntry } from '../api/types'

export default function AuditLogPage() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['audit-log'],
    queryFn: () => api.get<AuditEntry[]>('/audit-log'),
  })

  return (
    <div>
      <h1>Audit Log</h1>
      {isLoading && <p>Loading...</p>}
      {error && <p className="error">Failed to load the audit log.</p>}
      {data && (
        <table>
          <thead>
            <tr>
              <th>When</th>
              <th>Actor</th>
              <th>Action</th>
              <th>Subject</th>
              <th>Details</th>
            </tr>
          </thead>
          <tbody>
            {data.map((entry) => (
              <tr key={entry.id}>
                <td>{entry.occurredAt}</td>
                <td>{entry.actorType ? `${entry.actorType} #${entry.actorId}` : 'system'}</td>
                <td>{entry.action}</td>
                <td>{entry.subjectType ? `${entry.subjectType} #${entry.subjectId}` : '-'}</td>
                <td>{entry.details ?? ''}</td>
              </tr>
            ))}
            {data.length === 0 && (
              <tr>
                <td colSpan={5}>No audit entries found.</td>
              </tr>
            )}
          </tbody>
        </table>
      )}
    </div>
  )
}

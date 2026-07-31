import { Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export default function DashboardPage() {
  const { user } = useAuth()
  if (!user) return null

  return (
    <div>
      <h1>Welcome, {user.name}</h1>
      <p>Role: {user.role}</p>

      {user.role === 'APPLICANT' && (
        <ul>
          <li><Link to="/contracts/mine">View my contracts</Link></li>
          <li><Link to="/contracts/new">Apply for a new loan</Link></li>
          <li><Link to="/profile">View my profile and balance</Link></li>
        </ul>
      )}

      {user.role === 'MANAGER' && (
        <ul>
          <li><Link to="/applicants/new">Create a new applicant</Link></li>
          <li><Link to="/staff/new">Create a new staff member</Link></li>
          <li><Link to="/contracts">Browse all contracts</Link></li>
          <li><Link to="/audit-log">View the audit log</Link></li>
          <li><Link to="/settings">Bank settings</Link></li>
        </ul>
      )}

      {(user.role === 'LOAN_OFFICER' || user.role === 'CREDIT_COMMITTEE') && (
        <ul>
          <li><Link to="/contracts">Review contracts</Link></li>
          {user.role === 'LOAN_OFFICER' && <li><Link to="/contracts/new">Draft a new contract</Link></li>}
        </ul>
      )}
    </div>
  )
}

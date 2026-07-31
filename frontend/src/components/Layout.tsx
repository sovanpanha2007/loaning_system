import { Link, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export default function Layout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  async function handleLogout() {
    await logout()
    navigate('/login')
  }

  const isManager = user?.role === 'MANAGER'
  const isLoanOfficer = user?.role === 'LOAN_OFFICER'
  const isCreditCommittee = user?.role === 'CREDIT_COMMITTEE'
  const isApplicant = user?.role === 'APPLICANT'
  const isStaff = isManager || isLoanOfficer || isCreditCommittee

  return (
    <div className="app-shell">
      <header className="topbar">
        <div className="brand">KH Bank Loaning System</div>
        <nav>
          {isApplicant && (
            <>
              <Link to="/contracts/mine">My Contracts</Link>
              <Link to="/contracts/new">Apply for a Loan</Link>
            </>
          )}
          {isStaff && (
            <>
              <Link to="/contracts">Contracts</Link>
              {(isLoanOfficer) && <Link to="/contracts/new">New Contract</Link>}
            </>
          )}
          {isManager && (
            <>
              <Link to="/applicants">Applicants</Link>
              <Link to="/staff">Staff</Link>
              <Link to="/audit-log">Audit Log</Link>
              <Link to="/settings">Settings</Link>
            </>
          )}
          {user && <Link to="/profile">Profile</Link>}
        </nav>
        <div className="user-info">
          {user && (
            <>
              <span>
                {user.name} ({user.role})
              </span>
              <button onClick={handleLogout}>Logout</button>
            </>
          )}
        </div>
      </header>
      <main className="content">
        <Outlet />
      </main>
    </div>
  )
}

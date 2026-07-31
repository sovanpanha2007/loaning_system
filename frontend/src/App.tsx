import { Routes, Route } from 'react-router-dom'
import Layout from './components/Layout'
import ProtectedRoute from './components/ProtectedRoute'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import ApplicantsPage from './pages/ApplicantsPage'
import NewApplicantPage from './pages/NewApplicantPage'
import StaffPage from './pages/StaffPage'
import NewStaffPage from './pages/NewStaffPage'
import ContractsPage from './pages/ContractsPage'
import MyContractsPage from './pages/MyContractsPage'
import NewContractPage from './pages/NewContractPage'
import ContractDetailPage from './pages/ContractDetailPage'
import SchedulePage from './pages/SchedulePage'
import AuditLogPage from './pages/AuditLogPage'
import SettingsPage from './pages/SettingsPage'
import ProfilePage from './pages/ProfilePage'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        element={
          <ProtectedRoute>
            <Layout />
          </ProtectedRoute>
        }
      >
        <Route path="/" element={<DashboardPage />} />
        <Route path="/profile" element={<ProfilePage />} />

        <Route path="/applicants" element={<ProtectedRoute roles={['MANAGER']}><ApplicantsPage /></ProtectedRoute>} />
        <Route path="/applicants/new" element={<ProtectedRoute roles={['MANAGER']}><NewApplicantPage /></ProtectedRoute>} />

        <Route path="/staff" element={<ProtectedRoute roles={['MANAGER']}><StaffPage /></ProtectedRoute>} />
        <Route path="/staff/new" element={<ProtectedRoute roles={['MANAGER']}><NewStaffPage /></ProtectedRoute>} />

        <Route
          path="/contracts"
          element={<ProtectedRoute roles={['MANAGER', 'LOAN_OFFICER', 'CREDIT_COMMITTEE']}><ContractsPage /></ProtectedRoute>}
        />
        <Route path="/contracts/mine" element={<ProtectedRoute roles={['APPLICANT']}><MyContractsPage /></ProtectedRoute>} />
        <Route
          path="/contracts/new"
          element={<ProtectedRoute roles={['APPLICANT', 'LOAN_OFFICER']}><NewContractPage /></ProtectedRoute>}
        />
        <Route
          path="/contracts/:contractId"
          element={<ProtectedRoute roles={['MANAGER', 'LOAN_OFFICER', 'CREDIT_COMMITTEE']}><ContractDetailPage /></ProtectedRoute>}
        />
        <Route
          path="/contracts/:contractId/schedule"
          element={<ProtectedRoute roles={['APPLICANT']}><SchedulePage /></ProtectedRoute>}
        />

        <Route path="/audit-log" element={<ProtectedRoute roles={['MANAGER']}><AuditLogPage /></ProtectedRoute>} />
        <Route path="/settings" element={<ProtectedRoute roles={['MANAGER']}><SettingsPage /></ProtectedRoute>} />
      </Route>
    </Routes>
  )
}

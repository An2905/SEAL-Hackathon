import { Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { ToastProvider } from './context/ToastContext'
import HomePage from './pages/HomePage'
import RequireRole from './guards/RequireRole'
import StudentDashboard from './pages/dashboards/StudentDashboard'
import StaffLayout from './pages/dashboards/staff/StaffLayout'
import StaffOverviewPage from './pages/dashboards/staff/StaffOverviewPage'
import StaffAccountsPage from './pages/dashboards/staff/StaffAccountsPage'
import StaffAnnouncementsPage from './pages/dashboards/staff/StaffAnnouncementsPage'
import StaffEventsPage from './pages/dashboards/staff/StaffEventsPage'
import StaffAssignPage from './pages/dashboards/staff/StaffAssignPage'
import StaffEventSetupPage from './pages/dashboards/staff/StaffEventSetupPage'
import MentorDashboard from './pages/dashboards/MentorDashboard'
import JudgeDashboard from './pages/dashboards/JudgeDashboard'
import EventDetailsPage from './pages/dashboards/EventDetailsPage'

export default function App() {
  return (
    <AuthProvider>
      <ToastProvider>
        <Routes>
          <Route path='/' element={<HomePage />} />

          <Route
            path='/student'
            element={
              <RequireRole role='Student'>
                <StudentDashboard />
              </RequireRole>
            }
          />

          {/* Staff area — nested pages under a shared layout + sub-navbar */}
          <Route
            path='/staff'
            element={
              <RequireRole role='Staff'>
                <StaffLayout />
              </RequireRole>
            }
          >
            <Route index element={<StaffOverviewPage />} />
            <Route path='accounts' element={<StaffAccountsPage />} />
            <Route path='events' element={<StaffEventsPage />} />
            <Route path='assign' element={<StaffAssignPage />} />
            <Route path='setup' element={<StaffEventSetupPage />} />
            <Route path='announcements' element={<StaffAnnouncementsPage />} />
          </Route>

          {/* Event detail is a standalone full page (own shell) */}
          <Route
            path='/staff/events/:eventId'
            element={
              <RequireRole role='Staff'>
                <EventDetailsPage />
              </RequireRole>
            }
          />

          <Route
            path='/mentor'
            element={
              <RequireRole role='Mentor'>
                <MentorDashboard />
              </RequireRole>
            }
          />

          <Route
            path='/judge'
            element={
              <RequireRole role='Judge'>
                <JudgeDashboard />
              </RequireRole>
            }
          />

          {/* Catch-all */}
          <Route path='*' element={<Navigate to='/' replace />} />
        </Routes>
      </ToastProvider>
    </AuthProvider>
  )
}

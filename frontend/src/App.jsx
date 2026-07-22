import { lazy, Suspense } from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { ToastProvider } from './context/ToastContext'
import HomePage from './pages/HomePage'
import RequireRole from './guards/RequireRole'
import RequireAuth from './guards/RequireAuth'
import LoadingState from './components/common/LoadingState'

const StudentDashboard = lazy(() => import('./pages/dashboards/StudentDashboard'))
const StaffLayout = lazy(() => import('./pages/dashboards/staff/StaffLayout'))
const MentorDashboard = lazy(() => import('./pages/dashboards/MentorDashboard'))
const JudgeDashboard = lazy(() => import('./pages/dashboards/JudgeDashboard'))
const EventDetailsPage = lazy(() => import('./pages/dashboards/EventDetailsPage'))
const StaffCheckInPage = lazy(() => import('./pages/dashboards/staff/StaffCheckInPage'))
const ProfilePage = lazy(() => import('./pages/dashboards/staff/StaffProfilePage'))

function RouteLoading() {
  return <LoadingState className='page-loading' />
}

export default function App() {
  return (
    <AuthProvider>
      <ToastProvider>
        <Suspense fallback={<RouteLoading />}>
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

            {/* Staff area — tabs in StaffLayout; event detail is a nested child */}
            <Route
              path='/staff'
              element={
                <RequireRole role='Staff'>
                  <StaffLayout />
                </RequireRole>
              }
            >
              <Route path='events/:eventId' element={<EventDetailsPage />} />
            </Route>

            <Route
              path='/staff/events/:eventId/check-in'
              element={
                <RequireRole role='Staff'>
                  <StaffCheckInPage />
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

            <Route
              path='/profile'
              element={
                <RequireAuth>
                  <ProfilePage />
                </RequireAuth>
              }
            />

            {/* Catch-all */}
            <Route path='*' element={<Navigate to='/' replace />} />
          </Routes>
        </Suspense>
      </ToastProvider>
    </AuthProvider>
  )
}

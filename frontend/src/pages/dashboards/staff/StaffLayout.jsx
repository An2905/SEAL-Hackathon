import DashboardLayout from '../../../components/layout/DashboardLayout'
import StaffAccountsPage from './StaffAccountsPage'
import StaffEventsPage from './StaffEventsPage'
import StaffAssignPage from './StaffAssignPage'
import StaffUniversitiesPage from './StaffUniversitiesPage'
import StaffFilterEmailPage from './StaffFilterEmailPage'

const TABS = [
  { key: 'events', label: 'Sự kiện', content: <StaffEventsPage /> },
  { key: 'accounts', label: 'Tài khoản', content: <StaffAccountsPage /> },
  { key: 'assign', label: 'Phân công', content: <StaffAssignPage /> },
  { key: 'universities', label: 'Trường ĐH', content: <StaffUniversitiesPage /> },
  { key: 'emails', label: 'Email', content: <StaffFilterEmailPage /> }
]

export default function StaffLayout() {
  return <DashboardLayout roleLabel='Nhân viên' showStaffFields tabs={TABS} />
}

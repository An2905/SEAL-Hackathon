import DashboardLayout from '../../../components/layout/DashboardLayout'
import StaffAccountsPage from './StaffAccountsPage'
import StaffEventsPage from './StaffEventsPage'
import StaffAssignPage from './StaffAssignPage'
import StaffUniversitiesPage from './StaffUniversitiesPage'

const TABS = [
  { key: 'events', label: 'Sự kiện', content: <StaffEventsPage /> },
  { key: 'accounts', label: 'Tài khoản', content: <StaffAccountsPage /> },
  { key: 'assign', label: 'Phân công', content: <StaffAssignPage /> },
  { key: 'universities', label: 'Trường ĐH', content: <StaffUniversitiesPage /> }
]

export default function StaffLayout() {
  return <DashboardLayout roleLabel='Nhân viên' showStaffFields tabs={TABS} />
}

import DashboardLayout from '../../../components/layout/DashboardLayout'
import StaffOverviewPage from './StaffOverviewPage'
import StaffAccountsPage from './StaffAccountsPage'
import StaffEventsPage from './StaffEventsPage'
import StaffAssignPage from './StaffAssignPage'
import StaffAnnouncementsPage from './StaffAnnouncementsPage'
import StaffUniversitiesPage from './StaffUniversitiesPage'

const TABS = [
  { key: 'overview', label: 'Tổng quan', content: <StaffOverviewPage /> },
  { key: 'events', label: 'Sự kiện', content: <StaffEventsPage /> },
  { key: 'accounts', label: 'Tài khoản', content: <StaffAccountsPage /> },
  { key: 'assign', label: 'Phân công', content: <StaffAssignPage /> },
  { key: 'announcements', label: 'Thông báo', content: <StaffAnnouncementsPage /> },
  { key: 'universities', label: 'Trường ĐH', content: <StaffUniversitiesPage /> }
]

export default function StaffLayout() {
  return <DashboardLayout roleLabel='Nhân viên' showStaffFields tabs={TABS} />
}

import { useOutlet, useSearchParams } from 'react-router-dom'
import DashboardLayout from '../../../components/layout/DashboardLayout'
import StaffAccountsPage from './StaffAccountsPage'
import StaffEventsPage from './StaffEventsPage'
import StaffAssignPage from './StaffAssignPage'
import StaffUniversitiesPage from './StaffUniversitiesPage'
import StaffFilterEmailPage from './StaffFilterEmailPage'

const TAB_CONTENT = {
  events: <StaffEventsPage />,
  accounts: <StaffAccountsPage />,
  assign: <StaffAssignPage />,
  universities: <StaffUniversitiesPage />,
  emails: <StaffFilterEmailPage />
}

/**
 * Staff area content. Chrome (TopBar + staff TabNav) comes from MainLayout.
 * Tab selection is driven by `?tab=` / nested `/staff/events/:eventId`.
 */
export default function StaffLayout() {
  const nestedPage = useOutlet()
  const [searchParams] = useSearchParams()

  if (nestedPage) {
    return (
      <DashboardLayout moduleTitle='Chi tiết sự kiện' moduleSubtitle='Thông tin đầy đủ của hackathon.'>
        {nestedPage}
      </DashboardLayout>
    )
  }

  const tabKey = searchParams.get('tab') || 'events'
  const content = TAB_CONTENT[tabKey] ?? TAB_CONTENT.events

  return <DashboardLayout>{content}</DashboardLayout>
}

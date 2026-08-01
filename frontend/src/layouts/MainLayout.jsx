import { Outlet, useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import Navbar from '../components/Navbar/Navbar'
import SiteFooter from '../components/layout/SiteFooter'
import { useAuth } from '../context/AuthContext'
import { vietnameseRoleLabel, STUDENT_ROLES } from '../utils/roleLabels'

const STAFF_TABS = [
  { key: 'events', label: 'Sự kiện' },
  { key: 'accounts', label: 'Tài khoản' },
  { key: 'assign', label: 'Phân công' },
  { key: 'universities', label: 'Trường ĐH' },
  { key: 'emails', label: 'Email' }
]

function shellClassName(pathname) {
  if (pathname.startsWith('/student')) return 'dashboard-shell--student-zone'
  if (pathname.startsWith('/mentor')) return 'dashboard-shell--mentor-zone'
  return ''
}

/**
 * Layout for every authenticated app page (not the public landing `/`).
 * Renders the shared Navbar once; child routes render through <Outlet />.
 */
export default function MainLayout() {
  const { auth } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()

  const isStudent = STUDENT_ROLES.includes(auth.role)
  const onStaffArea = location.pathname.startsWith('/staff')
  const onStaffEventSubPage = /^\/staff\/events\//.test(location.pathname)

  const staffActiveKey = onStaffEventSubPage ? 'events' : searchParams.get('tab') || 'events'

  const handleStaffTabChange = (key) => {
    if (key === 'events') navigate('/staff')
    else navigate(`/staff?tab=${encodeURIComponent(key)}`)
  }

  const zoneClass = shellClassName(location.pathname)

  return (
    <div className={`dashboard-shell${zoneClass ? ` ${zoneClass}` : ''}`}>
      <Navbar
        roleLabel={vietnameseRoleLabel(auth.role)}
        showStudentFields={isStudent}
        showStaffFields={!isStudent}
        onNavigateProfile={() => navigate('/profile')}
        tabs={onStaffArea ? STAFF_TABS : undefined}
        activeKey={onStaffArea ? staffActiveKey : undefined}
        onChange={onStaffArea ? handleStaffTabChange : undefined}
      />

      <Outlet />

      <SiteFooter />
    </div>
  )
}

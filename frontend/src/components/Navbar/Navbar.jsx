import TopBar from '../layout/TopBar'
import TabNav from '../dashboard/TabNav'

/**
 * App chrome for authenticated pages: sticky TopBar + optional TabNav.
 * Tab highlighting is driven by activeKey (from useLocation / search params in the parent).
 */
export default function Navbar({
  roleLabel,
  onNavigateProfile,
  showStudentFields = false,
  showStaffFields = false,
  tabs,
  activeKey,
  onChange
}) {
  return (
    <>
      <TopBar
        roleLabel={roleLabel}
        onNavigateProfile={onNavigateProfile}
        showStudentFields={showStudentFields}
        showStaffFields={showStaffFields}
      />
      <TabNav tabs={tabs} activeKey={activeKey} onChange={onChange} />
    </>
  )
}

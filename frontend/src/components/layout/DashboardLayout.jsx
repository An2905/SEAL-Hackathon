import { useSearchParams } from 'react-router-dom'
import TopBar from './TopBar'
import SiteFooter from './SiteFooter'
import ModuleContainer from '../dashboard/ModuleContainer'
import DashboardHeader from '../dashboard/DashboardHeader'
import TabNav from '../dashboard/TabNav'

/**
 * DashboardLayout
 *
 * Shared shell for all role dashboards: TopBar + optional TabNav +
 * ModuleContainer (optional DashboardHeader + active tab content) + SiteFooter.
 *
 * Pass `tabs` (array of `{ key, label, content }`) for multi-tab dashboards,
 * or omit it and pass `children` directly for a single-tab dashboard.
 *
 * For multi-tab dashboards, the active tab is persisted in the URL as
 * `?tab=<key>` via useSearchParams, so the back/forward buttons and
 * shareable links work as expected.
 */
export default function DashboardLayout({
  roleLabel,
  moduleTitle,
  moduleSubtitle,
  showStudentFields = false,
  showStaffFields = false,
  className = '',
  tabs,
  children
}) {
  const tabList = tabs?.length ? tabs : [{ key: '_root', label: '', content: children }]
  const [searchParams, setSearchParams] = useSearchParams()

  const requestedTab = searchParams.get('tab')
  const activeTab = tabList.some((tab) => tab.key === requestedTab) ? requestedTab : tabList[0].key

  const setActiveTab = (key) => {
    const next = new URLSearchParams(searchParams)
    if (key === tabList[0].key) next.delete('tab')
    else next.set('tab', key)
    setSearchParams(next)
  }

  const active = tabList.find((tab) => tab.key === activeTab) || tabList[0]
  const hasProfileTab = tabList.some((tab) => tab.key === 'profile')

  return (
    <div className={`dashboard-shell${className ? ` ${className}` : ''}`}>
      <TopBar
        roleLabel={roleLabel}
        onNavigateProfile={hasProfileTab ? () => setActiveTab('profile') : undefined}
        showStudentFields={showStudentFields}
        showStaffFields={showStaffFields}
      />

      <TabNav tabs={tabList} activeKey={activeTab} onChange={setActiveTab} />

      <ModuleContainer>
        <DashboardHeader title={moduleTitle} subtitle={moduleSubtitle} />
        {active.content}
      </ModuleContainer>

      <SiteFooter />
    </div>
  )
}

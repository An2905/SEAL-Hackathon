import { useSearchParams, useLocation, useNavigate } from 'react-router-dom'
import TopBar from './TopBar'
import SiteFooter from './SiteFooter'
import ModuleContainer from '../dashboard/ModuleContainer'
import DashboardHeader from '../dashboard/DashboardHeader'
import TabNav from '../dashboard/TabNav'

function RoleSwitcher({ navLinks }) {
  const location = useLocation()
  const navigate = useNavigate()
  if (!navLinks?.length) return null
  return (
    <div
      style={{
        marginBottom: 20,
        display: 'inline-flex',
        background: 'var(--surface-alt,#f1f5f9)',
        borderRadius: 10,
        padding: 4,
        gap: 2
      }}
    >
      {navLinks.map((link) => {
        const active = location.pathname === link.to
        return (
          <button
            key={link.to}
            type='button'
            onClick={() => !active && navigate(link.to)}
            style={{
              padding: '6px 20px',
              borderRadius: 7,
              border: 'none',
              fontSize: 13,
              fontWeight: active ? 600 : 400,
              cursor: active ? 'default' : 'pointer',
              background: active ? 'var(--card-bg,#fff)' : 'transparent',
              color: active ? 'var(--accent,#2563eb)' : 'var(--text-dim,#64748b)',
              boxShadow: active ? '0 1px 4px rgba(0,0,0,0.1)' : 'none',
              transition: 'color .15s, background .15s, box-shadow .15s'
            }}
          >
            {link.label}
          </button>
        )
      })}
    </div>
  )
}

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
 *
 * Optional nested-route support (e.g. staff event detail):
 *   forcedTabKey     — highlight this tab while showing overrideContent
 *   overrideContent  — render instead of the active tab's content
 *   onForcedTabChange — called when a tab is clicked while forcedTabKey is set
 *                       (so the host can navigate off the nested route)
 */
export default function DashboardLayout({
  roleLabel,
  moduleTitle,
  moduleSubtitle,
  showStudentFields = false,
  showStaffFields = false,
  className = '',
  tabs,
  navLinks,
  children,
  forcedTabKey,
  overrideContent,
  onForcedTabChange
}) {
  const tabList = tabs?.length ? tabs : [{ key: '_root', label: '', content: children }]
  const [searchParams, setSearchParams] = useSearchParams()

  const requestedTab = searchParams.get('tab')
  const activeTab = forcedTabKey
    ? forcedTabKey
    : tabList.some((tab) => tab.key === requestedTab)
      ? requestedTab
      : tabList[0].key

  const setActiveTab = (key) => {
    if (forcedTabKey && onForcedTabChange) {
      onForcedTabChange(key)
      return
    }
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
        <RoleSwitcher navLinks={navLinks} />
        {overrideContent ?? active.content}
      </ModuleContainer>

      <SiteFooter />
    </div>
  )
}

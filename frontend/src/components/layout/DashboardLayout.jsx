import { useSearchParams, useLocation, useNavigate } from 'react-router-dom'
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
 * DashboardLayout — page content under MainLayout.
 *
 * TopBar / SiteFooter live in MainLayout. This component only renders
 * optional role TabNav (e.g. student) + ModuleContainer content.
 *
 * For multi-tab dashboards, the active tab is persisted as `?tab=<key>`.
 */
export default function DashboardLayout({
  moduleTitle,
  moduleSubtitle,
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
  const showTabNav = tabList.length > 1 && tabList.some((tab) => tab.label)

  return (
    <>
      {showTabNav && <TabNav tabs={tabList} activeKey={activeTab} onChange={setActiveTab} />}

      <ModuleContainer>
        <DashboardHeader title={moduleTitle} subtitle={moduleSubtitle} />
        <RoleSwitcher navLinks={navLinks} />
        {overrideContent ?? active.content}
      </ModuleContainer>
    </>
  )
}

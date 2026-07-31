import { NavLink } from 'react-router-dom'

/**
 * Horizontal tab bar under TopBar.
 *
 * Prefer tabs with `to` (NavLink + route-based active state).
 * Fallback: controlled `activeKey` + `onChange` for query-param tabs.
 */
export default function TabNav({ tabs, activeKey, onChange }) {
  if (!tabs || tabs.length <= 1) return null
  if (!tabs.some((tab) => tab.label)) return null

  const useLinks = tabs.every((tab) => tab.to)

  return (
    <div style={tabBarWrapStyle}>
      <div style={tabBarInnerStyle}>
        {tabs.map((tab) =>
          useLinks ? (
            <NavLink key={tab.key} to={tab.to} end={tab.end} style={({ isActive }) => tabBtnStyle(isActive)}>
              {tab.label}
            </NavLink>
          ) : (
            <button
              key={tab.key}
              type='button'
              onClick={() => onChange?.(tab.key)}
              style={tabBtnStyle(activeKey === tab.key)}
            >
              {tab.label}
            </button>
          )
        )}
      </div>
    </div>
  )
}

// ─── Styles ───────────────────────────────────────────────────────────────────

const tabBarWrapStyle = {
  borderBottom: '1px solid var(--border-soft,#f0f0f0)',
  background: 'var(--card-bg,#fff)',
  position: 'sticky',
  top: 56, // height of TopBar
  zIndex: 9
}

const tabBarInnerStyle = {
  maxWidth: 1200,
  margin: '0 auto',
  padding: '0 24px',
  display: 'flex',
  gap: 0,
  overflowX: 'auto',
  scrollbarWidth: 'none', // Firefox
  msOverflowStyle: 'none' // IE
}

const tabBtnStyle = (active) => ({
  padding: '12px 16px',
  background: 'transparent',
  border: 'none',
  borderBottom: active ? '2px solid var(--accent,#2563eb)' : '2px solid transparent',
  color: active ? 'var(--accent,#2563eb)' : 'var(--text-dim,#4a5568)',
  fontWeight: active ? 600 : 400,
  fontSize: 13,
  cursor: 'pointer',
  whiteSpace: 'nowrap',
  transition: 'color .15s, border-color .15s',
  flexShrink: 0,
  textDecoration: 'none'
})

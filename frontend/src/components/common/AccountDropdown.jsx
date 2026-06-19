import { useCallback, useEffect, useRef, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { getGithubLinkStatus, getGithubLinkUrl } from '../../api/auth'
import { useAuth } from '../../context/AuthContext'
import { useToast } from '../../context/ToastContext'
import { localizeError } from '../../utils/errors'
import { STUDENT_ROLES } from '../../utils/roleLabels'
import Avatar from './Avatar'

/**
 * AccountDropdown
 *
 * Unified account widget: [Avatar] Name / Role ▼. Opens a single menu with
 * Trang làm việc, Hồ sơ của tôi, Thông báo, Đăng xuất —
 * replacing any separate role-shortcut / logout buttons.
 */
export default function AccountDropdown({ roleLabel, onNavigateProfile }) {
  const { auth, clearAuth, pathForRole } = useAuth()
  const { showToast } = useToast()
  const navigate = useNavigate()
  const location = useLocation()

  const [open, setOpen] = useState(false)
  const [githubLinked, setGithubLinked] = useState(null)
  const [oauthLoading, setOauthLoading] = useState(false)
  const ref = useRef(null)

  const displayName = auth.fullName || auth.email || 'User'
  const isStudent = STUDENT_ROLES.includes(auth.role)

  const loadGithubStatus = useCallback(async () => {
    if (!isStudent) {
      setGithubLinked(null)
      return
    }
    try {
      const status = await getGithubLinkStatus()
      setGithubLinked(status.githubLinked)
    } catch {
      setGithubLinked(false)
    }
  }, [isStudent])

  useEffect(() => {
    loadGithubStatus()
  }, [loadGithubStatus])

  useEffect(() => {
    if (!isStudent) return
    const status = new URLSearchParams(location.search).get('github_oauth')
    if (status === 'success') loadGithubStatus()
  }, [location.search, isStudent, loadGithubStatus])

  useEffect(() => {
    if (open && isStudent) loadGithubStatus()
  }, [open, isStudent, loadGithubStatus])

  useEffect(() => {
    const handler = (e) => {
      if (ref.current && !ref.current.contains(e.target)) setOpen(false)
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [])

  useEffect(() => {
    if (!open) return
    const handler = (e) => {
      if (e.key === 'Escape') setOpen(false)
    }
    document.addEventListener('keydown', handler)
    return () => document.removeEventListener('keydown', handler)
  }, [open])

  const handleLogout = () => {
    setOpen(false)
    clearAuth()
    showToast('Đã đăng xuất', 'success')
    navigate('/')
  }

  const handleProfile = () => {
    setOpen(false)
    if (onNavigateProfile) onNavigateProfile()
    else navigate('/profile')
  }

  const handleWorkspace = () => {
    setOpen(false)
    navigate(pathForRole(auth.role))
  }

  const handleConnectGithub = async () => {
    setOpen(false)
    setOauthLoading(true)
    try {
      const authorizeUrl = await getGithubLinkUrl()
      window.location.href = authorizeUrl
    } catch (err) {
      showToast(localizeError(err.message), 'error')
      setOauthLoading(false)
    }
  }

  const showGithubLink = isStudent && githubLinked === false

  return (
    <div ref={ref} style={{ position: 'relative' }}>
      {/* ── Trigger: [Avatar] Name / Role ▼ ── */}
      <button
        type='button'
        onClick={() => setOpen((v) => !v)}
        style={triggerStyle}
        onMouseEnter={(e) => (e.currentTarget.style.background = 'var(--surface-alt, #f7f8fa)')}
        onMouseLeave={(e) => (e.currentTarget.style.background = 'transparent')}
        aria-haspopup='true'
        aria-expanded={open}
        aria-label='Menu tài khoản'
      >
        <Avatar name={displayName} avatarUrl={auth.avatarUrl} size={28} />
        <span style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-start', gap: 0 }}>
          <span style={{ fontSize: 13, fontWeight: 600, color: 'var(--text, #1a202c)', lineHeight: 1.3 }}>
            {displayName}
          </span>
          <span style={{ fontSize: 11, color: 'var(--text-dim, #718096)', lineHeight: 1.3 }}>{roleLabel}</span>
        </span>
        <svg
          width='12'
          height='12'
          viewBox='0 0 12 12'
          fill='none'
          style={{
            flexShrink: 0,
            opacity: 0.4,
            transform: open ? 'rotate(180deg)' : 'none',
            transition: 'transform .15s',
            marginLeft: 2
          }}
        >
          <path d='M2 4l4 4 4-4' stroke='currentColor' strokeWidth='1.5' strokeLinecap='round' strokeLinejoin='round' />
        </svg>
      </button>

      {/* ── Dropdown panel ── */}
      {open && (
        <div style={dropdownStyle} role='menu' aria-label='Menu tài khoản'>
          {/* Header block */}
          <div className='account-dropdown-header'>
            <Avatar name={displayName} avatarUrl={auth.avatarUrl} size={36} fontSize={14} />
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontWeight: 600, fontSize: 13, color: 'var(--text, #1a202c)' }}>{displayName}</div>
              <div style={{ fontSize: 12, color: 'var(--text-dim, #718096)', marginTop: 1 }}>{roleLabel}</div>
            </div>
          </div>

          <div style={sepStyle} />

          <DropdownItem icon={<IconBriefcase />} label='Trang làm việc' onClick={handleWorkspace} />
          <DropdownItem icon={<IconUser />} label='Hồ sơ của tôi' onClick={handleProfile} />
          {showGithubLink && (
            <DropdownItem
              icon={<IconGithub />}
              label={oauthLoading ? 'Đang chuyển hướng...' : 'Liên kết GitHub'}
              onClick={handleConnectGithub}
              disabled={oauthLoading}
              alert
            />
          )}
          <DropdownItem icon={<IconBell />} label='Thông báo' badge='Sắp ra mắt' disabled />

          <div style={sepStyle} />

          <DropdownItem icon={<IconLogout />} label='Đăng xuất' danger onClick={handleLogout} />
        </div>
      )}
    </div>
  )
}

// ─── Sub-components ────────────────────────────────────────────────────────────

function GithubAlertBadge({ className = '', title = 'Chưa liên kết GitHub' }) {
  return (
    <span className={`github-alert-badge ${className}`.trim()} title={title} aria-hidden='true'>
      !
    </span>
  )
}

function DropdownItem({ icon, label, onClick, danger, disabled, badge, alert }) {
  const [hovered, setHovered] = useState(false)

  if (disabled) {
    return (
      <div role='menuitem' aria-disabled='true' style={dropdownItemInertStyle}>
        <span style={{ opacity: 0.5, display: 'flex', alignItems: 'center' }}>{icon}</span>
        <span style={{ flex: 1 }}>{label}</span>
        {alert && <GithubAlertBadge />}
        {badge && <span className='card-badge'>{badge}</span>}
      </div>
    )
  }
  return (
    <button
      type='button'
      role='menuitem'
      onClick={onClick}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: 10,
        width: '100%',
        padding: '9px 14px',
        background: hovered ? (danger ? 'var(--danger-soft,#fff5f5)' : 'var(--surface-alt,#f7f8fa)') : 'transparent',
        border: 'none',
        cursor: 'pointer',
        fontSize: 13,
        color: danger ? 'var(--danger,#e53e3e)' : 'var(--text,#1a202c)',
        textAlign: 'left',
        transition: 'background .12s'
      }}
    >
      <span style={{ opacity: 0.6, display: 'flex', alignItems: 'center' }}>{icon}</span>
      <span style={{ flex: 1 }}>{label}</span>
      {alert && <GithubAlertBadge />}
      {badge && <span className='card-badge'>{badge}</span>}
    </button>
  )
}

// ─── Styles ───────────────────────────────────────────────────────────────────

const triggerStyle = {
  display: 'flex',
  alignItems: 'center',
  gap: 8,
  padding: '5px 10px 5px 6px',
  background: 'transparent',
  border: '1px solid var(--border,#e2e8f0)',
  borderRadius: 8,
  cursor: 'pointer',
  transition: 'background .15s'
}

const dropdownStyle = {
  position: 'absolute',
  top: 'calc(100% + 8px)',
  right: 0,
  width: 'min(220px, calc(100vw - 24px))',
  background: 'var(--card-bg,#fff)',
  border: '1px solid var(--border,#e2e8f0)',
  borderRadius: 'var(--radius-lg)',
  boxShadow: 'var(--shadow-md)',
  zIndex: 200,
  overflow: 'hidden'
}

const sepStyle = {
  height: 1,
  background: 'var(--border-soft,#f0f0f0)',
  margin: '4px 0'
}

const dropdownItemInertStyle = {
  display: 'flex',
  alignItems: 'center',
  gap: 10,
  width: '100%',
  padding: '9px 14px',
  fontSize: 13,
  color: 'var(--text-dim,#a0aec0)',
  cursor: 'default'
}

// Icons
function IconUser() {
  return (
    <svg
      width='15'
      height='15'
      viewBox='0 0 24 24'
      fill='none'
      stroke='currentColor'
      strokeWidth='1.8'
      strokeLinecap='round'
      strokeLinejoin='round'
    >
      <path d='M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2' />
      <circle cx='12' cy='7' r='4' />
    </svg>
  )
}
function IconBriefcase() {
  return (
    <svg
      width='15'
      height='15'
      viewBox='0 0 24 24'
      fill='none'
      stroke='currentColor'
      strokeWidth='1.8'
      strokeLinecap='round'
      strokeLinejoin='round'
    >
      <rect x='2' y='7' width='20' height='14' rx='2' ry='2' />
      <path d='M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16' />
    </svg>
  )
}
function IconBell() {
  return (
    <svg
      width='15'
      height='15'
      viewBox='0 0 24 24'
      fill='none'
      stroke='currentColor'
      strokeWidth='1.8'
      strokeLinecap='round'
      strokeLinejoin='round'
    >
      <path d='M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9' />
      <path d='M13.73 21a2 2 0 0 1-3.46 0' />
    </svg>
  )
}
function IconGithub() {
  return (
    <svg width='15' height='15' viewBox='0 0 24 24' fill='currentColor' aria-hidden='true'>
      <path d='M12 0C5.37 0 0 5.37 0 12c0 5.31 3.44 9.8 8.2 11.39.6.11.82-.26.82-.58 0-.29-.01-1.05-.02-2.06-3.34.73-4.04-1.61-4.04-1.61-.55-1.38-1.34-1.75-1.34-1.75-1.09-.75.08-.74.08-.74 1.21.08 1.84 1.24 1.84 1.24 1.07 1.85 2.81 1.31 3.49 1 .11-.78.42-1.31.76-1.61-2.67-.31-5.47-1.34-5.47-5.93 0-1.31.47-2.38 1.24-3.22-.12-.31-.54-1.56.12-3.25 0 0 1.01-.32 3.3 1.23a11.5 11.5 0 0 1 6 0c2.28-1.55 3.29-1.23 3.29-1.23.66 1.69.24 2.94.12 3.25.77.84 1.24 1.91 1.24 3.22 0 4.6-2.81 5.62-5.49 5.92.43.38.82 1.11.82 2.24 0 1.62-.02 2.92-.02 3.32 0 .32.21.69.83.57C20.57 21.79 24 17.31 24 12c0-6.63-5.37-12-12-12z' />
    </svg>
  )
}
function IconLogout() {
  return (
    <svg
      width='15'
      height='15'
      viewBox='0 0 24 24'
      fill='none'
      stroke='currentColor'
      strokeWidth='1.8'
      strokeLinecap='round'
      strokeLinejoin='round'
    >
      <path d='M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4' />
      <polyline points='16 17 21 12 16 7' />
      <line x1='21' y1='12' x2='9' y2='12' />
    </svg>
  )
}

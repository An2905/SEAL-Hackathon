import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { useToast } from '../../context/ToastContext'
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

  const [open, setOpen] = useState(false)
  const ref = useRef(null)

  const displayName = auth.fullName || auth.email || 'User'

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
          <div style={dropdownHeaderStyle}>
            <Avatar name={displayName} avatarUrl={auth.avatarUrl} size={36} fontSize={14} />
            <div>
              <div style={{ fontWeight: 600, fontSize: 13, color: 'var(--text, #1a202c)' }}>{displayName}</div>
              <div style={{ fontSize: 12, color: 'var(--text-dim, #718096)', marginTop: 1 }}>{roleLabel}</div>
            </div>
          </div>

          <div style={sepStyle} />

          <DropdownItem icon={<IconBriefcase />} label='Trang làm việc' onClick={handleWorkspace} />
          <DropdownItem icon={<IconUser />} label='Hồ sơ của tôi' onClick={handleProfile} />
          <DropdownItem icon={<IconBell />} label='Thông báo' badge='Sắp ra mắt' disabled />

          <div style={sepStyle} />

          <DropdownItem icon={<IconLogout />} label='Đăng xuất' danger onClick={handleLogout} />
        </div>
      )}
    </div>
  )
}

// ─── Sub-components ────────────────────────────────────────────────────────────

function DropdownItem({ icon, label, onClick, danger, disabled, badge }) {
  const [hovered, setHovered] = useState(false)

  if (disabled) {
    return (
      <div role='menuitem' aria-disabled='true' style={dropdownItemInertStyle}>
        <span style={{ opacity: 0.5, display: 'flex', alignItems: 'center' }}>{icon}</span>
        <span style={{ flex: 1 }}>{label}</span>
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

const dropdownHeaderStyle = {
  display: 'flex',
  alignItems: 'center',
  gap: 10,
  padding: '12px 14px',
  background: 'var(--surface-alt,#f7f8fa)'
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

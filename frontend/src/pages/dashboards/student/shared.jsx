export const PAGE_SIZE = 5

export const REGISTRATION_FILTERS = [
  { value: 'APPROVED', label: 'Đã duyệt' },
  { value: 'PENDING', label: 'Chờ duyệt' },
  { value: 'ALL', label: 'Tất cả' }
]

export function formatDateTime(value) {
  if (!value) return '—'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return String(value)
  return d.toLocaleString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

export function registrationStatusPillClass(status) {
  const key = (status || '').toUpperCase()
  if (key === 'APPROVED') return 'status-active'
  if (key === 'PENDING') return 'status-pending'
  if (key === 'REJECTED') return 'status-rejected'
  return 'status-default'
}

export function registrationStatusLabel(status) {
  const key = (status || '').toUpperCase()
  if (key === 'APPROVED') return 'Đã duyệt'
  if (key === 'PENDING') return 'Chờ duyệt'
  if (key === 'REJECTED') return 'Từ chối'
  return status || '—'
}

export function eventStatusPillClass(status) {
  const key = (status || '').toUpperCase()
  if (key === 'ONGOING') return 'status-active status-ongoing-highlight'
  if (key === 'BUILDING') return 'status-pending'
  if (key === 'UPCOMING') return 'status-pending'
  if (key === 'COMPLETED') return 'status-default'
  if (key === 'CANCELLED') return 'status-inactive'
  return 'status-default'
}

export function hasOngoingOrUpcomingRegistration(registrations) {
  return registrations.some((reg) => {
    const key = (reg.eventStatus || '').toUpperCase()
    return key === 'ONGOING' || key === 'UPCOMING'
  })
}

export function DashboardSection({ title, hint, children, spaced = false }) {
  return (
    <>
      <div className={`section-title${spaced ? ' section-title--spaced' : ''}`}>
        <h2>{title}</h2>
        {hint ? <span className='hint'>{hint}</span> : null}
      </div>
      {children}
    </>
  )
}

export function DetailMeta({ children, muted = false }) {
  return <div className={`student-meta-line${muted ? ' student-meta-line--muted' : ''}`}>{children}</div>
}

export function StatusStack({ children }) {
  return <div className='student-status-stack'>{children}</div>
}

export function StatusBadge({ status, className }) {
  return (
    <span className='status-picker' style={{ flexShrink: 0 }}>
      <span className={`status-pill ${className || eventStatusPillClass(status)}`} style={{ cursor: 'default' }}>
        {status || '—'}
      </span>
    </span>
  )
}

export function FilterTabs({ options, value, onChange }) {
  return (
    <div className='dashboard-filter-tabs'>
      {options.map((opt) => (
        <button
          key={opt.value}
          type='button'
          className={`btn ${value === opt.value ? 'btn-primary' : 'btn-outline'}`}
          onClick={() => onChange(opt.value)}
        >
          {opt.label}
        </button>
      ))}
    </div>
  )
}

export function IconGithub({ size = 20 }) {
  return (
    <svg width={size} height={size} viewBox='0 0 24 24' fill='currentColor' aria-hidden='true'>
      <path d='M12 0C5.37 0 0 5.37 0 12c0 5.31 3.44 9.8 8.2 11.39.6.11.82-.26.82-.58 0-.29-.01-1.05-.02-2.06-3.34.73-4.04-1.61-4.04-1.61-.55-1.38-1.34-1.75-1.34-1.75-1.09-.75.08-.74.08-.74 1.21.08 1.84 1.24 1.84 1.24 1.07 1.85 2.81 1.31 3.49 1 .11-.78.42-1.31.76-1.61-2.67-.31-5.47-1.34-5.47-5.93 0-1.31.47-2.38 1.24-3.22-.12-.31-.54-1.56.12-3.25 0 0 1.01-.32 3.3 1.23a11.5 11.5 0 0 1 6 0c2.28-1.55 3.29-1.23 3.29-1.23.66 1.69.24 2.94.12 3.25.77.84 1.24 1.91 1.24 3.22 0 4.6-2.81 5.62-5.49 5.92.43.38.82 1.11.82 2.24 0 1.62-.02 2.92-.02 3.32 0 .32.21.69.83.57C20.57 21.79 24 17.31 24 12c0-6.63-5.37-12-12-12z' />
    </svg>
  )
}

export function GithubRequiredBanner({ onConnect, loading, isWarning }) {
  return (
    <div className={`github-required-banner${isWarning ? ' github-required-banner--warning' : ''}`} role='status'>
      <div className='github-required-banner__icon' aria-hidden='true'>
        <IconGithub size={22} />
      </div>
      <div className='github-required-banner__body'>
        <p className='github-required-banner__title'>
          {isWarning ? 'Yêu cầu liên kết lại GitHub' : 'Liên kết GitHub để tham gia đội'}
        </p>
        <p className='github-required-banner__text'>
          {isWarning
            ? 'Tài khoản GitHub của bạn chưa được liên kết hoặc đã bị hủy liên kết do thay đổi username. Vui lòng liên kết lại trước khi check-in.'
            : 'Tạo hoặc tham gia đội yêu cầu tài khoản GitHub đã xác thực. Liên kết một lần để tiếp tục.'}
        </p>
      </div>
      <button
        type='button'
        className='btn github-connect-btn github-required-banner__action'
        onClick={() => onConnect()}
        disabled={loading}
      >
        <span className='github-connect-btn-content'>
          <IconGithub size={16} />
          {loading ? 'Đang chuyển hướng...' : isWarning ? 'Liên kết GitHub' : 'Kết nối ngay'}
        </span>
      </button>
    </div>
  )
}

export function GithubLinkedBadge({ username }) {
  if (!username) return null
  return (
    <div className='github-linked-badge'>
      <IconGithub size={14} />
      <span>
        GitHub đã liên kết · <strong>@{username}</strong>
      </span>
    </div>
  )
}

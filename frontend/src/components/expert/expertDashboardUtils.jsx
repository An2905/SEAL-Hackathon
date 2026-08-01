import { eventStatusLabel } from '../../utils/eventStatusLabels'

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

export function eventStatusPillClass(status) {
  const key = (status || '').toUpperCase()
  if (key === 'BUILDING' || key === 'UPCOMING') return 'status-pending'
  if (key === 'ONGOING') return 'status-active'
  if (key === 'COMPLETED') return 'status-default'
  if (key === 'CANCELLED') return 'status-rejected'
  return 'status-default'
}

export function StatusBadge({ status }) {
  return (
    <span className='status-picker' style={{ flexShrink: 0 }}>
      <span className={`status-pill ${eventStatusPillClass(status)}`} style={{ cursor: 'default' }}>
        {eventStatusLabel(status)}
      </span>
    </span>
  )
}

export function assignmentKey(a) {
  return `${a.eventId}-${a.roundId}-${a.groupId}`
}

export function assignmentLabel(a) {
  return [a.eventTitle, a.roundName, a.groupName].filter(Boolean).join(' · ')
}

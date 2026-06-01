import { useEffect, useState } from 'react'
import DashboardShell from './DashboardShell'
import { getAssignedEvents, getAssignedCurrentRounds } from '../../api/mentor'
import { useAuth } from '../../context/AuthContext'
import { useToast } from '../../context/ToastContext'
import { localizeError } from '../../utils/errors'

function formatDateTime(value) {
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

function roundStatusPillClass(status) {
  const key = (status || '').toUpperCase()
  if (key === 'ONGOING') return 'status-active'
  if (key === 'UPCOMING') return 'status-pending'
  if (key === 'COMPLETED') return 'status-default'
  return 'status-default'
}

export default function MentorDashboard() {
  const { auth } = useAuth()
  const { showToast } = useToast()

  const [events, setEvents] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const [rounds, setRounds] = useState([])
  const [loadingRounds, setLoadingRounds] = useState(true)
  const [errorRounds, setErrorRounds] = useState(null)

  useEffect(() => {
    let cancelled = false

    ;(async () => {
      const [evResult, rdResult] = await Promise.allSettled([getAssignedEvents(), getAssignedCurrentRounds()])
      if (cancelled) return

      if (evResult.status === 'fulfilled') {
        setEvents(evResult.value)
      } else {
        setError(localizeError(evResult.reason?.message))
        showToast('Không tải được sự kiện được phân công', 'error')
      }
      setLoading(false)

      if (rdResult.status === 'fulfilled') {
        setRounds(rdResult.value)
      } else {
        setErrorRounds(localizeError(rdResult.reason?.message))
      }
      setLoadingRounds(false)
    })()

    return () => {
      cancelled = true
    }
  }, [showToast])

  return (
    <DashboardShell
      roleLabel='Mentor'
      title='Tài khoản Mentor'
      subtitle='Bảng điều khiển dành cho Mentor — đồng hành cùng các đội thí sinh trong hackathon.'
      role='MENTOR'
    >
      {/* ── Sự kiện được phân công ── */}
      <div className='section-title'>
        <h2>Sự kiện được phân công</h2>
        <span className='hint'>Các track / sự kiện bạn được Coordinator gán</span>
      </div>

      <div className='card'>
        {loading && <div className='empty-state'>Đang tải danh sách…</div>}
        {!loading && error && <div className='empty-state'>{error}</div>}
        {!loading && !error && events.length === 0 && (
          <div className='empty-state'>Bạn chưa được phân công sự kiện nào.</div>
        )}
        {!loading && events.length > 0 && (
          <div className='kv-list'>
            {events.map((ev) => (
              <div className='kv' key={ev.eventId}>
                <span style={{ minWidth: 0, flex: 1, textAlign: 'left' }}>
                  <div style={{ fontWeight: 600, color: 'var(--text)' }}>{ev.title || '—'}</div>
                  <div style={{ fontSize: 11, color: 'var(--text-mute)', marginTop: 4 }}>ID: {ev.eventId}</div>
                </span>
                <span className='card-badge'>{ev.status}</span>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* ── Vòng đang diễn ra ── */}
      <div className='section-title' style={{ marginTop: 24 }}>
        <h2>Vòng đang diễn ra</h2>
        <span className='hint'>Các vòng thi hiện đang active trong sự kiện của bạn</span>
      </div>

      <div className='card'>
        {loadingRounds && <div className='empty-state'>Đang tải vòng thi…</div>}
        {!loadingRounds && errorRounds && <div className='empty-state'>{errorRounds}</div>}
        {!loadingRounds && !errorRounds && rounds.length === 0 && (
          <div className='empty-state'>Hiện không có vòng nào đang diễn ra.</div>
        )}
        {!loadingRounds && rounds.length > 0 && (
          <div className='kv-list'>
            {rounds.map((rd) => (
              <div className='kv' key={rd.roundId}>
                <span style={{ minWidth: 0, flex: 1, textAlign: 'left' }}>
                  <div style={{ fontWeight: 600, color: 'var(--text)' }}>{rd.roundName || '—'}</div>
                  <div style={{ fontSize: 12, color: 'var(--text-dim)', marginTop: 2 }}>{rd.eventTitle || '—'}</div>
                  <div style={{ fontSize: 11, color: 'var(--text-mute)', marginTop: 2 }}>
                    {formatDateTime(rd.startDate)} → {formatDateTime(rd.endDate)}
                  </div>
                </span>
                <span
                  className={`status-pill ${roundStatusPillClass(rd.roundStatus)}`}
                  style={{ cursor: 'default', flexShrink: 0 }}
                >
                  {rd.roundStatus || '—'}
                </span>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* ── Thông tin tài khoản ── */}
      <div className='section-title'>
        <h2>Thông tin tài khoản</h2>
      </div>
      <div className='card'>
        <div className='kv-list'>
          <div className='kv'>
            <span>Họ tên</span>
            <span>{auth.fullName || '—'}</span>
          </div>
          <div className='kv'>
            <span>Email</span>
            <span>{auth.email}</span>
          </div>
          <div className='kv'>
            <span>Vai trò</span>
            <span>Mentor</span>
          </div>
          <div className='kv'>
            <span>Trạng thái phiên</span>
            <span>Đã đăng nhập</span>
          </div>
        </div>
      </div>
    </DashboardShell>
  )
}

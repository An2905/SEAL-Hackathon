import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import AccordionCard from '../../../components/common/AccordionCard'
import Pagination from '../../../components/common/Pagination'
import LoadingState from '../../../components/common/LoadingState'
import { getAllEvents, attachPendingTeamsToEvents } from '../../../api/event'
import { changeEventStatus } from '../../../api/staff'
import { useToast } from '../../../context/ToastContext'
import { localizeError } from '../../../utils/errors'
import { CreateEventForm } from '../StaffDashboard'

const PAGE_SIZE = 5

const EVENT_STATUSES = ['BUILDING', 'UPCOMING', 'ONGOING', 'COMPLETED']

const STATUS_COLORS = {
  BUILDING: { bg: '#fffbeb', color: '#92400e', border: '#fde68a' },
  UPCOMING: { bg: '#eff6ff', color: '#1e40af', border: '#bfdbfe' },
  ONGOING: { bg: '#f0fdf4', color: '#166534', border: '#bbf7d0' },
  COMPLETED: { bg: '#f9fafb', color: '#6b7280', border: '#e5e7eb' }
}

function StatusPill({ status }) {
  const c = STATUS_COLORS[status] || STATUS_COLORS.COMPLETED
  return (
    <span
      style={{
        fontSize: 11,
        fontWeight: 600,
        padding: '2px 8px',
        borderRadius: 20,
        background: c.bg,
        color: c.color,
        border: `1px solid ${c.border}`
      }}
    >
      {status}
    </span>
  )
}

function formatDate(d) {
  if (!d) return '—'
  return new Date(d).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' })
}

export default function StaffEventsPage() {
  const { showToast } = useToast()
  const [events, setEvents] = useState([])
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState(1)
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [refreshKey, setRefreshKey] = useState(0)

  const fetchEvents = useCallback(async () => {
    setLoading(true)
    try {
      let data = await getAllEvents()
      data = await attachPendingTeamsToEvents(data)
      setEvents(data)
    } catch (err) {
      showToast('Không tải được danh sách sự kiện', 'error')
    } finally {
      setLoading(false)
    }
  }, [showToast, refreshKey])

  useEffect(() => {
    fetchEvents()
  }, [fetchEvents])

  const handleStatusChange = async (eventId, nextStatus) => {
    try {
      await changeEventStatus({ eventId, status: nextStatus })
      setEvents((prev) => prev.map((e) => (e.eventId === eventId ? { ...e, status: nextStatus } : e)))
      showToast(`Đã cập nhật trạng thái → ${nextStatus}`, 'success')
    } catch (err) {
      showToast(localizeError(err.message), 'error')
    }
  }

  const filtered = statusFilter === 'ALL' ? events : events.filter((e) => e.status === statusFilter)
  const paginated = filtered.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE)

  return (
    <div>
      {/* ── Page header ── */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 20 }}>
        <div>
          <h1 style={pageTitleStyle}>Sự kiện</h1>
          <p style={pageDescStyle}>Quản lý toàn bộ sự kiện Hackathon</p>
        </div>
        <button type='button' className='btn btn-primary' onClick={() => setShowCreateModal(true)}>
          + Tạo sự kiện
        </button>
      </div>

      {/* ── Filter bar ── */}
      <div style={filterBarStyle}>
        {['ALL', ...EVENT_STATUSES].map((s) => (
          <button
            key={s}
            type='button'
            onClick={() => {
              setStatusFilter(s)
              setPage(1)
            }}
            style={filterBtnStyle(statusFilter === s)}
          >
            {s === 'ALL' ? 'Tất cả' : s}
          </button>
        ))}
        <span style={{ marginLeft: 'auto', fontSize: 12, color: 'var(--text-dim,#718096)', alignSelf: 'center' }}>
          {filtered.length} sự kiện
        </span>
      </div>

      {/* ── Event list ── */}
      {loading && <LoadingState className='' style={emptyStyle} />}
      {!loading && filtered.length === 0 && <div style={emptyStyle}>Không có sự kiện nào.</div>}

      {!loading &&
        paginated.map((ev, idx) => (
          <AccordionCard
            key={ev.eventId}
            title={
              <span style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <span className='accounts-table-index' style={{ minWidth: 24 }}>
                  {(page - 1) * PAGE_SIZE + idx + 1}
                </span>
                <span>{ev.title || '(Không có tên)'}</span>
              </span>
            }
            badge={<StatusPill status={ev.status} />}
          >
            <div style={detailGridStyle}>
              <KvRow label='Mô tả' value={ev.description || '—'} />
              <KvRow label='Ngày bắt đầu' value={formatDate(ev.startDate)} />
              <KvRow label='Ngày kết thúc' value={formatDate(ev.endDate)} />
              {ev.pendingTeams > 0 && (
                <KvRow
                  label='Đội chờ duyệt'
                  value={
                    <span style={{ color: 'var(--warning,#d97706)', fontWeight: 600 }}>{ev.pendingTeams} đội</span>
                  }
                />
              )}
            </div>

            <div style={{ display: 'flex', gap: 8, marginTop: 12, flexWrap: 'wrap' }}>
              <Link
                to={`/staff/events/${ev.eventId}`}
                className='btn btn-outline'
                style={{ fontSize: 12, padding: '5px 12px' }}
              >
                Chi tiết
              </Link>
              <Link
                to={`/staff/events/${ev.eventId}/check-in`}
                className='btn btn-outline'
                style={{ fontSize: 12, padding: '5px 12px' }}
              >
                Check-in
              </Link>
              <select
                value={ev.status}
                onChange={(e) => handleStatusChange(ev.eventId, e.target.value)}
                style={{
                  fontSize: 12,
                  padding: '5px 10px',
                  borderRadius: 6,
                  border: '1px solid var(--border,#e2e8f0)',
                  cursor: 'pointer',
                  background: 'var(--card-bg,#fff)',
                  color: 'var(--text,#1a202c)'
                }}
              >
                {EVENT_STATUSES.map((s) => (
                  <option key={s} value={s}>
                    {s}
                  </option>
                ))}
              </select>
            </div>
          </AccordionCard>
        ))}

      {/* ── Pagination ── */}
      <Pagination total={filtered.length} pageSize={PAGE_SIZE} currentPage={page} onChange={setPage} />

      {/* ── Create modal ── */}
      <CreateEventForm
        open={showCreateModal}
        onClose={() => setShowCreateModal(false)}
        onSuccess={() => {
          setRefreshKey((k) => k + 1)
          setShowCreateModal(false)
        }}
      />
    </div>
  )
}

function KvRow({ label, value }) {
  return (
    <div style={{ display: 'flex', gap: 8, padding: '4px 0' }}>
      <span style={{ fontSize: 12, color: 'var(--text-dim,#718096)', minWidth: 120, flexShrink: 0 }}>{label}</span>
      <span style={{ fontSize: 13, color: 'var(--text,#1a202c)' }}>{value}</span>
    </div>
  )
}

const pageTitleStyle = { fontSize: 20, fontWeight: 600, color: 'var(--text,#1a202c)', margin: 0 }
const pageDescStyle = { fontSize: 13, color: 'var(--text-dim,#718096)', marginTop: 4, marginBottom: 0 }

const filterBarStyle = {
  display: 'flex',
  gap: 6,
  marginBottom: 16,
  flexWrap: 'wrap',
  alignItems: 'center',
  padding: '10px 14px',
  background: 'var(--card-bg,#fff)',
  border: '1px solid var(--border,#e2e8f0)',
  borderRadius: 8
}

const filterBtnStyle = (active) => ({
  padding: '4px 12px',
  borderRadius: 20,
  fontSize: 12,
  cursor: 'pointer',
  border: '1px solid ' + (active ? 'var(--accent,#2563eb)' : 'var(--border,#e2e8f0)'),
  background: active ? 'var(--accent,#2563eb)' : 'transparent',
  color: active ? '#fff' : 'var(--text-dim,#4a5568)',
  fontWeight: active ? 600 : 400,
  transition: 'all .12s'
})

const detailGridStyle = { display: 'flex', flexDirection: 'column', gap: 2 }
const emptyStyle = { textAlign: 'center', padding: '40px 0', color: 'var(--text-dim,#718096)', fontSize: 14 }

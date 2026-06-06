import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import DashboardShell from './DashboardShell'
import FormField from '../../components/common/FormField'
import FormMessage from '../../components/common/FormMessage'
import Modal from '../../components/common/Modal'
import PendingTeamsBadge from '../../components/common/PendingTeamsBadge'
import LoadingButton from '../../components/common/LoadingButton'
import {
  createStaffAccount,
  createEvent,
  changeEventStatus,
  changeAccountStatus,
  getAllAccounts,
  normalizeAccountUserId,
  exportEventsExcel
} from '../../api/staff'
import { getAllEvents, attachPendingTeamsToEvents } from '../../api/event'
import { useToast } from '../../context/ToastContext'
import { useAuth } from '../../context/AuthContext'
import { localizeError } from '../../utils/errors'
import { roleUiLabel } from '../../utils/roleLabels'

const EVENT_STATUSES = [
  { value: 'BUILDING', label: 'Đang thiết lập (BUILDING)' },
  { value: 'UPCOMING', label: 'Sắp diễn ra (UPCOMING)' },
  { value: 'ONGOING', label: 'Đang diễn ra (ONGOING)' },
  { value: 'COMPLETED', label: 'Đã kết thúc (COMPLETED)' }
]

const ACCOUNT_ROLE_FILTERS = [
  { value: 'ALL', label: 'Tất cả' },
  { value: 'EXPERT', label: 'Khách' },
  { value: 'EXPERT_INTERNAL', label: 'Khách (INTERNAL)' },
  { value: 'EXPERT_EXTERNAL', label: 'Khách (EXTERNAL)' },
  { value: 'STUDENT_EXTERNAL', label: 'Student' },
  { value: 'STUDENT_FPT', label: 'FPT Student' }
]

const ACCOUNT_STATUSES = ['PENDING', 'APPROVED', 'REJECTED']

function statusPillClass(status) {
  const key = (status || '').toLowerCase()
  if (key === 'approved') return 'status-active'
  if (key === 'pending') return 'status-pending'
  if (key === 'rejected') return 'status-rejected'
  return 'status-default'
}

function resolveAccountUserId(account) {
  return normalizeAccountUserId(account?.userId ?? account?.user_id)
}

// ─── Account Status Picker ────────────────────────────────────────────────────
function AccountStatusPicker({ account, onUpdated }) {
  const { showToast } = useToast()
  const [open, setOpen] = useState(false)
  const [saving, setSaving] = useState(false)
  const locked = account.role === 'COORDINATOR'

  const handleSelect = async (e) => {
    const next = e.target.value
    setOpen(false)
    const currentStatus = String(account.status ?? '')
      .trim()
      .toUpperCase()
    if (next === currentStatus) return

    const resolvedId = resolveAccountUserId(account)
    if (!resolvedId) {
      showToast('Không xác định được tài khoản — vui lòng tải lại danh sách', 'error')
      return
    }

    setSaving(true)
    try {
      await changeAccountStatus({ userId: resolvedId, status: next })
      onUpdated(resolvedId, next)
      showToast(`Đã cập nhật trạng thái tài khoản → ${next}`, 'success')
    } catch (err) {
      showToast(localizeError(err.message), 'error')
    } finally {
      setSaving(false)
    }
  }

  if (open && !locked) {
    return (
      <div className='status-picker'>
        <select
          className='status-picker-select'
          value={account.status}
          onChange={handleSelect}
          onBlur={() => setOpen(false)}
          disabled={saving}
          autoFocus
        >
          {ACCOUNT_STATUSES.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
      </div>
    )
  }

  return (
    <div className='status-picker'>
      <button
        type='button'
        className={`status-pill ${statusPillClass(account.status)}`}
        onClick={() => !locked && !saving && setOpen(true)}
        disabled={locked || saving}
        title={locked ? 'Không thể đổi trạng thái Coordinator' : 'Nhấn để đổi trạng thái'}
      >
        {account.status}
      </button>
    </div>
  )
}

// ─── Create Staff Account Form ────────────────────────────────────────────────
export function CreateStaffAccountForm({ onSuccess }) {
  const { showToast } = useToast()
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState(null)
  const [form, setForm] = useState({ email: '', fullName: '', role: 'EXPERT_INTERNAL' })

  const handle = (e) => setForm((f) => ({ ...f, [e.target.name]: e.target.value }))

  const handleSubmit = async (e) => {
    e.preventDefault()
    setMessage(null)
    const email = form.email.trim()
    const fullName = form.fullName.trim()
    if (!email || !fullName) {
      setMessage({ text: 'Vui lòng nhập đầy đủ thông tin', type: 'error' })
      return
    }
    setLoading(true)
    try {
      await createStaffAccount({ email, fullName, role: form.role })
      const createdLabel = roleUiLabel(form.role) || 'Khách'
      setMessage({
        text: `Đã tạo tài khoản ${createdLabel} cho ${email}. Mật khẩu tạm đã được gửi qua email.`,
        type: 'success'
      })
      showToast('Đã tạo tài khoản & gửi email mời', 'success')
      setForm({ email: '', fullName: '', role: form.role })
      onSuccess?.(`Tạo tài khoản ${createdLabel} — ${email}`)
    } catch (err) {
      setMessage({ text: localizeError(err.message), type: 'error' })
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className='card'>
      <div className='card-head'>
        <div className='card-title'>Tạo tài khoản Khách</div>
      </div>
      <p className='card-sub'>Khách có thể được phân công làm Mentor và/hoặc Judge theo từng sự kiện. Hệ thống sinh mật khẩu tạm và gửi email mời.</p>
      <form className='form' onSubmit={handleSubmit}>
        <FormField label='Họ và tên'>
          <input name='fullName' value={form.fullName} onChange={handle} required placeholder='Nguyễn Văn A' />
        </FormField>
        <FormField label='Email'>
          <input
            name='email'
            type='email'
            value={form.email}
            onChange={handle}
            required
            placeholder='judge@fpt.edu.vn'
          />
        </FormField>
        <FormField label='Loại khách'>
          <select name='role' value={form.role} onChange={handle} required>
            <option value='EXPERT_INTERNAL'>Khách (INTERNAL)</option>
            <option value='EXPERT_EXTERNAL'>Khách (EXTERNAL)</option>
          </select>
        </FormField>
        <LoadingButton loading={loading} type='submit'>
          Tạo tài khoản &amp; gửi email
        </LoadingButton>
        <FormMessage message={message?.text} type={message?.type} />
      </form>
    </div>
  )
}

// ─── Create Event Form ────────────────────────────────────────────────────────
export function CreateEventForm({ open, onClose, onSuccess }) {
  const { showToast } = useToast()
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState(null)
  const [lastCreated, setLastCreated] = useState(null)
  const [form, setForm] = useState({
    title: '',
    description: '',
    startDate: '',
    endDate: '',
    maxTeams: '',
    numRounds: '1'
  })

  const handle = (e) => setForm((f) => ({ ...f, [e.target.name]: e.target.value }))

  const handleSubmit = async (e) => {
    e.preventDefault()
    setMessage(null)
    setLastCreated(null)
    const title = form.title.trim()
    if (!title) {
      setMessage({ text: 'Vui lòng nhập tên sự kiện', type: 'error' })
      return
    }
    if (form.startDate && form.endDate && form.startDate > form.endDate) {
      setMessage({ text: 'Ngày bắt đầu phải trước hoặc bằng ngày kết thúc', type: 'error' })
      return
    }
    const maxTeams = form.maxTeams === '' ? null : Number(form.maxTeams)
    if (maxTeams != null && (!Number.isFinite(maxTeams) || maxTeams < 1)) {
      setMessage({ text: 'Số đội tối đa phải ≥ 1', type: 'error' })
      return
    }
    const numRounds = form.numRounds === '' ? 1 : Number(form.numRounds)
    if (!Number.isFinite(numRounds) || numRounds < 1) {
      setMessage({ text: 'Số vòng thi phải ≥ 1', type: 'error' })
      return
    }

    setLoading(true)
    try {
      const created = await createEvent({
        title,
        description: form.description,
        startDate: form.startDate || null,
        endDate: form.endDate || null,
        maxTeams,
        numRounds
      })
      setLastCreated(created)
      setMessage({
        text: `Đã tạo sự kiện "${created.title}" — trạng thái BUILDING.`,
        type: 'success'
      })
      showToast('Đã tạo sự kiện mới', 'success')
      setForm({
        title: '',
        description: '',
        startDate: '',
        endDate: '',
        maxTeams: '',
        numRounds: '1'
      })
      onSuccess?.(created)
      onClose?.()
    } catch (err) {
      setMessage({ text: localizeError(err.message), type: 'error' })
    } finally {
      setLoading(false)
    }
  }

  return (
    <Modal
      isOpen={open}
      onClose={onClose}
      title='Tạo sự kiện mới'
      subtitle='Sự kiện mới có trạng thái BUILDING — sinh viên chưa đăng ký được. Vào chi tiết sự kiện để thêm vòng/bảng, rồi chuyển sang UPCOMING.'
    >
      <form className='form' onSubmit={handleSubmit}>
        <FormField label='Tên sự kiện *'>
          <input
            name='title'
            value={form.title}
            onChange={handle}
            maxLength={200}
            required
            disabled={loading}
            placeholder='VD: FPT Tech Hackathon 2026'
          />
        </FormField>
        <FormField label='Mô tả'>
          <textarea
            name='description'
            value={form.description}
            onChange={handle}
            rows={3}
            disabled={loading}
            placeholder='Mô tả ngắn (tuỳ chọn)'
          />
        </FormField>
        <FormField label='Ngày bắt đầu'>
          <input
            type='datetime-local'
            name='startDate'
            value={form.startDate}
            onChange={handle}
            disabled={loading}
          />
        </FormField>
        <FormField label='Ngày kết thúc'>
          <input
            type='datetime-local'
            name='endDate'
            value={form.endDate}
            onChange={handle}
            disabled={loading}
          />
        </FormField>
        <FormField label='Số đội tối đa'>
          <input
            type='number'
            name='maxTeams'
            value={form.maxTeams}
            onChange={handle}
            min={1}
            disabled={loading}
            placeholder='Tuỳ chọn'
          />
        </FormField>
        <FormField label='Số vòng thi dự kiến'>
          <input
            type='number'
            name='numRounds'
            value={form.numRounds}
            onChange={handle}
            min={1}
            disabled={loading}
          />
        </FormField>
        <LoadingButton loading={loading} type='submit'>
          Tạo sự kiện
        </LoadingButton>
        <FormMessage message={message?.text} type={message?.type} />
        {lastCreated?.eventId && (
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginTop: 8 }}>
            <Link
              to={`/staff/events/${encodeURIComponent(lastCreated.eventId)}`}
              className='btn btn-outline'
              style={{ fontSize: 13 }}
            >
              Chi tiết sự kiện
            </Link>
          </div>
        )}
      </form>
    </Modal>
  )
}

// ─── Event Status Picker ──────────────────────────────────────────────────────
function eventStatusPillClass(status) {
  const key = (status || '').toUpperCase()
  if (key === 'BUILDING') return 'status-pending'
  if (key === 'UPCOMING') return 'status-pending'
  if (key === 'ONGOING') return 'status-active'
  if (key === 'COMPLETED') return 'status-default'
  if (key === 'CANCELLED') return 'status-rejected'
  return 'status-default'
}

function EventStatusPicker({ event, onUpdated }) {
  const { showToast } = useToast()
  const [open, setOpen] = useState(false)
  const [saving, setSaving] = useState(false)
  const eventId = event?.eventId ?? ''

  const handleSelect = async (e) => {
    const next = e.target.value
    setOpen(false)
    const currentStatus = String(event.status ?? '')
      .trim()
      .toUpperCase()
    if (next === currentStatus) return
    if (!eventId) {
      showToast('Không xác định được sự kiện — vui lòng tải lại danh sách', 'error')
      return
    }
    setSaving(true)
    try {
      await changeEventStatus({ eventId, newStatus: next })
      onUpdated(eventId, next)
      showToast(`Đã cập nhật trạng thái sự kiện → ${next}`, 'success')
    } catch (err) {
      showToast(localizeError(err.message), 'error')
    } finally {
      setSaving(false)
    }
  }

  if (open) {
    return (
      <div className='status-picker'>
        <select
          className='status-picker-select'
          value={event.status}
          onChange={handleSelect}
          onBlur={() => setOpen(false)}
          disabled={saving}
          autoFocus
        >
          {EVENT_STATUSES.map((s) => (
            <option key={s.value} value={s.value}>
              {s.value}
            </option>
          ))}
        </select>
      </div>
    )
  }

  return (
    <div className='status-picker'>
      <button
        type='button'
        className={`status-pill ${eventStatusPillClass(event.status)}`}
        onClick={() => !saving && setOpen(true)}
        disabled={saving}
        title='Nhấn để đổi trạng thái sự kiện'
        style={{ alignSelf: 'center', flexShrink: 0 }}
      >
        {event.status}
      </button>
    </div>
  )
}

function formatEventDate(value) {
  if (!value) return '—'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return String(value)
  return d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' })
}

// ─── Events List Section ──────────────────────────────────────────────────────
export function EventsListSection({ refreshKey = 0, onStatusChanged, onPendingTotalChange }) {
  const { showToast } = useToast()
  const [status, setStatus] = useState('ALL')
  const [events, setEvents] = useState([])
  const [loading, setLoading] = useState(false)
  const [exporting, setExporting] = useState(false)
  const [error, setError] = useState(null)
  const [loaded, setLoaded] = useState(false)

  const handleExport = async () => {
    setExporting(true)
    try {
      const blob = await exportEventsExcel()
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = 'events.xlsx'
      document.body.appendChild(a)
      a.click()
      a.remove()
      URL.revokeObjectURL(url)
      showToast('Xuất Excel thành công', 'success')
    } catch (err) {
      showToast(localizeError(err.message), 'error')
    } finally {
      setExporting(false)
    }
  }

  useEffect(() => {
    let cancelled = false
    ;(async () => {
      setLoading(true)
      setError(null)
      try {
        const data = await getAllEvents(status)
        const enriched = await attachPendingTeamsToEvents(data)
        if (!cancelled) {
          setEvents(enriched)
          setLoaded(true)
        }
      } catch (err) {
        if (!cancelled) {
          setError(localizeError(err.message))
          setEvents([])
          showToast('Không tải được danh sách sự kiện', 'error')
        }
      } finally {
        if (!cancelled) setLoading(false)
      }
    })()
    return () => {
      cancelled = true
    }
  }, [status, refreshKey, showToast])

  const totalPending = events.reduce((sum, ev) => sum + (Number(ev.pendingTeams) || 0), 0)
  useEffect(() => {
    onPendingTotalChange?.(totalPending)
  }, [totalPending, onPendingTotalChange])

  const handleEventStatusUpdated = (eventId, newStatus) => {
    setEvents((prev) => prev.map((ev) => (ev.eventId === eventId ? { ...ev, status: newStatus } : ev)))
    onStatusChanged?.(eventId, newStatus)
  }

  return (
    <div className='card'>
      <div className='card-head'>
        <div className='card-title'>Danh sách sự kiện</div>
        <LoadingButton
          loading={exporting}
          className='btn btn-success btn-sm'
          onClick={handleExport}
          type='button'
          style={{ marginLeft: 'auto' }}
        >
          Xuất Excel
        </LoadingButton>
      </div>
      <p className='card-sub'>
        Xem tất cả hackathon trong hệ thống. Nhấn badge trạng thái để đổi — chỉ Staff có quyền thao tác.
      </p>

      <FormField label='Lọc theo trạng thái'>
        <select name='status' value={status} onChange={(e) => setStatus(e.target.value)} disabled={loading}>
          <option value='ALL'>Tất cả</option>
          {EVENT_STATUSES.map((s) => (
            <option key={s.value} value={s.value}>
              {s.label}
            </option>
          ))}
        </select>
      </FormField>

      {error && <FormMessage message={error} type='error' />}
      {loading && (
        <div className='empty-state' style={{ marginTop: 12 }}>
          Đang tải danh sách…
        </div>
      )}
      {!loading && loaded && events.length === 0 && !error && (
        <div className='empty-state' style={{ marginTop: 12 }}>
          Chưa có sự kiện nào khớp với bộ lọc.
        </div>
      )}

      {!loading && events.length > 0 && (
        <>
          <div className='card-sub' style={{ marginTop: 12, marginBottom: 6 }}>
            Tổng cộng <strong>{events.length}</strong> sự kiện
          </div>
          <div className='kv-list'>
            {events.map((ev) => (
              <div
                className={`kv event-list-item${Number(ev.pendingTeams) > 0 ? ' has-pending-badge' : ''}`}
                key={ev.eventId}
              >
                <PendingTeamsBadge count={ev.pendingTeams} />
                <span style={{ minWidth: 0, flex: 1, textAlign: 'left' }}>
                  <div style={{ fontWeight: 600, color: 'var(--text)' }}>{ev.title || '—'}</div>
                  {ev.description && (
                    <div style={{ fontSize: 12, color: 'var(--text-dim)', marginTop: 2 }}>{ev.description}</div>
                  )}
                  <div style={{ fontSize: 11, color: 'var(--text-mute)', marginTop: 4 }}>
                    {ev.startDate || ev.endDate
                      ? ` · ${formatEventDate(ev.startDate)} → ${formatEventDate(ev.endDate)}`
                      : ''}
                  </div>
                </span>
                <span style={{ display: 'flex', alignItems: 'center', gap: 8, flexShrink: 0, alignSelf: 'center' }}>
                  <Link
                    to={`/staff/events/${ev.eventId}`}
                    className='btn btn-outline'
                    style={{ fontSize: 12, padding: '4px 10px' }}
                  >
                    Chi tiết
                  </Link>
                  <EventStatusPicker event={ev} onUpdated={handleEventStatusUpdated} />
                </span>
              </div>
            ))}
          </div>
        </>
      )}
    </div>
  )
}

// ─── Accounts List Section ────────────────────────────────────────────────────
export function AccountsListSection() {
  const { showToast } = useToast()
  const [role, setRole] = useState('ALL')
  const [search, setSearch] = useState('')
  const [accounts, setAccounts] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [loaded, setLoaded] = useState(false)

  const fetchAccounts = useCallback(
    async (selectedRole, input = '') => {
      setLoading(true)
      setError(null)
      try {
        const data = await getAllAccounts(selectedRole, input)
        setAccounts(data)
        setLoaded(true)
      } catch (err) {
        setError(localizeError(err.message))
        setAccounts([])
        showToast('Không tải được danh sách tài khoản', 'error')
      } finally {
        setLoading(false)
      }
    },
    [showToast]
  )

  useEffect(() => {
    fetchAccounts('ALL', '')
  }, [fetchAccounts])

  const handleRoleChange = (e) => {
    const next = e.target.value
    setRole(next)
    fetchAccounts(next, search)
  }
  const handleSearchSubmit = (e) => {
    e.preventDefault()
    fetchAccounts(role, search)
  }
  const handleStatusUpdated = (userId, newStatus) => {
    setAccounts((prev) => prev.map((a) => (a.userId === userId ? { ...a, status: newStatus } : a)))
  }

  return (
    <div className='card'>
      <div className='card-head'>
        <div className='card-title'>Danh sách tài khoản</div>
      </div>
      <p className='card-sub'>Lọc theo vai trò trong hệ thống. Chỉ Staff có quyền xem.</p>

      <form className='form' onSubmit={handleSearchSubmit}>
        <FormField label='Lọc theo vai trò'>
          <select name='role' value={role} onChange={handleRoleChange} disabled={loading}>
            {ACCOUNT_ROLE_FILTERS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </FormField>
        <FormField label='Tìm kiếm'>
          <input
            name='search'
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            disabled={loading}
            placeholder='Nhập tên hoặc email'
          />
        </FormField>
        <LoadingButton loading={loading} type='submit'>
          Tìm kiếm
        </LoadingButton>
      </form>

      {error && <FormMessage message={error} type='error' />}
      {loading && (
        <div className='empty-state' style={{ marginTop: 12 }}>
          Đang tải danh sách…
        </div>
      )}
      {!loading && loaded && accounts.length === 0 && !error && (
        <div className='empty-state' style={{ marginTop: 12 }}>
          Không có tài khoản nào khớp với bộ lọc.
        </div>
      )}

      {!loading && accounts.length > 0 && (
        <>
          <div className='card-sub' style={{ marginTop: 12, marginBottom: 6 }}>
            Tổng cộng <strong>{accounts.length}</strong> tài khoản
          </div>
          <div className='kv-list'>
            {accounts.map((a) => {
              const uid = resolveAccountUserId(a)
              return (
                <div className='kv' key={uid || a.email}>
                  <span style={{ minWidth: 0, flex: 1, textAlign: 'left' }}>
                    <div style={{ fontWeight: 600, color: 'var(--text)' }}>{a.fullName || '—'}</div>
                    <div style={{ fontSize: 12, color: 'var(--text-dim)' }}>{a.email}</div>
                  </span>
                  <span
                    style={{
                      display: 'flex',
                      gap: 6,
                      alignItems: 'center',
                      flexWrap: 'wrap',
                      justifyContent: 'flex-end'
                    }}
                  >
                    <span className='card-badge'>{roleUiLabel(a.role) || '—'}</span>
                    <AccountStatusPicker account={a} onUpdated={handleStatusUpdated} />
                  </span>
                </div>
              )
            })}
          </div>
        </>
      )}
    </div>
  )
}

// ─── Staff Dashboard — default export ────────────────────────────────────────
export default function StaffDashboard() {
  const { auth } = useAuth()
  const [refreshKey, setRefreshKey] = useState(0)
  const [pendingTotal, setPendingTotal] = useState(0)

  return (
    <DashboardShell
      roleLabel='Staff'
      title='Bảng điều khiển Coordinator'
      subtitle={`Xin chào${
        auth.fullName ? ', ' + auth.fullName : ''
      }! Quản lý sự kiện, tài khoản và phân công nhân sự tại đây.`}
      role='Staff'
    >
      <div className='section-title'>
        <h2>Sự kiện</h2>
        {pendingTotal > 0 && <span className='hint'>{pendingTotal} đội đang chờ duyệt</span>}
      </div>
      <EventsListSection
        refreshKey={refreshKey}
        onStatusChanged={() => setRefreshKey((k) => k + 1)}
        onPendingTotalChange={setPendingTotal}
      />

      <div className='section-title' style={{ marginTop: 32 }}>
        <h2>Tạo tài khoản</h2>
      </div>
      <CreateStaffAccountForm />

      <div className='section-title' style={{ marginTop: 32 }}>
        <h2>Danh sách tài khoản</h2>
      </div>
      <AccountsListSection />
    </DashboardShell>
  )
}

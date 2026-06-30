import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import FormField from '../../components/common/FormField'
import FormMessage from '../../components/common/FormMessage'
import Modal from '../../components/common/Modal'
import LoadingButton from '../../components/common/LoadingButton'
import Pagination from '../../components/common/Pagination'
import LoadingState from '../../components/common/LoadingState'
import {
  createStaffAccount,
  createEvent,
  changeAccountStatus,
  getAllAccounts,
  normalizeAccountUserId
} from '../../api/staff'
import { useToast } from '../../context/ToastContext'
import { localizeError } from '../../utils/errors'
import { roleUiLabel } from '../../utils/roleLabels'
import { eventStatusLabel } from '../../utils/eventStatusLabels'

const ACCOUNT_ROLE_FILTERS = [
  { value: 'ALL', label: 'Tất cả' },
  { value: 'EXPERT', label: 'Khách' },
  { value: 'EXPERT_INTERNAL', label: 'Khách - INTERNAL' },
  { value: 'EXPERT_EXTERNAL', label: 'Khách - EXTERNAL' },
  { value: 'STUDENT_EXTERNAL', label: 'Student' },
  { value: 'STUDENT_FPT', label: 'FPT Student' }
]

const ACCOUNT_STATUSES = ['PENDING', 'APPROVED', 'REJECTED']

function accountStatusLabel(status) {
  const key = String(status ?? '').toUpperCase()
  if (key === 'PENDING') return 'Chờ duyệt'
  if (key === 'APPROVED') return 'Đã duyệt'
  if (key === 'REJECTED') return 'Từ chối'
  return status || '—'
}

function resolveAccountUserId(account) {
  return normalizeAccountUserId(account?.userId ?? account?.user_id)
}

function accountSourceLabel(role) {
  const key = (role || '').toUpperCase()
  if (key.endsWith('_INTERNAL') || key === 'STUDENT_FPT' || key === 'COORDINATOR') return 'INTERNAL'
  if (key.endsWith('_EXTERNAL')) return 'EXTERNAL'
  return '—'
}

// ─── Account Status Picker ────────────────────────────────────────────────────
function AccountStatusPicker({ account, onUpdated }) {
  const { showToast } = useToast()
  const [saving, setSaving] = useState(false)
  const locked = account.role === 'COORDINATOR'
  const currentStatus = String(account.status ?? '')
    .trim()
    .toUpperCase()

  const handleSelect = async (e) => {
    const next = e.target.value
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
      showToast(`Đã cập nhật trạng thái tài khoản → ${accountStatusLabel(next)}`, 'success')
    } catch (err) {
      showToast(localizeError(err.message), 'error')
    } finally {
      setSaving(false)
    }
  }

  return (
    <select
      className='account-status-select'
      value={currentStatus || ACCOUNT_STATUSES[0]}
      onChange={handleSelect}
      disabled={locked || saving}
      title={locked ? 'Không thể đổi trạng thái Coordinator' : 'Đổi trạng thái tài khoản'}
      aria-label='Trạng thái tài khoản'
    >
      {ACCOUNT_STATUSES.map((s) => (
        <option key={s} value={s}>
          {accountStatusLabel(s)}
        </option>
      ))}
    </select>
  )
}

// ─── Create Staff Account Form ────────────────────────────────────────────────
export function CreateStaffAccountForm({ open, onClose, onSuccess }) {
  const { showToast } = useToast()
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState(null)
  const [form, setForm] = useState({ email: '', fullName: '', role: 'EXPERT_INTERNAL' })

  useEffect(() => {
    if (!open) return
    setMessage(null)
    setForm({ email: '', fullName: '', role: 'EXPERT_INTERNAL' })
  }, [open])

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
      title='Tạo tài khoản Khách'
      subtitle='Khách có thể được phân công làm Mentor và/hoặc Judge theo từng sự kiện. Hệ thống sinh mật khẩu tạm và gửi email mời.'
    >
      <form className='form' onSubmit={handleSubmit}>
        <FormField label='Họ và tên'>
          <input
            name='fullName'
            value={form.fullName}
            onChange={handle}
            required
            disabled={loading}
            placeholder='Nguyễn Văn A'
          />
        </FormField>
        <FormField label='Email'>
          <input
            name='email'
            type='email'
            value={form.email}
            onChange={handle}
            required
            disabled={loading}
            placeholder='judge@fpt.edu.vn'
          />
        </FormField>
        <FormField label='Loại khách'>
          <select name='role' value={form.role} onChange={handle} required disabled={loading}>
            <option value='EXPERT_INTERNAL'>Khách (INTERNAL)</option>
            <option value='EXPERT_EXTERNAL'>Khách (EXTERNAL)</option>
          </select>
        </FormField>
        <LoadingButton loading={loading} type='submit'>
          Tạo tài khoản &amp; gửi email
        </LoadingButton>
        <FormMessage message={message?.text} type={message?.type} />
      </form>
    </Modal>
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
    numRounds: '1',
    githubTemplateRepo: ''
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
        numRounds,
        githubTemplateRepo: form.githubTemplateRepo
      })
      setLastCreated(created)
      setMessage({
        text: `Đã tạo sự kiện "${created.title}" — trạng thái ${eventStatusLabel('BUILDING')}.`,
        type: 'success'
      })
      showToast('Đã tạo sự kiện mới', 'success')
      setForm({
        title: '',
        description: '',
        startDate: '',
        endDate: '',
        maxTeams: '',
        numRounds: '1',
        githubTemplateRepo: ''
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
      subtitle='Sự kiện mới ở trạng thái đang thiết lập — sinh viên chưa đăng ký được. Vào chi tiết sự kiện để thêm vòng/bảng, rồi chuyển sang sắp diễn ra.'
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
          <input type='datetime-local' name='startDate' value={form.startDate} onChange={handle} disabled={loading} />
        </FormField>
        <FormField label='Ngày kết thúc'>
          <input type='datetime-local' name='endDate' value={form.endDate} onChange={handle} disabled={loading} />
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
          <input type='number' name='numRounds' value={form.numRounds} onChange={handle} min={1} disabled={loading} />
        </FormField>
        <FormField label='GitHub Template Repository'>
          <input
            name='githubTemplateRepo'
            value={form.githubTemplateRepo}
            onChange={handle}
            disabled={loading}
            placeholder='owner/repo hoặc tên repo (tuỳ chọn)'
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

// ─── Accounts List Section ────────────────────────────────────────────────────
const ACCOUNTS_PAGE_SIZE = 5

export function AccountsListSection({ refreshKey = 0 }) {
  const { showToast } = useToast()
  const [role, setRole] = useState('ALL')
  const [search, setSearch] = useState('')
  const [accounts, setAccounts] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [loaded, setLoaded] = useState(false)
  const [page, setPage] = useState(1)

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
    fetchAccounts(role, search)
    setPage(1)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [fetchAccounts, refreshKey])

  const handleRoleChange = (e) => {
    const next = e.target.value
    setRole(next)
    setPage(1)
    fetchAccounts(next, search)
  }
  const handleSearchSubmit = (e) => {
    e.preventDefault()
    setPage(1)
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
      {loading && <LoadingState text='Đang tải danh sách…' style={{ marginTop: 12 }} />}
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
          <div className='accounts-table'>
            <div className='accounts-table-row accounts-table-row--head'>
              <span className='accounts-table-index'>#</span>
              <span>Họ và tên</span>
              <span>Email</span>
              <span>Vai trò</span>
              <span>Nguồn</span>
              <span>Trạng thái</span>
            </div>
            {accounts.slice((page - 1) * ACCOUNTS_PAGE_SIZE, page * ACCOUNTS_PAGE_SIZE).map((a, idx) => (
              <div className='accounts-table-row' key={resolveAccountUserId(a) || a.email}>
                <span className='accounts-table-index'>{(page - 1) * ACCOUNTS_PAGE_SIZE + idx + 1}</span>
                <span className='accounts-table-cell' style={{ fontWeight: 600, color: 'var(--text)' }}>
                  {a.fullName || '—'}
                </span>
                <span className='accounts-table-cell accounts-table-cell--muted'>{a.email}</span>
                <span className='accounts-table-cell'>{roleUiLabel(a.role) || '—'}</span>
                <span className='accounts-table-cell accounts-table-cell--muted'>{accountSourceLabel(a.role)}</span>
                <span className='accounts-table-cell accounts-table-cell--status'>
                  <AccountStatusPicker account={a} onUpdated={handleStatusUpdated} />
                </span>
              </div>
            ))}
          </div>
          <Pagination total={accounts.length} pageSize={ACCOUNTS_PAGE_SIZE} currentPage={page} onChange={setPage} />
        </>
      )}
    </div>
  )
}

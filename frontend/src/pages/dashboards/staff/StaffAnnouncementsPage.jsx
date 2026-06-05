import { useEffect, useState } from 'react'
import FormField from '../../../components/common/FormField'
import FormMessage from '../../../components/common/FormMessage'
import LoadingButton from '../../../components/common/LoadingButton'
import { getAllEvents } from '../../../api/event'
import { sendAnnouncementToAll, sendAnnouncementToParticipants } from '../../../api/staff'
import { useToast } from '../../../context/ToastContext'
import { localizeError } from '../../../utils/errors'

const RECIPIENT_ROLES = [
  { value: 'STUDENT_FPT', label: 'FPT Student' },
  { value: 'STUDENT_EXTERNAL', label: 'Student' },
  { value: 'MENTOR', label: 'Mentor (đã phân công)' },
  { value: 'JUDGE_INTERNAL', label: 'Judge (đã phân công)' }
]

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

// ─── Form A: Gửi toàn hệ thống ───────────────────────────────────────────────
function SendAllForm() {
  const { showToast } = useToast()
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState(null)
  const [form, setForm] = useState({ title: '', content: '' })
  const handle = (e) => setForm((f) => ({ ...f, [e.target.name]: e.target.value }))

  const handleSubmit = async (e) => {
    e.preventDefault()
    setMessage(null)
    setLoading(true)
    try {
      const result = await sendAnnouncementToAll(form)
      setMessage({
        text: `Đã gửi thành công tới ${result.totalRecipients} người nhận.`,
        type: 'success'
      })
      showToast(`Gửi thông báo thành công — ${result.totalRecipients} người nhận`, 'success')
      setForm({ title: '', content: '' })
    } catch (err) {
      setMessage({ text: localizeError(err.message), type: 'error' })
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className='card'>
      <div className='card-head'>
        <div className='card-title'>Gửi toàn hệ thống</div>
      </div>
      <p className='card-sub'>
        Thông báo sẽ được gửi đến <strong>tất cả</strong> người dùng trong hệ thống.
      </p>
      <form className='form' onSubmit={handleSubmit}>
        <FormField label='Tiêu đề'>
          <input
            name='title'
            value={form.title}
            onChange={handle}
            required
            placeholder='VD: Thông báo lịch thi chính thức'
          />
        </FormField>
        <FormField label='Nội dung'>
          <textarea
            name='content'
            value={form.content}
            onChange={handle}
            required
            rows={4}
            placeholder='Nhập nội dung thông báo...'
            style={{ resize: 'vertical' }}
          />
        </FormField>
        <LoadingButton loading={loading} type='submit'>
          Gửi toàn hệ thống
        </LoadingButton>
        <FormMessage message={message?.text} type={message?.type} />
      </form>
    </div>
  )
}

// ─── Form B: Gửi theo event + role ───────────────────────────────────────────
function SendParticipantForm() {
  const { showToast } = useToast()
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState(null)
  const [events, setEvents] = useState([])
  const [form, setForm] = useState({ eventId: '', title: '', content: '' })
  const [selectedRoles, setSelectedRoles] = useState([])

  const handle = (e) => setForm((f) => ({ ...f, [e.target.name]: e.target.value }))

  const toggleRole = (role) => {
    setSelectedRoles((prev) => (prev.includes(role) ? prev.filter((r) => r !== role) : [...prev, role]))
  }

  useEffect(() => {
    let cancelled = false
    getAllEvents('ALL')
      .then((data) => {
        if (!cancelled) setEvents(data)
      })
      .catch(() => {})
    return () => {
      cancelled = true
    }
  }, [])

  const handleSubmit = async (e) => {
    e.preventDefault()
    setMessage(null)
    setLoading(true)
    try {
      const result = await sendAnnouncementToParticipants({
        ...form,
        roles: selectedRoles
      })
      setMessage({
        text: `Đã gửi thành công · ${
          result.totalRecipients
        } người nhận · ${formatDateTime(result.createdAt)}`,
        type: 'success'
      })
      showToast(`Gửi thông báo thành công — ${result.totalRecipients} người nhận`, 'success')
      setForm({ eventId: '', title: '', content: '' })
      setSelectedRoles([])
    } catch (err) {
      setMessage({ text: localizeError(err.message), type: 'error' })
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className='card'>
      <div className='card-head'>
        <div className='card-title'>Gửi theo sự kiện &amp; vai trò</div>
      </div>
      <p className='card-sub'>
        Chọn sự kiện và vai trò nhận thông báo. Chỉ những người thuộc sự kiện đó mới nhận được.
      </p>
      <form className='form' onSubmit={handleSubmit}>
        <FormField label='Sự kiện'>
          <select name='eventId' value={form.eventId} onChange={handle} required>
            <option value=''>— Chọn sự kiện —</option>
            {events.map((ev) => (
              <option key={ev.eventId} value={ev.eventId}>
                {ev.title}
              </option>
            ))}
          </select>
        </FormField>

        <FormField label='Vai trò nhận thông báo'>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10, paddingTop: 4 }}>
            {RECIPIENT_ROLES.map((r) => (
              <label
                key={r.value}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 6,
                  cursor: 'pointer',
                  fontSize: 13,
                  padding: '5px 10px',
                  borderRadius: 'var(--radius)',
                  border: '1px solid var(--border)',
                  background: selectedRoles.includes(r.value) ? 'var(--accent)' : 'var(--bg-card)',
                  color: selectedRoles.includes(r.value) ? '#fff' : 'var(--text)',
                  transition: 'all 0.15s',
                  userSelect: 'none'
                }}
              >
                <input
                  type='checkbox'
                  checked={selectedRoles.includes(r.value)}
                  onChange={() => toggleRole(r.value)}
                  style={{ display: 'none' }}
                />
                {r.label}
              </label>
            ))}
          </div>
          {selectedRoles.length === 0 && (
            <div style={{ fontSize: 11, color: 'var(--text-mute)', marginTop: 4 }}>Chọn ít nhất một vai trò</div>
          )}
        </FormField>

        <FormField label='Tiêu đề'>
          <input name='title' value={form.title} onChange={handle} required placeholder='VD: Nhắc nhở nộp bài vòng 1' />
        </FormField>
        <FormField label='Nội dung'>
          <textarea
            name='content'
            value={form.content}
            onChange={handle}
            required
            rows={4}
            placeholder='Nhập nội dung thông báo...'
            style={{ resize: 'vertical' }}
          />
        </FormField>

        <LoadingButton loading={loading} type='submit'>
          Gửi theo sự kiện
        </LoadingButton>
        <FormMessage message={message?.text} type={message?.type} />
      </form>
    </div>
  )
}

// ─── Page ─────────────────────────────────────────────────────────────────────
export default function StaffAnnouncementsPage() {
  return (
    <>
      <div className='section-title'>
        <h2>Gửi toàn hệ thống</h2>
        <span className='hint'>Thông báo đến tất cả người dùng</span>
      </div>
      <div className='cards'>
        <SendAllForm />
      </div>

      <div className='section-title' style={{ marginTop: 24 }}>
        <h2>Gửi theo sự kiện</h2>
        <span className='hint'>Lọc theo event và vai trò cụ thể</span>
      </div>
      <div className='cards'>
        <SendParticipantForm />
      </div>
    </>
  )
}

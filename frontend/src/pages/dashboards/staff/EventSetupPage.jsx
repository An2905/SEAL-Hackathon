import { useEffect, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import FormField from '../../../components/common/FormField'
import FormMessage from '../../../components/common/FormMessage'
import LoadingButton from '../../../components/common/LoadingButton'
import { getAllEvents, getEventDetail } from '../../../api/event'
import { createEventGroup, createEventRound } from '../../../api/eventService'
import { useToast } from '../../../context/ToastContext'
import { localizeError } from '../../../utils/errors'

function SetupPanelFooter({ eventId }) {
  if (!eventId) return null
  return (
    <p className='card-sub' style={{ marginTop: 12 }}>
      <Link to={`/staff/events/${encodeURIComponent(eventId)}`}>
        ← Quay lại chi tiết sự kiện
      </Link>
    </p>
  )
}

function CreateGroupForm({ eventId, rounds, disabled }) {
  const { showToast } = useToast()
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState(null)
  const [form, setForm] = useState({ roundId: '', name: '', maxTeams: '' })
  const handle = (e) => setForm((f) => ({ ...f, [e.target.name]: e.target.value }))

  const submit = async (e) => {
    e.preventDefault()
    setMessage(null)
    if (!form.roundId) {
      setMessage({ text: 'Vui lòng chọn vòng thi', type: 'error' })
      return
    }
    if (!form.name.trim()) {
      setMessage({ text: 'Vui lòng nhập tên bảng', type: 'error' })
      return
    }
    setLoading(true)
    try {
      const created = await createEventGroup({
        eventId,
        roundId: form.roundId,
        name: form.name,
        maxTeams: form.maxTeams
      })
      setMessage({
        text: `Đã tạo bảng "${created.name}" trong vòng ${created.roundName || created.roundId}`,
        type: 'success'
      })
      showToast('Đã tạo bảng thi', 'success')
      setForm({ roundId: form.roundId, name: '', maxTeams: '' })
    } catch (err) {
      setMessage({ text: localizeError(err.message), type: 'error' })
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className='card' id='setup-group'>
      <div className='card-head'>
        <div className='card-title'>Thêm bảng thi</div>
      </div>
      <p className='card-sub'>
        Mỗi bảng thuộc một vòng cụ thể (ví dụ: Bảng A, Bảng B trong Vòng 1).
      </p>
      {!rounds.length ? (
        <div className='empty-state'>Tạo ít nhất một vòng thi trước khi thêm bảng.</div>
      ) : (
        <form className='form' onSubmit={submit}>
          <FormField label='Vòng thi *'>
            <select
              name='roundId'
              value={form.roundId}
              onChange={handle}
              disabled={disabled || loading}
              required
            >
              <option value=''>— Chọn vòng —</option>
              {rounds.map((r) => (
                <option key={r.roundId} value={r.roundId}>
                  {r.name}
                </option>
              ))}
            </select>
          </FormField>
          <FormField label='Tên bảng *'>
            <input
              name='name'
              value={form.name}
              onChange={handle}
              maxLength={100}
              disabled={disabled || loading}
              placeholder='VD: Bảng A'
            />
          </FormField>
          <FormField label='Số đội tối đa (tuỳ chọn)'>
            <input
              type='number'
              name='maxTeams'
              value={form.maxTeams}
              onChange={handle}
              min={1}
              disabled={disabled || loading}
              placeholder='Để trống = không giới hạn'
            />
          </FormField>
          <LoadingButton loading={loading} type='submit' disabled={disabled || !rounds.length}>
            Tạo bảng
          </LoadingButton>
          <FormMessage message={message?.text} type={message?.type} />
        </form>
      )}
    </div>
  )
}

function CreateRoundForm({ eventId, disabled }) {
  const { showToast } = useToast()
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState(null)
  const [form, setForm] = useState({
    name: '',
    startDate: '',
    endDate: '',
    submissionDeadline: ''
  })
  const handle = (e) => setForm((f) => ({ ...f, [e.target.name]: e.target.value }))

  const submit = async (e) => {
    e.preventDefault()
    setMessage(null)
    if (!form.name.trim()) {
      setMessage({ text: 'Vui lòng nhập tên vòng', type: 'error' })
      return
    }
    if (!form.startDate || !form.endDate || !form.submissionDeadline) {
      setMessage({ text: 'Vui lòng nhập đầy đủ thời gian', type: 'error' })
      return
    }
    setLoading(true)
    try {
      const created = await createEventRound({
        eventId,
        name: form.name,
        startDate: form.startDate,
        endDate: form.endDate,
        submissionDeadline: form.submissionDeadline
      })
      setMessage({
        text: `Đã tạo vòng "${created.name}" (thứ tự ${created.roundOrder})`,
        type: 'success'
      })
      showToast('Đã tạo vòng thi', 'success')
      setForm({
        name: '',
        startDate: '',
        endDate: '',
        submissionDeadline: ''
      })
    } catch (err) {
      setMessage({ text: localizeError(err.message), type: 'error' })
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className='card' id='setup-round'>
      <div className='card-head'>
        <div className='card-title'>Thêm vòng thi</div>
      </div>
      <p className='card-sub'>
        Thứ tự vòng được tự động tăng theo số vòng hiện có của sự kiện.
      </p>
      <form className='form' onSubmit={submit}>
        <FormField label='Tên vòng *'>
          <input
            name='name'
            value={form.name}
            onChange={handle}
            maxLength={100}
            disabled={disabled || loading}
            placeholder='VD: Vòng 1 — Ý tưởng'
          />
        </FormField>
        <FormField label='Bắt đầu *'>
          <input
            type='datetime-local'
            name='startDate'
            value={form.startDate}
            onChange={handle}
            disabled={disabled || loading}
          />
        </FormField>
        <FormField label='Kết thúc *'>
          <input
            type='datetime-local'
            name='endDate'
            value={form.endDate}
            onChange={handle}
            disabled={disabled || loading}
          />
        </FormField>
        <FormField label='Deadline nộp bài *'>
          <input
            type='datetime-local'
            name='submissionDeadline'
            value={form.submissionDeadline}
            onChange={handle}
            disabled={disabled || loading}
          />
        </FormField>
        <LoadingButton loading={loading} type='submit' disabled={disabled}>
          Tạo vòng thi
        </LoadingButton>
        <FormMessage message={message?.text} type={message?.type} />
      </form>
    </div>
  )
}

export default function EventSetupPage() {
  const { showToast } = useToast()
  const [searchParams] = useSearchParams()
  const [events, setEvents] = useState([])
  const [eventId, setEventId] = useState('')
  const [rounds, setRounds] = useState([])
  const groupRef = useRef(null)
  const roundRef = useRef(null)

  useEffect(() => {
    const fromUrl = searchParams.get('eventId') ?? ''
    if (fromUrl) setEventId(fromUrl)
  }, [searchParams])

  useEffect(() => {
    let cancelled = false
    ;(async () => {
      try {
        const list = await getAllEvents('ALL')
        if (!cancelled) setEvents(list)
      } catch {
        if (!cancelled) showToast('Không tải được danh sách sự kiện', 'error')
      }
    })()
    return () => {
      cancelled = true
    }
  }, [showToast])

  useEffect(() => {
    if (!eventId) {
      setRounds([])
      return
    }
    let cancelled = false
    ;(async () => {
      try {
        const detail = await getEventDetail(eventId)
        if (!cancelled) setRounds(detail?.rounds ?? [])
      } catch {
        if (!cancelled) {
          setRounds([])
          showToast('Không tải được vòng thi của sự kiện', 'error')
        }
      }
    })()
    return () => {
      cancelled = true
    }
  }, [eventId, showToast])

  useEffect(() => {
    const focus = searchParams.get('focus')
    if (!focus) return
    const el =
      focus === 'round'
        ? roundRef.current
        : focus === 'group'
          ? groupRef.current
          : null
    if (el) {
      requestAnimationFrame(() => {
        el.scrollIntoView({ behavior: 'smooth', block: 'start' })
      })
    }
  }, [searchParams, eventId])

  const ready = !!eventId

  return (
    <>
      <div className='section-title'>
        <h2>Cấu hình bảng & vòng thi</h2>
        <span className='hint'>Chọn sự kiện rồi thêm vòng thi và bảng trong từng vòng</span>
      </div>

      <div className='card'>
        <div className='card-head'>
          <div className='card-title'>Chọn sự kiện</div>
        </div>
        <FormField label='Sự kiện'>
          <select value={eventId} onChange={(e) => setEventId(e.target.value)}>
            <option value=''>— Chọn sự kiện —</option>
            {events.map((ev) => (
              <option key={ev.eventId} value={ev.eventId}>
                {ev.title}
              </option>
            ))}
          </select>
        </FormField>
        <SetupPanelFooter eventId={eventId} />
      </div>

      <div className='cards'>
        <div ref={roundRef}>
          <CreateRoundForm eventId={eventId} disabled={!ready} />
        </div>
        <div ref={groupRef}>
          <CreateGroupForm eventId={eventId} rounds={rounds} disabled={!ready} />
        </div>
      </div>
    </>
  )
}

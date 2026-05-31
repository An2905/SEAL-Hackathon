import { useEffect, useState } from 'react'
import FormField from '../../../components/common/FormField'
import FormMessage from '../../../components/common/FormMessage'
import LoadingButton from '../../../components/common/LoadingButton'
import { getAllEvents, getEventDetail } from '../../../api/event'
import { getAllAccounts, assignJudge, assignMentor } from '../../../api/staff'
import { useToast } from '../../../context/ToastContext'
import { localizeError } from '../../../utils/errors'

function AssignJudgeForm({ judges, rounds, categories, disabled }) {
  const { showToast } = useToast()
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState(null)
  const [form, setForm] = useState({ judgeId: '', roundId: '', categoryId: '' })
  const handle = (e) => setForm((f) => ({ ...f, [e.target.name]: e.target.value }))

  const submit = async (e) => {
    e.preventDefault()
    setMessage(null)
    if (!form.judgeId || !form.roundId || !form.categoryId) {
      setMessage({ text: 'Vui lòng chọn đầy đủ Judge, vòng và track', type: 'error' })
      return
    }
    setLoading(true)
    try {
      await assignJudge(form)
      setMessage({ text: 'Đã phân công judge thành công!', type: 'success' })
      showToast('Đã phân công judge', 'success')
      setForm({ judgeId: '', roundId: '', categoryId: '' })
    } catch (err) {
      setMessage({ text: localizeError(err.message), type: 'error' })
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className='card'>
      <div className='card-head'>
        <div className='card-title'>Phân công Giám khảo (theo vòng)</div>
      </div>
      <p className='card-sub'>
        Mỗi judge được gán vào một <strong>vòng</strong> trong một <strong>track</strong>.
      </p>
      <form className='form' onSubmit={submit}>
        <FormField label='Giám khảo'>
          <select name='judgeId' value={form.judgeId} onChange={handle} disabled={disabled || loading}>
            <option value=''>— Chọn judge —</option>
            {judges.map((j) => (
              <option key={j.userId} value={j.userId}>
                {j.fullName || j.email} (#{j.userId})
              </option>
            ))}
          </select>
        </FormField>
        <FormField label='Vòng'>
          <select name='roundId' value={form.roundId} onChange={handle} disabled={disabled || loading}>
            <option value=''>— Chọn vòng —</option>
            {rounds.map((r) => (
              <option key={r.roundId} value={r.roundId}>
                {r.name} (#{r.roundId})
              </option>
            ))}
          </select>
        </FormField>
        <FormField label='Track (category)'>
          <select name='categoryId' value={form.categoryId} onChange={handle} disabled={disabled || loading}>
            <option value=''>— Chọn track —</option>
            {categories.map((c) => (
              <option key={c.categoryId} value={c.categoryId}>
                {c.name} (#{c.categoryId})
              </option>
            ))}
          </select>
        </FormField>
        <LoadingButton loading={loading} type='submit' disabled={disabled}>
          Phân công judge
        </LoadingButton>
        <FormMessage message={message?.text} type={message?.type} />
      </form>
    </div>
  )
}

function AssignMentorForm({ mentors, categories, disabled }) {
  const { showToast } = useToast()
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState(null)
  const [form, setForm] = useState({ userId: '', categoryId: '' })
  const handle = (e) => setForm((f) => ({ ...f, [e.target.name]: e.target.value }))

  const submit = async (e) => {
    e.preventDefault()
    setMessage(null)
    if (!form.userId || !form.categoryId) {
      setMessage({ text: 'Vui lòng chọn Mentor và track', type: 'error' })
      return
    }
    setLoading(true)
    try {
      await assignMentor(form)
      setMessage({ text: 'Đã phân công mentor thành công!', type: 'success' })
      showToast('Đã phân công mentor', 'success')
      setForm({ userId: '', categoryId: '' })
    } catch (err) {
      setMessage({ text: localizeError(err.message), type: 'error' })
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className='card'>
      <div className='card-head'>
        <div className='card-title'>Phân công Mentor (theo track)</div>
      </div>
      <p className='card-sub'>
        Mỗi mentor được gán vào một <strong>track</strong> của sự kiện.
      </p>
      <form className='form' onSubmit={submit}>
        <FormField label='Mentor'>
          <select name='userId' value={form.userId} onChange={handle} disabled={disabled || loading}>
            <option value=''>— Chọn mentor —</option>
            {mentors.map((m) => (
              <option key={m.userId} value={m.userId}>
                {m.fullName || m.email} (#{m.userId})
              </option>
            ))}
          </select>
        </FormField>
        <FormField label='Track (category)'>
          <select name='categoryId' value={form.categoryId} onChange={handle} disabled={disabled || loading}>
            <option value=''>— Chọn track —</option>
            {categories.map((c) => (
              <option key={c.categoryId} value={c.categoryId}>
                {c.name} (#{c.categoryId})
              </option>
            ))}
          </select>
        </FormField>
        <LoadingButton loading={loading} type='submit' disabled={disabled}>
          Phân công mentor
        </LoadingButton>
        <FormMessage message={message?.text} type={message?.type} />
      </form>
    </div>
  )
}

export default function StaffAssignPage() {
  const { showToast } = useToast()
  const [events, setEvents] = useState([])
  const [eventId, setEventId] = useState('')
  const [detail, setDetail] = useState(null)
  const [judges, setJudges] = useState([])
  const [mentors, setMentors] = useState([])
  const [loadingDetail, setLoadingDetail] = useState(false)

  useEffect(() => {
    let cancelled = false
    ;(async () => {
      const [evResult, jsResult, msResult] = await Promise.allSettled([
        getAllEvents('ALL'),
        getAllAccounts('JUDGE_INTERNAL'),
        getAllAccounts('MENTOR')
      ])
      if (cancelled) return

      if (evResult.status === 'fulfilled') setEvents(evResult.value)
      else showToast('Không tải được danh sách sự kiện', 'error')

      if (jsResult.status === 'fulfilled') setJudges(jsResult.value)
      else showToast('Không tải được danh sách Judge (lỗi BE)', 'error')

      if (msResult.status === 'fulfilled') setMentors(msResult.value)
      else showToast('Không tải được danh sách Mentor (lỗi BE)', 'error')
    })()
    return () => {
      cancelled = true
    }
  }, [showToast])

  useEffect(() => {
    if (!eventId) {
      setDetail(null)
      return
    }
    let cancelled = false
    setLoadingDetail(true)
    ;(async () => {
      try {
        const d = await getEventDetail(eventId)
        if (!cancelled) setDetail(d)
      } catch (err) {
        if (!cancelled) {
          setDetail(null)
          showToast(localizeError(err.message), 'error')
        }
      } finally {
        if (!cancelled) setLoadingDetail(false)
      }
    })()
    return () => {
      cancelled = true
    }
  }, [eventId, showToast])

  const rounds = detail?.rounds ?? []
  const categories = detail?.categories ?? []
  const ready = !!detail && !loadingDetail

  return (
    <>
      <div className='section-title'>
        <h2>Phân công Judge / Mentor</h2>
        <span className='hint'>Chọn sự kiện rồi gán giám khảo theo vòng, mentor theo track</span>
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
                {ev.title} (#{ev.eventId})
              </option>
            ))}
          </select>
        </FormField>
        {loadingDetail && (
          <div className='empty-state' style={{ marginTop: 12 }}>
            Đang tải vòng & track…
          </div>
        )}
        {ready && rounds.length === 0 && (
          <div className='empty-state' style={{ marginTop: 12 }}>
            Sự kiện này chưa có vòng nào.
          </div>
        )}
        {ready && categories.length === 0 && (
          <div className='empty-state' style={{ marginTop: 12 }}>
            Sự kiện này chưa có track nào.
          </div>
        )}
      </div>

      <div className='cards'>
        <AssignJudgeForm judges={judges} rounds={rounds} categories={categories} disabled={!ready} />
        <AssignMentorForm mentors={mentors} categories={categories} disabled={!ready} />
      </div>
    </>
  )
}

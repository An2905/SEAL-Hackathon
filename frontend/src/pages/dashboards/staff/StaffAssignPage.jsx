import { useEffect, useMemo, useState } from 'react'
import FormField from '../../../components/common/FormField'
import FormMessage from '../../../components/common/FormMessage'
import LoadingButton from '../../../components/common/LoadingButton'
import { getAllEvents, getEventDetail } from '../../../api/event'
import { getAllAccounts, assignJudge, assignMentor } from '../../../api/staff'
import { useToast } from '../../../context/ToastContext'
import { localizeError } from '../../../utils/errors'

function groupLabel(g) {
  const round = g.roundName ? `${g.roundName} · ` : ''
  return `${round}${g.name || '—'}`
}

function AssignJudgeForm({ judges, rounds, groups, disabled }) {
  const { showToast } = useToast()
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState(null)
  const [form, setForm] = useState({ judgeId: '', roundId: '', groupId: '' })
  const handle = (e) => {
    const { name, value } = e.target
    setForm((f) => {
      const next = { ...f, [name]: value }
      if (name === 'roundId') next.groupId = ''
      return next
    })
  }

  const filteredGroups = useMemo(
    () => groups.filter((g) => !form.roundId || g.roundId === form.roundId),
    [groups, form.roundId]
  )

  const submit = async (e) => {
    e.preventDefault()
    setMessage(null)
    if (!form.judgeId || !form.roundId || !form.groupId) {
      setMessage({ text: 'Vui lòng chọn đầy đủ Judge, vòng và bảng', type: 'error' })
      return
    }
    setLoading(true)
    try {
      await assignJudge(form)
      setMessage({ text: 'Đã phân công judge thành công!', type: 'success' })
      showToast('Đã phân công judge', 'success')
      setForm({ judgeId: '', roundId: '', groupId: '' })
    } catch (err) {
      setMessage({ text: localizeError(err.message), type: 'error' })
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className='card'>
      <div className='card-head'>
        <div className='card-title'>Phân công Giám khảo</div>
      </div>
      <p className='card-sub'>
        Mỗi judge được gán vào một{' '}
        <strong>bảng</strong> trong một{' '}
        <strong>vòng</strong>.
      </p>
      <form className='form' onSubmit={submit}>
        <FormField label='Giám khảo'>
          <select name='judgeId' value={form.judgeId} onChange={handle} disabled={disabled || loading}>
            <option value=''>— Chọn judge —</option>
            {judges.map((j) => (
              <option key={j.userId} value={j.userId}>
                {j.fullName || j.email}
              </option>
            ))}
          </select>
        </FormField>
        <FormField label='Vòng'>
          <select name='roundId' value={form.roundId} onChange={handle} disabled={disabled || loading}>
            <option value=''>— Chọn vòng —</option>
            {rounds.map((r) => (
              <option key={r.roundId} value={r.roundId}>
                {r.name}
              </option>
            ))}
          </select>
        </FormField>
        <FormField label='Bảng'>
          <select name='groupId' value={form.groupId} onChange={handle} disabled={disabled || loading || !form.roundId}>
            <option value=''>— Chọn bảng —</option>
            {filteredGroups.map((g) => (
              <option key={g.groupId} value={g.groupId}>
                {g.name}
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

function AssignMentorForm({ mentors, rounds, groups, disabled }) {
  const { showToast } = useToast()
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState(null)
  const [form, setForm] = useState({ userId: '', roundId: '', groupId: '' })
  const handle = (e) => {
    const { name, value } = e.target
    setForm((f) => {
      const next = { ...f, [name]: value }
      if (name === 'roundId') next.groupId = ''
      return next
    })
  }

  const filteredGroups = useMemo(
    () => groups.filter((g) => !form.roundId || g.roundId === form.roundId),
    [groups, form.roundId]
  )

  const submit = async (e) => {
    e.preventDefault()
    setMessage(null)
    if (!form.userId || !form.roundId || !form.groupId) {
      setMessage({ text: 'Vui lòng chọn Mentor, vòng và bảng', type: 'error' })
      return
    }
    setLoading(true)
    try {
      await assignMentor(form)
      setMessage({ text: 'Đã phân công mentor thành công!', type: 'success' })
      showToast('Đã phân công mentor', 'success')
      setForm({ userId: '', roundId: '', groupId: '' })
    } catch (err) {
      setMessage({ text: localizeError(err.message), type: 'error' })
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className='card'>
      <div className='card-head'>
        <div className='card-title'>Phân công Mentor</div>
      </div>
      <p className='card-sub'>
        Mỗi mentor được gán vào một{' '}
        <strong>bảng</strong> trong một{' '}
        <strong>vòng</strong>.
      </p>
      <form className='form' onSubmit={submit}>
        <FormField label='Mentor'>
          <select name='userId' value={form.userId} onChange={handle} disabled={disabled || loading}>
            <option value=''>— Chọn mentor —</option>
            {mentors.map((m) => (
              <option key={m.userId} value={m.userId}>
                {m.fullName || m.email}
              </option>
            ))}
          </select>
        </FormField>
        <FormField label='Vòng'>
          <select name='roundId' value={form.roundId} onChange={handle} disabled={disabled || loading}>
            <option value=''>— Chọn vòng —</option>
            {rounds.map((r) => (
              <option key={r.roundId} value={r.roundId}>
                {r.name}
              </option>
            ))}
          </select>
        </FormField>
        <FormField label='Bảng'>
          <select name='groupId' value={form.groupId} onChange={handle} disabled={disabled || loading || !form.roundId}>
            <option value=''>— Chọn bảng —</option>
            {filteredGroups.map((g) => (
              <option key={g.groupId} value={g.groupId}>
                {g.name}
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
        getAllAccounts('EXPERT'),
        getAllAccounts('EXPERT')
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
  const groups = detail?.groups ?? []
  const ready = !!detail && !loadingDetail

  return (
    <>
      <div className='section-title'>
        <h2>Phân công Judge / Mentor</h2>
        <span className='hint'>Chọn sự kiện rồi gán theo vòng và bảng thi</span>
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
        {loadingDetail && (
          <div className='empty-state' style={{ marginTop: 12 }}>
            Đang tải vòng & bảng…
          </div>
        )}
        {ready && rounds.length === 0 && (
          <div className='empty-state' style={{ marginTop: 12 }}>
            Sự kiện này chưa có vòng nào.
          </div>
        )}
        {ready && groups.length === 0 && (
          <div className='empty-state' style={{ marginTop: 12 }}>
            Sự kiện này chưa có bảng thi nào.
          </div>
        )}
      </div>

      <div className='cards'>
        <AssignJudgeForm judges={judges} rounds={rounds} groups={groups} disabled={!ready} />
        <AssignMentorForm mentors={mentors} rounds={rounds} groups={groups} disabled={!ready} />
      </div>
    </>
  )
}

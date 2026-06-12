import { useEffect, useMemo, useState } from 'react'
import FullWidthSearchBar from '../common/FullWidthSearchBar'
import { getPublicEvents, PUBLIC_STATUS_ORDER } from '../../api/publicEvent'

const STATUS_LABELS = {
  ONGOING: 'Đang diễn ra',
  UPCOMING: 'Sắp diễn ra',
  COMPLETED: 'Đã kết thúc'
}

const STATUS_PILL_CLASS = {
  ONGOING: 'status-active',
  UPCOMING: 'status-pending',
  COMPLETED: 'status-default'
}

function formatEventDate(value) {
  if (!value) return '—'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return String(value)
  return d.toLocaleDateString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric'
  })
}

function formatDateRange(start, end) {
  const s = formatEventDate(start)
  const e = formatEventDate(end)
  if (s === '—' && e === '—') return '—'
  if (s === e || e === '—') return s
  if (s === '—') return e
  return `${s} – ${e}`
}

function eventMatchesSearch(event, query) {
  if (!query) return true
  const status = String(event.status ?? '').toUpperCase()
  const statusLabel = STATUS_LABELS[status] ?? status
  const haystack = [event.title, event.description, status, statusLabel].filter(Boolean).join(' ').toLowerCase()
  return haystack.includes(query)
}

function EventRow({ event }) {
  const status = String(event.status ?? '').toUpperCase()
  return (
    <article className='landing-event-row'>
      <div className='landing-event-row-status'>
        <span className={`status-pill ${STATUS_PILL_CLASS[status] ?? 'status-default'}`}>
          {STATUS_LABELS[status] ?? status}
        </span>
      </div>
      <div className='landing-event-row-main'>
        <h3 className='landing-event-row-title'>{event.title || '—'}</h3>
        <p className='landing-event-row-desc'>{event.description?.trim() || 'Chưa có mô tả.'}</p>
      </div>
      <div className='landing-event-row-date'>{formatDateRange(event.startDate, event.endDate)}</div>
    </article>
  )
}

export default function LandingEventsSection({ onOpenRegister }) {
  const [events, setEvents] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [searchInput, setSearchInput] = useState('')
  const [searchQuery, setSearchQuery] = useState('')

  useEffect(() => {
    let cancelled = false
    ;(async () => {
      setLoading(true)
      setError('')
      try {
        const rows = await getPublicEvents()
        if (!cancelled) setEvents(rows)
      } catch (err) {
        if (!cancelled) {
          setEvents([])
          setError(err?.message || 'Không tải được sự kiện')
        }
      } finally {
        if (!cancelled) setLoading(false)
      }
    })()
    return () => {
      cancelled = true
    }
  }, [])

  const filteredEvents = useMemo(() => {
    const q = searchQuery.trim().toLowerCase()
    return events.filter((ev) => eventMatchesSearch(ev, q))
  }, [events, searchQuery])

  const grouped = useMemo(() => {
    const map = new Map(PUBLIC_STATUS_ORDER.map((s) => [s, []]))
    for (const ev of filteredEvents) {
      const key = String(ev.status ?? '').toUpperCase()
      if (map.has(key)) map.get(key).push(ev)
    }
    return PUBLIC_STATUS_ORDER.map((status) => ({
      status,
      label: STATUS_LABELS[status],
      items: map.get(status)
    })).filter((g) => g.items.length > 0)
  }, [filteredEvents])

  const hasOpenEvents = filteredEvents.some((ev) => ev.status !== 'COMPLETED')

  return (
    <section className='landing-events-section' id='events'>
      <div className='landing-events-inner'>
        <div className='landing-events-section-head'>
          <h2>Sự kiện hackathon</h2>
          <p className='landing-events-lead'>Tìm cuộc thi phù hợp, đăng ký tài khoản và tham gia cùng đội của bạn.</p>
        </div>

        <div className='landing-events-panel'>
          <FullWidthSearchBar
            className='fullwidth-search-bar--compact landing-events-search'
            value={searchInput}
            onChange={(value) => {
              setSearchInput(value)
              setSearchQuery(
                String(value ?? '')
                  .trim()
                  .toLowerCase()
              )
            }}
            onSearch={(value) =>
              setSearchQuery(
                String(value ?? '')
                  .trim()
                  .toLowerCase()
              )
            }
            placeholder='Tìm theo tên, mô tả hoặc trạng thái…'
            disabled={loading}
          />

          {loading && <div className='landing-events-empty'>Đang tải sự kiện…</div>}
          {!loading && error && <div className='landing-events-empty'>{error}</div>}
          {!loading && !error && events.length === 0 && (
            <div className='landing-events-empty'>Hiện chưa có sự kiện công khai.</div>
          )}
          {!loading && !error && events.length > 0 && filteredEvents.length === 0 && (
            <div className='landing-events-empty'>Không tìm thấy sự kiện khớp với &quot;{searchQuery}&quot;.</div>
          )}

          {!loading && !error && grouped.length > 0 && (
            <div className='landing-events-groups'>
              {grouped.map((group) => (
                <div key={group.status} className='landing-events-group'>
                  <h3 className='landing-events-group-title'>{group.label}</h3>
                  <div className='landing-events-list'>
                    {group.items.map((ev) => (
                      <EventRow key={ev.eventId} event={ev} />
                    ))}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {onOpenRegister && !loading && hasOpenEvents && (
          <div className='landing-events-cta'>
            <button type='button' className='btn btn-primary btn-lg' onClick={onOpenRegister}>
              Đăng ký tham gia ngay
            </button>
          </div>
        )}
      </div>
    </section>
  )
}

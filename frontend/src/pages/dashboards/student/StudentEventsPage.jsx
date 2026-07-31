import { useState, useEffect, useCallback } from 'react'
import FormField from '../../../components/common/FormField'
import FormMessage from '../../../components/common/FormMessage'
import LoadingButton from '../../../components/common/LoadingButton'
import { getUpcomingEvents } from '../../../api/publicEvent'
import { joinEvent, getTeamRegistrations, getTeamTrackMentors, dropEvent } from '../../../api/team'
import { useToast } from '../../../context/ToastContext'
import { localizeError } from '../../../utils/errors'
import { eventStatusLabel } from '../../../utils/eventStatusLabels'
import Pagination from '../../../components/common/Pagination'
import LoadingState from '../../../components/common/LoadingState'
import {
  PAGE_SIZE,
  REGISTRATION_FILTERS,
  formatDateTime,
  registrationStatusPillClass,
  registrationStatusLabel,
  eventStatusPillClass,
  hasOngoingOrUpcomingRegistration,
  DashboardSection,
  DetailMeta,
  StatusStack,
  StatusBadge,
  FilterTabs,
  IconGithub,
  GithubRequiredBanner
} from './shared'

function EventMentorsBlock({ registration, mentorState }) {
  const [mentorsPage, setMentorsPage] = useState(1)
  const status = (registration.registrationStatus || '').toUpperCase()
  const state = mentorState || {}

  if (status !== 'APPROVED') {
    return <div className='student-inline-note'>Mentor hiển thị sau khi đăng ký được duyệt.</div>
  }

  if (state.loading) {
    return <LoadingState text='Đang tải mentor…' className='student-inline-loading' />
  }

  if (state.error) {
    return <div className='student-inline-note student-inline-note--error'>{state.error}</div>
  }

  const mentors = state.data?.mentors || []
  if (mentors.length === 0) {
    return <div className='student-inline-note'>Chưa có mentor cho bảng này.</div>
  }

  return (
    <div className='student-mentors-block'>
      <div className='student-mentors-block__label'>Mentor bảng</div>
      <div className='kv-list student-mentors-list'>
        {mentors.slice((mentorsPage - 1) * PAGE_SIZE, mentorsPage * PAGE_SIZE).map((m) => (
          <div className='member-row' key={m.mentorId}>
            <div className='avatar'>{(m.mentorName?.[0] || 'M').toUpperCase()}</div>
            <div className='member-info'>
              <div className='member-name-row'>
                <div className='member-name'>{m.mentorName || '—'}</div>
              </div>
              <div className='member-meta'>{m.mentorEmail || ''}</div>
            </div>
          </div>
        ))}
      </div>
      <Pagination total={mentors.length} pageSize={PAGE_SIZE} currentPage={mentorsPage} onChange={setMentorsPage} />
    </div>
  )
}

// ─── Sự kiện + mentor ─────────────────────────────────────────────────────────
function TeamEventsPanel({ refreshKey, isLeader, teamId, onRegisterSuccess }) {
  const { showToast } = useToast()
  const [list, setList] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [mentorsByEvent, setMentorsByEvent] = useState({})
  const [statusFilter, setStatusFilter] = useState('APPROVED')

  const handleDropEvent = useCallback(
    async (reg) => {
      if (!teamId) return
      try {
        await dropEvent({ teamId, eventId: reg.eventId })
        setList((prev) => prev.filter((r) => r.registrationId !== reg.registrationId))
        showToast('Đã rút đăng ký khỏi sự kiện.', 'success')
      } catch (err) {
        showToast(localizeError(err.message), 'error')
      }
    },
    [teamId, showToast]
  )

  useEffect(() => {
    let cancelled = false
    ;(async () => {
      setLoading(true)
      setError(null)
      setMentorsByEvent({})
      try {
        const data = await getTeamRegistrations()
        if (cancelled) return
        setList(data)

        const approved = data.filter((r) => (r.registrationStatus || '').toUpperCase() === 'APPROVED' && r.eventId)
        if (approved.length === 0) return

        const loadingMap = Object.fromEntries(approved.map((r) => [r.eventId, { loading: true }]))
        setMentorsByEvent(loadingMap)

        const results = await Promise.allSettled(approved.map((r) => getTeamTrackMentors(r.eventId)))

        if (cancelled) return

        const next = {}
        approved.forEach((r, i) => {
          const result = results[i]
          if (result.status === 'fulfilled') {
            next[r.eventId] = { loading: false, data: result.value }
          } else {
            next[r.eventId] = {
              loading: false,
              error: localizeError(result.reason?.message)
            }
          }
        })
        setMentorsByEvent(next)
      } catch (err) {
        if (!cancelled) {
          setError(localizeError(err.message))
          showToast('Không tải được danh sách đăng ký sự kiện', 'error')
        }
      } finally {
        if (!cancelled) setLoading(false)
      }
    })()
    return () => {
      cancelled = true
    }
  }, [refreshKey, showToast])

  const filteredList = list.filter((reg) => {
    if (statusFilter === 'ALL') return true
    return (reg.registrationStatus || '').toUpperCase() === statusFilter
  })

  const showRegisterForm = isLeader && !loading && !hasOngoingOrUpcomingRegistration(list)

  useEffect(() => {
    setStatusFilter('APPROVED')
  }, [refreshKey])

  return (
    <div className='card'>
      {showRegisterForm && (
        <div className='student-events-register'>
          <JoinEventForm embedded onSuccess={onRegisterSuccess} />
        </div>
      )}

      {!loading && list.length > 0 && (
        <FilterTabs options={REGISTRATION_FILTERS} value={statusFilter} onChange={setStatusFilter} />
      )}

      {loading && <LoadingState text='Đang tải danh sách sự kiện…' />}
      {!loading && error && <div className='empty-state'>{error}</div>}
      {!loading && !error && list.length === 0 && (
        <div className='empty-state'>
          {isLeader && showRegisterForm
            ? 'Đội chưa đăng ký sự kiện nào. Dùng form phía trên để đăng ký.'
            : 'Đội chưa đăng ký sự kiện nào.'}
        </div>
      )}
      {!loading && list.length > 0 && filteredList.length === 0 && (
        <div className='empty-state'>Không có sự kiện nào khớp bộ lọc.</div>
      )}
      {!loading && filteredList.length > 0 && (
        <TeamEventsList
          list={filteredList}
          mentorsByEvent={mentorsByEvent}
          isLeader={isLeader}
          onDropEvent={handleDropEvent}
        />
      )}
    </div>
  )
}

function TeamEventsList({ list, mentorsByEvent, isLeader, onDropEvent }) {
  const [page, setPage] = useState(1)
  const visibleItems = list.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE)

  useEffect(() => {
    setPage(1)
  }, [list])

  return (
    <>
      <div className='kv-list student-events-list'>
        {visibleItems.map((reg) => {
          const isOngoing = (reg.eventStatus || '').toUpperCase() === 'ONGOING'
          return (
            <div
              key={reg.registrationId || reg.eventId}
              className={`mentor-team-card student-event-card${isOngoing ? ' student-event-card--ongoing' : ''}`}
            >
              <div className='kv student-kv-row'>
                <span className='student-kv-main'>
                  <div className='student-kv-title'>{reg.eventTitle || '—'}</div>
                  <DetailMeta>
                    Bảng: <strong>{reg.groupName || '—'}</strong>
                    {isOngoing ? (
                      <span className='status-pill status-active student-event-live-pill'>
                        ● {eventStatusLabel(reg.eventStatus)}
                      </span>
                    ) : null}
                  </DetailMeta>
                  <DetailMeta muted>
                    {formatDateTime(reg.eventStartDate)} → {formatDateTime(reg.eventEndDate)}
                  </DetailMeta>
                  <DetailMeta muted>Đăng ký: {formatDateTime(reg.registeredAt)}</DetailMeta>
                  <EventMentorsBlock
                    registration={reg}
                    mentorState={mentorsByEvent[reg.eventId]}
                  />
                </span>
                <StatusStack>
                  {!isOngoing && reg.eventStatus ? (
                    <StatusBadge
                      status={eventStatusLabel(reg.eventStatus)}
                      className={eventStatusPillClass(reg.eventStatus)}
                    />
                  ) : null}
                  <StatusBadge
                    status={registrationStatusLabel(reg.registrationStatus)}
                    className={registrationStatusPillClass(reg.registrationStatus)}
                  />
                  {reg.registrationStatus === 'APPROVED' && reg.githubStatus === 'SUCCESS' && reg.githubRepoUrl && (
                    <div style={{ marginTop: 8 }}>
                      {reg.repoAccessGranted ? (
                        <a
                          href={reg.githubRepoUrl}
                          target='_blank'
                          rel='noopener noreferrer'
                          className='btn btn-success btn-sm'
                          style={{
                            fontSize: 10,
                            padding: '4px 8px',
                            height: 'auto',
                            minHeight: 0,
                            textDecoration: 'none',
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: 4
                          }}
                          title='Đi tới GitHub Repository'
                        >
                          <IconGithub size={12} />
                          Repo làm bài
                        </a>
                      ) : (
                        <button
                          type='button'
                          className='btn btn-sm'
                          disabled
                          style={{
                            fontSize: 10,
                            padding: '4px 8px',
                            height: 'auto',
                            minHeight: 0,
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: 4,
                            background: '#e5e7eb',
                            color: '#9ca3af',
                            borderColor: '#d1d5db',
                            cursor: 'not-allowed'
                          }}
                          title='Quyền truy cập repository chưa được Coordinator kích hoạt'
                        >
                          <IconGithub size={12} />
                          Repo (Khóa)
                        </button>
                      )}
                    </div>
                  )}
                  {isLeader && (reg.registrationStatus || '').toUpperCase() === 'PENDING' && (
                    <button
                      type='button'
                      className='btn btn-sm'
                      style={{
                        marginTop: 6,
                        fontSize: 10,
                        padding: '3px 8px',
                        height: 'auto',
                        minHeight: 0,
                        color: 'var(--danger, #ef4444)',
                        borderColor: 'var(--danger, #ef4444)',
                        background: 'transparent'
                      }}
                      onClick={() => onDropEvent?.(reg)}
                      title='Rút đăng ký khỏi sự kiện này'
                    >
                      Rời sự kiện
                    </button>
                  )}
                </StatusStack>
              </div>
            </div>
          )
        })}
      </div>
      <Pagination total={list.length} pageSize={PAGE_SIZE} currentPage={page} onChange={setPage} />
    </>
  )
}

// ─── Join Event Form ──────────────────────────────────────────────────────────
function JoinEventForm({ onSuccess, embedded = false }) {
  const { showToast } = useToast()
  const [loading, setLoading] = useState(false)
  const [eventsLoading, setEventsLoading] = useState(true)
  const [message, setMessage] = useState(null)
  const [eventId, setEventId] = useState('')
  const [events, setEvents] = useState([])

  useEffect(() => {
    let cancelled = false
    ;(async () => {
      setEventsLoading(true)
      try {
        const [upcoming, registrations] = await Promise.all([getUpcomingEvents(), getTeamRegistrations()])
        if (cancelled) return
        const registeredIds = new Set(registrations.map((r) => String(r.eventId ?? '').trim()).filter(Boolean))
        const available = upcoming.filter((ev) => !registeredIds.has(String(ev.eventId)))
        setEvents(available)
        if (available.length === 1) {
          setEventId(available[0].eventId)
        }
      } catch (err) {
        if (!cancelled) {
          setEvents([])
          setMessage({ text: localizeError(err.message), type: 'error' })
        }
      } finally {
        if (!cancelled) setEventsLoading(false)
      }
    })()
    return () => {
      cancelled = true
    }
  }, [])

  const handleSubmit = async (e) => {
    e.preventDefault()
    setMessage(null)
    if (!eventId) {
      setMessage({ text: 'Vui lòng chọn sự kiện', type: 'error' })
      return
    }
    setLoading(true)
    try {
      const joinedId = eventId
      await joinEvent({ eventId: joinedId })
      setEvents((prev) => prev.filter((ev) => ev.eventId !== joinedId))
      setMessage({ text: 'Đăng ký sự kiện thành công!', type: 'success' })
      showToast('Đăng ký sự kiện thành công', 'success')
      setEventId('')
      onSuccess?.()
    } catch (err) {
      setMessage({ text: localizeError(err.message), type: 'error' })
    } finally {
      setLoading(false)
    }
  }

  const formatEventOption = (ev) => {
    const start = ev.startDate
      ? new Date(ev.startDate).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' })
      : ''
    return start ? `${ev.title} (${start})` : ev.title
  }

  const formBody = (
    <form className='form student-register-event-form' onSubmit={handleSubmit}>
      <FormField label='Sự kiện'>
        {eventsLoading ? (
          <div className='empty-state' style={{ padding: '8px 0' }}>
            Đang tải danh sách sự kiện...
          </div>
        ) : events.length === 0 ? (
          <div className='empty-state' style={{ padding: '8px 0' }}>
            Hiện chưa có sự kiện sắp diễn ra hoặc đội đã đăng ký hết.
          </div>
        ) : (
          <select
            name='eventId'
            value={eventId}
            onChange={(e) => setEventId(e.target.value)}
            required
            disabled={loading}
          >
            <option value=''>-- Chọn sự kiện --</option>
            {events.map((ev) => (
              <option key={ev.eventId} value={ev.eventId}>
                {formatEventOption(ev)}
              </option>
            ))}
          </select>
        )}
      </FormField>
      <LoadingButton
        loading={loading}
        type='submit'
        disabled={eventsLoading || events.length === 0}
        className='btn btn-primary student-register-event-form__submit'
      >
        Đăng ký sự kiện
      </LoadingButton>
      <FormMessage message={message?.text} type={message?.type} />
    </form>
  )

  if (embedded) {
    return (
      <div className='student-events-register__inner'>
        <div className='student-events-register__head'>
          <div className='card-title'>Đăng ký sự kiện mới</div>
          <p className='card-sub' style={{ margin: 0 }}>
            Chọn sự kiện sắp diễn ra — BTC sẽ duyệt và phân bảng sau khi đội đăng ký.
          </p>
        </div>
        {formBody}
      </div>
    )
  }

  return (
    <div className='card'>
      <div className='card-head'>
        <div className='card-title'>Đăng ký sự kiện</div>
      </div>
      <p className='card-sub'>
        Chọn sự kiện đang mở đăng ký (sắp diễn ra). BTC sẽ duyệt và phân bảng sau khi đội đăng ký.
      </p>
      {formBody}
    </div>
  )
}

// ─── Page ─────────────────────────────────────────────────────────────────────
export default function StudentEventsPage({
  isLeader,
  teamId,
  refreshKey,
  onRegisterSuccess,
  githubStatus,
  oauthLoading,
  onConnectGithub
}) {
  return (
    <>
      {!githubStatus.loading && (!githubStatus.linked || !githubStatus.username) ? (
        <GithubRequiredBanner onConnect={onConnectGithub} loading={oauthLoading} isWarning />
      ) : null}

      <DashboardSection title='Sự kiện' hint='Các hackathon đội đã đăng ký — bảng và mentor sau khi BTC duyệt'>
        <TeamEventsPanel
          refreshKey={refreshKey}
          isLeader={isLeader}
          teamId={teamId}
          onRegisterSuccess={onRegisterSuccess}
        />
      </DashboardSection>
    </>
  )
}

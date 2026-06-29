import { useState, useEffect, useCallback, useRef } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import DashboardLayout from '../../components/layout/DashboardLayout'
import FormField from '../../components/common/FormField'
import FormMessage from '../../components/common/FormMessage'
import LoadingButton from '../../components/common/LoadingButton'
import { getUpcomingEvents } from '../../api/publicEvent'
import {
  getMyTeam,
  createTeam,
  joinTeam,
  joinEvent,
  deleteMember,
  getTeamRegistrations,
  getTeamTrackMentors
} from '../../api/team'
import { useToast } from '../../context/ToastContext'
import { localizeError } from '../../utils/errors'
import ChatPopup, { ChatOpenButton } from '../../components/chat/ChatPopup'
import Pagination from '../../components/common/Pagination'
import LoadingState from '../../components/common/LoadingState'
import { getGithubLinkStatus, getGithubLinkUrl } from '../../api/auth'
import Modal from '../../components/common/Modal'

const PAGE_SIZE = 5

const REGISTRATION_FILTERS = [
  { value: 'APPROVED', label: 'Đã duyệt' },
  { value: 'PENDING', label: 'Chờ duyệt' },
  { value: 'ALL', label: 'Tất cả' }
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

function registrationStatusPillClass(status) {
  const key = (status || '').toUpperCase()
  if (key === 'APPROVED') return 'status-active'
  if (key === 'PENDING') return 'status-pending'
  if (key === 'REJECTED') return 'status-rejected'
  return 'status-default'
}

function StatusBadge({ status, className }) {
  return (
    <span className='status-picker' style={{ flexShrink: 0 }}>
      <span className={`status-pill ${className || eventStatusPillClass(status)}`} style={{ cursor: 'default' }}>
        {status || '—'}
      </span>
    </span>
  )
}

function FilterTabs({ options, value, onChange }) {
  return (
    <div className='dashboard-filter-tabs'>
      {options.map((opt) => (
        <button
          key={opt.value}
          type='button'
          className={`btn ${value === opt.value ? 'btn-primary' : 'btn-outline'}`}
          onClick={() => onChange(opt.value)}
        >
          {opt.label}
        </button>
      ))}
    </div>
  )
}

function registrationStatusLabel(status) {
  const key = (status || '').toUpperCase()
  if (key === 'APPROVED') return 'Đã duyệt'
  if (key === 'PENDING') return 'Chờ duyệt'
  if (key === 'REJECTED') return 'Từ chối'
  return status || '—'
}

function eventStatusPillClass(status) {
  const key = (status || '').toUpperCase()
  if (key === 'ONGOING') return 'status-active status-ongoing-highlight'
  if (key === 'BUILDING') return 'status-pending'
  if (key === 'UPCOMING') return 'status-pending'
  if (key === 'COMPLETED') return 'status-default'
  if (key === 'CANCELLED') return 'status-inactive'
  return 'status-default'
}

function eventStatusLabel(status) {
  const key = (status || '').toUpperCase()
  if (key === 'ONGOING') return 'Đang diễn ra'
  if (key === 'BUILDING') return 'Đang thiết lập'
  if (key === 'UPCOMING') return 'Sắp diễn ra'
  if (key === 'COMPLETED') return 'Đã kết thúc'
  if (key === 'CANCELLED') return 'Đã hủy'
  return status || '—'
}

function hasOngoingOrUpcomingRegistration(registrations) {
  return registrations.some((reg) => {
    const key = (reg.eventStatus || '').toUpperCase()
    return key === 'ONGOING' || key === 'UPCOMING'
  })
}

function DashboardSection({ title, hint, children, spaced = false }) {
  return (
    <>
      <div className={`section-title${spaced ? ' section-title--spaced' : ''}`}>
        <h2>{title}</h2>
        {hint ? <span className='hint'>{hint}</span> : null}
      </div>
      {children}
    </>
  )
}

function DetailMeta({ children, muted = false }) {
  return <div className={`student-meta-line${muted ? ' student-meta-line--muted' : ''}`}>{children}</div>
}

function StatusStack({ children }) {
  return <div className='student-status-stack'>{children}</div>
}

// ─── Team Info Card ───────────────────────────────────────────────────────────
function TeamInfoCard({ data, onRefresh, onMemberDeleted }) {
  const { showToast } = useToast()
  const [membersPage, setMembersPage] = useState(1)
  const [deletingId, setDeletingId] = useState(null)
  const members = data.members || []
  const isLeader = Boolean(data.isLeader)

  const handleCopyEnroll = async () => {
    const code = data.enrollCode
    try {
      await navigator.clipboard.writeText(code)
      showToast('Đã sao chép mã enroll: ' + code, 'success')
    } catch {
      showToast('Mã enroll: ' + code, 'success')
    }
  }

  const handleDeleteMember = async (member) => {
    const label = member.fullName || member.email || 'thành viên này'
    if (!window.confirm(`Xóa ${label} khỏi đội?`)) return

    const memberId = (member.email || member.userId || '').trim()
    if (!memberId) return

    setDeletingId(member.userId || memberId)
    try {
      await deleteMember({ memberId })
      showToast('Đã xóa thành viên khỏi đội', 'success')
      onMemberDeleted?.()
    } catch (err) {
      showToast(localizeError(err.message), 'error')
    } finally {
      setDeletingId(null)
    }
  }

  return (
    <div className='card'>
      <div className='mentor-team-card student-team-summary'>
        <div className='kv student-kv-row'>
          <span className='student-kv-main'>
            <div className='student-kv-title'>{data.teamName || '—'}</div>
            <DetailMeta>
              Trưởng nhóm: {data.leaderName || '—'}
              {data.leaderEmail ? ` · ${data.leaderEmail}` : ''}
            </DetailMeta>
            {data.enrollCode && (
              <DetailMeta>
                Mã đội: <code>{data.enrollCode}</code>
              </DetailMeta>
            )}
            <DetailMeta>Thành viên: {data.memberCount ?? members.length} / 5</DetailMeta>
          </span>
          <StatusStack>
            <span className={`role-pill ${isLeader ? 'role-judge' : 'role-student'}`}>
              {isLeader ? 'Leader' : 'Thành viên'}
            </span>
            {data.status ? <StatusBadge status={data.status} className='status-default' /> : null}
          </StatusStack>
        </div>
      </div>

      <div className='student-panel-divider'>
        <span className='student-panel-divider__label'>Thành viên</span>
      </div>
      <div className='kv-list student-members-list'>
        {members.slice((membersPage - 1) * PAGE_SIZE, membersPage * PAGE_SIZE).map((m) => {
          const canDelete = isLeader && !m.isLeader
          const isDeleting = deletingId === (m.userId || m.email)
          return (
            <div className='member-row' key={m.userId}>
              <div className='avatar'>{(m.fullName?.[0] || m.email?.[0] || 'U').toUpperCase()}</div>
              <div className='member-info'>
                <div className='member-name-row'>
                  <div className='member-name'>
                    {m.fullName || '(Chưa có tên)'}
                    {m.isLeader && <span className='leader-tag'>Leader</span>}
                  </div>
                  {canDelete && (
                    <button
                      type='button'
                      className='member-delete-btn'
                      onClick={() => handleDeleteMember(m)}
                      disabled={isDeleting}
                      aria-label={`Xóa ${m.fullName || m.email}`}
                      title='Xóa thành viên'
                    >
                      {isDeleting ? (
                        <span className='spinner spinner-dark spinner--sm' aria-hidden='true' />
                      ) : (
                        <svg
                          width='14'
                          height='14'
                          viewBox='0 0 24 24'
                          fill='none'
                          stroke='currentColor'
                          strokeWidth='2'
                          aria-hidden='true'
                        >
                          <path d='M3 6h18M8 6V4h8v2M19 6l-1 14H6L5 6' strokeLinecap='round' strokeLinejoin='round' />
                          <path d='M10 11v6M14 11v6' strokeLinecap='round' />
                        </svg>
                      )}
                    </button>
                  )}
                </div>
                <div className='member-meta'>{m.email || ''}</div>
              </div>
            </div>
          )
        })}
      </div>
      <Pagination total={members.length} pageSize={PAGE_SIZE} currentPage={membersPage} onChange={setMembersPage} />

      <div className='card-actions student-card-actions'>
        <button type='button' className='btn btn-outline' onClick={handleCopyEnroll}>
          Sao chép mã enroll
        </button>
        <button type='button' className='btn btn-ghost' onClick={onRefresh}>
          Làm mới
        </button>
      </div>
    </div>
  )
}

// ─── GitHub connect ───────────────────────────────────────────────────────────
function IconGithub({ size = 20 }) {
  return (
    <svg width={size} height={size} viewBox='0 0 24 24' fill='currentColor' aria-hidden='true'>
      <path d='M12 0C5.37 0 0 5.37 0 12c0 5.31 3.44 9.8 8.2 11.39.6.11.82-.26.82-.58 0-.29-.01-1.05-.02-2.06-3.34.73-4.04-1.61-4.04-1.61-.55-1.38-1.34-1.75-1.34-1.75-1.09-.75.08-.74.08-.74 1.21.08 1.84 1.24 1.84 1.24 1.07 1.85 2.81 1.31 3.49 1 .11-.78.42-1.31.76-1.61-2.67-.31-5.47-1.34-5.47-5.93 0-1.31.47-2.38 1.24-3.22-.12-.31-.54-1.56.12-3.25 0 0 1.01-.32 3.3 1.23a11.5 11.5 0 0 1 6 0c2.28-1.55 3.29-1.23 3.29-1.23.66 1.69.24 2.94.12 3.25.77.84 1.24 1.91 1.24 3.22 0 4.6-2.81 5.62-5.49 5.92.43.38.82 1.11.82 2.24 0 1.62-.02 2.92-.02 3.32 0 .32.21.69.83.57C20.57 21.79 24 17.31 24 12c0-6.63-5.37-12-12-12z' />
    </svg>
  )
}

function GithubRequiredBanner({ onConnect, loading, isWarning }) {
  return (
    <div className={`github-required-banner${isWarning ? ' github-required-banner--warning' : ''}`} role='status'>
      <div className='github-required-banner__icon' aria-hidden='true'>
        <IconGithub size={22} />
      </div>
      <div className='github-required-banner__body'>
        <p className='github-required-banner__title'>
          {isWarning ? 'Yêu cầu liên kết lại GitHub' : 'Liên kết GitHub để tham gia đội'}
        </p>
        <p className='github-required-banner__text'>
          {isWarning
            ? 'Tài khoản GitHub của bạn chưa được liên kết hoặc đã bị hủy liên kết do thay đổi username. Vui lòng liên kết lại trước khi check-in.'
            : 'Tạo hoặc tham gia đội yêu cầu tài khoản GitHub đã xác thực. Liên kết một lần để tiếp tục.'}
        </p>
      </div>
      <button
        type='button'
        className='btn github-connect-btn github-required-banner__action'
        onClick={onConnect}
        disabled={loading}
      >
        <span className='github-connect-btn-content'>
          <IconGithub size={16} />
          {loading ? 'Đang chuyển hướng...' : isWarning ? 'Liên kết GitHub' : 'Kết nối ngay'}
        </span>
      </button>
    </div>
  )
}

function GithubLinkedBadge({ username }) {
  if (!username) return null
  return (
    <div className='github-linked-badge'>
      <IconGithub size={14} />
      <span>
        GitHub đã liên kết · <strong>@{username}</strong>
      </span>
    </div>
  )
}

// ─── Create Team Form ─────────────────────────────────────────────────────────
function CreateTeamForm({ onSuccess, githubLinked }) {
  const { showToast } = useToast()
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState(null)
  const [teamName, setTeamName] = useState('')

  const handleSubmit = async (e) => {
    e.preventDefault()
    setMessage(null)
    const normalized = teamName.trim().replace(/\s+/g, ' ')
    if (!normalized) {
      setMessage({ text: 'Tên đội không được để trống', type: 'error' })
      return
    }
    if (normalized.length > 100) {
      setMessage({ text: 'Tên đội tối đa 100 ký tự', type: 'error' })
      return
    }
    setLoading(true)
    try {
      const { enrollCode } = await createTeam({ teamName: normalized })
      setMessage({ text: `Tạo đội thành công! Mã enroll: ${enrollCode}`, type: 'success' })
      showToast('Đã tạo đội — mã enroll: ' + enrollCode, 'success')
      setTeamName('')
      setTimeout(onSuccess, 400)
    } catch (err) {
      setMessage({ text: localizeError(err.message), type: 'error' })
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className={`card student-team-card${!githubLinked ? ' student-team-card--locked' : ''}`}>
      <div className='student-team-card__head'>
        <span className='student-team-card__icon student-team-card__icon--create' aria-hidden='true'>
          <svg width='20' height='20' viewBox='0 0 24 24' fill='none' stroke='currentColor' strokeWidth='1.8'>
            <path d='M12 5v14M5 12h14' strokeLinecap='round' />
          </svg>
        </span>
        <div className='student-team-card__intro'>
          <div className='card-title'>Tạo đội mới</div>
          <p className='student-team-card__lead'>
            Khởi tạo đội thi của bạn. Hệ thống tự sinh mã <strong>enrollCode</strong> để mời thành viên.
          </p>
        </div>
      </div>
      <form className='form student-team-form' onSubmit={handleSubmit}>
        <FormField label='Tên đội'>
          <input
            name='teamName'
            value={teamName}
            onChange={(e) => setTeamName(e.target.value)}
            required
            maxLength={100}
            placeholder='VD: Code Hunters'
            disabled={!githubLinked || loading}
          />
        </FormField>
        <p className='student-team-form__hint'>
          Tên đội phải duy nhất trên toàn hệ thống (không phân biệt hoa thường).
        </p>
        <LoadingButton
          loading={loading}
          type='submit'
          disabled={!githubLinked}
          className='btn btn-primary btn-block student-team-form__submit'
        >
          Tạo đội
        </LoadingButton>
        <FormMessage message={message?.text} type={message?.type} />
      </form>
    </div>
  )
}

// ─── Join Team Form ───────────────────────────────────────────────────────────
function JoinTeamForm({ onSuccess, githubLinked }) {
  const { showToast } = useToast()
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState(null)
  const [enrollCode, setEnrollCode] = useState('')

  const handleSubmit = async (e) => {
    e.preventDefault()
    setMessage(null)
    setLoading(true)
    try {
      await joinTeam({ enrollCode: enrollCode.trim() })
      setMessage({ text: 'Đã tham gia đội thành công!', type: 'success' })
      showToast('Đã tham gia đội', 'success')
      setEnrollCode('')
      setTimeout(onSuccess, 400)
    } catch (err) {
      setMessage({ text: localizeError(err.message), type: 'error' })
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className={`card student-team-card${!githubLinked ? ' student-team-card--locked' : ''}`}>
      <div className='student-team-card__head'>
        <span className='student-team-card__icon student-team-card__icon--join' aria-hidden='true'>
          <svg width='20' height='20' viewBox='0 0 24 24' fill='none' stroke='currentColor' strokeWidth='1.8'>
            <path d='M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2' strokeLinecap='round' />
            <circle cx='9' cy='7' r='4' />
            <path d='M19 8v6M22 11h-6' strokeLinecap='round' />
          </svg>
        </span>
        <div className='student-team-card__intro'>
          <div className='card-title'>Tham gia đội</div>
          <p className='student-team-card__lead'>
            Nhập mã <strong>enrollCode</strong> do leader cung cấp để gia nhập đội có sẵn.
          </p>
        </div>
      </div>
      <form className='form student-team-form' onSubmit={handleSubmit}>
        <FormField label='Mã enroll'>
          <input
            name='enrollCode'
            value={enrollCode}
            onChange={(e) => setEnrollCode(e.target.value)}
            required
            placeholder='VD: 12345678'
            maxLength={16}
            disabled={!githubLinked || loading}
          />
        </FormField>
        <LoadingButton
          loading={loading}
          type='submit'
          disabled={!githubLinked}
          className='btn btn-primary btn-block student-team-form__submit'
        >
          Tham gia
        </LoadingButton>
        <FormMessage message={message?.text} type={message?.type} />
      </form>
    </div>
  )
}

function EventMentorsBlock({ registration, mentorState, onOpenChat }) {
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
                {onOpenChat ? (
                  <ChatOpenButton
                    title={`Nhắn tin với ${m.mentorName || 'mentor'}`}
                    onClick={() =>
                      onOpenChat({
                        eventId: registration.eventId,
                        eventTitle: registration.eventTitle,
                        mentorId: m.mentorId,
                        mentorName: m.mentorName
                      })
                    }
                  />
                ) : null}
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
function TeamEventsPanel({ refreshKey, onOpenChat, isLeader, onRegisterSuccess }) {
  const { showToast } = useToast()
  const [list, setList] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [mentorsByEvent, setMentorsByEvent] = useState({})
  const [statusFilter, setStatusFilter] = useState('APPROVED')

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

  const showRegisterForm =
    isLeader && !loading && !hasOngoingOrUpcomingRegistration(list)

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
        <TeamEventsList list={filteredList} mentorsByEvent={mentorsByEvent} onOpenChat={onOpenChat} />
      )}
    </div>
  )
}

function TeamEventsList({ list, mentorsByEvent, onOpenChat }) {
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
                    onOpenChat={onOpenChat}
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
            Hiện chưa có sự kiện UPCOMING hoặc đội đã đăng ký hết.
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
            Chọn sự kiện UPCOMING — BTC sẽ duyệt và phân bảng sau khi đội đăng ký.
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
        Chọn sự kiện đang mở đăng ký (UPCOMING). BTC sẽ duyệt và phân bảng sau khi đội đăng ký.
      </p>
      {formBody}
    </div>
  )
}

// ─── Main Page ────────────────────────────────────────────────────────────────
export default function StudentDashboard() {
  const { showToast } = useToast()
  const navigate = useNavigate()
  const location = useLocation()
  const [teamState, setTeamState] = useState('loading')
  const [teamData, setTeamData] = useState(null)
  const [registrationsRefreshKey, setRegistrationsRefreshKey] = useState(0)
  const [chatTarget, setChatTarget] = useState(null)
  const [githubStatus, setGithubStatus] = useState({ loading: true, linked: false, username: '' })
  const [oauthLoading, setOauthLoading] = useState(false)
  const handledOauthSearchRef = useRef('')
  const [oauthConflictError, setOauthConflictError] = useState(null)

  const loadGithubStatus = useCallback(async () => {
    setGithubStatus((prev) => ({ ...prev, loading: true }))
    try {
      const status = await getGithubLinkStatus()
      setGithubStatus({
        loading: false,
        linked: status.githubLinked,
        username: status.githubUsername
      })
    } catch {
      setGithubStatus({ loading: false, linked: false, username: '' })
    }
  }, [])

  const loadMyTeam = useCallback(async () => {
    setTeamState('loading')
    try {
      const result = await getMyTeam()
      if (result.hasTeam) {
        setTeamData(result.data)
        setTeamState('has-team')
      } else {
        setTeamState('no-team')
      }
    } catch (err) {
      showToast(localizeError(err.message), 'error')
      setTeamState('no-team')
    }
  }, [showToast])

  useEffect(() => {
    loadMyTeam()
  }, [loadMyTeam])

  useEffect(() => {
    loadGithubStatus()
  }, [loadGithubStatus])

  useEffect(() => {
    const params = new URLSearchParams(location.search)
    const status = params.get('github_oauth')
    if (!status) return
    if (handledOauthSearchRef.current === location.search) return
    handledOauthSearchRef.current = location.search

    if (status === 'success') {
      const username = params.get('github_username')
      showToast(username ? `Đã liên kết GitHub: @${username}` : 'Đã liên kết GitHub thành công.', 'success')
      loadGithubStatus()
    } else {
      const errorCode = params.get('code')
      const rawMessage = params.get('message') || 'Liên kết GitHub thất bại.'
      let message = rawMessage
      try {
        message = decodeURIComponent(rawMessage.replace(/\+/g, ' '))
      } catch {
        message = rawMessage
      }

      if (errorCode === 'ALREADY_LINKED' || message.includes('already linked to another user')) {
        setOauthConflictError(message)
      } else {
        showToast(localizeError(message), 'error')
      }
    }

    navigate('/student', { replace: true })
  }, [location.search, navigate, showToast, loadGithubStatus])

  const handleConnectGithub = async (prompt) => {
    setOauthLoading(true)
    try {
      const authorizeUrl = await getGithubLinkUrl(prompt)
      window.location.href = authorizeUrl
    } catch (err) {
      showToast(localizeError(err.message), 'error')
      setOauthLoading(false)
    }
  }

  const handleTeamCreated = () => {
    loadMyTeam()
  }
  const handleTeamJoined = () => {
    loadMyTeam()
  }
  const handleMemberDeleted = () => {
    loadMyTeam()
  }
  const handleRefresh = () => {
    showToast('Đang làm mới...', 'success')
    loadMyTeam()
  }
  const refreshRegistrations = () => setRegistrationsRefreshKey((k) => k + 1)

  return (
    <DashboardLayout
      roleLabel='Sinh viên'
      moduleTitle='Khu vực Sinh viên'
      moduleSubtitle='Quản lý đội thi, đăng ký sự kiện hackathon và liên kết GitHub.'
      showStudentFields
      className='dashboard-shell--student-zone'
    >
      {!githubStatus.loading && (!githubStatus.linked || !githubStatus.username) ? (
        <GithubRequiredBanner
          onConnect={handleConnectGithub}
          loading={oauthLoading}
          isWarning={teamState === 'has-team'}
        />
      ) : null}
      {teamState === 'has-team' && (
        <DashboardSection title='Sự kiện' hint='Các hackathon đội đã đăng ký — bảng và mentor sau khi BTC duyệt'>
          <TeamEventsPanel
            refreshKey={registrationsRefreshKey}
            isLeader={Boolean(teamData?.isLeader)}
            onRegisterSuccess={() => refreshRegistrations()}
            onOpenChat={(target) => setChatTarget({ ...target, teamName: teamData?.teamName })}
          />
        </DashboardSection>
      )}

      {teamState === 'loading' && (
        <DashboardSection title='Đội của tôi' hint='Mỗi sinh viên chỉ có thể tham gia 1 đội'>
          <div className='card'>
            <LoadingState text='Đang tải thông tin đội…' />
          </div>
        </DashboardSection>
      )}

      {teamState !== 'loading' && (
        <DashboardSection
          title='Đội của tôi'
          hint='Mỗi sinh viên chỉ có thể tham gia 1 đội'
          spaced={teamState === 'has-team'}
        >
          {teamState === 'has-team' && teamData ? (
            <TeamInfoCard data={teamData} onRefresh={handleRefresh} onMemberDeleted={handleMemberDeleted} />
          ) : null}

          {teamState === 'no-team' ? (
            <>
              {!githubStatus.loading && githubStatus.linked ? (
                <GithubLinkedBadge username={githubStatus.username} />
              ) : null}
              <div className='cards student-team-cards'>
                <CreateTeamForm onSuccess={handleTeamCreated} githubLinked={githubStatus.linked} />
                <JoinTeamForm onSuccess={handleTeamJoined} githubLinked={githubStatus.linked} />
              </div>
            </>
          ) : null}
        </DashboardSection>
      )}

      {chatTarget && (
        <ChatPopup
          open
          onClose={() => setChatTarget(null)}
          eventId={chatTarget.eventId}
          eventTitle={chatTarget.eventTitle}
          mentorId={chatTarget.mentorId}
          mentorName={chatTarget.mentorName}
          teamName={chatTarget.teamName}
        />
      )}

      {oauthConflictError && (
        <Modal isOpen={true} onClose={() => setOauthConflictError(null)} title='Liên kết GitHub thất bại'>
          <div className='oauth-conflict-modal' style={{ padding: '0.5rem' }}>
            <div
              className='oauth-conflict-modal__icon'
              style={{ color: '#ef4444', fontSize: '2.5rem', marginBottom: '1rem', textAlign: 'center' }}
            >
              ⚠️
            </div>
            <p
              style={{
                marginBottom: '1.5rem',
                lineHeight: '1.6',
                fontSize: '1rem',
                color: 'var(--text-color, #1f2937)',
                textAlign: 'center'
              }}
            >
              <strong>Mỗi tài khoản GitHub chỉ có thể liên kết với một tài khoản SEAL Hackathon duy nhất.</strong>
            </p>
            <div
              className='form-actions'
              style={{ display: 'flex', justifyContent: 'center', gap: '1rem', marginTop: '1.5rem' }}
            >
              <button
                type='button'
                className='btn btn-primary'
                onClick={() => {
                  setOauthConflictError(null)
                  handleConnectGithub('select_account')
                }}
                disabled={oauthLoading}
              >
                {oauthLoading ? 'Đang chuyển hướng...' : 'Chọn tài khoản liên kết khác'}
              </button>
            </div>
          </div>
        </Modal>
      )}
    </DashboardLayout>
  )
}

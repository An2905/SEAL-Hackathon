import { useState, useEffect, useCallback, useRef } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import DashboardLayout from '../../components/layout/DashboardLayout'
import FormField from '../../components/common/FormField'
import FormMessage from '../../components/common/FormMessage'
import LoadingButton from '../../components/common/LoadingButton'
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
import { getGithubLinkUrl } from '../../api/auth'

const PAGE_SIZE = 5

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
  if (key === 'REJECTED') return 'status-default'
  return 'status-default'
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

// ─── Team Info Card ───────────────────────────────────────────────────────────
function TeamInfoCard({ data, onRefresh }) {
  const { showToast } = useToast()
  const [membersPage, setMembersPage] = useState(1)
  const members = data.members || []

  const handleCopyEnroll = async () => {
    const code = data.enrollCode
    try {
      await navigator.clipboard.writeText(code)
      showToast('Đã sao chép mã enroll: ' + code, 'success')
    } catch {
      showToast('Mã enroll: ' + code, 'success')
    }
  }

  return (
    <div className='card team-info-card'>
      <div className='card-head'>
        <div>
          <div className='card-title'>{data.teamName}</div>
          <div className='card-sub' style={{ margin: 0 }}>
            {data.isLeader ? 'Bạn là leader của đội này' : 'Bạn đang là thành viên của đội'}
          </div>
        </div>
        <span className={`role-pill ${data.isLeader ? 'role-judge' : 'role-student'}`} style={{ marginLeft: 'auto' }}>
          {data.isLeader ? 'Leader' : 'Thành viên'}
        </span>
      </div>

      <div className='kv-list'>
        <div className='kv'>
          <span>Tên đội</span>
          <span>{data.teamName}</span>
        </div>
        <div className='kv'>
          <span>Mã enroll</span>
          <span>
            <code>{data.enrollCode}</code>
          </span>
        </div>
        <div className='kv'>
          <span>Leader</span>
          <span>
            {data.leaderName} ({data.leaderEmail})
          </span>
        </div>
        <div className='kv'>
          <span>Trạng thái</span>
          <span>{data.status}</span>
        </div>
        <div className='kv'>
          <span>Số thành viên</span>
          <span>{data.memberCount} / 5</span>
        </div>
      </div>

      <div className='section-title' style={{ margin: '22px 0 10px' }}>
        <h2 style={{ fontSize: 16 }}>Thành viên</h2>
      </div>
      <div className='kv-list'>
        {members.slice((membersPage - 1) * PAGE_SIZE, membersPage * PAGE_SIZE).map((m) => (
          <div className='member-row' key={m.userId}>
            <div className='avatar'>{(m.fullName?.[0] || m.email?.[0] || 'U').toUpperCase()}</div>
            <div className='member-info'>
              <div className='member-name'>
                {m.fullName || '(Chưa có tên)'}
                {m.isLeader && <span className='leader-tag'>Leader</span>}
              </div>
              <div className='member-meta'>{m.email || ''}</div>
            </div>
          </div>
        ))}
      </div>
      <Pagination total={members.length} pageSize={PAGE_SIZE} currentPage={membersPage} onChange={setMembersPage} />

      <div className='card-actions' style={{ marginTop: 18 }}>
        <button className='btn btn-outline' onClick={handleCopyEnroll}>
          Sao chép mã enroll
        </button>
        <button className='btn btn-ghost' onClick={onRefresh}>
          Làm mới
        </button>
      </div>
    </div>
  )
}

// ─── Create Team Form ─────────────────────────────────────────────────────────
function CreateTeamForm({ onSuccess }) {
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
    <div className='card'>
      <div className='card-head'>
        <div className='card-title'>Tạo đội mới</div>
      </div>
      <p className='card-sub'>
        Tạo đội của riêng bạn. Hệ thống sẽ sinh mã <strong>enrollCode</strong> để mời thành viên khác.
      </p>
      <form className='form' onSubmit={handleSubmit}>
        <FormField label='Tên đội'>
          <input
            name='teamName'
            value={teamName}
            onChange={(e) => setTeamName(e.target.value)}
            required
            maxLength={100}
            placeholder='VD: Code Hunters'
          />
        </FormField>
        <p className='card-sub' style={{ marginTop: 0 }}>
          Tên đội phải là duy nhất trên toàn hệ thống (không phân biệt hoa thường).
        </p>
        <LoadingButton loading={loading} type='submit'>
          Tạo đội
        </LoadingButton>
        <FormMessage message={message?.text} type={message?.type} />
      </form>
    </div>
  )
}

// ─── Join Team Form ───────────────────────────────────────────────────────────
function JoinTeamForm({ onSuccess }) {
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
    <div className='card'>
      <div className='card-head'>
        <div className='card-title'>Tham gia đội</div>
      </div>
      <p className='card-sub'>
        Nhập mã <strong>enrollCode</strong> mà leader cung cấp cho bạn.
      </p>
      <form className='form' onSubmit={handleSubmit}>
        <FormField label='Mã enroll'>
          <input
            name='enrollCode'
            value={enrollCode}
            onChange={(e) => setEnrollCode(e.target.value)}
            required
            placeholder='VD: 12345678'
            maxLength={16}
          />
        </FormField>
        <LoadingButton loading={loading} type='submit'>
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
    return (
      <div className='empty-state' style={{ marginTop: 12, padding: '12px 0', fontSize: 13 }}>
        Mentor hiển thị sau khi đăng ký được duyệt (APPROVED).
      </div>
    )
  }

  if (state.loading) {
    return <LoadingState text='Đang tải mentor…' style={{ marginTop: 12, padding: '12px 0', fontSize: 13 }} />
  }

  if (state.error) {
    return (
      <div className='empty-state' style={{ marginTop: 12, padding: '12px 0', fontSize: 13 }}>
        {state.error}
      </div>
    )
  }

  const mentors = state.data?.mentors || []
  if (mentors.length === 0) {
    return (
      <div className='empty-state' style={{ marginTop: 12, padding: '12px 0', fontSize: 13 }}>
        Chưa có mentor cho bảng này.
      </div>
    )
  }

  return (
    <div style={{ marginTop: 12 }}>
      <div style={{ fontSize: 13, fontWeight: 600, color: 'var(--text-dim)', marginBottom: 8 }}>Mentor bảng</div>
      <div className='kv-list'>
        {mentors.slice((mentorsPage - 1) * PAGE_SIZE, mentorsPage * PAGE_SIZE).map((m) => (
          <div className='member-row' key={m.mentorId}>
            <div className='avatar'>{(m.mentorName?.[0] || 'M').toUpperCase()}</div>
            <div className='member-info'>
              <div className='member-name' style={{ display: 'flex', alignItems: 'center' }}>
                <span>{m.mentorName || '—'}</span>
                {onOpenChat && (
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
                )}
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

// ─── Sự kiện + mentor (cùng một card) ───────────────────────────────────────
function TeamEventsPanel({ refreshKey, onOpenChat }) {
  const { showToast } = useToast()
  const [list, setList] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [mentorsByEvent, setMentorsByEvent] = useState({})

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

  return (
    <div className='card'>
      <div className='card-head'>
        <div className='card-title'>Sự kiện & mentor</div>
      </div>
      <p className='card-sub'>Các hackathon đội đã đăng ký — bảng và mentor được gán sau khi BTC duyệt và phân bảng.</p>
      {loading && <LoadingState />}
      {!loading && error && <div className='empty-state'>{error}</div>}
      {!loading && !error && list.length === 0 && <div className='empty-state'>Đội chưa đăng ký sự kiện nào.</div>}
      {!loading && list.length > 0 && (
        <TeamEventsList list={list} mentorsByEvent={mentorsByEvent} onOpenChat={onOpenChat} />
      )}
    </div>
  )
}

function TeamEventsList({ list, mentorsByEvent, onOpenChat }) {
  const [page, setPage] = useState(1)
  const visibleItems = list.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE)

  return (
    <>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
        {visibleItems.map((reg, index) => {
          const isOngoing = (reg.eventStatus || '').toUpperCase() === 'ONGOING'
          return (
            <div
              key={reg.registrationId || reg.eventId}
              className={isOngoing ? 'team-event-item team-event-item--ongoing' : 'team-event-item'}
              style={{
                paddingBottom: index < visibleItems.length - 1 ? (isOngoing ? 24 : 20) : isOngoing ? 4 : 0,
                borderBottom:
                  index < visibleItems.length - 1 ? '1px solid var(--border, rgba(255,255,255,0.08))' : 'none'
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12, alignItems: 'flex-start' }}>
                <div style={{ minWidth: 0, flex: 1 }}>
                  <div style={{ fontWeight: 600, color: 'var(--text)', fontSize: 16 }}>{reg.eventTitle || '—'}</div>
                  <div
                    style={{
                      fontSize: 12,
                      color: 'var(--text-dim)',
                      marginTop: 6,
                      display: 'flex',
                      flexWrap: 'wrap',
                      alignItems: 'center',
                      gap: 8
                    }}
                  >
                    <span>
                      Bảng:{' '}
                      <strong style={isOngoing ? { color: 'var(--text)' } : undefined}>{reg.groupName || '—'}</strong>
                    </span>
                    <span
                      className={`status-pill ${eventStatusPillClass(reg.eventStatus)}`}
                      style={{ cursor: 'default' }}
                      title={`Trạng thái sự kiện: ${reg.eventStatus || '—'}`}
                    >
                      {isOngoing ? '● ' : ''}
                      {eventStatusLabel(reg.eventStatus)}
                    </span>
                  </div>
                  <div style={{ fontSize: 11, color: 'var(--text-mute)', marginTop: 4 }}>
                    {formatDateTime(reg.eventStartDate)} → {formatDateTime(reg.eventEndDate)}
                  </div>
                  <div style={{ fontSize: 11, color: 'var(--text-mute)', marginTop: 2 }}>
                    Đăng ký: {formatDateTime(reg.registeredAt)}
                  </div>
                </div>
                <div
                  style={{ display: 'flex', flexDirection: 'column', gap: 6, alignItems: 'flex-end', flexShrink: 0 }}
                >
                  <span
                    className={`status-pill ${registrationStatusPillClass(reg.registrationStatus)}`}
                    style={{ cursor: 'default' }}
                    title='Trạng thái duyệt đăng ký'
                  >
                    {reg.registrationStatus || '—'}
                  </span>
                </div>
              </div>
              <EventMentorsBlock registration={reg} mentorState={mentorsByEvent[reg.eventId]} onOpenChat={onOpenChat} />
            </div>
          )
        })}
      </div>
      <Pagination total={list.length} pageSize={PAGE_SIZE} currentPage={page} onChange={setPage} />
    </>
  )
}

// ─── Join Event Form ──────────────────────────────────────────────────────────
function JoinEventForm({ onSuccess }) {
  const { showToast } = useToast()
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState(null)
  const [eventId, setEventId] = useState('')

  const handleSubmit = async (e) => {
    e.preventDefault()
    setMessage(null)
    setLoading(true)
    try {
      await joinEvent({ eventId: eventId.trim() })
      setMessage({ text: 'Đăng ký event thành công!', type: 'success' })
      showToast('Đăng ký event thành công', 'success')
      setEventId('')
      onSuccess?.()
    } catch (err) {
      setMessage({ text: localizeError(err.message), type: 'error' })
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className='card'>
      <div className='card-head'>
        <div className='card-title'>Đăng ký sự kiện</div>
      </div>
      <p className='card-sub'>
        Đăng ký đội tham gia sự kiện hackathon. Liên hệ BTC nếu chưa biết mã sự kiện. BTC sẽ phân bảng sau khi duyệt.
      </p>
      <form className='form' onSubmit={handleSubmit}>
        <FormField label='Sự kiện'>
          <input
            name='eventId'
            value={eventId}
            onChange={(e) => setEventId(e.target.value)}
            required
            placeholder='Mã sự kiện do BTC cung cấp'
          />
        </FormField>
        <LoadingButton loading={loading} type='submit'>
          Đăng ký event
        </LoadingButton>
        <FormMessage message={message?.text} type={message?.type} />
      </form>
    </div>
  )
}

// ─── Delete Member Form ───────────────────────────────────────────────────────
function DeleteMemberForm({ onSuccess }) {
  const { showToast } = useToast()
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState(null)
  const [memberId, setMemberId] = useState('')

  const handleSubmit = async (e) => {
    e.preventDefault()
    setMessage(null)
    setLoading(true)
    try {
      await deleteMember({ memberId: memberId.trim() })
      setMessage({ text: 'Đã xóa thành viên', type: 'success' })
      showToast('Đã xóa thành viên khỏi đội', 'success')
      setMemberId('')
      setTimeout(onSuccess, 400)
    } catch (err) {
      setMessage({ text: localizeError(err.message), type: 'error' })
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className='card'>
      <div className='card-head'>
        <div className='card-title'>Xóa thành viên</div>
      </div>
      <p className='card-sub'>Loại một thành viên ra khỏi đội. Chỉ leader mới có quyền này.</p>
      <form className='form' onSubmit={handleSubmit}>
        <FormField label='Email thành viên'>
          <input
            name='memberId'
            type='email'
            value={memberId}
            onChange={(e) => setMemberId(e.target.value)}
            required
            placeholder='Nhập email thành viên'
          />
        </FormField>
        <LoadingButton loading={loading} type='submit'>
          Xóa thành viên
        </LoadingButton>
        <FormMessage message={message?.text} type={message?.type} />
      </form>
    </div>
  )
}

// ─── Activity Log ─────────────────────────────────────────────────────────────
function ActivityLog({ activities }) {
  const [page, setPage] = useState(1)

  if (!activities.length) {
    return (
      <div className='empty-state'>
        Chưa có hoạt động nào trong phiên này. Hãy thử tạo đội hoặc tham gia một đội ở trên.
      </div>
    )
  }

  return (
    <>
      <div className='kv-list'>
        {activities.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE).map((a, i) => (
          <div className='kv' key={`${a.at?.getTime?.() ?? i}-${a.text}`}>
            <span>{a.at.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}</span>
            <span>{a.text}</span>
          </div>
        ))}
      </div>
      <Pagination total={activities.length} pageSize={PAGE_SIZE} currentPage={page} onChange={setPage} />
    </>
  )
}

// ─── Main Page ────────────────────────────────────────────────────────────────
export default function StudentDashboard() {
  const { showToast } = useToast()
  const navigate = useNavigate()
  const location = useLocation()
  const [teamState, setTeamState] = useState('loading') // 'loading' | 'no-team' | 'has-team'
  const [teamData, setTeamData] = useState(null)
  const [activities, setActivities] = useState([])
  const [registrationsRefreshKey, setRegistrationsRefreshKey] = useState(0)
  const [chatTarget, setChatTarget] = useState(null)
  const [oauthLoading, setOauthLoading] = useState(false)
  const handledOauthSearchRef = useRef('')

  const logActivity = (text) => setActivities((prev) => [{ text, at: new Date() }, ...prev])

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

  useEffect(() => { loadMyTeam() }, [loadMyTeam])

  const handleTeamCreated = () => {
    logActivity('Tạo đội mới')
    loadMyTeam()
  }
  const handleTeamJoined = () => {
    logActivity('Tham gia đội thành công')
    loadMyTeam()
  }
  const handleMemberDeleted = () => {
    logActivity('Xóa thành viên')
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
      moduleTitle='Tài khoản sinh viên'
      moduleSubtitle='Quản lý đội thi và đăng ký sự kiện hackathon ngay tại đây.'
      showStudentFields
    >
      <div className="section-title">
        <h2>Đội của tôi</h2>
        <span className='hint'>Mỗi sinh viên chỉ có thể tham gia 1 đội</span>
      </div>

      {teamState === 'loading' && <LoadingState text='Đang tải thông tin đội...' />}

      {teamState === 'has-team' && teamData && <TeamInfoCard data={teamData} onRefresh={handleRefresh} />}

      {teamState === 'has-team' && (
        <>
          <div className='section-title' style={{ marginTop: 24 }}>
            <h2>Đăng ký sự kiện</h2>
            <span className='hint'>Sự kiện, bảng và mentor của đội</span>
          </div>
          <TeamEventsPanel
            refreshKey={registrationsRefreshKey}
            onOpenChat={(target) => setChatTarget({ ...target, teamName: teamData?.teamName })}
          />
        </>
      )}

      {teamState === 'no-team' && (
        <div className='cards'>
          <CreateTeamForm onSuccess={handleTeamCreated} />
          <JoinTeamForm onSuccess={handleTeamJoined} />
        </div>
      )}

      {teamState === 'has-team' && teamData?.isLeader && (
        <>
          <div className='section-title'>
            <h2>Quản lý leader</h2>
            <span className='hint'>Chỉ leader mới thực hiện được các thao tác bên dưới</span>
          </div>
          <div className='cards'>
            <JoinEventForm
              onSuccess={() => {
                refreshRegistrations()
                logActivity('Đăng ký sự kiện')
              }}
            />
            <DeleteMemberForm onSuccess={handleMemberDeleted} />
          </div>
        </>
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

      <div className='section-title'>
        <h2>Hoạt động gần đây</h2>
      </div>
      <ActivityLog activities={activities} />
    </DashboardLayout>
  )
}

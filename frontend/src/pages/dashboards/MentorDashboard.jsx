import { useEffect, useMemo, useState } from 'react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import { getAssignedEvents, getAssignedTeams, getGroupColleagues, getMentorAssignments } from '../../api/mentor'
import ExpertGroupColleaguesBoard from '../../components/expert/ExpertGroupColleaguesBoard'
import { useAuth } from '../../context/AuthContext'
import { useToast } from '../../context/ToastContext'
import { eventStatusLabel } from '../../utils/eventStatusLabels'
import { localizeError } from '../../utils/errors'
import { vietnameseRoleLabel } from '../../utils/roleLabels'
import Pagination from '../../components/common/Pagination'
import LoadingState from '../../components/common/LoadingState'
import Modal from '../../components/common/Modal'

const PAGE_SIZE = 5

const TEAM_STATUS_FILTERS = [
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

function eventStatusPillClass(status) {
  const key = (status || '').toUpperCase()
  if (key === 'BUILDING') return 'status-pending'
  if (key === 'UPCOMING') return 'status-pending'
  if (key === 'ONGOING') return 'status-active'
  if (key === 'COMPLETED') return 'status-default'
  if (key === 'CANCELLED') return 'status-rejected'
  return 'status-default'
}

function registrationStatusPillClass(status) {
  const key = (status || '').toUpperCase()
  if (key === 'APPROVED') return 'status-active'
  if (key === 'PENDING') return 'status-pending'
  if (key === 'REJECTED') return 'status-rejected'
  return 'status-default'
}

function assignmentKey(a) {
  return `${a.eventId}-${a.roundId}-${a.groupId}`
}

// ─── Event List View ──────────────────────────────────────────────────────────
function EventListView({ events, onSelect }) {
  if (events.length === 0) {
    return <div className='empty-state'>Bạn chưa được phân công sự kiện nào.</div>
  }
  return (
    <div className='kv-list'>
      {events.map((ev) => (
        <button
          key={ev.eventId}
          type='button'
          className='mentor-team-card'
          onClick={() => onSelect(ev.eventId)}
          style={{
            width: '100%',
            textAlign: 'left',
            cursor: 'pointer',
            background: 'none',
            border: 'none',
            padding: 0
          }}
        >
          <div className='kv' style={{ alignItems: 'center' }}>
            <span style={{ minWidth: 0, flex: 1 }}>
              <div style={{ fontWeight: 700, fontSize: '1rem', color: 'var(--text)' }}>{ev.title || '—'}</div>
              <div style={{ fontSize: 12, color: 'var(--text-dim)', marginTop: 4 }}>
                {formatDateTime(ev.startDate)} → {formatDateTime(ev.endDate)}
              </div>
            </span>
            <span className='status-picker'>
              <span className={`status-pill ${eventStatusPillClass(ev.status)}`} style={{ cursor: 'default' }}>
                {eventStatusLabel(ev.status)}
              </span>
            </span>
          </div>
        </button>
      ))}
    </div>
  )
}

// ─── Team Detail Modal ────────────────────────────────────────────────────────
function TeamDetailModal({ team, onClose }) {
  const members = team.members || []
  return (
    <Modal isOpen title={team.teamName || 'Thông tin đội'} onClose={onClose}>
      <div style={{ padding: '0.5rem 0' }}>
        <div
          style={{
            marginBottom: 16,
            padding: '10px 14px',
            background: 'var(--surface-alt, #f5f7ff)',
            borderRadius: 8
          }}
        >
          <div style={{ fontSize: 12, color: 'var(--text-dim)' }}>Trưởng nhóm</div>
          <div style={{ fontWeight: 600, marginTop: 2 }}>{team.leaderName || '—'}</div>
          <div style={{ fontSize: 12, color: 'var(--text-dim)', marginTop: 2 }}>{team.leaderEmail || ''}</div>
        </div>
        <div
          style={{
            fontSize: 12,
            fontWeight: 600,
            color: 'var(--text-dim)',
            marginBottom: 8,
            textTransform: 'uppercase',
            letterSpacing: '0.05em'
          }}
        >
          Thành viên ({members.length})
        </div>
        <div className='kv-list'>
          {members.length === 0 && <div className='empty-state'>Chưa có thành viên.</div>}
          {members.map((m) => (
            <div key={m.userId} className='member-row'>
              <div className='avatar'>{(m.fullName?.[0] || m.email?.[0] || 'U').toUpperCase()}</div>
              <div className='member-info'>
                <div className='member-name'>{m.fullName || '—'}</div>
                <div className='member-meta'>{m.email || ''}</div>
                {m.githubUsername && (
                  <div className='member-meta'>
                    <span style={{ opacity: 0.6 }}>GitHub: </span>
                    <strong>@{m.githubUsername}</strong>
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      </div>
    </Modal>
  )
}

// ─── Event Detail View ────────────────────────────────────────────────────────
function EventDetailView({
  event,
  assignments,
  selectedAssignmentKey,
  onSelectAssignment,
  colleagues,
  loadingColleagues,
  errorColleagues,
  teams,
  loadingTeams,
  errorTeams,
  teamsPage,
  onTeamsPageChange,
  teamStatusFilter,
  onTeamStatusFilter,
  onBack,
  onTeamClick
}) {
  return (
    <>
      <div style={{ display: 'flex', alignItems: 'flex-start', gap: 12, marginBottom: 20 }}>
        <button
          type='button'
          className='btn btn-outline'
          style={{ flexShrink: 0, padding: '6px 14px' }}
          onClick={onBack}
        >
          ← Quay lại
        </button>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ fontWeight: 700, fontSize: '1.15rem', color: 'var(--text)' }}>{event?.title || '—'}</div>
          {event && (
            <div style={{ fontSize: 12, color: 'var(--text-dim)', marginTop: 4 }}>
              {formatDateTime(event.startDate)} → {formatDateTime(event.endDate)}
            </div>
          )}
        </div>
        {event && (
          <span
            className={`status-pill ${eventStatusPillClass(event.status)}`}
            style={{ cursor: 'default', flexShrink: 0 }}
          >
            {eventStatusLabel(event.status)}
          </span>
        )}
      </div>

      {assignments.length > 0 && (
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginBottom: 16 }}>
          {assignments.map((a) => {
            const key = assignmentKey(a)
            return (
              <button
                key={key}
                type='button'
                className={`btn ${key === selectedAssignmentKey ? 'btn-primary' : 'btn-outline'}`}
                style={{ fontSize: 12, padding: '6px 12px' }}
                onClick={() => onSelectAssignment(key)}
              >
                {a.roundName} · {a.groupName}
              </button>
            )
          })}
        </div>
      )}

      {selectedAssignmentKey && (
        <div style={{ marginBottom: 16 }}>
          <ExpertGroupColleaguesBoard colleagues={colleagues} loading={loadingColleagues} error={errorColleagues} />
        </div>
      )}

      <div className='section-title' style={{ marginTop: 8 }}>
        <h2>Danh sách đội</h2>
        <span className='hint'>Bấm vào đội để xem tên, email và GitHub</span>
      </div>

      <div className='card'>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginBottom: 16 }}>
          {TEAM_STATUS_FILTERS.map((f) => (
            <button
              key={f.value}
              type='button'
              className={`btn ${teamStatusFilter === f.value ? 'btn-primary' : 'btn-outline'}`}
              style={{ fontSize: 12, padding: '4px 10px' }}
              onClick={() => onTeamStatusFilter(f.value)}
            >
              {f.label}
            </button>
          ))}
        </div>

        {loadingTeams && <LoadingState text='Đang tải danh sách đội…' />}
        {!loadingTeams && errorTeams && <div className='empty-state'>{errorTeams}</div>}
        {!loadingTeams && !errorTeams && teams.length === 0 && (
          <div className='empty-state'>Không có đội nào trong bảng này.</div>
        )}
        {!loadingTeams && teams.length > 0 && (
          <>
            <div className='kv-list'>
              {teams.slice((teamsPage - 1) * PAGE_SIZE, teamsPage * PAGE_SIZE).map((team, idx) => (
                <button
                  key={team.teamId || team.registrationId}
                  type='button'
                  className='mentor-team-card'
                  onClick={() => onTeamClick(team)}
                  style={{
                    width: '100%',
                    textAlign: 'left',
                    cursor: 'pointer',
                    background: 'none',
                    border: 'none',
                    padding: 0
                  }}
                >
                  <div className='kv' style={{ alignItems: 'flex-start' }}>
                    <span style={{ minWidth: 0, flex: 1, textAlign: 'left' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                        <span className='accounts-table-index' style={{ minWidth: 24 }}>
                          {(teamsPage - 1) * PAGE_SIZE + idx + 1}
                        </span>
                        <span style={{ fontWeight: 600, color: 'var(--text)' }}>{team.teamName || '—'}</span>
                      </div>
                      <div style={{ fontSize: 12, color: 'var(--text-dim)', marginTop: 2, marginLeft: 34 }}>
                        Trưởng nhóm: {team.leaderName || '—'}
                        {team.leaderEmail ? ` · ${team.leaderEmail}` : ''}
                      </div>
                      <div style={{ fontSize: 11, color: 'var(--text-mute)', marginTop: 2, marginLeft: 34 }}>
                        {team.memberCount || 0} thành viên
                      </div>
                    </span>
                    <span className='status-picker' style={{ flexShrink: 0 }}>
                      <span
                        className={`status-pill ${registrationStatusPillClass(team.registrationStatus)}`}
                        style={{ cursor: 'default' }}
                      >
                        {team.registrationStatus || '—'}
                      </span>
                    </span>
                  </div>
                </button>
              ))}
            </div>
            <Pagination
              total={teams.length}
              pageSize={PAGE_SIZE}
              currentPage={teamsPage}
              onChange={onTeamsPageChange}
            />
          </>
        )}
      </div>
    </>
  )
}

// ─── Main ─────────────────────────────────────────────────────────────────────
export default function MentorDashboard() {
  const { auth } = useAuth()
  const { showToast } = useToast()

  const [events, setEvents] = useState([])
  const [loadingEvents, setLoadingEvents] = useState(true)
  const [assignments, setAssignments] = useState([])
  const [loadingAssignments, setLoadingAssignments] = useState(true)

  const [selectedEventId, setSelectedEventId] = useState(null)
  const [selectedAssignmentKey, setSelectedAssignmentKey] = useState('')

  const [teams, setTeams] = useState([])
  const [loadingTeams, setLoadingTeams] = useState(false)
  const [errorTeams, setErrorTeams] = useState(null)
  const [teamsPage, setTeamsPage] = useState(1)
  const [teamStatusFilter, setTeamStatusFilter] = useState('APPROVED')

  const [colleagues, setColleagues] = useState(null)
  const [loadingColleagues, setLoadingColleagues] = useState(false)
  const [errorColleagues, setErrorColleagues] = useState(null)

  const [teamPopup, setTeamPopup] = useState(null)

  useEffect(() => {
    let cancelled = false
    ;(async () => {
      const [evResult, asResult] = await Promise.allSettled([getAssignedEvents(), getMentorAssignments()])
      if (cancelled) return

      if (evResult.status === 'fulfilled') setEvents(evResult.value)
      else showToast('Không tải được sự kiện được phân công', 'error')
      setLoadingEvents(false)

      if (asResult.status === 'fulfilled') setAssignments(asResult.value)
      setLoadingAssignments(false)
    })()
    return () => {
      cancelled = true
    }
  }, [showToast])

  const eventAssignments = useMemo(
    () => assignments.filter((a) => String(a.eventId) === String(selectedEventId)),
    [assignments, selectedEventId]
  )

  const selectedEvent = useMemo(
    () => events.find((e) => String(e.eventId) === String(selectedEventId)) ?? null,
    [events, selectedEventId]
  )

  const selectedAssignment = useMemo(
    () => assignments.find((a) => assignmentKey(a) === selectedAssignmentKey) ?? null,
    [assignments, selectedAssignmentKey]
  )

  useEffect(() => {
    if (selectedEventId && eventAssignments.length > 0) {
      setSelectedAssignmentKey(assignmentKey(eventAssignments[0]))
    } else if (!selectedEventId) {
      setSelectedAssignmentKey('')
    }
  }, [selectedEventId, eventAssignments])

  useEffect(() => {
    if (!selectedAssignment) {
      setTeams([])
      setColleagues(null)
      setErrorColleagues(null)
      return
    }
    let cancelled = false
    setLoadingTeams(true)
    setErrorTeams(null)
    setTeamsPage(1)
    setLoadingColleagues(true)
    setErrorColleagues(null)
    ;(async () => {
      try {
        const rows = await getAssignedTeams({
          eventId: selectedAssignment.eventId,
          roundId: selectedAssignment.roundId,
          groupId: selectedAssignment.groupId,
          registrationStatus: teamStatusFilter
        })
        if (!cancelled) setTeams(rows)
      } catch (err) {
        if (!cancelled) {
          setTeams([])
          setErrorTeams(localizeError(err?.message))
        }
      } finally {
        if (!cancelled) setLoadingTeams(false)
      }
    })()
    ;(async () => {
      try {
        const data = await getGroupColleagues({
          eventId: selectedAssignment.eventId,
          roundId: selectedAssignment.roundId,
          groupId: selectedAssignment.groupId
        })
        if (!cancelled) setColleagues(data)
      } catch (err) {
        if (!cancelled) {
          setColleagues(null)
          setErrorColleagues(localizeError(err?.message))
        }
      } finally {
        if (!cancelled) setLoadingColleagues(false)
      }
    })()

    return () => {
      cancelled = true
    }
  }, [selectedAssignment, teamStatusFilter])

  const isDualRole = auth.role === 'EXPERT_INTERNAL'
  const roleLabel = vietnameseRoleLabel(auth.role)
  const navLinks = isDualRole
    ? [
        { label: 'Mentor', to: '/mentor' },
        { label: 'Judge', to: '/judge' }
      ]
    : null

  const isLoading = loadingEvents || loadingAssignments

  return (
    <DashboardLayout
      roleLabel={roleLabel}
      moduleTitle='Khu vực Mentor'
      moduleSubtitle='Khách được phân công mentor — đồng hành cùng các đội thí sinh.'
      showStaffFields
      className='dashboard-shell--mentor-zone'
      navLinks={navLinks}
    >
      {selectedEventId === null ? (
        <>
          <div className='section-title'>
            <h2>Sự kiện được phân công</h2>
            <span className='hint'>Chọn sự kiện để xem chi tiết và danh sách đội</span>
          </div>
          <div className='card'>
            {isLoading ? (
              <LoadingState text='Đang tải…' />
            ) : (
              <EventListView events={events} onSelect={setSelectedEventId} />
            )}
          </div>
        </>
      ) : (
        <EventDetailView
          event={selectedEvent}
          assignments={eventAssignments}
          selectedAssignmentKey={selectedAssignmentKey}
          onSelectAssignment={setSelectedAssignmentKey}
          colleagues={colleagues}
          loadingColleagues={loadingColleagues}
          errorColleagues={errorColleagues}
          teams={teams}
          loadingTeams={loadingTeams}
          errorTeams={errorTeams}
          teamsPage={teamsPage}
          onTeamsPageChange={setTeamsPage}
          teamStatusFilter={teamStatusFilter}
          onTeamStatusFilter={setTeamStatusFilter}
          onBack={() => setSelectedEventId(null)}
          onTeamClick={setTeamPopup}
        />
      )}

      {teamPopup && <TeamDetailModal team={teamPopup} onClose={() => setTeamPopup(null)} />}
    </DashboardLayout>
  )
}

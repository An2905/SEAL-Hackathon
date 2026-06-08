import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import DashboardShell from './DashboardShell'

import {
  getAssignedEvents,
  getAssignedCurrentRounds,
  getAssignedTeams,
  getGroupColleagues,
  getMentorAssignments
} from '../../api/mentor'
import ExpertGroupColleaguesBoard from '../../components/expert/ExpertGroupColleaguesBoard'
import { useAuth } from '../../context/AuthContext'
import { useToast } from '../../context/ToastContext'
import { localizeError } from '../../utils/errors'
import ChatPopup from '../../components/chat/ChatPopup'
import CollapsibleKvList from '../../components/common/CollapsibleList'

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

function StatusBadge({ status }) {
  return (
    <span className="status-picker" style={{ flexShrink: 0 }}>
      <span
        className={`status-pill ${eventStatusPillClass(status)}`}
        style={{ cursor: 'default' }}
      >
        {status || '—'}
      </span>
    </span>
  )
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

function assignmentLabel(a) {
  return [a.eventTitle, a.roundName, a.groupName].filter(Boolean).join(' · ')
}

const TEAM_STATUS_FILTERS = [
  { value: 'APPROVED', label: 'Đã duyệt' },
  { value: 'PENDING', label: 'Chờ duyệt' },
  { value: 'ALL', label: 'Tất cả' }
]

export default function MentorDashboard() {
  const { auth, pillLabelForRole } = useAuth()
  const guestLabel = pillLabelForRole(auth.role) || 'Khách'
  const { showToast } = useToast()

  const [events, setEvents] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const [rounds, setRounds] = useState([])
  const [loadingRounds, setLoadingRounds] = useState(true)
  const [errorRounds, setErrorRounds] = useState(null)

  const [assignments, setAssignments] = useState([])
  const [loadingAssignments, setLoadingAssignments] = useState(true)
  const [errorAssignments, setErrorAssignments] = useState(null)
  const [selectedAssignmentKey, setSelectedAssignmentKey] = useState('')
  const [teamStatusFilter, setTeamStatusFilter] = useState('APPROVED')

  const [teams, setTeams] = useState([])
  const [loadingTeams, setLoadingTeams] = useState(false)
  const [errorTeams, setErrorTeams] = useState(null)
  const [colleagues, setColleagues] = useState(null)
  const [loadingColleagues, setLoadingColleagues] = useState(false)
  const [errorColleagues, setErrorColleagues] = useState(null)
  const [chatOpen, setChatOpen] = useState(false)
  const selectedAssignment = useMemo(
    () => assignments.find((a) => assignmentKey(a) === selectedAssignmentKey) ?? null,
    [assignments, selectedAssignmentKey]
  )

  useEffect(() => {
    let cancelled = false

    ;(async () => {
      const [evResult, rdResult, asResult] = await Promise.allSettled([
        getAssignedEvents(),
        getAssignedCurrentRounds(),
        getMentorAssignments()
      ])
      if (cancelled) return

      if (evResult.status === 'fulfilled') {
        setEvents(evResult.value)
      } else {
        setError(localizeError(evResult.reason?.message))
        showToast('Không tải được sự kiện được phân công', 'error')
      }
      setLoading(false)

      if (rdResult.status === 'fulfilled') {
        setRounds(rdResult.value)
      } else {
        setErrorRounds(localizeError(rdResult.reason?.message))
      }
      setLoadingRounds(false)

      if (asResult.status === 'fulfilled') {
        setAssignments(asResult.value)
        if (asResult.value.length > 0) {
          setSelectedAssignmentKey(assignmentKey(asResult.value[0]))
        }
      } else {
        setErrorAssignments(localizeError(asResult.reason?.message))
      }
      setLoadingAssignments(false)
    })()

    return () => {
      cancelled = true
    }
  }, [showToast])


  useEffect(() => {
    if (!selectedAssignment) {
      setTeams([])
      setErrorTeams(null)
      setColleagues(null)
      setErrorColleagues(null)
      return
    }

    let cancelled = false
    setLoadingTeams(true)
    setErrorTeams(null)
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
  return (
    <DashboardShell
      roleLabel={guestLabel}
      title='Khu vực Mentor'
      subtitle='Khách được phân công mentor — đồng hành cùng các đội thí sinh. Cùng tài khoản có thể vào khu Judge nếu được gán chấm thi.'
      role={guestLabel}
      showStaffFields
    >
      <div className='action-row' style={{ marginBottom: '1rem' }}>
        <Link className='btn btn-outline' to='/judge'>Chuyển sang khu Judge</Link>
      </div>
      {/* ── Sự kiện được phân công ── */}
      <div className='section-title'>
        <h2>Sự kiện được phân công</h2>
        <span className='hint'>Các bảng / sự kiện bạn được Coordinator gán</span>
      </div>

      <div className='card'>
        {loading && <div className='empty-state'>Đang tải danh sách…</div>}
        {!loading && error && <div className='empty-state'>{error}</div>}
        {!loading && !error && events.length === 0 && (
          <div className='empty-state'>Bạn chưa được phân công sự kiện nào.</div>
        )}
        {!loading && events.length > 0 && (
          <CollapsibleKvList
            items={events}
            getItemKey={(ev) => ev.eventId}
            renderItem={(ev) => (
              <div className='kv'>
                <span style={{ minWidth: 0, flex: 1, textAlign: 'left' }}>
                  <div style={{ fontWeight: 600, color: 'var(--text)' }}>{ev.title || '—'}</div>
                </span>
                <StatusBadge status={ev.status} />
              </div>
            )}
          />
        )}
      </div>

      {/* ── Vòng đang diễn ra ── */}
      <div className='section-title' style={{ marginTop: 24 }}>
        <h2>Vòng đang diễn ra</h2>
        <span className='hint'>Các vòng thi hiện đang active trong sự kiện của bạn</span>
      </div>

      <div className='card'>
        {loadingRounds && <div className='empty-state'>Đang tải vòng thi…</div>}
        {!loadingRounds && errorRounds && <div className='empty-state'>{errorRounds}</div>}
        {!loadingRounds && !errorRounds && rounds.length === 0 && (
          <div className='empty-state'>Hiện không có vòng nào đang diễn ra.</div>
        )}
        {!loadingRounds && rounds.length > 0 && (
          <CollapsibleKvList
            items={rounds}
            getItemKey={(rd) => rd.roundId}
            renderItem={(rd) => (
              <div className='kv'>
                <span style={{ minWidth: 0, flex: 1, textAlign: 'left' }}>
                  <div style={{ fontWeight: 600, color: 'var(--text)' }}>{rd.roundName || '—'}</div>
                  <div style={{ fontSize: 12, color: 'var(--text-dim)', marginTop: 2 }}>{rd.eventTitle || '—'}</div>
                  <div style={{ fontSize: 11, color: 'var(--text-mute)', marginTop: 2 }}>
                    {formatDateTime(rd.startDate)} → {formatDateTime(rd.endDate)}
                  </div>
                </span>
                <StatusBadge status={rd.roundStatus} />
              </div>
            )}
          />
        )}
      </div>

      {/* ── Đội được phân công ── */}
      <div className='section-title' style={{ marginTop: 24 }}>
        <h2>Đội được phân công</h2>
        <span className='hint'>Các đội trong bảng bạn được gán mentor</span>
      </div>

      <div className='card'>
        {loadingAssignments && <div className='empty-state'>Đang tải phân công bảng…</div>}
        {!loadingAssignments && errorAssignments && (
          <div className='empty-state'>{errorAssignments}</div>
        )}
        {!loadingAssignments && !errorAssignments && assignments.length === 0 && (
          <div className='empty-state'>Bạn chưa được phân công bảng nào.</div>
        )}
        {!loadingAssignments && assignments.length > 0 && (
          <>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginBottom: 16 }}>
              {assignments.map((a) => {
                const key = assignmentKey(a)
                const active = key === selectedAssignmentKey
                return (
                  <button
                    key={key}
                    type='button'
                    className={`btn ${active ? 'btn-primary' : 'btn-outline'}`}
                    style={{ fontSize: 12, padding: '6px 12px' }}
                    onClick={() => setSelectedAssignmentKey(key)}
                  >
                    {assignmentLabel(a)}
                  </button>
                )
              })}

            </div>

            <div style={{ marginBottom: 12 }}>
              <ExpertGroupColleaguesBoard
                colleagues={colleagues}
                loading={loadingColleagues}
                error={errorColleagues}
              />
            </div>

            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginBottom: 16 }}>
              {TEAM_STATUS_FILTERS.map((f) => (
                <button
                  key={f.value}
                  type='button'
                  className={`btn ${teamStatusFilter === f.value ? 'btn-primary' : 'btn-outline'}`}
                  style={{ fontSize: 12, padding: '4px 10px' }}
                  onClick={() => setTeamStatusFilter(f.value)}
                >
                  {f.label}
                </button>
              ))}
            </div>

            {loadingTeams && <div className='empty-state'>Đang tải danh sách đội…</div>}
            {!loadingTeams && errorTeams && <div className='empty-state'>{errorTeams}</div>}
            {!loadingTeams && !errorTeams && teams.length === 0 && (
              <div className='empty-state'>Không có đội nào trong bảng này.</div>
            )}
            {!loadingTeams && teams.length > 0 && (
              <CollapsibleKvList
                items={teams}
                getItemKey={(team) => team.teamId || team.registrationId}
                renderItem={(team) => (
                  <div className='kv' style={{ alignItems: 'flex-start' }}>
                    <span style={{ minWidth: 0, flex: 1, textAlign: 'left' }}>
                      <div style={{ fontWeight: 600, color: 'var(--text)' }}>{team.teamName || '—'}</div>
                      <div style={{ fontSize: 12, color: 'var(--text-dim)', marginTop: 2 }}>
                        Trưởng nhóm: {team.leaderName || '—'}
                        {team.leaderEmail ? ` · ${team.leaderEmail}` : ''}
                      </div>
                      {team.members?.length > 0 && (
                        <div style={{ fontSize: 11, color: 'var(--text-mute)', marginTop: 4 }}>
                          Thành viên:{' '}
                          {team.members
                            .map((m) => m.fullName || m.email || '—')
                            .filter(Boolean)
                            .join(', ')}
                        </div>
                      )}
                      {team.enrollCode && (
                        <div style={{ fontSize: 11, color: 'var(--text-mute)', marginTop: 2 }}>
                          Mã đội: {team.enrollCode}
                        </div>
                      )}
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
                )}
              />
            )}
          </>
        )}
      </div>

      {/* ── Thông tin tài khoản ── */}
      <div className='section-title'>
        <h2>Thông tin tài khoản</h2>
      </div>
      <div className='card'>
        <div className='kv-list'>
          <div className='kv'>
            <span>Họ tên</span>
            <span>{auth.fullName || '—'}</span>
          </div>
          <div className='kv'>
            <span>Email</span>
            <span>{auth.email}</span>
          </div>
          <div className='kv'>
            <span>Vai trò</span>
            <span>Mentor</span>
          </div>
          <div className='kv'>
            <span>Trạng thái phiên</span>
            <span>Đã đăng nhập</span>
          </div>
        </div>
      </div>

      {!chatOpen && (
        <button type="button" className="chat-fab" onClick={() => setChatOpen(true)}>
          Chat đội
        </button>
      )}
      {chatOpen && (
        <ChatPopup open mode="mentor" onClose={() => setChatOpen(false)} />
      )}
    </DashboardShell>
  )
}

import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import DashboardShell from './DashboardShell'
import CollapsibleKvList from '../../components/common/CollapsibleList'
import ExpertGroupColleaguesBoard from '../../components/expert/ExpertGroupColleaguesBoard'
import {
  formatDateTime,
  StatusBadge,
  assignmentKey,
  assignmentLabel
} from '../../components/expert/expertDashboardUtils'
import JudgeCriteriaPanel from '../../components/judge/JudgeCriteriaPanel'
import JudgeScoreModal from '../../components/judge/JudgeScoreModal'
import SubmissionLinks from '../../components/judge/SubmissionLinks'
import {
  getAssignedEvents,
  getAssignedCurrentRounds,
  getAssignments,
  getGroupColleagues,
  getTeamsToScore
} from '../../api/judge'
import { useAuth } from '../../context/AuthContext'
import { useToast } from '../../context/ToastContext'
import { localizeError } from '../../utils/errors'

export default function JudgeDashboard() {
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

  const [teams, setTeams] = useState([])
  const [loadingTeams, setLoadingTeams] = useState(false)
  const [errorTeams, setErrorTeams] = useState(null)

  const [colleagues, setColleagues] = useState(null)
  const [loadingColleagues, setLoadingColleagues] = useState(false)
  const [errorColleagues, setErrorColleagues] = useState(null)

  const [scoreTeam, setScoreTeam] = useState(null)

  const selectedAssignment = useMemo(
    () => assignments.find((a) => assignmentKey(a) === selectedAssignmentKey) ?? null,
    [assignments, selectedAssignmentKey]
  )

  const reloadTeams = useCallback(async (assignment) => {
    if (!assignment) {
      setTeams([])
      return
    }
    setLoadingTeams(true)
    setErrorTeams(null)
    try {
      setTeams(
        await getTeamsToScore({
          eventId: assignment.eventId,
          roundId: assignment.roundId,
          groupId: assignment.groupId
        })
      )
    } catch (err) {
      setTeams([])
      setErrorTeams(localizeError(err?.message))
    } finally {
      setLoadingTeams(false)
    }
  }, [])

  useEffect(() => {
    let cancelled = false
    ;(async () => {
      const [evResult, rdResult, asResult] = await Promise.allSettled([
        getAssignedEvents(),
        getAssignedCurrentRounds(),
        getAssignments()
      ])
      if (cancelled) return

      if (evResult.status === 'fulfilled') setEvents(evResult.value)
      else {
        setError(localizeError(evResult.reason?.message))
        showToast('Không tải được sự kiện được phân công', 'error')
      }
      setLoading(false)

      if (rdResult.status === 'fulfilled') setRounds(rdResult.value)
      else setErrorRounds(localizeError(rdResult.reason?.message))
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
    reloadTeams(selectedAssignment)

    setLoadingColleagues(true)
    setErrorColleagues(null)
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
  }, [selectedAssignment, reloadTeams])

  const handleScoreSaved = () => {
    showToast('Đã lưu điểm', 'success')
    if (selectedAssignment) reloadTeams(selectedAssignment)
  }

  return (
    <DashboardShell
      roleLabel={guestLabel}
      title='Khu vực Judge'
      subtitle='Khách được phân công giám khảo — chấm điểm các đội thi. Cùng tài khoản có thể vào khu Mentor nếu được gán hướng dẫn.'
      role={guestLabel}
      showStaffFields
    >
      <div className='action-row' style={{ marginBottom: '1rem' }}>
        <Link className='btn btn-outline' to='/mentor'>
          Chuyển sang khu Mentor
        </Link>
      </div>

      <div className='section-title'>
        <h2>Sự kiện được phân công</h2>
        <span className='hint'>Các sự kiện bạn được Coordinator gán chấm thi</span>
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

      <div className='section-title' style={{ marginTop: 24 }}>
        <h2>Vòng đang diễn ra</h2>
        <span className='hint'>Các vòng thi hiện active trong phân công của bạn</span>
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

      <div className='section-title' style={{ marginTop: 24 }}>
        <h2>Phân công chấm thi</h2>
        <span className='hint'>Chọn bảng để xem đội và chấm điểm</span>
      </div>
      <div className='card'>
        {loadingAssignments && <div className='empty-state'>Đang tải phân công…</div>}
        {!loadingAssignments && errorAssignments && <div className='empty-state'>{errorAssignments}</div>}
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

            <div style={{ marginBottom: 16 }}>
              <ExpertGroupColleaguesBoard colleagues={colleagues} loading={loadingColleagues} error={errorColleagues} />
            </div>

            {loadingTeams && <div className='empty-state'>Đang tải danh sách đội…</div>}
            {!loadingTeams && errorTeams && <div className='empty-state'>{errorTeams}</div>}
            {!loadingTeams && !errorTeams && teams.length === 0 && (
              <div className='empty-state'>Không có đội nào trong bảng này.</div>
            )}
            {!loadingTeams && teams.length > 0 && (
              <CollapsibleKvList
                items={teams}
                getItemKey={(team) => team.teamId || team.submissionId}
                renderItem={(team) => (
                  <div className='kv' style={{ alignItems: 'flex-start' }}>
                    <span style={{ minWidth: 0, flex: 1, textAlign: 'left' }}>
                      <div style={{ fontWeight: 600, color: 'var(--text)' }}>{team.teamName || '—'}</div>
                      <div style={{ fontSize: 12, color: 'var(--text-dim)', marginTop: 2 }}>
                        {team.submissionId
                          ? `Nộp: ${formatDateTime(team.submittedAt)} · ${team.submissionStatus || '—'}`
                          : 'Chưa nộp bài'}
                      </div>
                      <SubmissionLinks team={team} />
                      {team.scored && team.totalScore != null && (
                        <div style={{ fontSize: 12, color: 'var(--accent)', marginTop: 4, fontWeight: 600 }}>
                          Điểm đã chấm: {team.totalScore}
                        </div>
                      )}
                    </span>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 6, alignItems: 'flex-end' }}>
                      <span className='status-picker' style={{ flexShrink: 0 }}>
                        <span
                          className={`status-pill ${team.scored ? 'status-active' : 'status-pending'}`}
                          style={{ cursor: 'default' }}
                        >
                          {team.scored ? 'Đã chấm' : 'Chưa chấm'}
                        </span>
                      </span>
                      <button
                        type='button'
                        className='btn btn-primary btn-sm'
                        style={{ fontSize: 12 }}
                        disabled={!team.submissionId}
                        onClick={() => setScoreTeam(team)}
                      >
                        {team.scored ? 'Sửa điểm' : 'Chấm điểm'}
                      </button>
                    </div>
                  </div>
                )}
              />
            )}
          </>
        )}
      </div>

      {selectedAssignment && (
        <>
          <div className='section-title' style={{ marginTop: 24 }}>
            <h2>Rubric chấm điểm</h2>
            <span className='hint'>Tiêu chí cho vòng đang chọn</span>
          </div>
          <div className='card'>
            <JudgeCriteriaPanel roundId={selectedAssignment.roundId} roundName={selectedAssignment.roundName} />
          </div>
        </>
      )}

      <div className='section-title' style={{ marginTop: 24 }}>
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
            <span>Vai trò tài khoản</span>
            <span>{guestLabel}</span>
          </div>
          <div className='kv'>
            <span>Trạng thái phiên</span>
            <span>Đã đăng nhập</span>
          </div>
        </div>
      </div>

      <JudgeScoreModal
        isOpen={Boolean(scoreTeam)}
        assignment={selectedAssignment}
        team={scoreTeam}
        onClose={() => setScoreTeam(null)}
        onSaved={handleScoreSaved}
      />
    </DashboardShell>
  )
}

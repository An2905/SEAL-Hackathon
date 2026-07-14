/* eslint-disable no-unused-vars */
import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import DashboardShell from './DashboardShell'
import FormMessage from '../../components/common/FormMessage'
import Modal from '../../components/common/Modal'
import ConfirmModal from '../../components/common/ConfirmModal'
import LoadingState from '../../components/common/LoadingState'
import { getEventDetail } from '../../api/event'
import {
  changeTeamRegistrationStatus,
  getAllAccounts,
  getTeamMembers,
  normalizeRegistrationId,
  retryGitHubProvisioning,
  updateEventRepoAccess
} from '../../api/staff'
import {
  deleteJudgeAssignment,
  deleteMentorAssignment,
  updateJudgeAssignment,
  updateMentorAssignment
} from '../../api/staffAssignment'
import {
  createEventGroup,
  createEventRound,
  deleteEventGroup,
  deleteEventRound,
  getEventGroupTeams,
  assignTeamToGroup,
  removeTeamFromGroup,
  getEventRoundDetail,
  updateEvent,
  updateEventGroup,
  updateEventRound,
  autoFillRoundGroups
} from '../../api/eventService'
import FormField from '../../components/common/FormField'
import LoadingButton from '../../components/common/LoadingButton'
import { useToast } from '../../context/ToastContext'
import { localizeError } from '../../utils/errors'
import {
  BUILDING_STATUS_OPTIONS,
  eventStatusLabel,
  eventStatusLockHint,
  isEventStatusLocked
} from '../../utils/eventStatusLabels'
import CriteriaManager from './staff/CriteriaManager'
import AwardsManager from './staff/AwardsManager'
import CommitListModal from '../../components/common/CommitListModal'
import { parseGitHubRepoUrl } from '../../api/githubRepo'
import Pagination from '../../components/common/Pagination'

const PAGE_SIZE = 5
const TEAM_REGISTRATIONS_PAGE_SIZE = 5
const REGISTRATION_STATUSES = ['PENDING', 'APPROVED', 'REJECTED']

function eventStatusPillClass(status) {
  const key = (status || '').toUpperCase()
  if (key === 'BUILDING') return 'status-pending'
  if (key === 'UPCOMING') return 'status-pending'
  if (key === 'ONGOING') return 'status-active'
  if (key === 'COMPLETED') return 'status-default'
  if (key === 'CANCELLED') return 'status-rejected'
  return 'status-default'
}

function eventAccentColor(status) {
  const key = (status || '').toUpperCase()
  if (key === 'BUILDING') return '#EF9F27'
  if (key === 'ONGOING') return '#1D9E75'
  if (key === 'COMPLETED') return '#888780'
  return '#888780'
}

function formatEventDateTime(value) {
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

function formatMaxTeams(value) {
  if (value == null || value === '') return 'Không giới hạn'
  return String(value)
}

function getRoundPhase(round) {
  const now = Date.now()
  const start = round?.startDate ? new Date(round.startDate).getTime() : NaN
  const end = round?.endDate ? new Date(round.endDate).getTime() : NaN
  if (!Number.isFinite(start) || !Number.isFinite(end)) return 'unknown'
  if (now < start) return 'upcoming'
  if (now > end) return 'past'
  return 'ongoing'
}

function isRoundOngoing(round) {
  return getRoundPhase(round) === 'ongoing'
}

function getRepoAccessSchedule(rounds, now) {
  let repoAccessAvailable = false
  let nextChangeTime = null

  for (const round of rounds ?? []) {
    const startTime = round?.startDate ? new Date(round.startDate).getTime() : NaN
    const endTime = round?.endDate ? new Date(round.endDate).getTime() : NaN
    if (!Number.isFinite(startTime)) continue

    if (startTime <= now && (!Number.isFinite(endTime) || now < endTime)) {
      repoAccessAvailable = true
    }

    for (const boundary of [startTime, endTime]) {
      if (Number.isFinite(boundary) && boundary > now && (nextChangeTime == null || boundary < nextChangeTime)) {
        nextChangeTime = boundary
      }
    }
  }

  return { repoAccessAvailable, nextChangeTime }
}

function roundDisplayLabel(round) {
  const name = String(round?.name ?? '').trim()
  if (name) return name
  const order = round?.roundOrder
  return order ? `Vòng ${order}` : 'Vòng'
}

function EventBoardCreateRoundModal({ eventId, isOpen, onClose, onCreated }) {
  const { showToast } = useToast()
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [form, setForm] = useState({
    name: '',
    winnersPerRound: '1',
    startDate: '',
    endDate: '',
    submissionDeadline: ''
  })

  useEffect(() => {
    if (!isOpen) return
    setError('')
    setForm({
      name: '',
      winnersPerRound: '1',
      startDate: '',
      endDate: '',
      submissionDeadline: ''
    })
  }, [isOpen])

  const handleChange = (e) => setForm((f) => ({ ...f, [e.target.name]: e.target.value }))

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const created = await createEventRound({
        eventId,
        name: form.name,
        winnersPerRound: form.winnersPerRound,
        startDate: form.startDate,
        endDate: form.endDate,
        submissionDeadline: form.submissionDeadline
      })
      onCreated?.(created)
      showToast('Đã tạo vòng thi', 'success')
      onClose()
    } catch (err) {
      setError(localizeError(err.message))
    } finally {
      setLoading(false)
    }
  }

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title='Thêm vòng thi'
      subtitle='Thứ tự vòng tự động tăng theo số vòng hiện có.'
    >
      <form className='form' onSubmit={handleSubmit}>
        <FormField label='Tên vòng *'>
          <input
            name='name'
            value={form.name}
            onChange={handleChange}
            maxLength={100}
            disabled={loading}
            placeholder='VD: Vòng 1 — Ý tưởng'
            required
          />
        </FormField>
        <FormField label='Winner mỗi vòng *'>
          <input
            type='number'
            name='winnersPerRound'
            min={1}
            step={1}
            value={form.winnersPerRound}
            onChange={handleChange}
            disabled={loading}
            required
          />
        </FormField>
        <FormField label='Bắt đầu *'>
          <input
            type='datetime-local'
            name='startDate'
            value={form.startDate}
            onChange={handleChange}
            disabled={loading}
            required
          />
        </FormField>
        <FormField label='Kết thúc *'>
          <input
            type='datetime-local'
            name='endDate'
            value={form.endDate}
            onChange={handleChange}
            disabled={loading}
            required
          />
        </FormField>
        <FormField label='Deadline nộp bài *'>
          <input
            type='datetime-local'
            name='submissionDeadline'
            value={form.submissionDeadline}
            onChange={handleChange}
            disabled={loading}
            required
          />
        </FormField>
        <FormMessage message={error} type='error' />
        <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
          <LoadingButton loading={loading} type='submit'>
            Tạo vòng thi
          </LoadingButton>
          <button type='button' className='btn btn-ghost' onClick={onClose} disabled={loading}>
            Hủy
          </button>
        </div>
      </form>
    </Modal>
  )
}

function EventBoardCreateGroupModal({ eventId, rounds, isOpen, onClose, onCreated }) {
  const { showToast } = useToast()
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [form, setForm] = useState({ roundId: '', name: '', maxTeams: '' })

  useEffect(() => {
    if (!isOpen) return
    setError('')
    setForm({ roundId: '', name: '', maxTeams: '' })
  }, [isOpen])

  const handleChange = (e) => setForm((f) => ({ ...f, [e.target.name]: e.target.value }))

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const created = await createEventGroup({
        eventId,
        roundId: form.roundId,
        name: form.name,
        maxTeams: form.maxTeams
      })
      onCreated?.(created)
      showToast('Đã tạo bảng thi', 'success')
      onClose()
    } catch (err) {
      setError(localizeError(err.message))
    } finally {
      setLoading(false)
    }
  }

  return (
    <Modal isOpen={isOpen} onClose={onClose} title='Thêm bảng thi' subtitle='Mỗi bảng thuộc một vòng cụ thể.'>
      {!rounds.length ? (
        <div className='empty-state'>Tạo ít nhất một vòng thi trước khi thêm bảng.</div>
      ) : (
        <form className='form' onSubmit={handleSubmit}>
          <FormField label='Vòng thi *'>
            <select name='roundId' value={form.roundId} onChange={handleChange} disabled={loading} required>
              <option value=''>— Chọn vòng —</option>
              {rounds.map((round) => (
                <option key={round.roundId} value={round.roundId}>
                  {roundDisplayLabel(round)}
                </option>
              ))}
            </select>
          </FormField>
          <FormField label='Tên bảng *'>
            <input
              name='name'
              value={form.name}
              onChange={handleChange}
              maxLength={100}
              disabled={loading}
              placeholder='VD: Bảng A'
              required
            />
          </FormField>
          <FormField label='Số đội tối đa (tuỳ chọn)'>
            <input
              type='number'
              name='maxTeams'
              value={form.maxTeams}
              onChange={handleChange}
              min={1}
              disabled={loading}
              placeholder='Để trống = không giới hạn'
            />
          </FormField>
          <FormMessage message={error} type='error' />
          <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
            <LoadingButton loading={loading} type='submit'>
              Tạo bảng
            </LoadingButton>
            <button type='button' className='btn btn-ghost' onClick={onClose} disabled={loading}>
              Hủy
            </button>
          </div>
        </form>
      )}
    </Modal>
  )
}

function filterAssignmentsForGroup(assignments, group) {
  if (!group?.groupId) return []
  return (assignments ?? []).filter(
    (item) => item.groupId === group.groupId && (!group.roundId || item.roundId === group.roundId)
  )
}

function GroupStaffAssignmentsPanel({ eventId, mentors = [], judges = [] }) {
  const [mentorsPage, setMentorsPage] = useState(1)
  const [judgesPage, setJudgesPage] = useState(1)
  const renderStaffName = (item, nameKey, emailKey, idKey) => item[nameKey] || item[emailKey] || item[idKey] || '—'

  return (
    <div className='event-group-staff-panel' style={{ marginBottom: 16 }}>
      <div className='event-group-staff-block'>
        <h4 className='section-subtitle'>Mentor ({mentors.length})</h4>
        {mentors.length === 0 ? (
          <p className='muted'>Chưa phân công mentor.</p>
        ) : (
          <>
            <ul className='simple-list'>
              {mentors.slice((mentorsPage - 1) * PAGE_SIZE, mentorsPage * PAGE_SIZE).map((m) => (
                <li className='simple-list-item event-group-staff-item' key={m.mentorId || `${m.roundId}-${m.groupId}`}>
                  <span className='event-group-staff-name'>
                    {renderStaffName(m, 'mentorName', 'mentorEmail', 'mentorId')}
                  </span>
                  {m.mentorEmail && m.mentorName ? (
                    <span className='event-group-staff-email'>{m.mentorEmail}</span>
                  ) : null}
                </li>
              ))}
            </ul>
            <Pagination
              total={mentors.length}
              pageSize={PAGE_SIZE}
              currentPage={mentorsPage}
              onChange={setMentorsPage}
            />
          </>
        )}
      </div>

      <div className='event-group-staff-block'>
        <h4 className='section-subtitle'>Judge ({judges.length})</h4>
        {judges.length === 0 ? (
          <p className='muted'>Chưa phân công judge.</p>
        ) : (
          <>
            <ul className='simple-list'>
              {judges.slice((judgesPage - 1) * PAGE_SIZE, judgesPage * PAGE_SIZE).map((j) => (
                <li className='simple-list-item event-group-staff-item' key={j.judgeId || `${j.roundId}-${j.groupId}`}>
                  <span className='event-group-staff-name'>
                    {renderStaffName(j, 'judgeName', 'judgeEmail', 'judgeId')}
                  </span>
                  {j.judgeEmail && j.judgeName ? <span className='event-group-staff-email'>{j.judgeEmail}</span> : null}
                </li>
              ))}
            </ul>
            <Pagination total={judges.length} pageSize={PAGE_SIZE} currentPage={judgesPage} onChange={setJudgesPage} />
          </>
        )}
      </div>

      <div className='event-group-staff-actions'>
        <AssignPanelLink eventId={eventId} focus='mentor'>
          + Phân công mentor
        </AssignPanelLink>
        <AssignPanelLink eventId={eventId} focus='judge'>
          + Phân công judge
        </AssignPanelLink>
      </div>
    </div>
  )
}

function EventBoardGroupDetailModal({
  eventId,
  group,
  rounds = [],
  assignedMentors = [],
  assignedJudges = [],
  isOpen,
  onClose,
  onUpdated,
  onDeleted
}) {
  const { showToast } = useToast()
  const [saving, setSaving] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const [confirmDeleteOpen, setConfirmDeleteOpen] = useState(false)
  const [teamsLoading, setTeamsLoading] = useState(false)
  const [teamActionId, setTeamActionId] = useState('')
  const [assignedTeams, setAssignedTeams] = useState([])
  const [assignedTeamsPage, setAssignedTeamsPage] = useState(1)
  const [availableTeams, setAvailableTeams] = useState([])
  const [teamCount, setTeamCount] = useState(0)
  const [selectedTeamId, setSelectedTeamId] = useState('')
  const [addingTeam, setAddingTeam] = useState(false)
  const [error, setError] = useState('')
  const [form, setForm] = useState({
    name: '',
    maxTeams: ''
  })

  const syncTeamsState = useCallback(
    (data, notifyParent = false) => {
      setAssignedTeams(data?.assigned ?? [])
      setAssignedTeamsPage(1)
      setAvailableTeams(data?.available ?? [])
      setTeamCount(data?.teamCount ?? 0)
      setSelectedTeamId('')
      if (notifyParent) {
        onUpdated?.({
          groupId: group?.groupId,
          name: form.name || group?.name || '',
          maxTeams: form.maxTeams === '' ? null : Number(form.maxTeams) || group?.maxTeams,
          teamCount: data?.teamCount ?? 0
        })
      }
    },
    [form.maxTeams, form.name, group?.groupId, group?.maxTeams, group?.name, onUpdated]
  )

  useEffect(() => {
    if (!isOpen || !group) return
    setError('')
    setForm({
      name: group.name || '',
      maxTeams: group.maxTeams == null ? '' : String(group.maxTeams)
    })
    setTeamCount(group.teamCount ?? 0)
    setAssignedTeams([])
    setAvailableTeams([])
    setSelectedTeamId('')

    let cancelled = false
    ;(async () => {
      setTeamsLoading(true)
      try {
        const data = await getEventGroupTeams({
          eventId,
          roundId: group.roundId,
          groupId: group.groupId
        })
        if (!cancelled) syncTeamsState(data, false)
      } catch (err) {
        if (!cancelled) setError(localizeError(err.message))
      } finally {
        if (!cancelled) setTeamsLoading(false)
      }
    })()

    return () => {
      cancelled = true
    }
  }, [isOpen, group, eventId, syncTeamsState])

  const groupRound = useMemo(() => rounds.find((r) => r.roundId === group?.roundId) ?? null, [group?.roundId, rounds])

  const roundLabel = useMemo(() => {
    if (!group) return '—'
    if (group.roundName) return group.roundName
    return groupRound ? roundDisplayLabel(groupRound) : '—'
  }, [group, groupRound])

  const isFirstRound = Number(groupRound?.roundOrder ?? 1) <= 1
  const addTeamLabel = isFirstRound ? 'Thêm đội đã duyệt' : 'Thêm đội winner vòng trước'
  const noTeamsHint = isFirstRound ? 'Không còn đội khả dụng' : 'Không còn đội winner vòng trước khả dụng'

  const groupMentors = useMemo(() => filterAssignmentsForGroup(assignedMentors, group), [assignedMentors, group])
  const groupJudges = useMemo(() => filterAssignmentsForGroup(assignedJudges, group), [assignedJudges, group])

  const handleChange = (e) => setForm((f) => ({ ...f, [e.target.name]: e.target.value }))

  const handleAddTeam = async () => {
    if (!group?.groupId || !selectedTeamId) return
    setAddingTeam(true)
    setError('')
    try {
      const data = await assignTeamToGroup({
        eventId,
        roundId: group.roundId,
        groupId: group.groupId,
        teamId: selectedTeamId
      })
      syncTeamsState(data, true)
      showToast('Đã thêm đội vào bảng', 'success')
    } catch (err) {
      setError(localizeError(err.message))
    } finally {
      setAddingTeam(false)
    }
  }

  const handleRemoveTeam = async (teamId) => {
    if (!group?.groupId || !teamId) return
    setTeamActionId(teamId)
    setError('')
    try {
      const data = await removeTeamFromGroup({
        eventId,
        roundId: group.roundId,
        groupId: group.groupId,
        teamId
      })
      syncTeamsState(data, true)
      showToast('Đã gỡ đội khỏi bảng', 'success')
    } catch (err) {
      setError(localizeError(err.message))
    } finally {
      setTeamActionId('')
    }
  }

  const handleSave = async (e) => {
    e.preventDefault()
    if (!group?.groupId) return
    setSaving(true)
    setError('')
    try {
      const updated = await updateEventGroup({
        eventId,
        roundId: group.roundId,
        groupId: group.groupId,
        name: form.name,
        maxTeams: form.maxTeams
      })
      onUpdated?.(updated)
      showToast('Đã cập nhật bảng thi', 'success')
      onClose()
    } catch (err) {
      setError(localizeError(err.message))
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async () => {
    if (!group?.groupId) return
    setDeleting(true)
    setError('')
    try {
      await deleteEventGroup({
        eventId,
        roundId: group.roundId,
        groupId: group.groupId
      })
      onDeleted?.(group.groupId)
      showToast('Đã xóa bảng thi', 'success')
      setConfirmDeleteOpen(false)
      onClose()
    } catch (err) {
      setError(localizeError(err.message))
      setConfirmDeleteOpen(false)
    } finally {
      setDeleting(false)
    }
  }

  const busy = saving || deleting || addingTeam || Boolean(teamActionId)

  return (
    <>
      <Modal
        isOpen={isOpen}
        onClose={onClose}
        title='Chi tiết bảng thi'
        subtitle={group?.name || undefined}
        className='modal-wide'
      >
        {!group ? null : (
          <>
            <div className='kv-list' style={{ marginBottom: 16 }}>
              <div className='kv'>
                <span>Vòng</span>
                <span>{roundLabel}</span>
              </div>
              <div className='kv'>
                <span>Số đội</span>
                <span>
                  {teamCount}/{group.maxTeams ?? '—'}
                </span>
              </div>

              <div className='kv'>
                <span>Giới hạn</span>
                <span>{group.maxTeams != null ? `Tối đa ${group.maxTeams} đội` : 'Không giới hạn'}</span>
              </div>
              <div className='kv'>
                <span>Mentor</span>
                <span>{groupMentors.length}</span>
              </div>
              <div className='kv'>
                <span>Judge</span>
                <span>{groupJudges.length}</span>
              </div>
            </div>

            <GroupStaffAssignmentsPanel eventId={eventId} mentors={groupMentors} judges={groupJudges} />

            <div className='event-group-teams-panel' style={{ marginBottom: 16 }}>
              {' '}
              <h4 className='section-subtitle'>Đội trong bảng</h4>
              {teamsLoading ? (
                <LoadingState text='Đang tải danh sách đội…' className='muted' />
              ) : assignedTeams.length === 0 ? (
                <p className='muted'>Chưa có đội nào trong bảng.</p>
              ) : (
                <>
                  <ul className='simple-list'>
                    {assignedTeams
                      .slice((assignedTeamsPage - 1) * PAGE_SIZE, assignedTeamsPage * PAGE_SIZE)
                      .map((team) => (
                        <li
                          className='simple-list-item'
                          key={team.teamId}
                          style={{
                            display: 'flex',
                            justifyContent: 'space-between',
                            alignItems: 'center',
                            gap: 8
                          }}
                        >
                          <span>{team.teamName || team.teamId}</span>
                          <button
                            type='button'
                            className='btn btn-ghost btn-sm'
                            onClick={() => handleRemoveTeam(team.teamId)}
                            disabled={busy}
                          >
                            {teamActionId === team.teamId ? '…' : 'Gỡ'}
                          </button>
                        </li>
                      ))}
                  </ul>
                  <Pagination
                    total={assignedTeams.length}
                    pageSize={PAGE_SIZE}
                    currentPage={assignedTeamsPage}
                    onChange={setAssignedTeamsPage}
                  />
                </>
              )}
              <div style={{ marginTop: 12 }}>
                <FormField label={addTeamLabel}>
                  <div
                    style={{
                      display: 'flex',
                      flexWrap: 'wrap',
                      gap: 8,
                      alignItems: 'center'
                    }}
                  >
                    <select
                      value={selectedTeamId}
                      onChange={(e) => setSelectedTeamId(e.target.value)}
                      disabled={busy || teamsLoading || availableTeams.length === 0}
                      style={{ flex: '1 1 200px', minWidth: 0 }}
                    >
                      <option value=''>{availableTeams.length === 0 ? noTeamsHint : 'Chọn đội…'}</option>{' '}
                      {availableTeams.map((team) => (
                        <option key={team.teamId} value={team.teamId}>
                          {team.teamName || team.teamId}
                        </option>
                      ))}
                    </select>
                    <LoadingButton
                      type='button'
                      loading={addingTeam}
                      onClick={handleAddTeam}
                      disabled={busy || teamsLoading || !selectedTeamId}
                    >
                      Thêm đội
                    </LoadingButton>
                  </div>
                </FormField>
              </div>
            </div>

            <form className='form' onSubmit={handleSave}>
              <FormField label='Tên bảng *'>
                <input name='name' value={form.name} onChange={handleChange} maxLength={100} disabled={busy} required />
              </FormField>
              <FormField label='Số đội tối đa'>
                <input
                  type='number'
                  name='maxTeams'
                  value={form.maxTeams}
                  onChange={handleChange}
                  min={1}
                  disabled={busy}
                  placeholder='Để trống = không giới hạn'
                />
              </FormField>
              <FormMessage message={error} type='error' />
              <div className='event-board-modal-actions'>
                <div className='event-board-modal-actions-left'>
                  <LoadingButton loading={saving} type='submit' disabled={busy}>
                    Lưu thay đổi
                  </LoadingButton>
                  <button type='button' className='btn btn-ghost' onClick={onClose} disabled={busy}>
                    Đóng
                  </button>
                </div>
                <div className='event-board-modal-actions-right'>
                  <button
                    type='button'
                    className='btn btn-danger btn-sm'
                    onClick={() => setConfirmDeleteOpen(true)}
                    disabled={busy}
                  >
                    Xóa bảng
                  </button>
                </div>
              </div>
            </form>
          </>
        )}
      </Modal>
      <ConfirmModal
        isOpen={confirmDeleteOpen}
        onClose={() => setConfirmDeleteOpen(false)}
        onConfirm={handleDelete}
        title='Xóa bảng thi'
        message={`Xóa "${group?.name || 'bảng thi'}"? Phân công mentor/judge liên quan cũng sẽ bị gỡ.`}
        confirmLabel='Xóa'
        loading={deleting}
        danger
      />
    </>
  )
}

function EventBoardRoundDetailModal({ eventId, round, isOpen, onClose, onUpdated, onDeleted, onAutoFilled }) {
  const { showToast } = useToast()
  const [loadingDetail, setLoadingDetail] = useState(false)
  const [saving, setSaving] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const [autoFilling, setAutoFilling] = useState(false)
  const [confirmDeleteOpen, setConfirmDeleteOpen] = useState(false)
  const [error, setError] = useState('')
  const [form, setForm] = useState({
    name: '',
    roundOrder: '1',
    winnersPerRound: '1',
    startDate: '',
    endDate: '',
    submissionDeadline: ''
  })

  const loadDetail = useCallback(async () => {
    if (!round?.roundId) return
    setLoadingDetail(true)
    setError('')
    try {
      const d = await getEventRoundDetail({
        eventId,
        roundId: round.roundId
      })
      setForm({
        name: d.name || '',
        roundOrder: d.roundOrder || '1',
        winnersPerRound: String(d.winnersPerRound ?? 1),
        startDate: toDatetimeLocalValue(d.startDate),
        endDate: toDatetimeLocalValue(d.endDate),
        submissionDeadline: toDatetimeLocalValue(d.submissionDeadline)
      })
    } catch (err) {
      setError(localizeError(err.message))
    } finally {
      setLoadingDetail(false)
    }
  }, [eventId, round?.roundId])

  useEffect(() => {
    if (isOpen && round) loadDetail()
  }, [isOpen, round, loadDetail])

  const handleChange = (e) => setForm((f) => ({ ...f, [e.target.name]: e.target.value }))

  const handleSave = async (e) => {
    e.preventDefault()
    if (!round?.roundId) return
    setSaving(true)
    setError('')
    try {
      const updated = await updateEventRound({
        eventId,
        roundId: round.roundId,
        name: form.name,
        roundOrder: form.roundOrder,
        winnersPerRound: form.winnersPerRound,
        startDate: form.startDate,
        endDate: form.endDate,
        submissionDeadline: form.submissionDeadline
      })
      onUpdated?.(updated)
      showToast('Đã cập nhật vòng thi', 'success')
      onClose()
    } catch (err) {
      setError(localizeError(err.message))
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async () => {
    if (!round?.roundId) return
    setDeleting(true)
    setError('')
    try {
      await deleteEventRound({ eventId, roundId: round.roundId })
      onDeleted?.(round.roundId)
      showToast('Đã xóa vòng thi', 'success')
      setConfirmDeleteOpen(false)
      onClose()
    } catch (err) {
      setError(localizeError(err.message))
      setConfirmDeleteOpen(false)
    } finally {
      setDeleting(false)
    }
  }

  const handleAutoFill = async () => {
    if (!round?.roundId) return
    setAutoFilling(true)
    setError('')
    try {
      const result = await autoFillRoundGroups({ eventId, roundId: round.roundId })
      const roundNum = Number(round.roundOrder ?? 1)
      const hint =
        result.assignedCount === 0 && roundNum > 1
          ? ' Vòng 2+ chỉ phân các đội winner vòng trước — hãy thử vòng 1 (IDEA round).'
          : ''
      showToast((result.message || 'Đã phân bảng tự động') + hint, result.assignedCount > 0 ? 'success' : 'info')
      await onAutoFilled?.()
    } catch (err) {
      setError(localizeError(err.message))
      showToast(localizeError(err.message), 'error')
    } finally {
      setAutoFilling(false)
    }
  }

  const busy = loadingDetail || saving || deleting || autoFilling
  const phase = round ? getRoundPhase(round) : 'unknown'
  const phaseLabel =
    phase === 'ongoing' ? 'Đang diễn ra' : phase === 'upcoming' ? 'Sắp diễn ra' : phase === 'past' ? 'Đã kết thúc' : '—'

  return (
    <>
      <Modal
        isOpen={isOpen}
        onClose={onClose}
        title='Chi tiết vòng thi'
        subtitle={round ? roundDisplayLabel(round) : undefined}
        className='modal-wide'
      >
        {loadingDetail ? (
          <LoadingState text='Đang tải thông tin vòng…' />
        ) : (
          <>
            <div className='kv-list' style={{ marginBottom: 16 }}>
              <div className='kv'>
                <span>Trạng thái</span>
                <span>{phaseLabel}</span>
              </div>
              <div className='kv'>
                <span>Winner</span>
                <span>
                  {round?.winnerCount ?? 0}/{round?.winnersPerRound ?? 1}
                </span>
              </div>
              <div className='kv'>
                <span>Bắt đầu</span>
                <span>{formatEventDateTime(round?.startDate)}</span>
              </div>
              <div className='kv'>
                <span>Kết thúc</span>
                <span>{formatEventDateTime(round?.endDate)}</span>
              </div>
              <div className='kv'>
                <span>Deadline nộp bài</span>
                <span>{formatEventDateTime(round?.submissionDeadline)}</span>
              </div>
            </div>

            <form className='form' onSubmit={handleSave}>
              <FormField label='Tên vòng *'>
                <input name='name' value={form.name} onChange={handleChange} maxLength={100} disabled={busy} required />
              </FormField>
              <FormField label='Thứ tự vòng *'>
                <input
                  type='number'
                  name='roundOrder'
                  min={1}
                  step={1}
                  value={form.roundOrder}
                  onChange={handleChange}
                  disabled={busy}
                  required
                />
              </FormField>
              <FormField label='Winner mỗi vòng *'>
                <input
                  type='number'
                  name='winnersPerRound'
                  min={1}
                  step={1}
                  value={form.winnersPerRound}
                  onChange={handleChange}
                  disabled={busy}
                  required
                />
              </FormField>
              <FormField label='Bắt đầu *'>
                <input
                  type='datetime-local'
                  name='startDate'
                  value={form.startDate}
                  onChange={handleChange}
                  disabled={busy}
                  required
                />
              </FormField>
              <FormField label='Kết thúc *'>
                <input
                  type='datetime-local'
                  name='endDate'
                  value={form.endDate}
                  onChange={handleChange}
                  disabled={busy}
                  required
                />
              </FormField>
              <FormField label='Deadline nộp bài *'>
                <input
                  type='datetime-local'
                  name='submissionDeadline'
                  value={form.submissionDeadline}
                  onChange={handleChange}
                  disabled={busy}
                  required
                />
              </FormField>
              <FormMessage message={error} type='error' />
              <div className='event-board-modal-actions'>
                <div className='event-board-modal-actions-left'>
                  <LoadingButton
                    type='button'
                    loading={autoFilling}
                    disabled={busy}
                    onClick={handleAutoFill}
                    className='btn btn-primary btn-sm'
                  >
                    Tự động phân bảng (vòng {round?.roundOrder ?? '—'})
                  </LoadingButton>
                </div>
                <div className='event-board-modal-actions-right'>
                  <LoadingButton
                    loading={saving}
                    type='submit'
                    disabled={busy}
                    className='btn btn-primary btn-sm'
                  >
                    Lưu
                  </LoadingButton>
                  <button
                    type='button'
                    className='btn btn-danger btn-sm'
                    onClick={() => setConfirmDeleteOpen(true)}
                    disabled={busy}
                  >
                    Xóa
                  </button>
                </div>
              </div>
            </form>
          </>
        )}
      </Modal>
      <ConfirmModal
        isOpen={confirmDeleteOpen}
        onClose={() => setConfirmDeleteOpen(false)}
        onConfirm={handleDelete}
        title='Xóa vòng thi'
        message={`Xóa "${round?.name || roundDisplayLabel(round)}"? Các bảng và phân công liên quan cũng sẽ bị gỡ.`}
        confirmLabel='Xóa'
        loading={deleting}
        danger
      />
    </>
  )
}

function EventBoardWinnerCell({ round, active = false }) {
  const winnerAssigned = round?.winnerCount ?? 0
  const winnerTotal = round?.winnersPerRound ?? 1

  return (
    <div className={`event-board-winner-pill${active ? ' is-active' : ' is-idle'}`}>
      <span className='event-board-winner-pill-title'>Winner</span>
      <span className='event-board-winner-pill-meta'>
        {winnerAssigned}/{winnerTotal}
      </span>
    </div>
  )
}

function EventBoardSection({
  event,
  onRoundCreated,
  onGroupCreated,
  onRoundUpdated,
  onRoundDeleted,
  onGroupUpdated,
  onGroupDeleted,
  onAutoFilled
}) {
  const [roundModalOpen, setRoundModalOpen] = useState(false)
  const [groupModalOpen, setGroupModalOpen] = useState(false)
  const [roundDetailOpen, setRoundDetailOpen] = useState(false)
  const [roundDetailRound, setRoundDetailRound] = useState(null)
  const [groupDetailOpen, setGroupDetailOpen] = useState(false)
  const [groupDetailGroup, setGroupDetailGroup] = useState(null)

  const rounds = useMemo(
    () => [...(event?.rounds ?? [])].sort((a, b) => Number(a.roundOrder) - Number(b.roundOrder)),
    [event?.rounds]
  )

  const groupsByRound = useMemo(() => {
    const map = new Map()
    for (const group of event?.groups ?? []) {
      const roundId = group.roundId
      if (!roundId) continue
      if (!map.has(roundId)) map.set(roundId, [])
      map.get(roundId).push(group)
    }
    for (const groups of map.values()) {
      groups.sort((a, b) => String(a.name).localeCompare(String(b.name), 'vi'))
    }
    return map
  }, [event?.groups])

  const staffCountByGroup = useMemo(() => {
    const map = new Map()
    for (const group of event?.groups ?? []) {
      const key = `${group.roundId}:${group.groupId}`
      map.set(key, {
        mentors: filterAssignmentsForGroup(event?.assignedMentors, group).length,
        judges: filterAssignmentsForGroup(event?.assignedJudges, group).length
      })
    }
    return map
  }, [event?.groups, event?.assignedMentors, event?.assignedJudges])
  return (
    <section className='event-board' style={{ marginTop: 16 }}>
      <div className='event-board-head' aria-hidden='true'>
        <span>Vòng</span>
        <span>Bảng thi</span>
        <span>Winner</span>
      </div>
      {rounds.length ? (
        rounds.map((round) => {
          const groups = groupsByRound.get(round.roundId) ?? []
          const roundOngoing = isRoundOngoing(round)

          return (
            <div key={round.roundId} className={`event-board-row${roundOngoing ? ' is-ongoing' : ''}`}>
              <div className='event-board-row-round'>
                <button
                  type='button'
                  className='event-board-round-pill event-board-round-pill-btn'
                  onClick={() => {
                    setRoundDetailRound(round)
                    setRoundDetailOpen(true)
                  }}
                >
                  {roundDisplayLabel(round)}
                </button>
              </div>

              <div className='event-board-row-groups'>
                {groups.length ? (
                  <div className='event-board-groups-track'>
                    {groups.map((group) => {
                      const staffCounts = staffCountByGroup.get(`${group.roundId}:${group.groupId}`) ?? {
                        mentors: 0,
                        judges: 0
                      }
                      return (
                        <button
                          key={group.groupId}
                          type='button'
                          className='event-board-group-card event-board-group-card-btn'
                          onClick={() => {
                            setGroupDetailGroup(group)
                            setGroupDetailOpen(true)
                          }}
                        >
                          <div className='event-board-card-title'>{group.name || '—'}</div>
                          <div className='event-board-card-meta'>
                            {group.teamCount ?? 0}/{group.maxTeams ?? '—'} Team · {staffCounts.mentors} Mentor ·{' '}
                            {staffCounts.judges} Judge
                          </div>
                        </button>
                      )
                    })}{' '}
                  </div>
                ) : (
                  <div className='event-board-groups-empty'>Chưa có bảng thi</div>
                )}
              </div>

              <div className='event-board-row-winner'>
                <EventBoardWinnerCell round={round} active={roundOngoing} />
              </div>
            </div>
          )
        })
      ) : (
        <div className='event-board-row'>
          <div className='event-board-row-round'>
            <span className='event-board-round-pill event-board-round-pill-empty'>—</span>
          </div>
          <div className='event-board-row-groups'>
            <div className='event-stat-dropdown-empty'>Chưa có vòng thi.</div>
          </div>
          <div className='event-board-row-winner'>
            <EventBoardWinnerCell round={null} active={false} />
          </div>
        </div>
      )}
      <div className='event-board-row event-board-row-add'>
        <div className='event-board-row-round'>
          <button type='button' className='event-board-add-btn' onClick={() => setRoundModalOpen(true)}>
            + Vòng
          </button>
        </div>
        <div className='event-board-row-groups event-board-row-groups-add'>
          <button type='button' className='event-board-add-btn' onClick={() => setGroupModalOpen(true)}>
            + Bảng
          </button>
        </div>
        <div className='event-board-row-winner' />
      </div>
      <EventBoardCreateRoundModal
        eventId={event.eventId}
        isOpen={roundModalOpen}
        onClose={() => setRoundModalOpen(false)}
        onCreated={onRoundCreated}
      />
      <EventBoardCreateGroupModal
        eventId={event.eventId}
        rounds={rounds}
        isOpen={groupModalOpen}
        onClose={() => setGroupModalOpen(false)}
        onCreated={onGroupCreated}
      />
      <EventBoardRoundDetailModal
        eventId={event.eventId}
        round={roundDetailRound}
        isOpen={roundDetailOpen}
        onClose={() => {
          setRoundDetailOpen(false)
          setRoundDetailRound(null)
        }}
        onUpdated={onRoundUpdated}
        onDeleted={onRoundDeleted}
        onAutoFilled={onAutoFilled}
      />
      <EventBoardGroupDetailModal
        eventId={event.eventId}
        group={groupDetailGroup}
        rounds={rounds}
        assignedMentors={event.assignedMentors ?? []}
        assignedJudges={event.assignedJudges ?? []}
        isOpen={groupDetailOpen}
        onClose={() => {
          setGroupDetailOpen(false)
          setGroupDetailGroup(null)
        }}
        onUpdated={onGroupUpdated}
        onDeleted={onGroupDeleted}
      />{' '}
    </section>
  )
}

function toDatetimeLocalValue(value) {
  if (!value) return ''
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) {
    const s = String(value).trim()
    if (s.length >= 16) return s.slice(0, 16)
    return ''
  }
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function registrationStatusPillClass(status) {
  const key = (status || '').toLowerCase()
  if (key === 'approved') return 'status-active'
  if (key === 'pending') return 'status-pending'
  if (key === 'rejected') return 'status-rejected'
  return 'status-default'
}

function teamStatusLabel(status) {
  const key = String(status ?? '')
    .trim()
    .toUpperCase()
  if (key === 'APPROVED') return 'Đã duyệt'
  if (key === 'PENDING') return 'Chờ duyệt'
  if (key === 'REJECTED') return 'Từ chối'
  return status || '—'
}

function githubStatusShortLabel(status) {
  const key = String(status ?? '').toUpperCase()
  if (key === 'SUCCESS') return 'GitHub đã cấp'
  if (key === 'PENDING') return 'GitHub đang xử lý'
  if (key === 'FAILED') return 'GitHub lỗi'
  return 'Chưa khởi tạo GitHub'
}

function registrationMatches(a, b) {
  return normalizeRegistrationId(a) === normalizeRegistrationId(b)
}

function patchTeamRegistration(list, registrationId, updates) {
  if (!Array.isArray(list)) return list
  return list.map((team) =>
    registrationMatches(team.registrationId, registrationId) ? { ...team, ...updates } : team
  )
}

function TeamRegistrationStatusPicker({ team, onUpdated }) {
  const [saving, setSaving] = useState(false)
  const currentStatus = String(team?.status ?? '')
    .trim()
    .toUpperCase()

  const handleSelect = async (e) => {
    const next = e.target.value
    if (next === currentStatus) return
    if (!team?.registrationId) return

    const previousStatus = currentStatus
    onUpdated?.(team.registrationId, { status: next })

    setSaving(true)
    try {
      await changeTeamRegistrationStatus({ registrationId: team.registrationId, status: next })
    } catch {
      onUpdated?.(team.registrationId, { status: previousStatus })
    } finally {
      setSaving(false)
    }
  }

  return (
    <span className='status-picker'>
      <select
        className='status-picker-select'
        value={currentStatus || REGISTRATION_STATUSES[0]}
        onChange={handleSelect}
        disabled={saving}
        aria-label='Trạng thái đăng ký'
      >
        {REGISTRATION_STATUSES.map((s) => (
          <option key={s} value={s}>
            {teamStatusLabel(s)}
          </option>
        ))}
      </select>
    </span>
  )
}

function TeamRegistrationDetailModal({ team, isOpen, onClose, onTeamUpdated }) {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [members, setMembers] = useState([])

  const loadMembers = useCallback(async () => {
    if (!team?.teamId) return
    setLoading(true)
    setError('')
    try {
      setMembers(await getTeamMembers(team.teamId))
    } catch (err) {
      setError(localizeError(err.message))
      setMembers([])
    } finally {
      setLoading(false)
    }
  }, [team?.teamId])

  useEffect(() => {
    if (isOpen && team?.teamId) loadMembers()
  }, [isOpen, team?.teamId, loadMembers])

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title='Đăng ký & GitHub'
      className='modal-wide team-registration-detail-modal'
    >
      {error ? <FormMessage message={error} type='error' /> : null}

      <div className='team-registration-detail-head'>
        <p className='team-registration-detail-team-name'>{team?.teamName || '—'}</p>
        <div className='team-registration-detail-head-controls'>
          {team ? <GitHubStatusBadge team={team} onGitHubUpdated={onTeamUpdated} /> : null}
          {team ? <TeamRegistrationStatusPicker team={team} onUpdated={onTeamUpdated} /> : null}
        </div>
      </div>

      {loading ? (
        <LoadingState text='Đang tải thành viên…' />
      ) : members.length === 0 ? (
        <div className='empty-state team-registration-members-empty'>Đội chưa có thành viên.</div>
      ) : (
        <div className='team-registration-members'>
          <div className='accounts-table team-registration-members-table'>
            <div className='accounts-table-row accounts-table-row--head'>
              <span className='accounts-table-index'>#</span>
              <span>Họ và tên</span>
              <span>Email</span>
              <span>GitHub</span>
              <span>Trạng thái</span>
            </div>
            {members.map((member, idx) => (
              <div className='accounts-table-row' key={member.userId || `${member.email}-${idx}`}>
                <span className='accounts-table-index'>{idx + 1}</span>
                <span className='accounts-table-cell' style={{ fontWeight: 600, color: 'var(--text)' }}>
                  {member.fullName || '—'}
                </span>
                <span className='accounts-table-cell accounts-table-cell--muted'>{member.email || '—'}</span>
                <span className='accounts-table-cell accounts-table-cell--muted'>
                  {member.githubUsername || '—'}
                </span>
                <span className='accounts-table-cell accounts-table-cell--status'>
                  <span className={`status-pill ${registrationStatusPillClass(member.status)}`} style={{ cursor: 'default' }}>
                    {teamStatusLabel(member.status)}
                  </span>
                </span>
              </div>
            ))}
          </div>
        </div>
      )}
    </Modal>
  )
}

function GitHubStatusBadge({ team, onGitHubUpdated }) {
  const { showToast } = useToast()
  const [loading, setLoading] = useState(false)
  const [commitModalOpen, setCommitModalOpen] = useState(false)
  const parsedRepo = parseGitHubRepoUrl(team.githubRepoUrl)

  const handleRetry = async () => {
    if (!team.registrationId) return
    setLoading(true)
    try {
      await retryGitHubProvisioning(team.registrationId)
      showToast('Đang khởi tạo lại cấp phát tài nguyên GitHub...', 'success')
      // Update local state to PENDING so coordinator sees loading status immediately
      onGitHubUpdated?.(team.registrationId, { githubStatus: 'PENDING' })
    } catch (err) {
      showToast(localizeError(err.message), 'error')
    } finally {
      setLoading(false)
    }
  }

  const status = String(team.githubStatus || '').toUpperCase()

  if (status === 'SUCCESS') {
    return (
      <>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <span
            className='status-pill'
            style={{
              background: '#e6fffa',
              color: '#047481',
              border: '1px solid #b2f5ea',
              cursor: 'default',
              fontSize: 11,
              padding: '2px 8px',
              borderRadius: 20,
              fontWeight: 600
            }}
          >
            GitHub SUCCESS
          </span>
          {team.githubRepoUrl && (
            <a
              href={team.githubRepoUrl}
              target='_blank'
              rel='noopener noreferrer'
              className='btn btn-ghost btn-sm'
              style={{ fontSize: 11, padding: '2px 6px', textDecoration: 'underline' }}
              title='Repo URL'
            >
              Repo
            </a>
          )}
          {parsedRepo && (
            <button
              type='button'
              className='btn btn-outline btn-sm'
              style={{ fontSize: 11, padding: '2px 8px' }}
              onClick={() => setCommitModalOpen(true)}
            >
              Commits
            </button>
          )}
        </div>
        {parsedRepo && (
          <CommitListModal
            isOpen={commitModalOpen}
            onClose={() => setCommitModalOpen(false)}
            owner={parsedRepo.owner}
            repo={parsedRepo.repo}
            teamName={team.teamName}
          />
        )}
      </>
    )
  }

  if (status === 'PENDING') {
    return (
      <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
        <span
          className='status-pill'
          style={{
            background: '#fffbeb',
            color: '#92400e',
            border: '1px solid #fde68a',
            display: 'inline-flex',
            alignItems: 'center',
            gap: 4,
            cursor: 'default',
            fontSize: 11,
            padding: '2px 8px',
            borderRadius: 20,
            fontWeight: 600
          }}
        >
          <span
            className='spinner spinner-dark spinner--sm'
            style={{ borderTopColor: '#92400e', width: 10, height: 10, borderWidth: 1.5 }}
          />
          GitHub PENDING
        </span>
      </div>
    )
  }

  if (status === 'FAILED') {
    const showRetry = String(team.status || '').toUpperCase() === 'APPROVED'
    return (
      <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
        <span
          className='status-pill'
          style={{
            background: '#fef2f2',
            color: '#b91c1c',
            border: '1px solid #fecaca',
            cursor: 'default',
            fontSize: 11,
            padding: '2px 8px',
            borderRadius: 20,
            fontWeight: 600
          }}
        >
          GitHub FAILED
        </span>
        {showRetry && (
          <button
            type='button'
            className='btn btn-outline btn-sm'
            style={{ fontSize: 11, padding: '2px 8px', height: 'auto', minHeight: 0 }}
            onClick={handleRetry}
            disabled={loading}
          >
            {loading ? '...' : 'Thử lại'}
          </button>
        )}
      </div>
    )
  }

  return (
    <span
      className='status-pill'
      style={{
        background: '#f3f4f6',
        color: '#374151',
        border: '1px solid #e5e7eb',
        cursor: 'default',
        fontSize: 11,
        padding: '2px 8px',
        borderRadius: 20,
        fontWeight: 600
      }}
    >
      Chưa khởi tạo
    </span>
  )
}

function TeamRegistrationsBulkActions({ onBulkAccess, repoAccessAvailable, firstRoundStartTime }) {
  if (!repoAccessAvailable) {
    const availableAt = firstRoundStartTime ? formatEventDateTime(new Date(firstRoundStartTime)) : 'khi cấu hình vòng đầu tiên'
    return (
      <span className='card-sub' style={{ margin: 0 }}>
        Quyền repository sẽ khả dụng từ {availableAt}.
      </span>
    )
  }

  return (
    <div className='team-registrations-bulk-actions'>
      <button
        type='button'
        className='btn btn-outline btn-sm'
        style={{
          borderColor: '#22c55e',
          color: '#16a34a',
          padding: '6px 12px',
          height: 'auto',
          minHeight: 0
        }}
        onClick={() => onBulkAccess?.(true)}
      >
        Mở quyền làm bài (Tất cả)
      </button>
      <button
        type='button'
        className='btn btn-outline btn-sm'
        style={{
          borderColor: '#ef4444',
          color: '#dc2626',
          padding: '6px 12px',
          height: 'auto',
          minHeight: 0
        }}
        onClick={() => onBulkAccess?.(false)}
      >
        Khóa quyền làm bài (Tất cả)
      </button>
    </div>
  )
}

function TeamRegistrationsSection({
  teams,
  onTeamUpdated,
  onBulkAccess,
  repoAccessAvailable,
  firstRoundStartTime
}) {
  const [page, setPage] = useState(1)
  const [activeTeam, setActiveTeam] = useState(null)
  const [teamRows, setTeamRows] = useState(teams)

  useEffect(() => {
    setTeamRows(teams)
  }, [teams])

  useEffect(() => {
    const totalPages = Math.max(1, Math.ceil(teamRows.length / TEAM_REGISTRATIONS_PAGE_SIZE))
    if (page > totalPages) setPage(totalPages)
  }, [teamRows.length, page])

  const sortedTeamRows = useMemo(
    () =>
      [...teamRows].sort((a, b) =>
        String(a.teamName ?? '').localeCompare(String(b.teamName ?? ''), 'vi', { sensitivity: 'base' })
      ),
    [teamRows]
  )

  const paginatedTeams = useMemo(
    () =>
      sortedTeamRows.slice(
        (page - 1) * TEAM_REGISTRATIONS_PAGE_SIZE,
        page * TEAM_REGISTRATIONS_PAGE_SIZE
      ),
    [sortedTeamRows, page]
  )

  const handlePageChange = (nextPage) => {
    setPage(nextPage)
  }

  const handleTeamUpdated = (registrationId, updates) => {
    setTeamRows((prev) => patchTeamRegistration(prev, registrationId, updates))
    setActiveTeam((prev) =>
      prev && registrationMatches(prev.registrationId, registrationId) ? { ...prev, ...updates } : prev
    )
    onTeamUpdated?.(registrationId, updates)
  }

  if (!teamRows.length) {
    return (
      <section className='criteria-manager'>
        <div className='criteria-manager-head'>
          <div className='team-registrations-title-row'>
            <h3 className='section-title' style={{ margin: 0 }}>
              Đăng ký và tích hợp GitHub
            </h3>
            <TeamRegistrationsBulkActions
              onBulkAccess={onBulkAccess}
              repoAccessAvailable={repoAccessAvailable}
              firstRoundStartTime={firstRoundStartTime}
            />
          </div>
        </div>
        <div className='empty-state'>Chưa có đội nào tham gia.</div>
      </section>
    )
  }

  return (
    <section className='criteria-manager'>
      <div className='criteria-manager-head'>
        <div className='team-registrations-title-row'>
          <h3 className='section-title' style={{ margin: 0 }}>
            Đăng ký và tích hợp GitHub
          </h3>
          <TeamRegistrationsBulkActions
            onBulkAccess={onBulkAccess}
            repoAccessAvailable={repoAccessAvailable}
            firstRoundStartTime={firstRoundStartTime}
          />
        </div>
      </div>

      <div className='criteria-round-list team-registrations-list'>
        {paginatedTeams.map((team) => (
          <button
            key={team.registrationId || team.teamId}
            type='button'
            className='criteria-round-row'
            onClick={() => setActiveTeam(team)}
          >
            <span className='criteria-round-row-main'>
              <span className='criteria-round-row-name'>{team.teamName || '—'}</span>
              <span className='criteria-round-row-meta'>
                {teamStatusLabel(team.status)} · {githubStatusShortLabel(team.githubStatus)}
              </span>
            </span>
            <span className='criteria-round-row-action'>Chi tiết ›</span>
          </button>
        ))}
      </div>

      <Pagination
        className='team-registrations-pagination'
        total={sortedTeamRows.length}
        pageSize={TEAM_REGISTRATIONS_PAGE_SIZE}
        currentPage={page}
        onChange={handlePageChange}
        itemLabel='đội'
        showSinglePageSummary
      />

      <TeamRegistrationDetailModal
        team={activeTeam}
        isOpen={Boolean(activeTeam)}
        onClose={() => setActiveTeam(null)}
        onTeamUpdated={handleTeamUpdated}
      />
    </section>
  )
}

function StaffActionLink({ eventId, tab, focus, children }) {
  const qs = new URLSearchParams({ eventId: String(eventId), tab })
  if (focus) qs.set('focus', focus)
  return (
    <div
      style={{
        marginTop: 10,
        paddingTop: 10,
        borderTop: '1px solid var(--border)'
      }}
    >
      <Link
        to={`/staff?${qs.toString()}`}
        className='btn btn-outline'
        style={{ width: '100%', fontSize: 12, justifyContent: 'center' }}
      >
        {children}
      </Link>
    </div>
  )
}

function AssignPanelLink({ eventId, focus, children }) {
  return (
    <StaffActionLink eventId={eventId} tab='assign' focus={focus}>
      {children}
    </StaffActionLink>
  )
}

function SetupPanelLink({ eventId, focus, children }) {
  return (
    <StaffActionLink eventId={eventId} tab='events' focus={focus}>
      {children}
    </StaffActionLink>
  )
}

function StatItemDeleteButton({ itemLabel, onDelete, confirmMessage }) {
  const { showToast } = useToast()
  const [loading, setLoading] = useState(false)
  const [confirmOpen, setConfirmOpen] = useState(false)

  const message = confirmMessage ?? `Xóa "${itemLabel}"? Phân công mentor/judge liên quan cũng sẽ bị gỡ.`

  const handleConfirm = async () => {
    setLoading(true)
    try {
      await onDelete()
      showToast('Đã xóa thành công', 'success')
      setConfirmOpen(false)
    } catch (err) {
      showToast(localizeError(err.message), 'error')
    } finally {
      setLoading(false)
    }
  }

  return (
    <>
      <button
        type='button'
        className='btn btn-danger btn-sm event-stat-item-delete'
        onClick={() => setConfirmOpen(true)}
        disabled={loading}
      >
        {loading ? '…' : 'Xóa'}
      </button>
      <ConfirmModal
        isOpen={confirmOpen}
        onClose={() => setConfirmOpen(false)}
        onConfirm={handleConfirm}
        title='Xóa mục'
        message={message}
        confirmLabel='Xóa'
        loading={loading}
        danger
      />
    </>
  )
}

function GroupStatItem({ eventId, group, onUpdated, onDeleted }) {
  const { showToast } = useToast()
  const [editOpen, setEditOpen] = useState(false)
  const [saving, setSaving] = useState(false)
  const [form, setForm] = useState({
    name: group.name || '',
    maxTeams: group.maxTeams == null ? '' : String(group.maxTeams)
  })

  useEffect(() => {
    if (!editOpen) {
      setForm({
        name: group.name || '',
        maxTeams: group.maxTeams == null ? '' : String(group.maxTeams)
      })
    }
  }, [group.name, group.maxTeams, editOpen])

  const handleChange = (e) => setForm((f) => ({ ...f, [e.target.name]: e.target.value }))

  const handleSave = async (e) => {
    e.preventDefault()
    setSaving(true)
    try {
      const updated = await updateEventGroup({
        eventId,
        roundId: group.roundId,
        groupId: group.groupId,
        name: form.name,
        maxTeams: form.maxTeams
      })
      onUpdated?.(updated)
      showToast('Đã cập nhật bảng thi', 'success')
      setEditOpen(false)
    } catch (err) {
      showToast(localizeError(err.message), 'error')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className={`event-stat-item-card${editOpen ? ' is-edit-open' : ''}`}>
      <div className='kv event-stat-item'>
        <span style={{ minWidth: 0, flex: 1, textAlign: 'left' }}>
          <div style={{ fontWeight: 600 }}>{group.name || '—'}</div>
          <div style={{ fontSize: 11, color: 'var(--text-dim)' }}>
            Vòng: {group.roundName || '—'}
            {group.maxTeams != null ? ` · Tối đa ${group.maxTeams} đội` : ''}
          </div>
        </span>
        <div className='event-stat-item-actions'>
          <button
            type='button'
            className='btn btn-outline btn-sm'
            onClick={() => setEditOpen((v) => !v)}
            aria-expanded={editOpen}
          >
            {editOpen ? 'Thu gọn' : 'Sửa'}
          </button>
          <StatItemDeleteButton
            itemLabel={group.name || 'bảng'}
            onDelete={async () => {
              await deleteEventGroup({
                eventId,
                roundId: group.roundId,
                groupId: group.groupId
              })
              onDeleted?.(group.groupId)
            }}
          />
        </div>
      </div>
      {editOpen ? (
        <form className='event-stat-item-edit' onSubmit={handleSave}>
          <FormField label='Tên bảng *'>
            <input name='name' value={form.name} onChange={handleChange} maxLength={100} disabled={saving} required />
          </FormField>
          <FormField label='Số đội tối đa'>
            <input
              type='number'
              name='maxTeams'
              value={form.maxTeams}
              onChange={handleChange}
              min={1}
              disabled={saving}
              placeholder='Để trống = không giới hạn'
            />
          </FormField>
          <div className='event-stat-item-edit-foot'>
            <LoadingButton loading={saving} type='submit'>
              Lưu thay đổi
            </LoadingButton>
            <button type='button' className='btn btn-ghost btn-sm' onClick={() => setEditOpen(false)} disabled={saving}>
              Hủy
            </button>
          </div>
        </form>
      ) : null}
    </div>
  )
}

function RoundStatItem({ eventId, round, onUpdated, onDeleted }) {
  const { showToast } = useToast()
  const [editOpen, setEditOpen] = useState(false)
  const [loadingDetail, setLoadingDetail] = useState(false)
  const [saving, setSaving] = useState(false)
  const [form, setForm] = useState({
    name: round.name || '',
    roundOrder: '1',
    winnersPerRound: String(round.winnersPerRound ?? 1),
    startDate: toDatetimeLocalValue(round.startDate),
    endDate: toDatetimeLocalValue(round.endDate),
    submissionDeadline: toDatetimeLocalValue(round.submissionDeadline)
  })

  const loadDetail = useCallback(async () => {
    setLoadingDetail(true)
    try {
      const d = await getEventRoundDetail({
        eventId,
        roundId: round.roundId
      })
      setForm({
        name: d.name || '',
        roundOrder: d.roundOrder || '1',
        winnersPerRound: String(d.winnersPerRound ?? 1),
        startDate: toDatetimeLocalValue(d.startDate),
        endDate: toDatetimeLocalValue(d.endDate),
        submissionDeadline: toDatetimeLocalValue(d.submissionDeadline)
      })
    } catch (err) {
      showToast(localizeError(err.message), 'error')
      setEditOpen(false)
    } finally {
      setLoadingDetail(false)
    }
  }, [eventId, round.roundId, showToast])

  useEffect(() => {
    if (editOpen) loadDetail()
  }, [editOpen, loadDetail])

  const handleChange = (e) => setForm((f) => ({ ...f, [e.target.name]: e.target.value }))

  const handleSave = async (e) => {
    e.preventDefault()
    setSaving(true)
    try {
      const updated = await updateEventRound({
        eventId,
        roundId: round.roundId,
        name: form.name,
        roundOrder: form.roundOrder,
        winnersPerRound: form.winnersPerRound,
        startDate: form.startDate,
        endDate: form.endDate,
        submissionDeadline: form.submissionDeadline
      })
      onUpdated?.(updated)
      showToast('Đã cập nhật vòng thi', 'success')
      setEditOpen(false)
    } catch (err) {
      showToast(localizeError(err.message), 'error')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className={`event-stat-item-card${editOpen ? ' is-edit-open' : ''}`}>
      <div className='kv event-stat-item'>
        <span style={{ minWidth: 0, flex: 1, textAlign: 'left' }}>
          <div style={{ fontWeight: 600 }}>{round.name || '—'}</div>
          <div style={{ fontSize: 11, color: 'var(--text-dim)' }}>
            Vòng {round.roundOrder || '—'} · {formatEventDateTime(round.startDate)} →{' '}
            {formatEventDateTime(round.endDate)}
          </div>
          <div style={{ fontSize: 11, color: 'var(--text-mute)' }}>
            Deadline nộp bài: {formatEventDateTime(round.submissionDeadline)}
          </div>
          <div style={{ fontSize: 11, color: 'var(--text-mute)' }}>
            Winner/vòng: {round.winnerCount ?? 0}/{round.winnersPerRound ?? 1}
          </div>
        </span>
        <div className='event-stat-item-actions'>
          <button
            type='button'
            className='btn btn-outline btn-sm'
            onClick={() => setEditOpen((v) => !v)}
            aria-expanded={editOpen}
          >
            {editOpen ? 'Thu gọn' : 'Sửa'}
          </button>
          <StatItemDeleteButton
            itemLabel={round.name || 'vòng thi'}
            onDelete={async () => {
              await deleteEventRound({
                eventId,
                roundId: round.roundId
              })
              onDeleted?.(round.roundId)
            }}
          />
        </div>
      </div>
      {editOpen ? (
        <form className='event-stat-item-edit' onSubmit={handleSave}>
          {loadingDetail ? (
            <LoadingState className='event-stat-dropdown-empty' />
          ) : (
            <>
              <FormField label='Tên vòng *'>
                <input
                  name='name'
                  value={form.name}
                  onChange={handleChange}
                  maxLength={100}
                  disabled={saving}
                  required
                />
              </FormField>
              <FormField label='Thứ tự vòng *'>
                <input
                  type='number'
                  name='roundOrder'
                  min={1}
                  step={1}
                  value={form.roundOrder}
                  onChange={handleChange}
                  disabled={saving}
                  required
                />
              </FormField>
              <FormField label='Winner mỗi vòng *'>
                <input
                  type='number'
                  name='winnersPerRound'
                  min={1}
                  step={1}
                  value={form.winnersPerRound}
                  onChange={handleChange}
                  disabled={saving}
                  required
                />
              </FormField>
              <FormField label='Bắt đầu *'>
                <input
                  type='datetime-local'
                  name='startDate'
                  value={form.startDate}
                  onChange={handleChange}
                  disabled={saving}
                  required
                />
              </FormField>
              <FormField label='Kết thúc *'>
                <input
                  type='datetime-local'
                  name='endDate'
                  value={form.endDate}
                  onChange={handleChange}
                  disabled={saving}
                  required
                />
              </FormField>
              <FormField label='Deadline nộp bài *'>
                <input
                  type='datetime-local'
                  name='submissionDeadline'
                  value={form.submissionDeadline}
                  onChange={handleChange}
                  disabled={saving}
                  required
                />
              </FormField>
              <div className='event-stat-item-edit-foot'>
                <LoadingButton loading={saving} type='submit'>
                  Lưu thay đổi
                </LoadingButton>
                <button
                  type='button'
                  className='btn btn-ghost btn-sm'
                  onClick={() => setEditOpen(false)}
                  disabled={saving}
                >
                  Hủy
                </button>
              </div>
            </>
          )}
        </form>
      ) : null}
    </div>
  )
}

function GroupsDropdownContent({ eventId, groups, onGroupDeleted, onGroupUpdated }) {
  const [page, setPage] = useState(1)
  return (
    <>
      {!groups.length ? (
        <div className='event-stat-dropdown-empty'>Chưa có bảng thi nào.</div>
      ) : (
        <>
          <div className='kv-list'>
            {groups.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE).map((group) => (
              <GroupStatItem
                key={group.groupId}
                eventId={eventId}
                group={group}
                onUpdated={onGroupUpdated}
                onDeleted={onGroupDeleted}
              />
            ))}
          </div>
          <Pagination total={groups.length} pageSize={PAGE_SIZE} currentPage={page} onChange={setPage} />
        </>
      )}
      <SetupPanelLink eventId={eventId} focus='group'>
        + Thêm bảng thi
      </SetupPanelLink>
    </>
  )
}

function RoundsDropdownContent({ eventId, rounds, onRoundDeleted, onRoundUpdated }) {
  const [page, setPage] = useState(1)
  return (
    <>
      {!rounds.length ? (
        <div className='event-stat-dropdown-empty'>Chưa có vòng thi nào.</div>
      ) : (
        <>
          <div className='kv-list'>
            {rounds.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE).map((round) => (
              <RoundStatItem
                key={round.roundId}
                eventId={eventId}
                round={round}
                onUpdated={onRoundUpdated}
                onDeleted={onRoundDeleted}
              />
            ))}
          </div>
          <Pagination total={rounds.length} pageSize={PAGE_SIZE} currentPage={page} onChange={setPage} />
        </>
      )}
      <SetupPanelLink eventId={eventId} focus='round'>
        + Thêm vòng thi
      </SetupPanelLink>
    </>
  )
}

function MentorStatItem({ eventId, assignment, rounds, groups, onUpdated, onDeleted }) {
  const { showToast } = useToast()
  const [editOpen, setEditOpen] = useState(false)
  const [saving, setSaving] = useState(false)
  const [loadingAccounts, setLoadingAccounts] = useState(false)
  const [mentorAccounts, setMentorAccounts] = useState([])
  const [form, setForm] = useState({
    roundId: assignment.roundId || '',
    groupId: assignment.groupId || '',
    mentorId: assignment.mentorId || ''
  })

  const filteredGroups = groups.filter((g) => !form.roundId || g.roundId === form.roundId)

  const handleStartEdit = async () => {
    setForm({
      roundId: assignment.roundId || '',
      groupId: assignment.groupId || '',
      mentorId: assignment.mentorId || ''
    })
    setEditOpen(true)
    setLoadingAccounts(true)
    try {
      setMentorAccounts(await getAllAccounts('EXPERT'))
    } catch (err) {
      showToast(localizeError(err.message), 'error')
      setEditOpen(false)
    } finally {
      setLoadingAccounts(false)
    }
  }

  const handleCancel = () => {
    setForm({
      roundId: assignment.roundId || '',
      groupId: assignment.groupId || '',
      mentorId: assignment.mentorId || ''
    })
    setEditOpen(false)
  }

  const handleSave = async (e) => {
    e.preventDefault()
    setSaving(true)
    try {
      const updated = await updateMentorAssignment({
        eventId,
        roundId: assignment.roundId,
        groupId: assignment.groupId,
        mentorId: assignment.mentorId,
        newRoundId: form.roundId,
        newGroupId: form.groupId,
        newMentorId: form.mentorId
      })
      onUpdated?.(assignment, updated)
      showToast('Đã cập nhật phân công mentor', 'success')
      setEditOpen(false)
    } catch (err) {
      showToast(localizeError(err.message), 'error')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className={`event-stat-item-card${editOpen ? ' is-edit-open' : ''}`}>
      <div className='kv event-stat-item'>
        <span style={{ minWidth: 0, flex: 1, textAlign: 'left' }}>
          <div style={{ fontWeight: 600 }}>{assignment.mentorName || '—'}</div>
          <div style={{ fontSize: 11, color: 'var(--text-dim)' }}>
            {assignment.roundName || '—'} · {assignment.groupName || '—'}
          </div>
          <div style={{ fontSize: 11, color: 'var(--text-mute)' }}>{assignment.mentorEmail || '—'}</div>
        </span>
        <div className='event-stat-item-actions'>
          <button
            type='button'
            className='btn btn-outline btn-sm'
            onClick={() => (editOpen ? handleCancel() : handleStartEdit())}
          >
            {editOpen ? 'Thu gọn' : 'Sửa'}
          </button>
          <StatItemDeleteButton
            itemLabel={assignment.mentorName || 'mentor'}
            onDelete={async () => {
              await deleteMentorAssignment({
                eventId,
                roundId: assignment.roundId,
                groupId: assignment.groupId,
                mentorId: assignment.mentorId
              })
              onDeleted?.(assignment)
            }}
          />
        </div>
      </div>
      {editOpen ? (
        <form className='event-stat-item-edit' onSubmit={handleSave}>
          {loadingAccounts ? (
            <LoadingState className='event-stat-dropdown-empty' />
          ) : (
            <>
              <FormField label='Vòng *'>
                <select
                  name='roundId'
                  value={form.roundId}
                  onChange={(e) =>
                    setForm((f) => ({
                      ...f,
                      roundId: e.target.value,
                      groupId: ''
                    }))
                  }
                  disabled={saving || !rounds.length}
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
              <FormField label='Bảng *'>
                <select
                  name='groupId'
                  value={form.groupId}
                  onChange={(e) =>
                    setForm((f) => ({
                      ...f,
                      groupId: e.target.value
                    }))
                  }
                  disabled={saving || !filteredGroups.length}
                  required
                >
                  <option value=''>— Chọn bảng —</option>
                  {filteredGroups.map((g) => (
                    <option key={g.groupId} value={g.groupId}>
                      {g.name}
                    </option>
                  ))}
                </select>
              </FormField>
              <FormField label='Mentor *'>
                <select
                  name='mentorId'
                  value={form.mentorId}
                  onChange={(e) =>
                    setForm((f) => ({
                      ...f,
                      mentorId: e.target.value
                    }))
                  }
                  disabled={saving || !mentorAccounts.length}
                  required
                >
                  <option value=''>— Chọn mentor —</option>
                  {mentorAccounts.map((m) => (
                    <option key={m.userId} value={m.userId}>
                      {m.fullName || m.email}
                    </option>
                  ))}
                </select>
              </FormField>
              <div className='event-stat-item-edit-foot'>
                <LoadingButton loading={saving} type='submit'>
                  Lưu
                </LoadingButton>
                <button type='button' className='btn btn-ghost btn-sm' onClick={handleCancel} disabled={saving}>
                  Hủy
                </button>
              </div>
            </>
          )}
        </form>
      ) : null}
    </div>
  )
}

function JudgeStatItem({ eventId, assignment, rounds, groups, onUpdated, onDeleted }) {
  const { showToast } = useToast()
  const [editOpen, setEditOpen] = useState(false)
  const [saving, setSaving] = useState(false)
  const [loadingAccounts, setLoadingAccounts] = useState(false)
  const [judgeAccounts, setJudgeAccounts] = useState([])
  const [form, setForm] = useState({
    judgeId: assignment.judgeId || '',
    roundId: assignment.roundId || '',
    groupId: assignment.groupId || ''
  })

  const filteredGroups = groups.filter((g) => !form.roundId || g.roundId === form.roundId)

  const handleStartEdit = async () => {
    setForm({
      judgeId: assignment.judgeId || '',
      roundId: assignment.roundId || '',
      groupId: assignment.groupId || ''
    })
    setEditOpen(true)
    setLoadingAccounts(true)
    try {
      setJudgeAccounts(await getAllAccounts('EXPERT'))
    } catch (err) {
      showToast(localizeError(err.message), 'error')
      setEditOpen(false)
    } finally {
      setLoadingAccounts(false)
    }
  }

  const handleCancel = () => {
    setForm({
      judgeId: assignment.judgeId || '',
      roundId: assignment.roundId || '',
      groupId: assignment.groupId || ''
    })
    setEditOpen(false)
  }

  const handleSave = async (e) => {
    e.preventDefault()
    setSaving(true)
    try {
      const updated = await updateJudgeAssignment({
        eventId,
        judgeId: assignment.judgeId,
        roundId: assignment.roundId,
        groupId: assignment.groupId,
        newJudgeId: form.judgeId,
        newRoundId: form.roundId,
        newGroupId: form.groupId
      })
      onUpdated?.(assignment, updated)
      showToast('Đã cập nhật phân công judge', 'success')
      setEditOpen(false)
    } catch (err) {
      showToast(localizeError(err.message), 'error')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className={`event-stat-item-card${editOpen ? ' is-edit-open' : ''}`}>
      <div className='kv event-stat-item'>
        <span style={{ minWidth: 0, flex: 1, textAlign: 'left' }}>
          <div style={{ fontWeight: 600 }}>{assignment.judgeName || '—'}</div>
          <div style={{ fontSize: 11, color: 'var(--text-dim)' }}>
            {assignment.roundName || '—'} · {assignment.groupName || '—'}
          </div>
          <div style={{ fontSize: 11, color: 'var(--text-mute)' }}>{assignment.judgeEmail || '—'}</div>
        </span>
        <div className='event-stat-item-actions'>
          <button
            type='button'
            className='btn btn-outline btn-sm'
            onClick={() => (editOpen ? handleCancel() : handleStartEdit())}
          >
            {editOpen ? 'Thu gọn' : 'Sửa'}
          </button>
          <StatItemDeleteButton
            itemLabel={assignment.judgeName || 'judge'}
            onDelete={async () => {
              await deleteJudgeAssignment({
                eventId,
                judgeId: assignment.judgeId,
                roundId: assignment.roundId,
                groupId: assignment.groupId
              })
              onDeleted?.(assignment)
            }}
          />
        </div>
      </div>
      {editOpen ? (
        <form className='event-stat-item-edit' onSubmit={handleSave}>
          {loadingAccounts ? (
            <LoadingState className='event-stat-dropdown-empty' />
          ) : (
            <>
              <FormField label='Vòng *'>
                <select
                  name='roundId'
                  value={form.roundId}
                  onChange={(e) =>
                    setForm((f) => ({
                      ...f,
                      roundId: e.target.value,
                      groupId: ''
                    }))
                  }
                  disabled={saving || !rounds.length}
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
              <FormField label='Bảng *'>
                <select
                  name='groupId'
                  value={form.groupId}
                  onChange={(e) =>
                    setForm((f) => ({
                      ...f,
                      groupId: e.target.value
                    }))
                  }
                  disabled={saving || !filteredGroups.length}
                  required
                >
                  <option value=''>— Chọn bảng —</option>
                  {filteredGroups.map((g) => (
                    <option key={g.groupId} value={g.groupId}>
                      {g.name}
                    </option>
                  ))}
                </select>
              </FormField>
              <FormField label='Judge *'>
                <select
                  name='judgeId'
                  value={form.judgeId}
                  onChange={(e) => setForm((f) => ({ ...f, judgeId: e.target.value }))}
                  disabled={saving || !judgeAccounts.length}
                  required
                >
                  <option value=''>— Chọn judge —</option>
                  {judgeAccounts.map((j) => (
                    <option key={j.userId} value={j.userId}>
                      {j.fullName || j.email}
                    </option>
                  ))}
                </select>
              </FormField>
              <div className='event-stat-item-edit-foot'>
                <LoadingButton loading={saving} type='submit'>
                  Lưu
                </LoadingButton>
                <button type='button' className='btn btn-ghost btn-sm' onClick={handleCancel} disabled={saving}>
                  Hủy
                </button>
              </div>
            </>
          )}
        </form>
      ) : null}
    </div>
  )
}

function MentorsDropdownContent({ eventId, assignedMentors = [], rounds = [], groups = [], onUpdated, onDeleted }) {
  const [page, setPage] = useState(1)
  return (
    <>
      {!assignedMentors.length ? (
        <div className='event-stat-dropdown-empty'>Chưa phân công mentor nào.</div>
      ) : (
        <>
          <div className='kv-list'>
            {assignedMentors.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE).map((m) => (
              <MentorStatItem
                key={`${m.roundId}-${m.groupId}-${m.mentorId}`}
                eventId={eventId}
                assignment={m}
                rounds={rounds}
                groups={groups}
                onUpdated={onUpdated}
                onDeleted={onDeleted}
              />
            ))}
          </div>
          <Pagination total={assignedMentors.length} pageSize={PAGE_SIZE} currentPage={page} onChange={setPage} />
        </>
      )}
      <AssignPanelLink eventId={eventId} focus='mentor'>
        + Thêm mentor
      </AssignPanelLink>
    </>
  )
}

function JudgesDropdownContent({ eventId, assignedJudges = [], rounds = [], groups = [], onUpdated, onDeleted }) {
  const [page, setPage] = useState(1)
  return (
    <>
      {!assignedJudges.length ? (
        <div className='event-stat-dropdown-empty'>Chưa phân công judge nào.</div>
      ) : (
        <>
          <div className='kv-list'>
            {assignedJudges.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE).map((j) => (
              <JudgeStatItem
                key={`${j.roundId}-${j.groupId}-${j.judgeId}`}
                eventId={eventId}
                assignment={j}
                rounds={rounds}
                groups={groups}
                onUpdated={onUpdated}
                onDeleted={onDeleted}
              />
            ))}
          </div>
          <Pagination total={assignedJudges.length} pageSize={PAGE_SIZE} currentPage={page} onChange={setPage} />
        </>
      )}
      <AssignPanelLink eventId={eventId} focus='judge'>
        + Thêm judge
      </AssignPanelLink>
    </>
  )
}

function buildEventInfoForm(event) {
  return {
    title: event.title || '',
    description: event.description || '',
    startDate: toDatetimeLocalValue(event.startDate),
    endDate: toDatetimeLocalValue(event.endDate),
    status: (event.status || 'BUILDING').toUpperCase(),
    maxTeams: event.maxTeams == null ? '' : String(event.maxTeams),
    numRounds: event.numRounds == null ? '1' : String(event.numRounds),
    githubTemplateRepo: event.githubTemplateRepo || ''
  }
}

function EventDetailInfoPanel({ event, onUpdated }) {
  const { showToast } = useToast()
  const [detailOpen, setDetailOpen] = useState(false)
  const [editOpen, setEditOpen] = useState(false)
  const [saving, setSaving] = useState(false)
  const [form, setForm] = useState(() => buildEventInfoForm(event))

  const handleChange = (e) => setForm((f) => ({ ...f, [e.target.name]: e.target.value }))

  const handleStartEdit = () => {
    setForm(buildEventInfoForm(event))
    setEditOpen(true)
  }

  const handleCancel = () => {
    setForm(buildEventInfoForm(event))
    setEditOpen(false)
  }

  const handleCloseDetail = () => {
    setForm(buildEventInfoForm(event))
    setEditOpen(false)
    setDetailOpen(false)
  }

  const handleConfirm = async (e) => {
    e.preventDefault()
    setSaving(true)
    try {
      const updated = await updateEvent({
        eventId: event.eventId,
        title: form.title,
        description: form.description,
        startDate: form.startDate || null,
        endDate: form.endDate || null,
        status: form.status,
        maxTeams: form.maxTeams,
        numRounds: form.numRounds,
        githubTemplateRepo: form.githubTemplateRepo || null
      })
      onUpdated?.(updated)
      showToast('Đã cập nhật sự kiện', 'success')
      setEditOpen(false)
    } catch (err) {
      showToast(localizeError(err.message), 'error')
    } finally {
      setSaving(false)
    }
  }

  const statusLocked = isEventStatusLocked(event.status)
  const editLocked = String(event.status ?? '').toUpperCase() === 'COMPLETED'

  const editFormId = `event-info-edit-${event.eventId}`

  const detailBody = !editOpen ? (
    <div className='kv-list'>
      <div className='kv'>
        <span>Tên sự kiện</span>
        <span>{event.title || '—'}</span>
      </div>
      <div className='kv'>
        <span>Mô tả</span>
        <span style={{ textAlign: 'right', maxWidth: '70%' }}>{event.description || '—'}</span>
      </div>
      <div className='kv'>
        <span>Ngày bắt đầu</span>
        <span>{formatEventDateTime(event.startDate)}</span>
      </div>
      <div className='kv'>
        <span>Ngày kết thúc</span>
        <span>{formatEventDateTime(event.endDate)}</span>
      </div>
      <div className='kv'>
        <span>Trạng thái</span>
        <span>{eventStatusLabel(event.status)}</span>
      </div>
      <div className='kv'>
        <span>Số vòng dự kiến</span>
        <span>{event.numRounds ?? '—'}</span>
      </div>
      <div className='kv'>
        <span>Giới hạn đội</span>
        <span>{formatMaxTeams(event.maxTeams)}</span>
      </div>
      <div className='kv'>
        <span>Đội đã đăng ký</span>
        <span>{event.totalTeams ?? '0'}</span>
      </div>

      <div className='kv'>
        <span>Ngày tạo</span>
        <span>{formatEventDateTime(event.createdAt)}</span>
      </div>
    </div>
  ) : (
    <form id={editFormId} className='event-stat-item-edit event-detail-info-edit' onSubmit={handleConfirm}>
      <div className='kv-list event-detail-info-readonly'>
        <div className='kv'>
          <span>Ngày tạo</span>
          <span>{formatEventDateTime(event.createdAt)}</span>
        </div>
      </div>
      <FormField label='Tên sự kiện *'>
        <input name='title' value={form.title} onChange={handleChange} maxLength={200} disabled={saving} required />
      </FormField>
      <FormField label='Mô tả'>
        <textarea name='description' value={form.description} onChange={handleChange} rows={3} disabled={saving} />
      </FormField>
      <FormField label='Ngày bắt đầu'>
        <input
          type='datetime-local'
          name='startDate'
          value={form.startDate}
          onChange={handleChange}
          disabled={saving}
        />
      </FormField>
      <FormField label='Ngày kết thúc'>
        <input type='datetime-local' name='endDate' value={form.endDate} onChange={handleChange} disabled={saving} />
      </FormField>
      <FormField label='Số vòng dự kiến *'>
        <input
          type='number'
          name='numRounds'
          min={1}
          value={form.numRounds}
          onChange={handleChange}
          disabled={saving}
          required
        />
      </FormField>
      <FormField label='Giới hạn đội'>
        <input
          type='number'
          name='maxTeams'
          min={1}
          value={form.maxTeams}
          onChange={handleChange}
          disabled={saving}
          placeholder='Để trống = không giới hạn'
        />
      </FormField>
      <FormField label='Trạng thái *'>
        {statusLocked ? (
          <>
            <input type='text' value={eventStatusLabel(event.status)} disabled readOnly />
            {eventStatusLockHint(event.status) ? (
              <p className='event-stat-edit-hint' style={{ marginTop: 6 }}>
                {eventStatusLockHint(event.status)}
              </p>
            ) : null}
          </>
        ) : (
          <select name='status' value={form.status} onChange={handleChange} disabled={saving} required>
            {BUILDING_STATUS_OPTIONS.map((s) => (
              <option key={s} value={s}>
                {eventStatusLabel(s)}
              </option>
            ))}
          </select>
        )}
      </FormField>

    </form>
  )

  return (
    <>
      <div className='event-detail-info-wrap' style={{ marginTop: 16 }}>
        <button
          type='button'
          className='criteria-round-row event-detail-info-compact'
          onClick={() => setDetailOpen(true)}
          aria-haspopup='dialog'
        >
          <span className='criteria-round-row-main'>
            <span className='criteria-round-row-name'>Thông tin sự kiện</span>
            <span className='criteria-round-row-meta'>
              {formatEventDateTime(event.startDate)} → {formatEventDateTime(event.endDate)} ·{' '}
              {eventStatusLabel(event.status)} · {event.totalTeams ?? 0} đội
            </span>
          </span>
          <span className='criteria-round-row-action'>Chi tiết ›</span>
        </button>
      </div>

      <Modal
        isOpen={detailOpen}
        onClose={handleCloseDetail}
        title='Thông tin sự kiện'
        subtitle={event.title || undefined}
        className='modal-wide'
      >
        <div className={`event-detail-info${editOpen ? ' is-edit-open' : ''}`}>{detailBody}</div>
        <div className='event-detail-info-actions-outside' style={{ marginTop: 16 }}>
          {!editOpen ? (
            editLocked ? (
              <p className='event-stat-edit-hint' style={{ margin: 0 }}>
                Sự kiện đã kết thúc — không thể chỉnh sửa.
              </p>
            ) : (
              <button type='button' className='btn btn-outline' onClick={handleStartEdit}>
                Sửa
              </button>
            )
          ) : (
            <>
              <LoadingButton loading={saving} type='submit' form={editFormId} className='btn btn-primary'>
                Xác nhận
              </LoadingButton>
              <button type='button' className='btn btn-outline' onClick={handleCancel} disabled={saving}>
                Hủy
              </button>
            </>
          )}
        </div>
      </Modal>
    </>
  )
}

export default function EventDetailsPage() {
  const { eventId } = useParams()
  const { showToast } = useToast()
  const [event, setEvent] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [bulkAccessModal, setBulkAccessModal] = useState({ isOpen: false, grant: false, loading: false })
  const [now, setNow] = useState(() => Date.now())
  const repoAccessSchedule = useMemo(() => getRepoAccessSchedule(event?.rounds, now), [event?.rounds, now])
  const repoAccessAvailable = repoAccessSchedule.repoAccessAvailable

  useEffect(() => {
    if (repoAccessSchedule.nextChangeTime == null) return undefined

    const delay = Math.max(0, repoAccessSchedule.nextChangeTime - Date.now())
    const timeoutId = window.setTimeout(() => setNow(Date.now()), delay)
    return () => window.clearTimeout(timeoutId)
  }, [repoAccessSchedule.nextChangeTime])

  const handleBulkAccessConfirm = async () => {
    setBulkAccessModal((prev) => ({ ...prev, loading: true }))
    try {
      const res = await updateEventRepoAccess({ eventId, grant: bulkAccessModal.grant })
      showToast(res.message || 'Cập nhật quyền truy cập thành công!', 'success')
      setBulkAccessModal({ isOpen: false, grant: false, loading: false })
      await loadEvent()
    } catch (err) {
      showToast(localizeError(err.message), 'error')
      setBulkAccessModal((prev) => ({ ...prev, loading: false }))
    }
  }

  const loadEvent = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await getEventDetail(eventId)
      setEvent(data)

      const firstRound = [...(data.rounds ?? [])].sort(
        (a, b) => Number(a.roundOrder ?? 0) - Number(b.roundOrder ?? 0)
      )[0]
      if (firstRound?.roundId) {
        try {
          const fillResult = await autoFillRoundGroups({
            eventId,
            roundId: firstRound.roundId
          })
          if (fillResult.assignedCount > 0) {
            setEvent(await getEventDetail(eventId))
            showToast(fillResult.message, 'success')
          }
        } catch {
          // Bỏ qua nếu auto-fill không chạy được (vd. chưa có bảng)
        }
      }
    } catch (err) {
      setError(localizeError(err.message))
      showToast('Không tải được chi tiết sự kiện', 'error')
    } finally {
      setLoading(false)
    }
  }, [eventId, showToast])

  useEffect(() => {
    loadEvent()
  }, [loadEvent])

  const handleTeamUpdated = (registrationId, updates) => {
    setEvent((prev) => {
      if (!prev) return prev
      return {
        ...prev,
        teams: patchTeamRegistration(prev.teams, registrationId, updates)
      }
    })
  }

  const handleEventUpdated = (updated) => {
    setEvent((prev) => {
      if (!prev) return prev
      return {
        ...prev,
        title: updated.title,
        description: updated.description ?? '',
        startDate: updated.startDate,
        endDate: updated.endDate,
        status: updated.status,
        maxTeams: updated.maxTeams ?? null,
        numRounds: updated.numRounds ?? prev.numRounds,
        createdAt: updated.createdAt || prev.createdAt
      }
    })
  }

  const handleGroupUpdated = (updated) => {
    setEvent((prev) => {
      if (!prev) return prev
      return {
        ...prev,
        groups: prev.groups.map((g) =>
          g.groupId === updated.groupId
            ? {
                ...g,
                name: updated.name ?? g.name,
                maxTeams: updated.maxTeams !== undefined ? (updated.maxTeams ?? null) : g.maxTeams,
                teamCount: updated.teamCount !== undefined ? updated.teamCount : g.teamCount,
                roundName: updated.roundName ?? g.roundName
              }
            : g
        ),
        assignedMentors: (prev.assignedMentors ?? []).map((m) =>
          m.groupId === updated.groupId ? { ...m, groupName: updated.name } : m
        ),
        assignedJudges: (prev.assignedJudges ?? []).map((j) =>
          j.groupId === updated.groupId ? { ...j, groupName: updated.name } : j
        )
      }
    })
  }

  const handleRoundUpdated = (updated) => {
    setEvent((prev) => {
      if (!prev) return prev
      const nextRounds = prev.rounds
        .map((r) =>
          r.roundId === updated.roundId
            ? {
                ...r,
                name: updated.name,
                startDate: updated.startDate,
                endDate: updated.endDate,
                submissionDeadline: updated.submissionDeadline,
                roundOrder: updated.roundOrder,
                winnersPerRound: updated.winnersPerRound
              }
            : r
        )
        .sort((a, b) => Number(a.roundOrder ?? 0) - Number(b.roundOrder ?? 0))
      return {
        ...prev,
        rounds: nextRounds,
        assignedJudges: (prev.assignedJudges ?? []).map((j) =>
          j.roundId === updated.roundId ? { ...j, roundName: updated.name } : j
        )
      }
    })
  }

  const handleMentorDeleted = (assignment) => {
    setEvent((prev) => {
      if (!prev) return prev
      const next = (prev.assignedMentors ?? []).filter(
        (m) =>
          !(m.roundId === assignment.roundId && m.groupId === assignment.groupId && m.mentorId === assignment.mentorId)
      )
      return { ...prev, assignedMentors: next }
    })
  }

  const handleMentorUpdated = (oldAssignment, updated) => {
    setEvent((prev) => {
      if (!prev) return prev
      return {
        ...prev,
        assignedMentors: (prev.assignedMentors ?? []).map((m) =>
          m.roundId === oldAssignment.roundId &&
          m.groupId === oldAssignment.groupId &&
          m.mentorId === oldAssignment.mentorId
            ? { ...m, ...updated }
            : m
        )
      }
    })
  }

  const handleJudgeDeleted = (assignment) => {
    setEvent((prev) => {
      if (!prev) return prev
      const next = (prev.assignedJudges ?? []).filter(
        (j) =>
          !(j.roundId === assignment.roundId && j.groupId === assignment.groupId && j.judgeId === assignment.judgeId)
      )
      return { ...prev, assignedJudges: next }
    })
  }

  const handleJudgeUpdated = (oldAssignment, updated) => {
    setEvent((prev) => {
      if (!prev) return prev
      return {
        ...prev,
        assignedJudges: (prev.assignedJudges ?? []).map((j) =>
          j.roundId === oldAssignment.roundId &&
          j.groupId === oldAssignment.groupId &&
          j.judgeId === oldAssignment.judgeId
            ? { ...j, ...updated }
            : j
        )
      }
    })
  }

  const handleGroupDeleted = (groupId) => {
    setEvent((prev) => {
      if (!prev) return prev
      const nextGroups = prev.groups.filter((g) => g.groupId !== groupId)
      return {
        ...prev,
        groups: nextGroups,
        totalGroups: String(nextGroups.length),
        assignedMentors: (prev.assignedMentors ?? []).filter((m) => m.groupId !== groupId),
        assignedJudges: (prev.assignedJudges ?? []).filter((j) => j.groupId !== groupId)
      }
    })
  }

  const handleRoundCreated = (created) => {
    setEvent((prev) => {
      if (!prev) return prev
      const nextRounds = [
        ...prev.rounds,
        {
          roundId: created.roundId,
          name: created.name,
          roundOrder: created.roundOrder,
          startDate: created.startDate,
          endDate: created.endDate,
          submissionDeadline: created.submissionDeadline,
          winnersPerRound: created.winnersPerRound ?? 1,
          winnerCount: 0
        }
      ].sort((a, b) => Number(a.roundOrder ?? 0) - Number(b.roundOrder ?? 0))
      return {
        ...prev,
        rounds: nextRounds,
        totalRounds: String(nextRounds.length)
      }
    })
  }

  const handleGroupCreated = (created) => {
    setEvent((prev) => {
      if (!prev) return prev
      const nextGroups = [
        ...prev.groups,
        {
          groupId: created.groupId,
          roundId: created.roundId,
          roundName: created.roundName ?? '',
          name: created.name,
          maxTeams: created.maxTeams ?? null,
          teamCount: 0
        }
      ]
      return {
        ...prev,
        groups: nextGroups,
        totalGroups: String(nextGroups.length)
      }
    })
  }

  const handleRoundDeleted = (roundId) => {
    setEvent((prev) => {
      if (!prev) return prev
      const nextRounds = prev.rounds.filter((r) => r.roundId !== roundId)
      return {
        ...prev,
        rounds: nextRounds,
        totalRounds: String(nextRounds.length),
        assignedJudges: (prev.assignedJudges ?? []).filter((j) => j.roundId !== roundId)
      }
    })
  }

  const handleAwardCreated = (created) => {
    setEvent((prev) => {
      if (!prev) return prev
      const nextAwards = [...(prev.awards ?? []), created].sort(
        (a, b) =>
          (a.rank ?? 999999) - (b.rank ?? 999999) || String(a.title ?? '').localeCompare(String(b.title ?? ''), 'vi')
      )
      return {
        ...prev,
        awards: nextAwards,
        totalAwards: String(nextAwards.length)
      }
    })
  }

  const handleAwardUpdated = (updated) => {
    setEvent((prev) => {
      if (!prev) return prev
      const nextAwards = (prev.awards ?? [])
        .map((a) => (a.awardId === updated.awardId ? { ...a, title: updated.title, rank: updated.rank ?? null } : a))
        .sort(
          (a, b) =>
            (a.rank ?? 999999) - (b.rank ?? 999999) || String(a.title ?? '').localeCompare(String(b.title ?? ''), 'vi')
        )
      return { ...prev, awards: nextAwards }
    })
  }

  const handleAwardDeleted = (awardId) => {
    setEvent((prev) => {
      if (!prev) return prev
      const nextAwards = (prev.awards ?? []).filter((a) => a.awardId !== awardId)
      return {
        ...prev,
        awards: nextAwards,
        totalAwards: String(nextAwards.length)
      }
    })
  }

  return (
    <DashboardShell
      roleLabel='Nhân viên'
      title='Chi tiết sự kiện'
      subtitle='Thông tin đầy đủ của hackathon.'
      role='Staff'
    >
      <div className='action-row' style={{ marginBottom: 16 }}>
        <Link to='/staff?tab=events' className='btn btn-ghost'>
          ← Quay lại danh sách
        </Link>
      </div>

      {loading && <LoadingState text='Đang tải chi tiết sự kiện…' />}

      {error && !loading && <FormMessage message={error} type='error' />}

      {!loading && event && (
        <div className='card event-detail-card' style={{ '--event-accent': eventAccentColor(event.status) }}>
          <div className='event-detail-card-head'>
            <div className='event-detail-card-title-row'>
              <div className='card-title'>{event.title || '—'}</div>
              <div className='event-detail-card-status-wrap'>
                <span
                  className={`status-pill ${eventStatusPillClass(event.status)} event-detail-card-status`}
                  style={{ cursor: 'default' }}
                >
                  {eventStatusLabel(event.status)}
                </span>
              </div>
              <div className='card-sub event-detail-card-sub'>
                {event.numRounds ?? 1} vòng dự kiến · {event.totalTeams ?? 0} đội đăng ký
              </div>
            </div>
          </div>

          {event.description ? (
            <p className='card-sub' style={{ marginTop: 12 }}>
              {event.description}
            </p>
          ) : null}

          <EventDetailInfoPanel event={event} onUpdated={handleEventUpdated} />

          <EventBoardSection
            event={event}
            onRoundCreated={handleRoundCreated}
            onGroupCreated={handleGroupCreated}
            onRoundUpdated={handleRoundUpdated}
            onRoundDeleted={handleRoundDeleted}
            onGroupUpdated={handleGroupUpdated}
            onGroupDeleted={handleGroupDeleted}
            onAutoFilled={loadEvent}
          />

          <div style={{ marginTop: 24 }}>
            <TeamRegistrationsSection
              teams={event.teams || []}
              onTeamUpdated={handleTeamUpdated}
              onBulkAccess={(grant) => setBulkAccessModal({ isOpen: true, grant, loading: false })}
              repoAccessAvailable={repoAccessAvailable}
              firstRoundStartTime={repoAccessSchedule.nextChangeTime}
            />
          </div>

          <div style={{ marginTop: 24 }}>
            <CriteriaManager rounds={event.rounds ?? []} />
          </div>

          <div style={{ marginTop: 24 }}>
            <AwardsManager
              eventId={event.eventId}
              awards={event.awards ?? []}
              onAwardCreated={handleAwardCreated}
              onAwardUpdated={handleAwardUpdated}
              onAwardDeleted={handleAwardDeleted}
            />
          </div>

          <ConfirmModal
            isOpen={bulkAccessModal.isOpen}
            onClose={() => setBulkAccessModal((prev) => ({ ...prev, isOpen: false }))}
            onConfirm={handleBulkAccessConfirm}
            title={bulkAccessModal.grant ? 'Mở quyền làm bài' : 'Khóa quyền làm bài'}
            message={
              bulkAccessModal.grant
                ? 'Hành động này sẽ thêm tất cả các thành viên của các đội đã duyệt của sự kiện làm Collaborator trực tiếp trên GitHub. Các thí sinh sẽ có quyền clone/push code làm bài. Bạn có chắc chắn muốn tiếp tục?'
                : 'Hành động này sẽ xóa tất cả các thành viên khỏi Collaborator của repository trên GitHub. Thí sinh sẽ mất quyền truy cập code ngay lập tức. Bạn có chắc chắn muốn tiếp tục?'
            }
            confirmLabel={bulkAccessModal.grant ? 'Mở quyền' : 'Khóa quyền'}
            loading={bulkAccessModal.loading}
            danger={!bulkAccessModal.grant}
          />
        </div>
      )}
    </DashboardShell>
  )
}

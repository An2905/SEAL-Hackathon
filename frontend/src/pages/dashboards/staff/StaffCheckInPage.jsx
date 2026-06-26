import { useCallback, useEffect, useMemo, useRef, useState } from 'react'

import { Link, useParams } from 'react-router-dom'

import DashboardShell from '../DashboardShell'

import FormMessage from '../../../components/common/FormMessage'

import Pagination from '../../../components/common/Pagination'
import FullWidthSearchBar from '../../../components/common/FullWidthSearchBar'
import LoadingState from '../../../components/common/LoadingState'

import { getCheckInPage, setMemberCheckIn, setTeamCheckIn } from '../../../api/checkIn'
import { retryGitHubProvisioning } from '../../../api/staff'

import { useToast } from '../../../context/ToastContext'

import { localizeError } from '../../../utils/errors'

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

  return 'status-default'
}

function teamMatchesSearch(team, query) {
  if (!query) return true

  const haystack = [team.teamName, team.registrationStatus]

    .filter(Boolean)

    .join(' ')

    .toLowerCase()

  return haystack.includes(query)
}

function isTeamFullyChecked(team) {
  const members = team.members || []

  return members.length > 0 && members.every((m) => m.checkedIn)
}

function isTeamPartiallyChecked(team) {
  const members = team.members || []

  const checkedCount = members.filter((m) => m.checkedIn).length

  return checkedCount > 0 && checkedCount < members.length
}

function TeamAccordionItem({ team, eventId, onTeamUpdated, busyKey, setBusyKey }) {
  const { showToast } = useToast()
  const [open, setOpen] = useState(false)
  const [membersPage, setMembersPage] = useState(1)

  const teamCheckboxRef = useRef(null)

  const members = team.members || []

  const handleGithubRetry = async (e) => {
    e.stopPropagation()
    setBusyKey(`github:${team.teamId}`)
    try {
      await retryGitHubProvisioning(team.registrationId)
      showToast('Đã kích hoạt lại tiến trình cấp phát GitHub thành công.', 'success')
      onTeamUpdated({
        ...team,
        githubStatus: 'PENDING'
      })
    } catch (err) {
      showToast(localizeError(err.message), 'error')
    } finally {
      setBusyKey(null)
    }
  }

  const allChecked = isTeamFullyChecked(team)

  const someChecked = isTeamPartiallyChecked(team)

  const teamBusy = busyKey === `team:${team.teamId}`

  useEffect(() => {
    if (teamCheckboxRef.current) {
      teamCheckboxRef.current.indeterminate = someChecked
    }
  }, [someChecked, team])

  const handleTeamCheck = async (e) => {
    e.stopPropagation()

    const checked = e.target.checked

    setBusyKey(`team:${team.teamId}`)

    try {
      const updated = await setTeamCheckIn({ eventId, teamId: team.teamId, checked })

      onTeamUpdated(updated)
    } catch (err) {
      e.target.checked = !checked
      showToast(localizeError(err.message), 'error')
    } finally {
      setBusyKey(null)
    }
  }

  const handleMemberCheck = async (member, checked) => {
    setBusyKey(`member:${team.teamId}:${member.userId}`)

    try {
      const updated = await setMemberCheckIn({
        eventId,

        teamId: team.teamId,

        userId: member.userId,

        checked
      })

      onTeamUpdated(updated)
    } catch (err) {
      showToast(localizeError(err.message), 'error')
      throw err
    } finally {
      setBusyKey(null)
    }
  }

  return (
    <div className={`checkin-team-item${open ? ' is-open' : ''}`}>
      <div className='checkin-team-header'>
        <label
          className='checkin-checkbox-label'
          onClick={(e) => e.stopPropagation()}
          title={allChecked ? 'Bỏ check-in cả đội' : 'Check-in cả đội'}
        >
          <input
            ref={teamCheckboxRef}
            type='checkbox'
            className='checkin-checkbox'
            checked={allChecked}
            disabled={teamBusy || members.length === 0 || Boolean(busyKey)}
            onChange={handleTeamCheck}
          />
        </label>

        <button
          type='button'
          className='checkin-team-header-btn'
          onClick={() => setOpen((v) => !v)}
          aria-expanded={open}
        >
          <span className='checkin-team-header-main'>
            <span className='checkin-team-chevron' aria-hidden='true'>
              {open ? '▾' : '▸'}
            </span>

            <span style={{ minWidth: 0, flex: 1, textAlign: 'left' }}>
              <div style={{ fontWeight: 600, color: 'var(--text)' }}>{team.teamName || '—'}</div>

              <div style={{ fontSize: 12, color: 'var(--text-dim)', marginTop: 2 }}>
                {team.memberCount} thành viên · Đăng ký {formatDateTime(team.registeredAt)}
              </div>
            </span>
          </span>
        </button>

        <span
          className={`status-pill ${registrationStatusPillClass(team.registrationStatus)}`}
          style={{ cursor: 'default', flexShrink: 0 }}
        >
          {team.registrationStatus || '—'}
        </span>

        {team.registrationStatus === 'APPROVED' && (
          <div
            className='github-provision-info'
            onClick={(e) => e.stopPropagation()}
            style={{ display: 'flex', alignItems: 'center', gap: '8px', marginLeft: '12px', flexShrink: 0 }}
          >
            {team.githubStatus === 'SUCCESS' ? (
              <a
                href={team.githubRepoUrl || '#'}
                target='_blank'
                rel='noopener noreferrer'
                className='status-pill status-active'
                style={{ textDecoration: 'none', display: 'flex', alignItems: 'center', gap: '4px' }}
                title='Mở GitHub Repository'
              >
                <svg className='icon' viewBox='0 0 16 16' width='14' height='14' fill='currentColor'>
                  <path d='M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0016 8c0-4.42-3.58-8-8-8z' />
                </svg>
                GitHub Repo
              </a>
            ) : team.githubStatus === 'FAILED' ? (
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                <span className='status-pill status-inactive' style={{ cursor: 'default' }}>
                  GitHub thất bại
                </span>
                <button
                  type='button'
                  className='btn btn-xs btn-outline'
                  style={{
                    padding: '2px 8px',
                    fontSize: '11px',
                    height: 'auto',
                    minHeight: 'auto',
                    border: '1px solid var(--border)',
                    borderRadius: '4px',
                    background: 'transparent',
                    color: 'var(--text-dim)',
                    cursor: 'pointer'
                  }}
                  disabled={Boolean(busyKey)}
                  onClick={handleGithubRetry}
                  title='Nhấn để thử lại quy trình tạo GitHub'
                >
                  {busyKey === `github:${team.teamId}` ? 'Đang gửi...' : 'Thử lại'}
                </button>
              </div>
            ) : (
              <span
                className='status-pill status-pending'
                style={{ cursor: 'default', display: 'flex', alignItems: 'center', gap: '6px' }}
              >
                <span
                  className='spinner-tiny'
                  style={{
                    width: '10px',
                    height: '10px',
                    border: '2px solid currentColor',
                    borderTopColor: 'transparent',
                    borderRadius: '50%',
                    display: 'inline-block',
                    animation: 'spin 1s linear infinite'
                  }}
                />
                Cung cấp tài nguyên GitHub...
              </span>
            )}
          </div>
        )}
      </div>

      {open && (
        <div className='checkin-team-members'>
          {members.length === 0 ? (
            <div className='empty-state' style={{ padding: '12px 0', fontSize: 13 }}>
              Đội chưa có thành viên.
            </div>
          ) : (
            <>
              <div className='kv-list'>
                {members.slice((membersPage - 1) * PAGE_SIZE, membersPage * PAGE_SIZE).map((m) => {
                  const memberBusy = busyKey === `member:${team.teamId}:${m.userId}`
                  return (
                    <div className='kv checkin-member-row' key={m.userId} style={{ alignItems: 'flex-start' }}>
                      <label className='checkin-checkbox-label'>
                        <input
                          type='checkbox'
                          className='checkin-checkbox'
                          checked={Boolean(m.checkedIn)}
                          disabled={memberBusy || Boolean(busyKey)}
                          onChange={async (e) => {
                            const checked = e.target.checked
                            try {
                              await handleMemberCheck(m, checked)
                            } catch (err) {
                              e.target.checked = !checked
                            }
                          }}
                        />
                      </label>
                      <span style={{ minWidth: 0, flex: 1, textAlign: 'left' }}>
                        <div style={{ fontWeight: 600, color: 'var(--text)' }}>
                          {m.fullName || '—'}
                          {m.leader && (
                            <span className='leader-tag' style={{ marginLeft: 8 }}>
                              Leader
                            </span>
                          )}
                        </div>
                        <div style={{ fontSize: 12, color: 'var(--text-dim)', marginTop: 2 }}>{m.email || '—'}</div>
                      </span>
                      <span
                        className={`status-pill ${m.checkedIn ? 'status-active' : 'status-pending'}`}
                        style={{ cursor: 'default', flexShrink: 0 }}
                      >
                        {m.checkedIn ? 'Đã check-in' : 'Chưa check-in'}
                      </span>
                    </div>
                  )
                })}
              </div>
              <Pagination
                total={members.length}
                pageSize={PAGE_SIZE}
                currentPage={membersPage}
                onChange={setMembersPage}
              />
            </>
          )}
        </div>
      )}
    </div>
  )
}

export default function StaffCheckInPage() {
  const { eventId } = useParams()

  const { showToast } = useToast()

  const [loading, setLoading] = useState(true)

  const [error, setError] = useState(null)

  const [page, setPage] = useState(null)

  const [searchInput, setSearchInput] = useState('')
  const [searchQuery, setSearchQuery] = useState('')

  const [busyKey, setBusyKey] = useState(null)

  const loadPage = useCallback(async () => {
    setLoading(true)

    setError(null)

    try {
      const data = await getCheckInPage(eventId)

      setPage(data)
    } catch (err) {
      const msg = localizeError(err.message)

      setError(msg)

      showToast('Không tải được trang check-in', 'error')
    } finally {
      setLoading(false)
    }
  }, [eventId, showToast])

  useEffect(() => {
    loadPage()
  }, [loadPage])

  const handleTeamUpdated = useCallback(
    (updatedTeam) => {
      let statusChanged = false
      setPage((prev) => {
        if (!prev) return prev
        const existing = prev.teams.find((t) => t.teamId === updatedTeam.teamId)
        if (existing && existing.registrationStatus !== updatedTeam.registrationStatus) {
          statusChanged = true
        }
        return {
          ...prev,
          teams: prev.teams.map((t) => (t.teamId === updatedTeam.teamId ? updatedTeam : t))
        }
      })

      if (statusChanged) {
        const status = (updatedTeam.registrationStatus || '').toUpperCase()
        showToast(
          status === 'APPROVED' ? 'Đã check-in đủ thành viên' : 'Chưa đủ thành viên',
          status === 'APPROVED' ? 'success' : 'info'
        )
      }
    },
    [showToast]
  )

  const teams = useMemo(() => page?.teams ?? [], [page?.teams])

  const filteredTeams = useMemo(
    () => teams.filter((team) => teamMatchesSearch(team, searchQuery)),

    [teams, searchQuery]
  )

  const [teamsPage, setTeamsPage] = useState(1)

  const visibleTeams = filteredTeams.slice((teamsPage - 1) * PAGE_SIZE, teamsPage * PAGE_SIZE)

  useEffect(() => {
    setTeamsPage(1)
  }, [searchQuery])

  // Polling for pending GitHub provisioning
  useEffect(() => {
    if (!page || !page.teams) return

    const hasPendingProvisioning = page.teams.some(
      (t) => t.registrationStatus === 'APPROVED' && t.githubStatus !== 'SUCCESS' && t.githubStatus !== 'FAILED'
    )

    if (!hasPendingProvisioning) return

    const interval = setInterval(async () => {
      try {
        const data = await getCheckInPage(eventId)
        setPage(data)
      } catch (err) {
        console.error('Error polling check-in page data:', err)
      }
    }, 3000)

    return () => clearInterval(interval)
  }, [eventId, page])

  return (
    <DashboardShell
      roleLabel='Nhân viên'
      title='Check-in sự kiện'
      subtitle={page?.eventTitle ? `Sự kiện: ${page.eventTitle}` : 'Điểm danh các đội tham gia'}
      role='Staff'
      showStaffFields
    >
      <div className='action-row' style={{ marginBottom: 16 }}>
        <Link className='btn btn-outline' to='/staff?tab=events'>
          ← Quay lại danh sách sự kiện
        </Link>

        <Link className='btn btn-outline' to={`/staff/events/${eventId}`}>
          Chi tiết sự kiện
        </Link>
      </div>

      <div className='card'>
        <div className='card-head'>
          <div className='card-title'>Đội đăng ký tham gia</div>
        </div>

        <p className='card-sub'>
          Tick cả đội để check-in tất cả thành viên. Khi <strong>đủ</strong> thành viên đã check-in, trạng thái đăng ký
          chuyển <strong>APPROVED</strong>; nếu <strong>chưa đủ</strong> sẽ là <strong>PENDING</strong>.
        </p>

        {error && <FormMessage message={error} type='error' />}

        {loading && <LoadingState text='Đang tải danh sách đội…' />}

        {!loading && !error && teams.length === 0 && (
          <div className='empty-state'>Chưa có đội nào đăng ký với trạng thái PENDING hoặc APPROVED.</div>
        )}

        {!loading && teams.length > 0 && (
          <>
            <FullWidthSearchBar
              value={searchInput}
              onChange={setSearchInput}
              onSearch={() => setSearchQuery(searchInput.trim().toLowerCase())}
              placeholder='Tìm tên đội…'
              disabled={loading}
            />

            <div className='card-sub' style={{ marginTop: 10, marginBottom: 8 }}>
              {searchQuery ? (
                <>
                  Hiển thị <strong>{filteredTeams.length}</strong> / {teams.length} đội
                </>
              ) : (
                <>
                  Tổng cộng <strong>{teams.length}</strong> đội
                </>
              )}
            </div>

            {filteredTeams.length === 0 ? (
              <div className='empty-state'>Không tìm thấy đội khớp với &quot;{searchInput.trim()}&quot;.</div>
            ) : (
              <>
                <div className='checkin-team-list'>
                  {visibleTeams.map((team) => (
                    <TeamAccordionItem
                      key={team.teamId || team.registrationId}
                      team={team}
                      eventId={eventId}
                      onTeamUpdated={handleTeamUpdated}
                      busyKey={busyKey}
                      setBusyKey={setBusyKey}
                    />
                  ))}
                </div>

                <Pagination
                  total={filteredTeams.length}
                  pageSize={PAGE_SIZE}
                  currentPage={teamsPage}
                  onChange={setTeamsPage}
                />
              </>
            )}
          </>
        )}
      </div>
    </DashboardShell>
  )
}

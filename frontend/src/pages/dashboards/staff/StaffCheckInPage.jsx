import { useCallback, useEffect, useMemo, useRef, useState } from 'react'

import { Link, useParams } from 'react-router-dom'

import DashboardShell from '../DashboardShell'

import FormMessage from '../../../components/common/FormMessage'

import CollapsibleKvList, { CollapsibleListToggle, useCollapsibleList } from '../../../components/common/CollapsibleList'
import FullWidthSearchBar from '../../../components/common/FullWidthSearchBar'

import { getCheckInPage, setMemberCheckIn, setTeamCheckIn } from '../../../api/checkIn'

import { useToast } from '../../../context/ToastContext'

import { localizeError } from '../../../utils/errors'



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

  const haystack = [

    team.teamName,

    team.registrationStatus

  ]

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

  const teamCheckboxRef = useRef(null)

  const members = team.members || []

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

      </div>



      {open && (

        <div className='checkin-team-members'>

          {members.length === 0 ? (

            <div className='empty-state' style={{ padding: '12px 0', fontSize: 13 }}>

              Đội chưa có thành viên.

            </div>

          ) : (
            <CollapsibleKvList
              items={members}
              getItemKey={(m) => m.userId}

              renderItem={(m) => {

                const memberBusy = busyKey === `member:${team.teamId}:${m.userId}`

                return (

                  <div className='kv checkin-member-row' style={{ alignItems: 'flex-start' }}>

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

              }}

            />
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



  const handleTeamUpdated = useCallback((updatedTeam) => {

    setPage((prev) => {

      if (!prev) return prev

      return {

        ...prev,

        teams: prev.teams.map((t) => (t.teamId === updatedTeam.teamId ? updatedTeam : t))

      }

    })

    const status = (updatedTeam.registrationStatus || '').toUpperCase()

    showToast(

      status === 'APPROVED' ? 'Đã check-in đủ — đăng ký APPROVED' : 'Chưa đủ thành viên — đăng ký PENDING',

      status === 'APPROVED' ? 'success' : 'info'

    )

  }, [showToast])



  const teams = page?.teams ?? []

  const filteredTeams = useMemo(

    () => teams.filter((team) => teamMatchesSearch(team, searchQuery)),

    [teams, searchQuery]

  )

  const {

    visibleItems: visibleTeams,

    hasMore: hasMoreTeams,

    expanded: teamsExpanded,

    hiddenCount: hiddenTeamCount,

    toggle: toggleTeams,

    setExpanded: setTeamsExpanded

  } = useCollapsibleList(filteredTeams)



  useEffect(() => {

    setTeamsExpanded(false)

  }, [searchQuery, setTeamsExpanded])



  return (

    <DashboardShell

      roleLabel='Staff'

      title='Check-in sự kiện'

      subtitle={page?.eventTitle ? `Sự kiện: ${page.eventTitle}` : 'Điểm danh các đội tham gia'}

      role='Staff'

      showStaffFields

    >

      <div className='action-row' style={{ marginBottom: 16 }}>

        <Link className='btn btn-outline' to='/staff/events'>

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

        {loading && <div className='empty-state'>Đang tải danh sách đội…</div>}

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

                <CollapsibleListToggle

                  hasMore={hasMoreTeams}

                  expanded={teamsExpanded}

                  hiddenCount={hiddenTeamCount}

                  onToggle={toggleTeams}

                />

              </>

            )}

          </>

        )}

      </div>

    </DashboardShell>

  )

}



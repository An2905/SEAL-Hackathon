import { apiFetch } from './client'
import { normalizeEventId } from './normalizers'

function mapMember(row) {
  return {
    userId: row.userId ?? row.user_id ?? '',
    fullName: row.fullName ?? row.full_name ?? '',
    email: row.email ?? '',
    leader: Boolean(row.leader),
    checkedIn: Boolean(row.checkedIn ?? row.checked_in)
  }
}

function mapTeam(row) {
  const membersRaw = row.members ?? []
  return {
    registrationId: row.registrationId ?? row.registration_id ?? '',
    teamId: row.teamId ?? row.team_id ?? '',
    teamName: row.teamName ?? row.team_name ?? '',
    registrationStatus: row.registrationStatus ?? row.registration_status ?? '',
    registeredAt: row.registeredAt ?? row.registered_at ?? null,
    githubStatus: row.githubStatus ?? row.github_status ?? null,
    githubRepoUrl: row.githubRepoUrl ?? row.github_repo_url ?? '',
    memberCount: row.memberCount ?? row.member_count ?? membersRaw.length,
    members: Array.isArray(membersRaw) ? membersRaw.map(mapMember) : []
  }
}

function parseTeamResponse(text) {
  const data = JSON.parse(text)
  return mapTeam(data)
}

// GET /api/staff/check-in?eventId=...
export async function getCheckInPage(eventId) {
  const id = normalizeEventId(eventId)
  if (!id) throw new Error('Sự kiện không hợp lệ')

  const params = new URLSearchParams({ eventId: id })
  const text = await apiFetch(`/api/staff/check-in?${params.toString()}`)
  const data = JSON.parse(text)

  return {
    eventId: data.eventId ?? data.event_id ?? id,
    eventTitle: data.eventTitle ?? data.event_title ?? '',
    checkInOpen: Boolean(data.checkInOpen ?? data.check_in_open),
    teams: Array.isArray(data.teams) ? data.teams.map(mapTeam) : []
  }
}

// PUT /api/staff/check-in/team
export async function setTeamCheckIn({ eventId, teamId, checked }) {
  const id = normalizeEventId(eventId)
  if (!id) throw new Error('Sự kiện không hợp lệ')
  if (!teamId) throw new Error('Đội không hợp lệ')

  const text = await apiFetch('/api/staff/check-in/team', {
    method: 'PUT',
    body: { eventId: id, teamId, checked: Boolean(checked) }
  })
  return parseTeamResponse(text)
}

// PUT /api/staff/check-in/member
export async function setMemberCheckIn({ eventId, teamId, userId, checked }) {
  const id = normalizeEventId(eventId)
  if (!id) throw new Error('Sự kiện không hợp lệ')
  if (!teamId) throw new Error('Đội không hợp lệ')
  if (!userId) throw new Error('Thành viên không hợp lệ')

  const text = await apiFetch('/api/staff/check-in/member', {
    method: 'PUT',
    body: { eventId: id, teamId, userId, checked: Boolean(checked) }
  })
  return parseTeamResponse(text)
}

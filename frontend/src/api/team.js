import { apiFetch } from './client'
import { normalizeAccountUserId, normalizeEventId, normalizeRegistrationId } from './normalizers'

function parseJson(text) {
  const trimmed = (text || '').trim()
  if (!trimmed) return {}
  if (trimmed.startsWith('{') || trimmed.startsWith('[')) {
    return JSON.parse(trimmed)
  }
  return { message: trimmed }
}

function errorMessage(err) {
  const raw = err?.message || ''
  try {
    const parsed = parseJson(raw)
    return parsed.message || raw
  } catch {
    return raw
  }
}

// GET /api/team/me
// Response: MyTeamResponse JSON object
export async function getMyTeam() {
  try {
    const data = parseJson(await apiFetch('/api/team/me', { method: 'GET' }))
    if (data.teamId) return { hasTeam: true, data }
    return { hasTeam: false }
  } catch (err) {
    if (/no team/i.test(errorMessage(err))) return { hasTeam: false }
    throw new Error(errorMessage(err))
  }
}

function normalizeTeamName(value) {
  return String(value ?? '')
    .trim()
    .replace(/\s+/g, ' ')
}

// PUT /api/team/create
// Response: { message, teamId, teamName, enrollCode }
export async function createTeam({ teamName }) {
  const name = normalizeTeamName(teamName)
  if (!name) throw new Error('Tên đội không được để trống')
  if (name.length > 100) throw new Error('Tên đội tối đa 100 ký tự')

  const data = parseJson(
    await apiFetch('/api/team/create', {
      method: 'PUT',
      body: { teamName: name }
    })
  )

  if (!data.enrollCode) {
    throw new Error(data.message || 'Tạo đội thất bại')
  }

  return { enrollCode: data.enrollCode, teamId: data.teamId, teamName: data.teamName }
}

// PUT /api/team/join
// Response: { message, teamId }
export async function joinTeam({ enrollCode }) {
  const data = parseJson(
    await apiFetch('/api/team/join', {
      method: 'PUT',
      body: { enrollCode }
    })
  )
  if (!/join team successfully/i.test(data.message || '')) {
    throw new Error(data.message || 'Tham gia đội thất bại')
  }
  return true
}

// PUT /api/team/join-event
// Response: { message }
export async function joinEvent({ eventId }) {
  const id = normalizeEventId(eventId)
  if (!id) throw new Error('Sự kiện không hợp lệ')

  const data = parseJson(
    await apiFetch('/api/team/join-event', {
      method: 'PUT',
      body: { eventId: id }
    })
  )
  if (!/join event successfully/i.test(data.message || '')) {
    throw new Error(data.message || 'Đăng ký sự kiện thất bại')
  }
  return true
}

// DELETE /api/team/delete-member
// Response: { message }
export async function deleteMember({ memberId }) {
  const id = (memberId || '').trim()
  if (!id) {
    throw new Error('Vui lòng nhập email thành viên')
  }

  const data = parseJson(
    await apiFetch('/api/team/delete-member', {
      method: 'DELETE',
      body: { memberId: id }
    })
  )
  if (!/delete team member successfully/i.test(data.message || '')) {
    throw new Error(data.message || 'Xóa thành viên thất bại')
  }
  return true
}

function mapTeamRegistrationRow(row) {
  const r = row && typeof row === 'object' ? row : {}
  return {
    registrationId: normalizeRegistrationId(r.registrationId ?? r.registration_id),
    eventId: normalizeEventId(r.eventId ?? r.event_id),
    eventTitle: r.eventTitle ?? r.event_title ?? '',
    eventDescription: r.eventDescription ?? r.event_description ?? '',
    eventStartDate: r.eventStartDate ?? r.event_start_date ?? '',
    eventEndDate: r.eventEndDate ?? r.event_end_date ?? '',
    eventStatus: r.eventStatus ?? r.event_status ?? '',
    groupId: String(r.groupId ?? r.group_id ?? ''),
    groupName: r.groupName ?? r.group_name ?? '',
    registrationStatus: r.registrationStatus ?? r.registration_status ?? '',
    registeredAt: r.registeredAt ?? r.registered_at ?? ''
  }
}

function mapMentorItem(row) {
  const r = row && typeof row === 'object' ? row : {}
  return {
    mentorId: normalizeAccountUserId(r.mentorId ?? r.mentor_id),
    mentorName: r.mentorName ?? r.mentor_name ?? '',
    mentorEmail: r.mentorEmail ?? r.mentor_email ?? ''
  }
}

function mapTeamTrackMentors(data) {
  const r = data && typeof data === 'object' ? data : {}
  const mentors = Array.isArray(r.mentors) ? r.mentors.map(mapMentorItem) : []
  return {
    eventId: normalizeEventId(r.eventId ?? r.event_id),
    eventTitle: r.eventTitle ?? r.event_title ?? '',
    groupId: String(r.groupId ?? r.group_id ?? ''),
    groupName: r.groupName ?? r.group_name ?? '',
    registrationId: normalizeRegistrationId(r.registrationId ?? r.registration_id),
    registrationStatus: r.registrationStatus ?? r.registration_status ?? '',
    mentors
  }
}

// GET /api/team/registrations
// Response: TeamEventRegistrationResponse[]
export async function getTeamRegistrations() {
  const text = await apiFetch('/api/team/registrations', { method: 'GET' })
  try {
    const data = parseJson(text)
    if (!Array.isArray(data)) return []
    return data.map(mapTeamRegistrationRow)
  } catch {
    throw new Error(text?.trim() || 'Không thể tải danh sách đăng ký sự kiện')
  }
}

// GET /api/team/mentors?eventId=...
// Response: TeamTrackMentorsResponse (mentors only when registration APPROVED)
export async function getTeamTrackMentors(eventId) {
  const id = normalizeEventId(eventId)
  if (!id) throw new Error('Sự kiện không hợp lệ')

  const params = new URLSearchParams({ eventId: id })
  const text = await apiFetch(`/api/team/mentors?${params}`, { method: 'GET' })
  try {
    return mapTeamTrackMentors(parseJson(text))
  } catch {
    throw new Error(text?.trim() || 'Không thể tải mentor của bảng')
  }
}

function mapRoundRow(row) {
  const r = row && typeof row === 'object' ? row : {}
  return {
    roundId: String(r.roundId ?? r.round_id ?? ''),
    name: r.name ?? '',
    startDate: r.startDate ?? r.start_date ?? '',
    endDate: r.endDate ?? r.end_date ?? '',
    submissionDeadline: r.submissionDeadline ?? r.submission_deadline ?? ''
  }
}

// GET /api/team/rounds?eventId=...
export async function getTeamRounds(eventId) {
  const id = normalizeEventId(eventId)
  if (!id) throw new Error('Sự kiện không hợp lệ')

  const params = new URLSearchParams({ eventId: id })
  const text = await apiFetch(`/api/team/rounds?${params}`, { method: 'GET' })
  try {
    const data = parseJson(text)
    if (!Array.isArray(data)) return []
    return data.map(mapRoundRow)
  } catch {
    throw new Error(text?.trim() || 'Không thể tải danh sách round')
  }
}

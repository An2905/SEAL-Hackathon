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

// PUT /api/team/create
// Response: { message, teamId, teamName, enrollCode }
export async function createTeam({ teamName }) {
  const data = parseJson(
    await apiFetch('/api/team/create', {
      method: 'PUT',
      body: { teamName }
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
export async function joinEvent({ eventId, categoryId }) {
  const data = parseJson(
    await apiFetch('/api/team/join-event', {
      method: 'PUT',
      body: { eventId, categoryId }
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
  const id = normalizeAccountUserId(memberId)
  if (!id || !/^\d+$/.test(id)) {
    throw new Error('Member ID không hợp lệ')
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
    categoryId: normalizeEventId(r.categoryId ?? r.category_id),
    categoryName: r.categoryName ?? r.category_name ?? '',
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
    categoryId: normalizeEventId(r.categoryId ?? r.category_id),
    categoryName: r.categoryName ?? r.category_name ?? '',
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
  if (!id) throw new Error('Event ID không hợp lệ')

  const params = new URLSearchParams({ eventId: id })
  const text = await apiFetch(`/api/team/mentors?${params}`, { method: 'GET' })
  try {
    return mapTeamTrackMentors(parseJson(text))
  } catch {
    throw new Error(text?.trim() || 'Không thể tải mentor của track')
  }
}

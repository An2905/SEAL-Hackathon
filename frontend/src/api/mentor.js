import { apiFetch } from './client'
import { normalizeEventId } from './event'
import { mapMentorAssignmentRow, mapMentorAssignedTeamRow } from './normalizers'

function mapEventRow(row) {
  const r = row && typeof row === 'object' ? row : {}
  return {
    eventId: normalizeEventId(r.eventId ?? r.event_id),
    title: r.title ?? '',
    description: r.description ?? '',
    startDate: r.startDate ?? r.start_date ?? '',
    endDate: r.endDate ?? r.end_date ?? '',
    status: r.status ?? '',
    createdAt: r.createdAt ?? r.created_at ?? ''
  }
}

// GET /api/mentor/events
// Requires a Bearer token of an EXPERT. Returns events assigned via mentor_assignments.
// Returns: [{ eventId, title, description, startDate, endDate, status, createdAt? }, ...]
export async function getAssignedEvents() {
  const text = await apiFetch('/api/mentor/events', { method: 'GET' })
  try {
    const data = JSON.parse(text)
    if (!Array.isArray(data)) return []
    return data.map(mapEventRow)
  } catch {
    throw new Error(text || 'Không thể tải sự kiện được phân công')
  }
}

// GET /api/mentor/events/current-rounds
// Response: [{ eventId, eventTitle, roundId, roundName, startDate, endDate, roundStatus }]
export async function getAssignedCurrentRounds() {
  const text = await apiFetch('/api/mentor/events/current-rounds', { method: 'GET' })
  try {
    const data = JSON.parse(text)
    return Array.isArray(data) ? data : []
  } catch {
    throw new Error(text || 'Không thể tải vòng hiện tại')
  }
}

// GET /api/mentor/assignments
// Response: [{ eventId, eventTitle, roundId, roundName, groupId, groupName }, ...]
export async function getMentorAssignments() {
  const text = await apiFetch('/api/mentor/assignments', { method: 'GET' })
  try {
    const data = JSON.parse(text)
    if (!Array.isArray(data)) return []
    return data.map(mapMentorAssignmentRow)
  } catch {
    throw new Error(text || 'Không thể tải phân công bảng')
  }
}

// GET /api/mentor/teams?eventId&roundId&groupId&registrationStatus
export async function getAssignedTeams({ eventId, roundId, groupId, registrationStatus = 'APPROVED' }) {
  const eid = normalizeEventId(eventId)
  const rid = String(roundId ?? '').trim()
  const gid = String(groupId ?? '').trim()
  if (!eid || !rid || !gid) {
    throw new Error('Thiếu thông tin sự kiện, vòng hoặc bảng')
  }

  const params = new URLSearchParams({
    eventId: eid,
    roundId: rid,
    groupId: gid,
    registrationStatus: registrationStatus || 'APPROVED'
  })

  const text = await apiFetch(`/api/mentor/teams?${params.toString()}`, { method: 'GET' })
  try {
    const data = JSON.parse(text)
    if (!Array.isArray(data)) return []
    return data.map(mapMentorAssignedTeamRow)
  } catch {
    throw new Error(text || 'Không thể tải danh sách đội')
  }
}

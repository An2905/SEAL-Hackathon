import { apiFetch } from './client'

import { normalizeEventId } from './event'

import {
  mapEventRow,
  mapMentorAssignmentRow,
  mapMentorAssignedTeamRow,
  mapGroupColleaguesResponse
} from './normalizers'

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

// GET /api/mentor/colleagues?eventId&roundId&groupId
export async function getGroupColleagues({ eventId, roundId, groupId }) {
  const eid = normalizeEventId(eventId)
  const rid = String(roundId ?? '').trim()
  const gid = String(groupId ?? '').trim()
  if (!eid || !rid || !gid) {
    throw new Error('Thiếu thông tin sự kiện, vòng hoặc bảng')
  }

  const params = new URLSearchParams({ eventId: eid, roundId: rid, groupId: gid })
  const text = await apiFetch(`/api/mentor/colleagues?${params.toString()}`, { method: 'GET' })
  try {
    return mapGroupColleaguesResponse(JSON.parse(text))
  } catch {
    throw new Error(text || 'Không thể tải danh sách đồng nghiệp')
  }
}

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

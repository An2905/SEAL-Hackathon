import { apiFetch } from './client'
import {
  normalizeEventId,
  countPendingTeams,
  mapEventRow,
  mapEventDetailRow,
  mergeTeamGitHubFields
} from './normalizers'
import { getCheckInPage } from './checkIn'

// Re-export normalizers so existing callers can keep importing them from "./event".
export { normalizeEventId, countPendingTeams }

// GET /api/staff/events/detail?eventId=...
// Requires a Bearer token with COORDINATOR role.
// Returns: { ...event fields, totalTeams, totalGroups, totalRounds, totalAwards,
//            teams[], groups[], rounds[], awards[] }
// Pass includeGitHub: true to enrich teams with GitHub provisioning fields (extra API call).
export async function getEventDetail(eventId, { includeGitHub = false } = {}) {
  const id = normalizeEventId(eventId)
  if (!id) throw new Error('Sự kiện không hợp lệ')

  const params = new URLSearchParams({ eventId: id })
  const text = await apiFetch(`/api/staff/events/detail?${params.toString()}`, {
    method: 'GET'
  })
  try {
    const data = JSON.parse(text)
    const mapped = mapEventDetailRow(data)
    if (!mapped.eventId) throw new Error('Không tìm thấy sự kiện')

    if (includeGitHub) {
      try {
        const checkIn = await getCheckInPage(id)
        mapped.teams = mergeTeamGitHubFields(mapped.teams, checkIn.teams)
      } catch {
        // GitHub enrichment is optional — event detail still usable without it.
      }
    }

    return mapped
  } catch (err) {
    if (err.message === 'Không tìm thấy sự kiện') throw err
    throw new Error(text || 'Không thể tải chi tiết sự kiện')
  }
}

// pendingTeams is included in GET /api/staff/events — no N× detail fetch needed.
export async function attachPendingTeamsToEvents(events) {
  if (!Array.isArray(events) || events.length === 0) return []
  return events.map((ev) => ({
    ...ev,
    pendingTeams: ev.pendingTeams ?? '0',
    pendingTeamsError: false
  }))
}

// GET /api/staff/events?status=...
// status ∈ { ALL, UPCOMING, ONGOING, COMPLETED, CANCELLED } — defaults to ALL when omitted.
// Requires a Bearer token of a COORDINATOR.
// Returns: [{ eventId, title, description, startDate, endDate, status, createdAt? }, ...]
export async function getAllEvents(status = 'ALL') {
  const params = new URLSearchParams()
  const normalizedStatus = String(status ?? 'ALL')
    .trim()
    .toUpperCase()
  if (normalizedStatus && normalizedStatus !== 'ALL') {
    params.set('status', normalizedStatus)
  }

  const query = params.toString() ? `?${params.toString()}` : ''
  const text = await apiFetch(`/api/staff/events${query}`, { method: 'GET' })
  try {
    const data = JSON.parse(text)
    if (!Array.isArray(data)) return []
    return data.map(mapEventRow)
  } catch {
    throw new Error(text || 'Không thể tải danh sách sự kiện')
  }
}

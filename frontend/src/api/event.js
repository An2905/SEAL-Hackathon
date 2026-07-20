import { apiFetch } from './client'
import { normalizeEventId, countPendingTeams, mapEventRow, mapEventDetailRow } from './normalizers'

// Re-export normalizers so existing callers can keep importing them from "./event".
export { normalizeEventId, countPendingTeams }

// GET /api/staff/events/detail?eventId=...
// Requires a Bearer token (any authenticated role per BE).
// Returns: { ...event fields, totalTeams, totalGroups, totalRounds, totalAwards,
//            teams[], groups[], rounds[], awards[] }
export async function getEventDetail(eventId) {
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

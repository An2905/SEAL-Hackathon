import { apiFetch } from './client'
import { normalizeEventId, countPendingTeams, mapEventRow, mapEventDetailRow } from './normalizers'

// Re-export normalizers so existing callers can keep importing them from "./event".
export { normalizeEventId, countPendingTeams }

// GET /api/staff/events/detail?eventId=...
// Requires a Bearer token (any authenticated role per BE).
// Returns: { ...event fields, totalTeams, totalCategories, totalRounds, totalAwards,
//            teams[], categories[], rounds[], awards[] }
export async function getEventDetail(eventId) {
  const id = normalizeEventId(eventId)
  if (!id) throw new Error('Event ID không hợp lệ')

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

// Enrich a list of events with their pending-team counts (fetched in parallel).
// FIX: Thêm field `pendingTeamsError: true` khi fetch thất bại, để UI phân biệt
// được "thực sự 0 pending" với "fetch bị lỗi mạng / auth / server".
export async function attachPendingTeamsToEvents(events) {
  if (!Array.isArray(events) || events.length === 0) return []

  return Promise.all(
    events.map(async (ev) => {
      try {
        const detail = await getEventDetail(ev.eventId)
        return { ...ev, pendingTeams: countPendingTeams(detail.teams), pendingTeamsError: false }
      } catch {
        return { ...ev, pendingTeams: '0', pendingTeamsError: true }
      }
    })
  )
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

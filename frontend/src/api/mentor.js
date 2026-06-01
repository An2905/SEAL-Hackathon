import { apiFetch } from './client'
import { normalizeEventId } from './event'

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
// Requires a Bearer token of a MENTOR. Returns events assigned via category_mentors.
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

import { apiFetch } from './client'
import { mapEventRow } from './normalizers'

const PUBLIC_STATUS_ORDER = ['ONGOING', 'UPCOMING', 'COMPLETED']

// GET /api/events — public, no auth
export async function getPublicEvents() {
  const text = await apiFetch('/api/events', { method: 'GET', auth: false })
  try {
    const data = JSON.parse(text)
    if (!Array.isArray(data)) return []
    const visible = new Set(PUBLIC_STATUS_ORDER)
    return data
      .map(mapEventRow)
      .filter((ev) => visible.has(String(ev.status ?? '').toUpperCase()))
      .sort((a, b) => {
        const ai = PUBLIC_STATUS_ORDER.indexOf(String(a.status).toUpperCase())
        const bi = PUBLIC_STATUS_ORDER.indexOf(String(b.status).toUpperCase())
        if (ai !== bi) return ai - bi
        return String(b.startDate).localeCompare(String(a.startDate))
      })
  } catch {
    throw new Error(text || 'Không thể tải danh sách sự kiện')
  }
}

export async function getUpcomingEvents() {
  const events = await getPublicEvents()
  return events.filter((ev) => String(ev.status ?? '').toUpperCase() === 'UPCOMING')
}

export { PUBLIC_STATUS_ORDER }

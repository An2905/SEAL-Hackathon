import { apiFetch } from './client'

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

// POST /api/chat/rooms/open — mở hoặc tạo phòng chat (event + mentor)
export async function openChatRoom({ eventId, mentorId }) {
  const text = await apiFetch('/api/chat/rooms/open', {
    method: 'POST',
    body: { eventId, mentorId }
  })
  try {
    return parseJson(text)
  } catch {
    throw new Error(text?.trim() || 'Không thể mở phòng chat')
  }
}

// POST /api/chat/rooms
export async function createChatRoom({ eventId, roundId, mentorId }) {
  const text = await apiFetch('/api/chat/rooms', {
    method: 'POST',
    body: { eventId, roundId, mentorId }
  })
  try {
    return parseJson(text)
  } catch {
    throw new Error(text?.trim() || 'Không thể tạo phòng chat')
  }
}

// GET /api/chat/rooms
export async function listChatRooms({ eventId, roundId } = {}) {
  const params = new URLSearchParams()
  if (eventId) params.set('eventId', eventId)
  if (roundId) params.set('roundId', roundId)
  const qs = params.toString()
  const text = await apiFetch(`/api/chat/rooms${qs ? `?${qs}` : ''}`, { method: 'GET' })
  try {
    const data = parseJson(text)
    return Array.isArray(data) ? data : []
  } catch {
    throw new Error(text?.trim() || 'Không thể tải danh sách phòng chat')
  }
}

// GET /api/chat/rooms/:roomId
export async function getChatRoom(roomId) {
  const text = await apiFetch(`/api/chat/rooms/${roomId}`, { method: 'GET' })
  try {
    return parseJson(text)
  } catch {
    throw new Error(text?.trim() || 'Không thể tải phòng chat')
  }
}

// GET /api/chat/rooms/:roomId/messages
export async function getChatMessages(roomId) {
  const text = await apiFetch(`/api/chat/rooms/${roomId}/messages`, { method: 'GET' })
  try {
    const data = parseJson(text)
    return Array.isArray(data) ? data : []
  } catch {
    throw new Error(text?.trim() || 'Không thể tải tin nhắn')
  }
}

export function getWebSocketUrl() {
  const apiBase = (import.meta.env.VITE_API_BASE || '').replace(/\/$/, '')
  if (apiBase) return `${apiBase}/ws`
  return `${window.location.origin}/ws`
}

export { errorMessage as chatErrorMessage }

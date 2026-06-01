import { apiFetch } from './client'
import {
  normalizeEventId,
  normalizeAccountUserId,
  normalizeRegistrationId,
  normalizeId,
  mapAccountRow
} from './normalizers'

// Re-export normalizers so existing callers can keep importing them from "./staff".
export { normalizeAccountUserId, normalizeRegistrationId }

// POST /api/staff/register
// Body: { email, fullName, role } — role ∈ { JUDGE, MENTOR }.
// Requires a Bearer token of a COORDINATOR. BE returns a plain-text result line.
export async function createStaffAccount({ email, fullName, role }) {
  const text = await apiFetch('/api/staff/register', {
    method: 'POST',
    body: { email, fullName, role }
  })
  if (!/account created.*email sent successfully/i.test(text)) throw new Error(text)
  return true
}

// PUT /api/staff/events/status
// Body: { eventId, newStatus } — newStatus ∈ { UPCOMING, ONGOING, COMPLETED }.
// Requires a Bearer token of a COORDINATOR.
export async function changeEventStatus({ eventId, newStatus }) {
  const id = normalizeEventId(eventId)
  if (!id) {
    throw new Error('Event ID không hợp lệ — vui lòng tải lại danh sách sự kiện')
  }
  const nextStatus = String(newStatus ?? '')
    .trim()
    .toUpperCase()
  const text = await apiFetch('/api/staff/events/status', {
    method: 'PUT',
    body: { eventId: id, newStatus: nextStatus }
  })
  if (!/event status updated successfully/i.test(text)) throw new Error(text)
  return true
}

// GET /api/staff/accounts?role=...&input=...
// role ∈ { ALL, JUDGE_INTERNAL, MENTOR, STUDENT_FPT, STUDENT_EXTERNAL } — defaults to ALL.
// input searches by the backend-supported account fields.
// Requires a Bearer token of a COORDINATOR.
// Returns: [{ userId, email, fullName, role, status }, ...]
export async function getAllAccounts(role = 'ALL', input = '') {
  const params = new URLSearchParams()
  if (role && role !== 'ALL') params.set('role', role)
  const normalizedInput = input.trim()
  if (normalizedInput) params.set('input', normalizedInput)

  const query = params.toString() ? `?${params.toString()}` : ''
  const text = await apiFetch(`/api/staff/accounts${query}`, { method: 'GET' })
  try {
    const data = JSON.parse(text)
    if (!Array.isArray(data)) return []
    return data.map(mapAccountRow)
  } catch {
    throw new Error(text || 'Không thể tải danh sách tài khoản')
  }
}

// PUT /api/staff/change-status
// Body: { userId, status } — status ∈ { PENDING, APPROVED, REJECTED }.
// Requires a Bearer token of a COORDINATOR.
export async function changeAccountStatus({ userId, status }) {
  const id = normalizeAccountUserId(userId)
  if (!id || !/^\d+$/.test(id)) {
    throw new Error('User ID không hợp lệ — vui lòng tải lại danh sách tài khoản')
  }
  const nextStatus = String(status ?? '')
    .trim()
    .toUpperCase()
  const text = await apiFetch('/api/staff/change-status', {
    method: 'PUT',
    body: { userId: id, status: nextStatus }
  })
  if (!/account status updated successfully/i.test(text)) throw new Error(text)
  return true
}

// PUT /api/staff/team-registration/status
// Body: { registrationId, status } — status ∈ { PENDING, APPROVED, REJECTED }.
// Requires a Bearer token of a COORDINATOR.
export async function changeTeamRegistrationStatus({ registrationId, status }) {
  const id = normalizeRegistrationId(registrationId)
  if (!id || !/^\d+$/.test(id)) {
    throw new Error('Registration ID không hợp lệ — vui lòng tải lại danh sách đội')
  }
  const nextStatus = String(status ?? '')
    .trim()
    .toUpperCase()
  const text = await apiFetch('/api/staff/team-registration/status', {
    method: 'PUT',
    body: { registrationId: id, status: nextStatus }
  })
  if (!/registration status updated successfully/i.test(text)) throw new Error(text)
  return true
}

// POST /api/staff/assign/judge
// Body: { judgeId, roundId, categoryId }. Assigns a judge to a round + track.
// Requires a Bearer token of a COORDINATOR.
export async function assignJudge({ judgeId, roundId, categoryId }) {
  const jId = normalizeAccountUserId(judgeId)
  const rId = normalizeId(roundId)
  const cId = normalizeId(categoryId)

  if (!jId || !/^\d+$/.test(jId)) {
    throw new Error('Judge ID không hợp lệ')
  }
  if (!rId || !/^\d+$/.test(rId)) {
    throw new Error('Round ID không hợp lệ')
  }
  if (!cId || !/^\d+$/.test(cId)) {
    throw new Error('Category ID không hợp lệ')
  }

  const text = await apiFetch('/api/staff/assign/judge', {
    method: 'POST',
    body: { judgeId: jId, roundId: rId, categoryId: cId }
  })
  if (!/judge assigned successfully/i.test(text)) throw new Error(text)
  return true
}

// POST /api/staff/assign/mentor
// Body: { userId, categoryId } — userId is the mentor's account id. Mentors are
// assigned per track (category). Requires a Bearer token of a COORDINATOR.
// FIX: Đổi param từ { mentorId } sang { userId } để khớp với field BE gửi lên,
// tránh trường hợp caller truyền nhầm tên và gửi undefined.
export async function assignMentor({ userId, categoryId }) {
  const uId = normalizeAccountUserId(userId)
  const cId = normalizeId(categoryId)

  if (!uId || !/^\d+$/.test(uId)) {
    throw new Error('Mentor ID không hợp lệ')
  }
  if (!cId || !/^\d+$/.test(cId)) {
    throw new Error('Category ID không hợp lệ')
  }

  const text = await apiFetch('/api/staff/assign/mentor', {
    method: 'POST',
    body: { userId: uId, categoryId: cId }
  })
  if (!/mentor assigned successfully/i.test(text)) throw new Error(text)
  return true
}

// POST /api/staff/announcements/send-all
// Body: { title, content }
// Response: { totalRecipients: "42", status: "SENT" }
export async function sendAnnouncementToAll({ title, content }) {
  const t = title.trim()
  const c = content.trim()
  if (!t || !c) throw new Error('Tiêu đề và nội dung không được để trống')

  const text = await apiFetch('/api/staff/announcements/send-all', {
    method: 'POST',
    body: { title: t, content: c }
  })
  try {
    const data = JSON.parse(text)
    return data // { totalRecipients, status }
  } catch {
    throw new Error(text || 'Gửi thông báo thất bại')
  }
}

// POST /api/staff/announcements/send-participant
// Body: { eventId, title, content, roles: string[] }
// Response: { announcementId, totalRecipients, createdAt, status }
export async function sendAnnouncementToParticipants({ eventId, title, content, roles }) {
  const id = normalizeEventId(eventId)
  if (!id) throw new Error('Vui lòng chọn sự kiện')

  const t = title.trim()
  const c = content.trim()
  if (!t || !c) throw new Error('Tiêu đề và nội dung không được để trống')
  if (!Array.isArray(roles) || roles.length === 0) throw new Error('Vui lòng chọn ít nhất một vai trò nhận thông báo')

  const text = await apiFetch('/api/staff/announcements/send-participant', {
    method: 'POST',
    body: { eventId: id, title: t, content: c, roles }
  })
  try {
    const data = JSON.parse(text)
    return data // { announcementId, totalRecipients, createdAt, status }
  } catch {
    throw new Error(text || 'Gửi thông báo thất bại')
  }
}

// GET /api/staff/events/export
// Response: file Excel binary (.xlsx), header Content-Disposition: attachment; filename=events.xlsx
// KHÔNG dùng apiFetch vì response không phải JSON/text — dùng fetch + blob.
export async function exportEventsExcel() {
  const token = localStorage.getItem('hh_token')
  const headers = { 'Content-Type': 'application/json' }
  if (token && token !== 'null' && token !== 'undefined') {
    headers['Authorization'] = `Bearer ${token}`
  }

  let response
  try {
    response = await fetch('/api/staff/events/export', {
      method: 'GET',
      headers,
      credentials: 'include'
    })
  } catch {
    throw new Error('NETWORK')
  }

  if (!response.ok) {
    const text = await response.text()
    throw new Error(text || `HTTP_${response.status}`)
  }

  // Trả về Blob để caller tự tạo download link
  return await response.blob()
}

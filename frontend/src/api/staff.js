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

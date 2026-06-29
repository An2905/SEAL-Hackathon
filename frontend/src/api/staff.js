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
// Body: { email, fullName, role } — role ∈ { EXPERT_INTERNAL, EXPERT_EXTERNAL }.
// Requires a Bearer token of a COORDINATOR. BE returns a plain-text result line.
export async function createStaffAccount({ email, fullName, role }) {
  const text = await apiFetch('/api/staff/register', {
    method: 'POST',
    body: { email, fullName, role }
  })
  if (!/account created successfully/i.test(text)) throw new Error(text)
  return true
}

function parseStaffJson(text) {
  try {
    return JSON.parse(text)
  } catch {
    throw new Error(text || 'Phản hồi không hợp lệ từ server')
  }
}

// POST /api/staff/events — status mặc định BUILDING (server set)
export async function createEvent({ title, description, startDate, endDate, maxTeams, numRounds, githubTemplateRepo }) {
  const t = String(title ?? '').trim()
  if (!t) throw new Error('Tên sự kiện không được để trống')

  const body = { title: t }
  const desc = String(description ?? '').trim()
  if (desc) body.description = desc
  if (startDate) body.startDate = startDate
  if (endDate) body.endDate = endDate
  if (maxTeams != null && maxTeams !== '') body.maxTeams = Number(maxTeams)
  if (numRounds != null && numRounds !== '') body.numRounds = Number(numRounds)
  const repo = String(githubTemplateRepo ?? '').trim()
  if (repo) body.githubTemplateRepo = repo

  const data = parseStaffJson(
    await apiFetch('/api/staff/events', {
      method: 'POST',
      body
    })
  )
  return {
    eventId: String(data.eventId ?? data.event_id ?? ''),
    title: data.title ?? '',
    description: data.description ?? '',
    startDate: data.startDate ?? data.start_date ?? '',
    endDate: data.endDate ?? data.end_date ?? '',
    status: data.status ?? 'BUILDING',
    maxTeams: data.maxTeams ?? data.max_teams ?? null,
    numRounds: data.numRounds ?? data.num_rounds ?? 1,
    createdAt: data.createdAt ?? data.created_at ?? ''
  }
}

// PUT /api/staff/events/status
// Body: { eventId, newStatus } — newStatus ∈ { BUILDING, UPCOMING, ONGOING, COMPLETED }.
// Requires a Bearer token of a COORDINATOR.
export async function changeEventStatus({ eventId, newStatus }) {
  const id = normalizeEventId(eventId)
  if (!id) {
    throw new Error('Sự kiện không hợp lệ — vui lòng tải lại danh sách sự kiện')
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
// role ∈ { ALL, EXPERT, EXPERT_INTERNAL, EXPERT_EXTERNAL, STUDENT_FPT, STUDENT_EXTERNAL } — defaults to ALL.
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
  if (!id) {
    throw new Error('Không xác định được tài khoản — vui lòng tải lại danh sách')
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
  if (!id) {
    throw new Error('Không xác định được đăng ký — vui lòng tải lại danh sách đội')
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
// Body: { judgeId, roundId, groupId }
export async function assignJudge({ judgeId, roundId, groupId }) {
  const jId = normalizeAccountUserId(judgeId)
  const rId = normalizeId(roundId)
  const gId = normalizeId(groupId)

  if (!jId) throw new Error('Vui lòng chọn giám khảo')
  if (!rId) throw new Error('Vui lòng chọn vòng')
  if (!gId) throw new Error('Vui lòng chọn bảng')

  const text = await apiFetch('/api/staff/assign/judge', {
    method: 'POST',
    body: { judgeId: jId, roundId: rId, groupId: gId }
  })
  if (!/judge assigned successfully/i.test(text)) throw new Error(text)
  return true
}

// POST /api/staff/assign/mentor
// Body: { userId, roundId, groupId }
export async function assignMentor({ userId, roundId, groupId }) {
  const uId = normalizeAccountUserId(userId)
  const rId = normalizeId(roundId)
  const gId = normalizeId(groupId)

  if (!uId) throw new Error('Vui lòng chọn mentor')
  if (!rId) throw new Error('Vui lòng chọn vòng')
  if (!gId) throw new Error('Vui lòng chọn bảng')

  const text = await apiFetch('/api/staff/assign/mentor', {
    method: 'POST',
    body: { userId: uId, roundId: rId, groupId: gId }
  })
  if (!/mentor assigned successfully/i.test(text)) throw new Error(text)
  return true
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

// POST /api/github/registrations/{registrationId}/retry
// Requires a Bearer token of a COORDINATOR.
export async function retryGitHubProvisioning(registrationId) {
  const id = normalizeRegistrationId(registrationId)
  if (!id) {
    throw new Error('Không xác định được đăng ký — vui lòng tải lại danh sách đội')
  }
  const text = await apiFetch(`/api/github/registrations/${id}/retry`, {
    method: 'POST'
  })
  return parseStaffJson(text)
}

// GET /api/staff/emails/filter
// Lọc danh sách email theo nhiều tiêu chí. Trả về danh sách recipients + copyText.
export async function filterEmails({
  audiences,
  eventId,
  roundId,
  groupId,
  teamId,
  userRole,
  registrationStatus,
  emailContains,
  nameContains,
  teamNameContains,
  accountStatus,
  separator,
  includeCopyText
} = {}) {
  const params = new URLSearchParams()
  if (audiences) params.set('audiences', audiences)
  if (eventId) params.set('eventId', eventId)
  if (roundId) params.set('roundId', roundId)
  if (groupId) params.set('groupId', groupId)
  if (teamId) params.set('teamId', teamId)
  if (userRole) params.set('userRole', userRole)
  if (registrationStatus) params.set('registrationStatus', registrationStatus)
  if (emailContains) params.set('emailContains', emailContains)
  if (nameContains) params.set('nameContains', nameContains)
  if (teamNameContains) params.set('teamNameContains', teamNameContains)
  if (accountStatus) params.set('accountStatus', accountStatus)
  if (separator) params.set('separator', separator)
  if (includeCopyText != null) params.set('includeCopyText', String(includeCopyText))

  const query = params.toString() ? `?${params.toString()}` : ''
  const text = await apiFetch(`/api/staff/emails/filter${query}`)
  return parseStaffJson(text)
}

// PUT /api/staff/events/{eventId}/github-access?grant=true/false
export async function updateEventRepoAccess({ eventId, grant }) {
  const id = normalizeEventId(eventId)
  if (!id) {
    throw new Error('Không xác định được sự kiện — vui lòng tải lại trang')
  }
  const text = await apiFetch(`/api/staff/events/${id}/github-access?grant=${grant}`, {
    method: 'PUT'
  })
  return parseStaffJson(text)
}

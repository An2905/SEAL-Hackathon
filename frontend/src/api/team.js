import { apiFetch } from './client'
import { normalizeAccountUserId } from './normalizers'

// GET /api/team/me
// Requires a Bearer token. Returns { hasTeam: true, data } when the user belongs
// to a team, or { hasTeam: false } otherwise.
// Note: BE replies HTTP 400 with "No team found for this user." when the student
// has no team yet, so apiFetch throws — we treat that specific case as "no team"
// instead of surfacing it as an error.
export async function getMyTeam() {
  let text
  try {
    text = (await apiFetch('/api/team/me', { method: 'GET' })).trim()
  } catch (err) {
    if (/no team/i.test(err.message)) return { hasTeam: false }
    throw err
  }
  if (text.startsWith('{')) return { hasTeam: true, data: JSON.parse(text) }
  if (/^no team/i.test(text) || text === '') return { hasTeam: false }
  throw new Error(text)
}

// PUT /api/team/create
// Body: { teamName }. Requires a Bearer token of a student.
// Returns { enrollCode } parsed from the success message.
export async function createTeam({ teamName }) {
  const text = await apiFetch('/api/team/create', {
    method: 'PUT',
    body: { teamName }
  })

  // FIX: Kiểm tra thành công TRƯỚC, rồi mới extract enrollCode.
  // Logic cũ dùng AND (&&) nên nếu BE trả lỗi có chứa "enrollCode:" thì vẫn
  // được coi là thành công — đây là bug nghiêm trọng.
  if (!/^Added Team /i.test(text)) {
    throw new Error(text)
  }

  const enrollMatch = text.match(/enrollCode:\s*(\S+)/i)
  return { enrollCode: enrollMatch ? enrollMatch[1] : null }
}

// PUT /api/team/join
// Body: { enrollCode }. Requires a Bearer token of a student.
export async function joinTeam({ enrollCode }) {
  const text = await apiFetch('/api/team/join', {
    method: 'PUT',
    body: { enrollCode }
  })
  if (!/join team successfully/i.test(text)) throw new Error(text)
  return true
}

// PUT /api/team/join-event
// Body: { eventId, categoryId }. Requires a Bearer token of a team member.
export async function joinEvent({ eventId, categoryId }) {
  const text = await apiFetch('/api/team/join-event', {
    method: 'PUT',
    body: { eventId, categoryId }
  })
  if (!/join event successfully/i.test(text)) throw new Error(text)
  return true
}

// DELETE /api/team/delete-member
// Body: { memberId }. Requires a Bearer token of the team leader.
export async function deleteMember({ memberId }) {
  const id = normalizeAccountUserId(memberId)
  if (!id || !/^\d+$/.test(id)) {
    throw new Error('Member ID không hợp lệ')
  }

  const text = await apiFetch('/api/team/delete-member', {
    method: 'DELETE',
    body: { memberId: id }
  })
  if (!/delete team member successfully/i.test(text)) throw new Error(text)
  return true
}

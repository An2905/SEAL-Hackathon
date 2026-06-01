import { apiFetch } from './client'
import { normalizeAccountUserId } from './normalizers'

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

// GET /api/team/me
// Response: MyTeamResponse JSON object
export async function getMyTeam() {
  try {
    const data = parseJson(await apiFetch('/api/team/me', { method: 'GET' }))
    if (data.teamId) return { hasTeam: true, data }
    return { hasTeam: false }
  } catch (err) {
    if (/no team/i.test(errorMessage(err))) return { hasTeam: false }
    throw new Error(errorMessage(err))
  }
}

// PUT /api/team/create
// Response: { message, teamId, teamName, enrollCode }
export async function createTeam({ teamName }) {
  const data = parseJson(
    await apiFetch('/api/team/create', {
      method: 'PUT',
      body: { teamName }
    })
  )

  if (!data.enrollCode) {
    throw new Error(data.message || 'Tạo đội thất bại')
  }

  return { enrollCode: data.enrollCode, teamId: data.teamId, teamName: data.teamName }
}

// PUT /api/team/join
// Response: { message, teamId }
export async function joinTeam({ enrollCode }) {
  const data = parseJson(
    await apiFetch('/api/team/join', {
      method: 'PUT',
      body: { enrollCode }
    })
  )
  if (!/join team successfully/i.test(data.message || '')) {
    throw new Error(data.message || 'Tham gia đội thất bại')
  }
  return true
}

// PUT /api/team/join-event
// Response: { message }
export async function joinEvent({ eventId, categoryId }) {
  const data = parseJson(
    await apiFetch('/api/team/join-event', {
      method: 'PUT',
      body: { eventId, categoryId }
    })
  )
  if (!/join event successfully/i.test(data.message || '')) {
    throw new Error(data.message || 'Đăng ký sự kiện thất bại')
  }
  return true
}

// DELETE /api/team/delete-member
// Response: { message }
export async function deleteMember({ memberId }) {
  const id = normalizeAccountUserId(memberId)
  if (!id || !/^\d+$/.test(id)) {
    throw new Error('Member ID không hợp lệ')
  }

  const data = parseJson(
    await apiFetch('/api/team/delete-member', {
      method: 'DELETE',
      body: { memberId: id }
    })
  )
  if (!/delete team member successfully/i.test(data.message || '')) {
    throw new Error(data.message || 'Xóa thành viên thất bại')
  }
  return true
}

import { apiFetch } from './client'
import { normalizeId } from './normalizers'

function parseJson(text) {
  try {
    return JSON.parse(text)
  } catch {
    throw new Error(text || 'Phản hồi không hợp lệ từ server')
  }
}

function mapUniversityRow(row) {
  const r = row && typeof row === 'object' ? row : {}
  return {
    universityId: normalizeId(r.universityId ?? r.university_id),
    universityName: r.universityName ?? r.university_name ?? '',
    linkedUserCount: String(r.linkedUserCount ?? r.linked_user_count ?? '0')
  }
}

function mapPreviewRow(row) {
  const r = row && typeof row === 'object' ? row : {}
  return {
    universityId: normalizeId(r.universityId ?? r.university_id),
    universityName: r.universityName ?? r.university_name ?? '',
    linkedUserCount: String(r.linkedUserCount ?? r.linked_user_count ?? '0'),
    canDeleteDirectly: Boolean(r.canDeleteDirectly ?? r.can_delete_directly),
    requiresUserHandling: Boolean(r.requiresUserHandling ?? r.requires_user_handling),
    message: r.message ?? ''
  }
}

// GET /api/staff/universities
export async function getStaffUniversities() {
  const text = await apiFetch('/api/staff/universities', { method: 'GET' })
  const data = parseJson(text)
  if (!Array.isArray(data)) return []
  return data.map(mapUniversityRow)
}

// POST /api/staff/universities
export async function createUniversity({ universityName }) {
  const name = String(universityName ?? '').trim()
  if (!name) throw new Error('Tên trường không được để trống')

  const data = parseJson(
    await apiFetch('/api/staff/universities', {
      method: 'POST',
      body: { universityName: name }
    })
  )
  return {
    universityId: normalizeId(data.universityId ?? data.university_id),
    universityName: data.universityName ?? data.university_name ?? name
  }
}

// PUT /api/staff/universities
export async function updateUniversity({ universityId, universityName }) {
  const id = normalizeId(universityId)
  const name = String(universityName ?? '').trim()
  if (!id) throw new Error('Trường không hợp lệ')
  if (!name) throw new Error('Tên trường không được để trống')

  const text = await apiFetch('/api/staff/universities', {
    method: 'PUT',
    body: { universityId: id, universityName: name }
  })
  const data = parseJson(text)
  return data.message ?? text
}

// GET /api/staff/universities/delete-preview?universityId=
export async function getDeleteUniversityPreview(universityId) {
  const id = normalizeId(universityId)
  if (!id) throw new Error('Trường không hợp lệ')

  const params = new URLSearchParams({ universityId: id })
  const text = await apiFetch(`/api/staff/universities/delete-preview?${params.toString()}`, {
    method: 'GET'
  })
  return mapPreviewRow(parseJson(text))
}

// DELETE /api/staff/universities
export async function deleteUniversity({ universityId, replacementUniversityName, clearLinkedUsers }) {
  const id = normalizeId(universityId)
  if (!id) throw new Error('Trường không hợp lệ')

  const body = { universityId: id }
  const replacement = String(replacementUniversityName ?? '').trim()
  if (replacement) body.replacementUniversityName = replacement
  if (clearLinkedUsers) body.clearLinkedUsers = true

  const text = await apiFetch('/api/staff/universities', {
    method: 'DELETE',
    body
  })
  const data = parseJson(text)
  return data.message ?? text
}

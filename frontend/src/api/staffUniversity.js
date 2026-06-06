import { apiFetch } from './client'

// GET /api/staff/universities
export async function getStaffUniversities() {
  const text = await apiFetch('/api/staff/universities', { method: 'GET' })
  try {
    return JSON.parse(text)
  } catch {
    throw new Error(text || 'Không thể tải danh sách trường đại học')
  }
}

// POST /api/staff/universities
export async function createUniversity({ universityName }) {
  const text = await apiFetch('/api/staff/universities', {
    method: 'POST',
    body: { universityName }
  })
  try {
    return JSON.parse(text)
  } catch {
    throw new Error(text || 'Không thể thêm trường đại học')
  }
}

// PUT /api/staff/universities
export async function updateUniversity({ universityId, universityName }) {
  const text = await apiFetch('/api/staff/universities', {
    method: 'PUT',
    body: { universityId, universityName }
  })
  try {
    return JSON.parse(text)
  } catch {
    throw new Error(text || 'Không thể sửa trường đại học')
  }
}

// GET /api/staff/universities/delete-preview?universityId=
export async function getDeleteUniversityPreview(universityId) {
  const text = await apiFetch(`/api/staff/universities/delete-preview?universityId=${encodeURIComponent(universityId)}`, {
    method: 'GET'
  })
  try {
    return JSON.parse(text)
  } catch {
    throw new Error(text || 'Không thể lấy thông tin trước khi xóa')
  }
}

// DELETE /api/staff/universities
export async function deleteUniversity({ universityId, replacementUniversityName, clearLinkedUsers }) {
  const body = { universityId }
  if (replacementUniversityName !== undefined) {
    body.replacementUniversityName = replacementUniversityName
  }
  if (clearLinkedUsers !== undefined) {
    body.clearLinkedUsers = clearLinkedUsers
  }
  const text = await apiFetch('/api/staff/universities', {
    method: 'DELETE',
    body
  })
  try {
    return JSON.parse(text)
  } catch {
    throw new Error(text || 'Không thể xóa trường đại học')
  }
}

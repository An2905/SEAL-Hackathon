const BASE = '/api'

async function request(url, options = {}) {
  const token = localStorage.getItem('token')
  const res = await fetch(url, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers
    }
  })
  const data = await res.json()
  if (!res.ok) throw new Error(data.message || 'Lỗi server')
  return data
}

/**
 * Lấy danh sách tiêu chí của event (có tổng weight & tổng maxScore)
 * @returns { criteria: [], count, totalWeight, totalMaxScore }
 */
export async function getCriteriaByEvent(eventId) {
  const res = await request(`${BASE}/staff/criteria?eventId=${eventId}`)
  return res.data
}

/**
 * Lấy chi tiết 1 tiêu chí
 */
export async function getCriteriaDetail(criteriaId) {
  const res = await request(`${BASE}/staff/criteria/detail?criteriaId=${criteriaId}`)
  return res.data
}

/**
 * Tạo tiêu chí mới
 * @param {Object} payload - { eventId, criterionName, weight, maxScore, description }
 */
export async function createCriteria(payload) {
  const res = await request(`${BASE}/staff/criteria`, {
    method: 'POST',
    body: JSON.stringify(payload)
  })
  return res.data
}

/**
 * Cập nhật tiêu chí
 * @param {string} criteriaId
 * @param {Object} payload - { eventId, criterionName, weight, maxScore, description }
 */
export async function updateCriteria(criteriaId, payload) {
  const res = await request(`${BASE}/staff/criteria?criteriaId=${criteriaId}`, {
    method: 'PUT',
    body: JSON.stringify(payload)
  })
  return res.data
}

/**
 * Xóa tiêu chí
 */
export async function deleteCriteria(criteriaId) {
  const res = await request(`${BASE}/staff/criteria?criteriaId=${criteriaId}`, { method: 'DELETE' })
  return res
}

/**
 * Judge xem tiêu chí của round mình được assign
 * @returns { roundId, criteria: [], count, totalWeight, totalMaxScore }
 */
export async function getCriteriaForJudge(roundId) {
  const res = await request(`${BASE}/judge/criteria?roundId=${roundId}`)
  return res.data
}

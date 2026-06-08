const BASE = '/api'

async function request(url, options = {}) {
  const token = localStorage.getItem('hh_token')
  const res = await fetch(url, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers
    }
  })

  const text = await res.text()

  let data
  try {
    data = JSON.parse(text)
  } catch {
    // BE trả về plain string (lỗi validation)
    if (!res.ok) throw new Error(text)
    return text
  }

  if (!res.ok) throw new Error(data.message || data || 'Lỗi server')
  return data
}

/**
 * Lấy danh sách tiêu chí của event (có tổng weight & tổng maxScore)
 * @returns { criteria: [], count, totalWeight, totalMaxScore }
 */
export async function getCriteriaByEvent(eventId) {
  return await request(`${BASE}/staff/criteria?eventId=${eventId}`)
}

/**
 * Lấy chi tiết 1 tiêu chí
 */
export async function getCriteriaDetail(criteriaId) {
  return await request(`${BASE}/staff/criteria/detail?criteriaId=${criteriaId}`)
}

/**
 * Tạo tiêu chí mới
 * @param {Object} payload - { eventId, criterionName, weight, maxScore, description }
 */
export async function createCriteria(payload) {
  return await request(`${BASE}/staff/criteria`, {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

/**
 * Cập nhật tiêu chí
 * @param {string} criteriaId
 * @param {Object} payload - { eventId, criterionName, weight, maxScore, description }
 */
export async function updateCriteria(criteriaId, payload) {
  return await request(`${BASE}/staff/criteria?criteriaId=${criteriaId}`, {
    method: 'PUT',
    body: JSON.stringify(payload)
  })
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
  return await request(`${BASE}/judge/criteria?roundId=${roundId}`)
}

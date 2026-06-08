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
    if (!res.ok) throw new Error(text)
    return text
  }

  if (!res.ok) throw new Error(data.message || data || 'Lỗi server')
  return data
}

function mapCriteriaResponse(data) {
  const criteria = Array.isArray(data.criteria) ? data.criteria : []
  const totalWeight = data.totalWeight ?? 0
  const totalMaxScore = criteria.reduce((sum, c) => sum + (Number(c.maxScore) || 0), 0)
  return {
    roundId: data.roundId ?? data.round_id ?? '',
    criteria,
    count: criteria.length,
    totalWeight,
    totalMaxScore
  }
}

/**
 * Lấy danh sách tiêu chí của round (có tổng weight & tổng maxScore)
 */
export async function getCriteriaByRound(roundId) {
  const data = await request(`${BASE}/staff/criteria?roundId=${encodeURIComponent(roundId)}`)
  return mapCriteriaResponse(data)
}

/** @deprecated dùng getCriteriaByRound */
export async function getCriteriaByEvent(roundId) {
  return getCriteriaByRound(roundId)
}

export async function getCriteriaDetail(criteriaId) {
  return await request(`${BASE}/staff/criteria/detail?criteriaId=${encodeURIComponent(criteriaId)}`)
}

/**
 * Tạo tiêu chí mới cho round
 * @param {Object} payload - { roundId, criterionName, weight, maxScore, description }
 */
export async function createCriteria(payload) {
  return await request(`${BASE}/staff/criteria`, {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export async function updateCriteria(criteriaId, payload) {
  return await request(`${BASE}/staff/criteria?criteriaId=${encodeURIComponent(criteriaId)}`, {
    method: 'PUT',
    body: JSON.stringify(payload)
  })
}

export async function deleteCriteria(criteriaId) {
  return await request(`${BASE}/staff/criteria?criteriaId=${encodeURIComponent(criteriaId)}`, {
    method: 'DELETE'
  })
}

export async function getCriteriaForJudge(roundId) {
  return await request(`${BASE}/judge/criteria?roundId=${encodeURIComponent(roundId)}`)
}

import { apiFetch } from './client'
import { mapCriteriaResponse } from './normalizers'

function parseJson(text) {
  if (!text || !String(text).trim()) return text
  try {
    return JSON.parse(text)
  } catch {
    throw new Error(text || 'Phản hồi không hợp lệ từ server')
  }
}

export async function getCriteriaByRound(roundId) {
  const text = await apiFetch(`/api/staff/criteria?roundId=${encodeURIComponent(roundId)}`)
  return mapCriteriaResponse(parseJson(text))
}

/** @deprecated dùng getCriteriaByRound */
export async function getCriteriaByEvent(roundId) {
  return getCriteriaByRound(roundId)
}

export async function getCriteriaDetail(criteriaId) {
  return parseJson(await apiFetch(`/api/staff/criteria/detail?criteriaId=${encodeURIComponent(criteriaId)}`))
}

export async function createCriteria(payload) {
  return parseJson(await apiFetch('/api/staff/criteria', { method: 'POST', body: payload }))
}

export async function updateCriteria(criteriaId, payload) {
  return parseJson(
    await apiFetch(`/api/staff/criteria?criteriaId=${encodeURIComponent(criteriaId)}`, {
      method: 'PUT',
      body: payload
    })
  )
}

export async function deleteCriteria(criteriaId) {
  return parseJson(
    await apiFetch(`/api/staff/criteria?criteriaId=${encodeURIComponent(criteriaId)}`, { method: 'DELETE' })
  )
}

/** @deprecated dùng getCriteriaForJudge từ api/judge.js */
export async function getCriteriaForJudge(roundId) {
  const text = await apiFetch(`/api/judge/criteria?roundId=${encodeURIComponent(roundId)}`)
  return mapCriteriaResponse(parseJson(text))
}

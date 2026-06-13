import { apiFetch } from './client'
import { normalizeEventId } from './event'
import {
  mapEventRow,
  mapMentorAssignmentRow,
  mapGroupColleaguesResponse,
  mapCriteriaResponse,
  mapTeamToScoreRow,
  mapJudgeScoreRow
} from './normalizers'

// GET /api/judge/events
export async function getAssignedEvents() {
  const text = await apiFetch('/api/judge/events', { method: 'GET' })
  try {
    const data = JSON.parse(text)
    if (!Array.isArray(data)) return []
    return data.map(mapEventRow)
  } catch {
    throw new Error(text || 'Không thể tải sự kiện được phân công')
  }
}

// GET /api/judge/events/current-rounds
export async function getAssignedCurrentRounds() {
  const text = await apiFetch('/api/judge/events/current-rounds', { method: 'GET' })
  try {
    const data = JSON.parse(text)
    return Array.isArray(data) ? data : []
  } catch {
    throw new Error(text || 'Không thể tải vòng hiện tại')
  }
}

// GET /api/judge/assignments
export async function getAssignments() {
  const text = await apiFetch('/api/judge/assignments', { method: 'GET' })
  try {
    const data = JSON.parse(text)
    if (!Array.isArray(data)) return []
    return data.map(mapMentorAssignmentRow)
  } catch {
    throw new Error(text || 'Không thể tải phân công bảng')
  }
}

/** @deprecated dùng getAssignments */
export const getJudgeAssignments = getAssignments

// GET /api/judge/colleagues?eventId&roundId&groupId
export async function getGroupColleagues({ eventId, roundId, groupId }) {
  const eid = normalizeEventId(eventId)
  const rid = String(roundId ?? '').trim()
  const gid = String(groupId ?? '').trim()
  if (!eid || !rid || !gid) {
    throw new Error('Thiếu thông tin sự kiện, vòng hoặc bảng')
  }

  const params = new URLSearchParams({ eventId: eid, roundId: rid, groupId: gid })
  const text = await apiFetch(`/api/judge/colleagues?${params.toString()}`, { method: 'GET' })
  try {
    return mapGroupColleaguesResponse(JSON.parse(text))
  } catch {
    throw new Error(text || 'Không thể tải danh sách đồng nghiệp')
  }
}

// GET /api/judge/criteria?roundId=
export async function getCriteriaForJudge(roundId) {
  const rid = String(roundId ?? '').trim()
  if (!rid) throw new Error('Thiếu roundId')
  const text = await apiFetch(`/api/judge/criteria?roundId=${encodeURIComponent(rid)}`, { method: 'GET' })
  try {
    return mapCriteriaResponse(JSON.parse(text))
  } catch {
    throw new Error(text || 'Không thể tải tiêu chí chấm điểm')
  }
}

// GET /api/judge/teams-to-score?eventId&roundId&groupId
export async function getTeamsToScore({ eventId, roundId, groupId }) {
  const eid = normalizeEventId(eventId)
  const rid = String(roundId ?? '').trim()
  const gid = String(groupId ?? '').trim()
  if (!eid || !rid || !gid) {
    throw new Error('Thiếu thông tin sự kiện, vòng hoặc bảng')
  }

  const params = new URLSearchParams({ eventId: eid, roundId: rid, groupId: gid })
  const text = await apiFetch(`/api/judge/teams-to-score?${params}`, { method: 'GET' })
  try {
    const data = JSON.parse(text)
    if (!Array.isArray(data)) return []
    return data.map(mapTeamToScoreRow)
  } catch {
    throw new Error(text || 'Không thể tải danh sách đội cần chấm')
  }
}

// GET /api/judge/scores?submissionId=
export async function getScoreBySubmission(submissionId) {
  const sid = String(submissionId ?? '').trim()
  if (!sid) throw new Error('Thiếu submissionId')
  const text = await apiFetch(`/api/judge/scores?submissionId=${encodeURIComponent(sid)}`, { method: 'GET' })
  try {
    return mapJudgeScoreRow(JSON.parse(text))
  } catch {
    throw new Error(text || 'Không thể tải điểm đã chấm')
  }
}

// POST /api/judge/scores
export async function submitScore(payload) {
  const text = await apiFetch('/api/judge/scores', { method: 'POST', body: payload })
  return String(text ?? '')
    .trim()
    .replace(/^"|"$/g, '')
}

// PUT /api/judge/scores?scoreId=
export async function updateScore(scoreId, payload) {
  const sid = String(scoreId ?? '').trim()
  if (!sid) throw new Error('Thiếu scoreId')
  await apiFetch(`/api/judge/scores?scoreId=${encodeURIComponent(sid)}`, {
    method: 'PUT',
    body: payload
  })
}

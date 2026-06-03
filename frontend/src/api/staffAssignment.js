import { apiFetch } from './client'
import { normalizeEventId, normalizeId } from './normalizers'

function parseJson(text) {
  try {
    return JSON.parse(text)
  } catch {
    throw new Error(text || 'Phản hồi không hợp lệ từ server')
  }
}

function parseMessage(text) {
  const data = parseJson(text)
  return data.message ?? text
}

function mapMentorAssignmentRow(data) {
  return {
    categoryId: String(data.categoryId ?? data.category_id ?? ''),
    categoryName: data.categoryName ?? data.category_name ?? '',
    mentorId: String(data.mentorId ?? data.mentor_id ?? ''),
    mentorName: data.mentorName ?? data.mentor_name ?? '',
    mentorEmail: data.mentorEmail ?? data.mentor_email ?? ''
  }
}

function mapJudgeAssignmentRow(data) {
  return {
    roundId: String(data.roundId ?? data.round_id ?? ''),
    roundName: data.roundName ?? data.round_name ?? '',
    categoryId: String(data.categoryId ?? data.category_id ?? ''),
    categoryName: data.categoryName ?? data.category_name ?? '',
    judgeId: String(data.judgeId ?? data.judge_id ?? ''),
    judgeName: data.judgeName ?? data.judge_name ?? '',
    judgeEmail: data.judgeEmail ?? data.judge_email ?? ''
  }
}

// DELETE /api/staff/assign/mentor?eventId=&categoryId=&mentorId=
export async function deleteMentorAssignment({ eventId, categoryId, mentorId }) {
  const eid = normalizeEventId(eventId)
  const cid = normalizeId(categoryId)
  const mid = normalizeId(mentorId)
  if (!eid || !cid || !mid) throw new Error('Thiếu thông tin phân công mentor')

  const params = new URLSearchParams({ eventId: eid, categoryId: cid, mentorId: mid })
  const text = await apiFetch(`/api/staff/assign/mentor?${params}`, { method: 'DELETE' })
  const message = parseMessage(text)
  if (!/mentor assignment deleted successfully/i.test(message)) throw new Error(message)
  return true
}

// PUT /api/staff/assign/mentor
export async function updateMentorAssignment({
  eventId,
  categoryId,
  mentorId,
  newCategoryId,
  newMentorId
}) {
  const eid = normalizeEventId(eventId)
  const oldCid = normalizeId(categoryId)
  const oldMid = normalizeId(mentorId)
  const newCid = normalizeId(newCategoryId)
  const newMid = normalizeId(newMentorId)
  if (!eid || !oldCid || !oldMid || !newCid || !newMid) {
    throw new Error('Thiếu thông tin cập nhật mentor')
  }

  const text = await apiFetch('/api/staff/assign/mentor', {
    method: 'PUT',
    body: {
      eventId: eid,
      categoryId: oldCid,
      mentorId: oldMid,
      newCategoryId: newCid,
      newMentorId: newMid
    }
  })
  return mapMentorAssignmentRow(parseJson(text))
}

// DELETE /api/staff/assign/judge?eventId=&judgeId=&roundId=&categoryId=
export async function deleteJudgeAssignment({
  eventId,
  judgeId,
  roundId,
  categoryId
}) {
  const eid = normalizeEventId(eventId)
  const jid = normalizeId(judgeId)
  const rid = normalizeId(roundId)
  const cid = normalizeId(categoryId)
  if (!eid || !jid || !rid || !cid) throw new Error('Thiếu thông tin phân công judge')

  const params = new URLSearchParams({
    eventId: eid,
    judgeId: jid,
    roundId: rid,
    categoryId: cid
  })
  const text = await apiFetch(`/api/staff/assign/judge?${params}`, { method: 'DELETE' })
  const message = parseMessage(text)
  if (!/judge assignment deleted successfully/i.test(message)) throw new Error(message)
  return true
}

// PUT /api/staff/assign/judge
export async function updateJudgeAssignment({
  eventId,
  judgeId,
  roundId,
  categoryId,
  newJudgeId,
  newRoundId,
  newCategoryId
}) {
  const eid = normalizeEventId(eventId)
  const oldJid = normalizeId(judgeId)
  const oldRid = normalizeId(roundId)
  const oldCid = normalizeId(categoryId)
  const newJid = normalizeId(newJudgeId)
  const newRid = normalizeId(newRoundId)
  const newCid = normalizeId(newCategoryId)
  if (!eid || !oldJid || !oldRid || !oldCid || !newJid || !newRid || !newCid) {
    throw new Error('Thiếu thông tin cập nhật judge')
  }

  const text = await apiFetch('/api/staff/assign/judge', {
    method: 'PUT',
    body: {
      eventId: eid,
      judgeId: oldJid,
      roundId: oldRid,
      categoryId: oldCid,
      newJudgeId: newJid,
      newRoundId: newRid,
      newCategoryId: newCid
    }
  })
  return mapJudgeAssignmentRow(parseJson(text))
}

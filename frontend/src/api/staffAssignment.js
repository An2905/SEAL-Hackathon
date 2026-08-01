import { apiFetch } from './client'
import { normalizeEventId, normalizeId } from './normalizers'

function parseJson(text) {
  try {
    return JSON.parse(text)
  } catch {
    throw new Error(text || 'Phản hồi không hợp lệ từ server')
  }
}

function mapMentorAssignmentRow(data) {
  return {
    roundId: String(data.roundId ?? data.round_id ?? ''),
    roundName: data.roundName ?? data.round_name ?? '',
    groupId: String(data.groupId ?? data.group_id ?? ''),
    groupName: data.groupName ?? data.group_name ?? '',
    mentorId: String(data.mentorId ?? data.mentor_id ?? ''),
    mentorName: data.mentorName ?? data.mentor_name ?? '',
    mentorEmail: data.mentorEmail ?? data.mentor_email ?? ''
  }
}

function mapJudgeAssignmentRow(data) {
  return {
    roundId: String(data.roundId ?? data.round_id ?? ''),
    roundName: data.roundName ?? data.round_name ?? '',
    groupId: String(data.groupId ?? data.group_id ?? ''),
    groupName: data.groupName ?? data.group_name ?? '',
    judgeId: String(data.judgeId ?? data.judge_id ?? ''),
    judgeName: data.judgeName ?? data.judge_name ?? '',
    judgeEmail: data.judgeEmail ?? data.judge_email ?? ''
  }
}

// DELETE /api/staff/assign/mentor?eventId=&roundId=&groupId=&mentorId=
export async function deleteMentorAssignment({ eventId, roundId, groupId, mentorId }) {
  const eid = normalizeEventId(eventId)
  const rid = normalizeId(roundId)
  const gid = normalizeId(groupId)
  const mid = normalizeId(mentorId)
  if (!eid || !rid || !gid || !mid) throw new Error('Thiếu thông tin phân công mentor')

  const params = new URLSearchParams({
    eventId: eid,
    roundId: rid,
    groupId: gid,
    mentorId: mid
  })
  await apiFetch(`/api/staff/assign/mentor?${params}`, { method: 'DELETE' })
  return true
}

// PUT /api/staff/assign/mentor
export async function updateMentorAssignment({
  eventId,
  roundId,
  groupId,
  mentorId,
  newRoundId,
  newGroupId,
  newMentorId
}) {
  const eid = normalizeEventId(eventId)
  const oldRid = normalizeId(roundId)
  const oldGid = normalizeId(groupId)
  const oldMid = normalizeId(mentorId)
  const newRid = normalizeId(newRoundId)
  const newGid = normalizeId(newGroupId)
  const newMid = normalizeId(newMentorId)
  if (!eid || !oldRid || !oldGid || !oldMid || !newRid || !newGid || !newMid) {
    throw new Error('Thiếu thông tin cập nhật mentor')
  }

  const text = await apiFetch('/api/staff/assign/mentor', {
    method: 'PUT',
    body: {
      eventId: eid,
      roundId: oldRid,
      groupId: oldGid,
      mentorId: oldMid,
      newRoundId: newRid,
      newGroupId: newGid,
      newMentorId: newMid
    }
  })
  return mapMentorAssignmentRow(parseJson(text))
}

// DELETE /api/staff/assign/judge?eventId=&judgeId=&roundId=&groupId=
export async function deleteJudgeAssignment({ eventId, judgeId, roundId, groupId }) {
  const eid = normalizeEventId(eventId)
  const jid = normalizeId(judgeId)
  const rid = normalizeId(roundId)
  const gid = normalizeId(groupId)
  if (!eid || !jid || !rid || !gid) throw new Error('Thiếu thông tin phân công judge')

  const params = new URLSearchParams({
    eventId: eid,
    judgeId: jid,
    roundId: rid,
    groupId: gid
  })
  await apiFetch(`/api/staff/assign/judge?${params}`, { method: 'DELETE' })
  return true
}

// PUT /api/staff/assign/judge
export async function updateJudgeAssignment({
  eventId,
  judgeId,
  roundId,
  groupId,
  newJudgeId,
  newRoundId,
  newGroupId
}) {
  const eid = normalizeEventId(eventId)
  const oldJid = normalizeId(judgeId)
  const oldRid = normalizeId(roundId)
  const oldGid = normalizeId(groupId)
  const newJid = normalizeId(newJudgeId)
  const newRid = normalizeId(newRoundId)
  const newGid = normalizeId(newGroupId)
  if (!eid || !oldJid || !oldRid || !oldGid || !newJid || !newRid || !newGid) {
    throw new Error('Thiếu thông tin cập nhật judge')
  }

  const text = await apiFetch('/api/staff/assign/judge', {
    method: 'PUT',
    body: {
      eventId: eid,
      judgeId: oldJid,
      roundId: oldRid,
      groupId: oldGid,
      newJudgeId: newJid,
      newRoundId: newRid,
      newGroupId: newGid
    }
  })
  return mapJudgeAssignmentRow(parseJson(text))
}

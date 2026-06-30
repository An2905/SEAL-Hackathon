import { apiFetch } from './client'
import { normalizeEventId, normalizeId } from './normalizers'

function parseJson(text) {
  try {
    return JSON.parse(text)
  } catch {
    throw new Error(text || 'Phản hồi không hợp lệ từ server')
  }
}

// POST /api/staff/events/groups
// Body: { eventId, roundId, name, maxTeams? }
export async function createEventGroup({ eventId, roundId, name, maxTeams }) {
  const id = normalizeEventId(eventId)
  const rid = normalizeId(roundId)
  if (!id) throw new Error('Sự kiện không hợp lệ')
  if (!rid) throw new Error('Vui lòng chọn vòng thi')

  const n = String(name ?? '').trim()
  if (!n) throw new Error('Tên bảng không được để trống')

  const body = { eventId: id, roundId: rid, name: n }
  const max = maxTeams === '' || maxTeams == null ? null : Number(maxTeams)
  if (max != null) {
    if (!Number.isFinite(max) || max < 1) throw new Error('Số đội tối đa phải ≥ 1')
    body.maxTeams = max
  }

  const text = await apiFetch('/api/staff/events/groups', {
    method: 'POST',
    body
  })
  return mapGroupResponse(parseJson(text), { eventId: id, roundId: rid, name: n, maxTeams: max })
}

// POST /api/staff/events/rounds
// Body: { eventId, name, startDate, endDate, submissionDeadline }
export async function createEventRound({ eventId, name, startDate, endDate, submissionDeadline, winnersPerRound }) {
  const id = normalizeEventId(eventId)
  if (!id) throw new Error('Sự kiện không hợp lệ')

  const n = String(name ?? '').trim()
  if (!n) throw new Error('Tên vòng không được để trống')
  if (!startDate || !endDate || !submissionDeadline) {
    throw new Error('Vui lòng nhập đầy đủ thời gian vòng thi')
  }

  const winners = mapOptionalInt(winnersPerRound)
  if (winners != null && winners < 1) {
    throw new Error('Số winner mỗi vòng phải ≥ 1')
  }

  const body = {
    eventId: id,
    name: n,
    startDate,
    endDate,
    submissionDeadline
  }
  if (winners != null) body.winnersPerRound = winners

  const text = await apiFetch('/api/staff/events/rounds', {
    method: 'POST',
    body
  })
  const data = parseJson(text)
  return {
    roundId: String(data.roundId ?? data.round_id ?? ''),
    eventId: String(data.eventId ?? data.event_id ?? id),
    name: data.name ?? n,
    roundOrder: String(data.roundOrder ?? data.round_order ?? ''),
    startDate: data.startDate ?? data.start_date ?? startDate,
    endDate: data.endDate ?? data.end_date ?? endDate,
    submissionDeadline: data.submissionDeadline ?? data.submission_deadline ?? submissionDeadline
  }
}

function parseMessage(text) {
  const data = parseJson(text)
  return data.message ?? text
}

// DELETE /api/staff/events/groups?eventId=&roundId=&groupId=
export async function deleteEventGroup({ eventId, roundId, groupId }) {
  const eid = normalizeEventId(eventId)
  const rid = normalizeId(roundId)
  const gid = normalizeId(groupId)
  if (!eid || !rid || !gid) throw new Error('Thiếu thông tin bảng thi')

  const params = new URLSearchParams({ eventId: eid, roundId: rid, groupId: gid })
  const text = await apiFetch(`/api/staff/events/groups?${params}`, {
    method: 'DELETE'
  })
  const message = parseMessage(text)
  if (!/group deleted successfully/i.test(message)) throw new Error(message)
  return true
}

// DELETE /api/staff/events/rounds?eventId=&roundId=
export async function deleteEventRound({ eventId, roundId }) {
  const eid = normalizeEventId(eventId)
  const rid = normalizeId(roundId)
  if (!eid || !rid) throw new Error('Sự kiện hoặc vòng không hợp lệ')

  const params = new URLSearchParams({ eventId: eid, roundId: rid })
  const text = await apiFetch(`/api/staff/events/rounds?${params}`, {
    method: 'DELETE'
  })
  const message = parseMessage(text)
  if (!/round deleted successfully/i.test(message)) throw new Error(message)
  return true
}

function mapTeamItem(data) {
  return {
    teamId: String(data.teamId ?? data.team_id ?? ''),
    teamName: data.teamName ?? data.team_name ?? '',
    status: data.status ?? '',
    registrationId: String(data.registrationId ?? data.registration_id ?? '')
  }
}

function mapGroupTeamsResponse(data, fallback = {}) {
  const teamCountRaw = data.teamCount ?? data.team_count ?? fallback.teamCount
  const teamCountNum = teamCountRaw == null || teamCountRaw === '' ? 0 : Number(teamCountRaw)
  const assignedRaw = data.assigned ?? data.Assigned ?? []
  const availableRaw = data.available ?? data.Available ?? []
  return {
    groupId: String(data.groupId ?? data.group_id ?? fallback.groupId ?? ''),
    teamCount: Number.isFinite(teamCountNum) ? teamCountNum : 0,
    assigned: Array.isArray(assignedRaw) ? assignedRaw.map(mapTeamItem) : [],
    available: Array.isArray(availableRaw) ? availableRaw.map(mapTeamItem) : []
  }
}

// GET /api/staff/events/groups/teams?eventId=&roundId=&groupId=
export async function getEventGroupTeams({ eventId, roundId, groupId }) {
  const eid = normalizeEventId(eventId)
  const rid = normalizeId(roundId)
  const gid = normalizeId(groupId)
  if (!eid || !rid || !gid) throw new Error('Thiếu thông tin bảng thi')

  const params = new URLSearchParams({ eventId: eid, roundId: rid, groupId: gid })
  const text = await apiFetch(`/api/staff/events/groups/teams?${params}`, {
    method: 'GET'
  })
  return mapGroupTeamsResponse(parseJson(text), { groupId: gid })
}

// POST /api/staff/events/groups/teams
export async function assignTeamToGroup({ eventId, roundId, groupId, teamId }) {
  const eid = normalizeEventId(eventId)
  const rid = normalizeId(roundId)
  const gid = normalizeId(groupId)
  const tid = normalizeId(teamId)
  if (!eid || !rid || !gid || !tid) throw new Error('Thiếu thông tin phân bảng')

  const text = await apiFetch('/api/staff/events/groups/teams', {
    method: 'POST',
    body: { eventId: eid, roundId: rid, groupId: gid, teamId: tid }
  })
  return mapGroupTeamsResponse(parseJson(text), { groupId: gid })
}

// DELETE /api/staff/events/groups/teams?eventId=&roundId=&groupId=&teamId=
export async function removeTeamFromGroup({ eventId, roundId, groupId, teamId }) {
  const eid = normalizeEventId(eventId)
  const rid = normalizeId(roundId)
  const gid = normalizeId(groupId)
  const tid = normalizeId(teamId)
  if (!eid || !rid || !gid || !tid) throw new Error('Thiếu thông tin phân bảng')

  const params = new URLSearchParams({
    eventId: eid,
    roundId: rid,
    groupId: gid,
    teamId: tid
  })
  const text = await apiFetch(`/api/staff/events/groups/teams?${params}`, {
    method: 'DELETE'
  })
  return mapGroupTeamsResponse(parseJson(text), { groupId: gid })
}

function mapGroupResponse(data, fallback = {}) {
  const maxRaw = data.maxTeams ?? data.max_teams ?? fallback.maxTeams
  const maxNum = maxRaw == null || maxRaw === '' ? null : Number(maxRaw)
  return {
    groupId: String(data.groupId ?? data.group_id ?? fallback.groupId ?? ''),
    eventId: String(data.eventId ?? data.event_id ?? fallback.eventId ?? ''),
    roundId: String(data.roundId ?? data.round_id ?? fallback.roundId ?? ''),
    roundName: data.roundName ?? data.round_name ?? fallback.roundName ?? '',
    roundOrder: String(data.roundOrder ?? data.round_order ?? fallback.roundOrder ?? ''),
    name: data.name ?? fallback.name ?? '',
    maxTeams: Number.isFinite(maxNum) ? maxNum : null
  }
}

function mapRoundResponse(data, fallback = {}) {
  const winnersPerRoundRaw = data.winnersPerRound ?? data.winners_per_round ?? fallback.winnersPerRound
  const winnersPerRoundNum = winnersPerRoundRaw == null || winnersPerRoundRaw === '' ? 1 : Number(winnersPerRoundRaw)
  const winnerCountRaw = data.winnerCount ?? data.winner_count ?? fallback.winnerCount
  const winnerCountNum = winnerCountRaw == null || winnerCountRaw === '' ? 0 : Number(winnerCountRaw)
  return {
    roundId: String(data.roundId ?? data.round_id ?? fallback.roundId ?? ''),
    eventId: String(data.eventId ?? data.event_id ?? fallback.eventId ?? ''),
    name: data.name ?? fallback.name ?? '',
    roundOrder: String(data.roundOrder ?? data.round_order ?? fallback.roundOrder ?? ''),
    startDate: data.startDate ?? data.start_date ?? fallback.startDate ?? '',
    endDate: data.endDate ?? data.end_date ?? fallback.endDate ?? '',
    submissionDeadline: data.submissionDeadline ?? data.submission_deadline ?? fallback.submissionDeadline ?? '',
    winnersPerRound: Number.isFinite(winnersPerRoundNum) ? winnersPerRoundNum : 1,
    winnerCount: Number.isFinite(winnerCountNum) ? winnerCountNum : 0
  }
}

// PUT /api/staff/events/groups
export async function updateEventGroup({ eventId, roundId, groupId, name, maxTeams }) {
  const eid = normalizeEventId(eventId)
  const rid = normalizeId(roundId)
  const gid = normalizeId(groupId)
  if (!eid || !rid || !gid) throw new Error('Thiếu thông tin bảng thi')
  const n = String(name ?? '').trim()
  if (!n) throw new Error('Tên bảng không được để trống')

  const body = { eventId: eid, roundId: rid, groupId: gid, name: n }
  const max = maxTeams === '' || maxTeams == null ? null : Number(maxTeams)
  if (max != null) {
    if (!Number.isFinite(max) || max < 1) throw new Error('Số đội tối đa phải ≥ 1')
    body.maxTeams = max
  } else {
    body.maxTeams = null
  }

  const text = await apiFetch('/api/staff/events/groups', {
    method: 'PUT',
    body
  })
  return mapGroupResponse(parseJson(text), {
    eventId: eid,
    roundId: rid,
    groupId: gid,
    name: n,
    maxTeams: max
  })
}

// GET /api/staff/events/rounds/detail?eventId=&roundId=
export async function getEventRoundDetail({ eventId, roundId }) {
  const eid = normalizeEventId(eventId)
  const rid = normalizeId(roundId)
  if (!eid || !rid) throw new Error('Sự kiện hoặc vòng không hợp lệ')

  const params = new URLSearchParams({ eventId: eid, roundId: rid })
  const text = await apiFetch(`/api/staff/events/rounds/detail?${params}`, {
    method: 'GET'
  })
  return mapRoundResponse(parseJson(text), { eventId: eid, roundId: rid })
}

// PUT /api/staff/events/rounds
export async function updateEventRound({
  eventId,
  roundId,
  name,
  roundOrder,
  startDate,
  endDate,
  submissionDeadline,
  winnersPerRound
}) {
  const eid = normalizeEventId(eventId)
  const rid = normalizeId(roundId)
  if (!eid || !rid) throw new Error('Sự kiện hoặc vòng không hợp lệ')

  const n = String(name ?? '').trim()
  if (!n) throw new Error('Tên vòng không được để trống')
  const order = Number(roundOrder)
  if (!Number.isFinite(order) || order < 1) {
    throw new Error('Thứ tự vòng phải là số nguyên ≥ 1')
  }
  if (!startDate || !endDate || !submissionDeadline) {
    throw new Error('Vui lòng nhập đầy đủ thời gian vòng thi')
  }

  const winners = mapOptionalInt(winnersPerRound)
  if (winners != null && winners < 1) {
    throw new Error('Số winner mỗi vòng phải ≥ 1')
  }

  const body = {
    eventId: eid,
    roundId: rid,
    name: n,
    roundOrder: order,
    startDate,
    endDate,
    submissionDeadline
  }
  if (winners != null) body.winnersPerRound = winners

  const text = await apiFetch('/api/staff/events/rounds', {
    method: 'PUT',
    body
  })
  return mapRoundResponse(parseJson(text), {
    eventId: eid,
    roundId: rid,
    name: n,
    roundOrder: String(order),
    startDate,
    endDate,
    submissionDeadline,
    winnersPerRound: winners ?? 1
  })
}

function mapOptionalInt(value) {
  if (value == null || value === '') return null
  const num = Number(value)
  return Number.isFinite(num) ? num : null
}

function mapEventUpdateResponse(data, fallback = {}) {
  return {
    eventId: String(data.eventId ?? data.event_id ?? fallback.eventId ?? ''),
    title: data.title ?? fallback.title ?? '',
    description: data.description ?? fallback.description ?? '',
    startDate: data.startDate ?? data.start_date ?? fallback.startDate ?? '',
    endDate: data.endDate ?? data.end_date ?? fallback.endDate ?? '',
    status: data.status ?? fallback.status ?? '',
    maxTeams: mapOptionalInt(data.maxTeams ?? data.max_teams ?? fallback.maxTeams),
    numRounds: mapOptionalInt(data.numRounds ?? data.num_rounds ?? fallback.numRounds) ?? 1,
    createdAt: data.createdAt ?? data.created_at ?? fallback.createdAt ?? '',
    githubTemplateRepo: data.githubTemplateRepo ?? data.github_template_repo ?? fallback.githubTemplateRepo ?? ''
  }
}

// PUT /api/staff/events
export async function updateEvent({
  eventId,
  title,
  description,
  startDate,
  endDate,
  status,
  maxTeams,
  numRounds,
  githubTemplateRepo
}) {
  const id = normalizeEventId(eventId)
  if (!id) throw new Error('Sự kiện không hợp lệ')

  const t = String(title ?? '').trim()
  if (!t) throw new Error('Tên sự kiện không được để trống')

  const nextStatus = String(status ?? '')
    .trim()
    .toUpperCase()
  if (!['BUILDING', 'UPCOMING', 'ONGOING', 'COMPLETED'].includes(nextStatus)) {
    throw new Error('Trạng thái không hợp lệ.')
  }

  const body = {
    eventId: id,
    title: t,
    description: String(description ?? '').trim() || null,
    status: nextStatus,
    githubTemplateRepo:
      githubTemplateRepo == null || String(githubTemplateRepo).trim() === '' ? null : String(githubTemplateRepo).trim()
  }
  if (startDate) body.startDate = startDate
  if (endDate) body.endDate = endDate

  const max = maxTeams === '' || maxTeams == null ? null : Number(maxTeams)
  if (max != null) {
    if (!Number.isFinite(max) || max < 1) throw new Error('Số đội tối đa phải ≥ 1')
    body.maxTeams = max
  } else {
    body.maxTeams = null
  }

  const rounds = numRounds === '' || numRounds == null ? 1 : Number(numRounds)
  if (!Number.isFinite(rounds) || rounds < 1) {
    throw new Error('Số vòng thi dự kiến phải ≥ 1')
  }
  body.numRounds = rounds

  const text = await apiFetch('/api/staff/events', {
    method: 'PUT',
    body
  })
  return mapEventUpdateResponse(parseJson(text), {
    eventId: id,
    title: t,
    startDate: startDate ?? '',
    endDate: endDate ?? '',
    status: nextStatus,
    maxTeams: max,
    numRounds: rounds,
    githubTemplateRepo: githubTemplateRepo ?? ''
  })
}

function mapAwardResponse(data, fallback = {}) {
  const rankRaw = data.rank ?? fallback.rank
  const rankNum = rankRaw == null || rankRaw === '' ? null : Number(rankRaw)
  return {
    awardId: String(data.awardId ?? data.award_id ?? fallback.awardId ?? ''),
    eventId: String(data.eventId ?? data.event_id ?? fallback.eventId ?? ''),
    title: data.title ?? fallback.title ?? '',
    rank: Number.isFinite(rankNum) ? rankNum : null,
    teamName: data.teamName ?? data.team_name ?? fallback.teamName ?? ''
  }
}

// POST /api/staff/events/awards
export async function createEventAward({ eventId, title, rank }) {
  const id = normalizeEventId(eventId)
  if (!id) throw new Error('Sự kiện không hợp lệ')

  const t = String(title ?? '').trim()
  if (!t) throw new Error('Tên giải thưởng không được để trống')

  const body = { eventId: id, title: t }
  const r = rank === '' || rank == null ? null : Number(rank)
  if (r != null) {
    if (!Number.isFinite(r) || r < 1) throw new Error('Hạng phải là số nguyên ≥ 1')
    body.rank = r
  }

  const text = await apiFetch('/api/staff/events/awards', {
    method: 'POST',
    body
  })
  return mapAwardResponse(parseJson(text), { eventId: id, title: t, rank: r })
}

// PUT /api/staff/events/awards
export async function updateEventAward({ eventId, awardId, title, rank }) {
  const eid = normalizeEventId(eventId)
  const aid = normalizeId(awardId)
  if (!eid || !aid) throw new Error('Thiếu thông tin giải thưởng')

  const t = String(title ?? '').trim()
  if (!t) throw new Error('Tên giải thưởng không được để trống')

  const body = { eventId: eid, awardId: aid, title: t }
  const r = rank === '' || rank == null ? null : Number(rank)
  if (r != null) {
    if (!Number.isFinite(r) || r < 1) throw new Error('Hạng phải là số nguyên ≥ 1')
    body.rank = r
  } else {
    body.rank = null
  }

  const text = await apiFetch('/api/staff/events/awards', {
    method: 'PUT',
    body
  })
  return mapAwardResponse(parseJson(text), {
    eventId: eid,
    awardId: aid,
    title: t,
    rank: r
  })
}

// DELETE /api/staff/events/awards?eventId=&awardId=
export async function deleteEventAward({ eventId, awardId }) {
  const eid = normalizeEventId(eventId)
  const aid = normalizeId(awardId)
  if (!eid || !aid) throw new Error('Thiếu thông tin giải thưởng')

  const params = new URLSearchParams({ eventId: eid, awardId: aid })
  const text = await apiFetch(`/api/staff/events/awards?${params}`, {
    method: 'DELETE'
  })
  const message = parseMessage(text)
  if (!/award deleted successfully/i.test(message)) throw new Error(message)
  return true
}

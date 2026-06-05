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
export async function createEventRound({
  eventId,
  name,
  startDate,
  endDate,
  submissionDeadline
}) {
  const id = normalizeEventId(eventId)
  if (!id) throw new Error('Sự kiện không hợp lệ')

  const n = String(name ?? '').trim()
  if (!n) throw new Error('Tên vòng không được để trống')
  if (!startDate || !endDate || !submissionDeadline) {
    throw new Error('Vui lòng nhập đầy đủ thời gian vòng thi')
  }

  const text = await apiFetch('/api/staff/events/rounds', {
    method: 'POST',
    body: {
      eventId: id,
      name: n,
      startDate,
      endDate,
      submissionDeadline
    }
  })
  const data = parseJson(text)
  return {
    roundId: String(data.roundId ?? data.round_id ?? ''),
    eventId: String(data.eventId ?? data.event_id ?? id),
    name: data.name ?? n,
    roundOrder: String(data.roundOrder ?? data.round_order ?? ''),
    startDate: data.startDate ?? data.start_date ?? startDate,
    endDate: data.endDate ?? data.end_date ?? endDate,
    submissionDeadline:
      data.submissionDeadline ?? data.submission_deadline ?? submissionDeadline
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
  return {
    roundId: String(data.roundId ?? data.round_id ?? fallback.roundId ?? ''),
    eventId: String(data.eventId ?? data.event_id ?? fallback.eventId ?? ''),
    name: data.name ?? fallback.name ?? '',
    roundOrder: String(data.roundOrder ?? data.round_order ?? fallback.roundOrder ?? ''),
    startDate: data.startDate ?? data.start_date ?? fallback.startDate ?? '',
    endDate: data.endDate ?? data.end_date ?? fallback.endDate ?? '',
    submissionDeadline:
      data.submissionDeadline ?? data.submission_deadline ?? fallback.submissionDeadline ?? ''
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
  submissionDeadline
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

  const text = await apiFetch('/api/staff/events/rounds', {
    method: 'PUT',
    body: {
      eventId: eid,
      roundId: rid,
      name: n,
      roundOrder: order,
      startDate,
      endDate,
      submissionDeadline
    }
  })
  return mapRoundResponse(parseJson(text), {
    eventId: eid,
    roundId: rid,
    name: n,
    roundOrder: String(order),
    startDate,
    endDate,
    submissionDeadline
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
    createdAt: data.createdAt ?? data.created_at ?? fallback.createdAt ?? ''
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
  numRounds
}) {
  const id = normalizeEventId(eventId)
  if (!id) throw new Error('Sự kiện không hợp lệ')

  const t = String(title ?? '').trim()
  if (!t) throw new Error('Tên sự kiện không được để trống')

  const nextStatus = String(status ?? '').trim().toUpperCase()
  if (!['BUILDING', 'UPCOMING', 'ONGOING', 'COMPLETED'].includes(nextStatus)) {
    throw new Error('Trạng thái phải là BUILDING, UPCOMING, ONGOING hoặc COMPLETED')
  }

  const body = {
    eventId: id,
    title: t,
    description: String(description ?? '').trim() || null,
    status: nextStatus
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
    numRounds: rounds
  })
}

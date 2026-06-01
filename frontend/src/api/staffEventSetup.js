import { apiFetch } from './client'
import { normalizeEventId, normalizeId } from './normalizers'

function parseJson(text) {
  try {
    return JSON.parse(text)
  } catch {
    throw new Error(text || 'Phản hồi không hợp lệ từ server')
  }
}

// POST /api/staff/events/categories
// Body: { eventId, name, description? }
export async function createEventCategory({ eventId, name, description }) {
  const id = normalizeEventId(eventId)
  if (!id) throw new Error('Event ID không hợp lệ')

  const n = String(name ?? '').trim()
  if (!n) throw new Error('Tên track không được để trống')

  const text = await apiFetch('/api/staff/events/categories', {
    method: 'POST',
    body: {
      eventId: id,
      name: n,
      description: String(description ?? '').trim() || null
    }
  })
  const data = parseJson(text)
  return {
    categoryId: String(data.categoryId ?? data.category_id ?? ''),
    eventId: String(data.eventId ?? data.event_id ?? id),
    name: data.name ?? n,
    description: data.description ?? ''
  }
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
  if (!id) throw new Error('Event ID không hợp lệ')

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

// DELETE /api/staff/events/categories?eventId=&categoryId=
export async function deleteEventCategory({ eventId, categoryId }) {
  const eid = normalizeEventId(eventId)
  const cid = normalizeId(categoryId)
  if (!eid || !cid) throw new Error('Event ID hoặc Category ID không hợp lệ')

  const params = new URLSearchParams({ eventId: eid, categoryId: cid })
  const text = await apiFetch(`/api/staff/events/categories?${params}`, {
    method: 'DELETE'
  })
  const message = parseMessage(text)
  if (!/category deleted successfully/i.test(message)) throw new Error(message)
  return true
}

// DELETE /api/staff/events/rounds?eventId=&roundId=
export async function deleteEventRound({ eventId, roundId }) {
  const eid = normalizeEventId(eventId)
  const rid = normalizeId(roundId)
  if (!eid || !rid) throw new Error('Event ID hoặc Round ID không hợp lệ')

  const params = new URLSearchParams({ eventId: eid, roundId: rid })
  const text = await apiFetch(`/api/staff/events/rounds?${params}`, {
    method: 'DELETE'
  })
  const message = parseMessage(text)
  if (!/round deleted successfully/i.test(message)) throw new Error(message)
  return true
}

function mapCategoryResponse(data, fallback = {}) {
  return {
    categoryId: String(data.categoryId ?? data.category_id ?? fallback.categoryId ?? ''),
    eventId: String(data.eventId ?? data.event_id ?? fallback.eventId ?? ''),
    name: data.name ?? fallback.name ?? '',
    description: data.description ?? fallback.description ?? ''
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

// PUT /api/staff/events/categories
export async function updateEventCategory({ eventId, categoryId, name, description }) {
  const eid = normalizeEventId(eventId)
  const cid = normalizeId(categoryId)
  if (!eid || !cid) throw new Error('Event ID hoặc Category ID không hợp lệ')
  const n = String(name ?? '').trim()
  if (!n) throw new Error('Tên track không được để trống')

  const text = await apiFetch('/api/staff/events/categories', {
    method: 'PUT',
    body: {
      eventId: eid,
      categoryId: cid,
      name: n,
      description: String(description ?? '').trim() || null
    }
  })
  return mapCategoryResponse(parseJson(text), { eventId: eid, categoryId: cid, name: n })
}

// GET /api/staff/events/rounds/detail?eventId=&roundId=
export async function getEventRoundDetail({ eventId, roundId }) {
  const eid = normalizeEventId(eventId)
  const rid = normalizeId(roundId)
  if (!eid || !rid) throw new Error('Event ID hoặc Round ID không hợp lệ')

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
  if (!eid || !rid) throw new Error('Event ID hoặc Round ID không hợp lệ')

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

function mapEventUpdateResponse(data, fallback = {}) {
  return {
    eventId: String(data.eventId ?? data.event_id ?? fallback.eventId ?? ''),
    title: data.title ?? fallback.title ?? '',
    description: data.description ?? fallback.description ?? '',
    startDate: data.startDate ?? data.start_date ?? fallback.startDate ?? '',
    endDate: data.endDate ?? data.end_date ?? fallback.endDate ?? '',
    status: data.status ?? fallback.status ?? '',
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
  status
}) {
  const id = normalizeEventId(eventId)
  if (!id) throw new Error('Event ID không hợp lệ')

  const t = String(title ?? '').trim()
  if (!t) throw new Error('Tên sự kiện không được để trống')
  if (!startDate || !endDate) {
    throw new Error('Vui lòng nhập ngày bắt đầu và kết thúc')
  }

  const nextStatus = String(status ?? '').trim().toUpperCase()
  if (!['UPCOMING', 'ONGOING', 'COMPLETED'].includes(nextStatus)) {
    throw new Error('Trạng thái phải là UPCOMING, ONGOING hoặc COMPLETED')
  }

  const text = await apiFetch('/api/staff/events', {
    method: 'PUT',
    body: {
      eventId: id,
      title: t,
      description: String(description ?? '').trim() || null,
      startDate,
      endDate,
      status: nextStatus
    }
  })
  return mapEventUpdateResponse(parseJson(text), {
    eventId: id,
    title: t,
    startDate,
    endDate,
    status: nextStatus
  })
}

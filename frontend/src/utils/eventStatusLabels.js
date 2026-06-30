export const EVENT_STATUSES = ['BUILDING', 'UPCOMING', 'ONGOING', 'COMPLETED']

/** Chỉ BUILDING mới được đổi trạng thái thủ công (→ UPCOMING). */
export const BUILDING_STATUS_OPTIONS = ['BUILDING', 'UPCOMING']

export const EVENT_STATUS_LABELS = {
  ALL: 'Tất cả',
  BUILDING: 'Đang thiết lập',
  UPCOMING: 'Sắp diễn ra',
  ONGOING: 'Đang diễn ra',
  COMPLETED: 'Đã kết thúc',
  CANCELLED: 'Đã hủy'
}

export function normalizeEventStatus(status) {
  return String(status ?? '').trim().toUpperCase()
}

export function eventStatusLabel(status) {
  const key = normalizeEventStatus(status)
  if (!key) return '—'
  return EVENT_STATUS_LABELS[key] ?? status
}

export function eventStatusFilterLabel(status) {
  const key = normalizeEventStatus(status)
  if (key === 'ALL') return EVENT_STATUS_LABELS.ALL
  return eventStatusLabel(key)
}

export function canManuallyChangeEventStatus(status) {
  return normalizeEventStatus(status) === 'BUILDING'
}

export function isEventStatusLocked(status) {
  return !canManuallyChangeEventStatus(status)
}

export function eventStatusLockHint(status) {
  const key = normalizeEventStatus(status)
  if (key === 'COMPLETED') {
    return 'Sự kiện đã kết thúc — trạng thái do hệ thống tự cập nhật.'
  }
  if (key === 'ONGOING') {
    return 'Sự kiện đang diễn ra — trạng thái do hệ thống tự cập nhật khi đến thời gian bắt đầu/kết thúc.'
  }
  if (key === 'UPCOMING') {
    return 'Sự kiện đã công bố — không thể hạ về đang thiết lập.'
  }
  return ''
}

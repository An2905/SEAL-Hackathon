import { describe, expect, it } from 'vitest'
import {
  canManuallyChangeEventStatus,
  eventStatusLabel,
  eventStatusLockHint,
  isEventStatusLocked,
  normalizeEventStatus
} from './eventStatusLabels'

describe('eventStatusLabels', () => {
  it('normalizes status to uppercase trimmed string', () => {
    expect(normalizeEventStatus(' building ')).toBe('BUILDING')
    expect(normalizeEventStatus(null)).toBe('')
  })

  it('returns Vietnamese labels for known statuses', () => {
    expect(eventStatusLabel('BUILDING')).toBe('Đang thiết lập')
    expect(eventStatusLabel('ONGOING')).toBe('Đang diễn ra')
    expect(eventStatusLabel('UNKNOWN_X')).toBe('UNKNOWN_X')
  })

  it('only BUILDING can be manually changed', () => {
    expect(canManuallyChangeEventStatus('BUILDING')).toBe(true)
    expect(canManuallyChangeEventStatus('UPCOMING')).toBe(false)
    expect(isEventStatusLocked('UPCOMING')).toBe(true)
  })

  it('provides lock hints for non-building statuses', () => {
    expect(eventStatusLockHint('BUILDING')).toBe('')
    expect(eventStatusLockHint('ONGOING')).toContain('đang diễn ra')
    expect(eventStatusLockHint('COMPLETED')).toContain('đã kết thúc')
  })
})

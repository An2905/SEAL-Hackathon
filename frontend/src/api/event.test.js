import { describe, expect, it } from 'vitest'
import { attachPendingTeamsToEvents } from './event'

describe('attachPendingTeamsToEvents', () => {
  it('returns empty array for empty input', async () => {
    expect(await attachPendingTeamsToEvents([])).toEqual([])
    expect(await attachPendingTeamsToEvents(null)).toEqual([])
  })

  it('preserves pendingTeams from list payload without extra API calls', async () => {
    const events = [
      { eventId: '1', title: 'A', pendingTeams: '2' },
      { eventId: '2', title: 'B' }
    ]
    const result = await attachPendingTeamsToEvents(events)
    expect(result).toEqual([
      { eventId: '1', title: 'A', pendingTeams: '2', pendingTeamsError: false },
      { eventId: '2', title: 'B', pendingTeams: '0', pendingTeamsError: false }
    ])
  })

  it('does not mutate the original array items', async () => {
    const events = [{ eventId: '1', pendingTeams: '5' }]
    const result = await attachPendingTeamsToEvents(events)
    expect(result[0]).not.toBe(events[0])
    expect(events[0]).toEqual({ eventId: '1', pendingTeams: '5' })
  })
})

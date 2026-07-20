import { describe, expect, it } from 'vitest'
import {
  countPendingTeams,
  mapAccountRow,
  mapCount,
  mapEventDetailRow,
  mapEventRow,
  normalizeId
} from './normalizers'

describe('normalizeId', () => {
  it('returns empty string for nullish values', () => {
    expect(normalizeId(null)).toBe('')
    expect(normalizeId('')).toBe('')
  })

  it('truncates float-like bigint ids', () => {
    expect(normalizeId(12.0)).toBe('12')
    expect(normalizeId('12.0')).toBe('12')
    expect(normalizeId('99.000')).toBe('99')
  })

  it('keeps non-numeric string ids', () => {
    expect(normalizeId('abc-123')).toBe('abc-123')
  })
})

describe('mapCount', () => {
  it('returns "0" for empty values', () => {
    expect(mapCount(null)).toBe('0')
    expect(mapCount('')).toBe('0')
  })

  it('truncates numeric values to integer strings', () => {
    expect(mapCount(3.9)).toBe('3')
    expect(mapCount('7')).toBe('7')
  })
})

describe('countPendingTeams', () => {
  it('counts only PENDING registrations (case-insensitive)', () => {
    const teams = [
      { status: 'PENDING' },
      { status: 'pending' },
      { status: 'APPROVED' },
      { status: 'REJECTED' }
    ]
    expect(countPendingTeams(teams)).toBe('2')
  })

  it('returns "0" for non-array input', () => {
    expect(countPendingTeams(null)).toBe('0')
    expect(countPendingTeams([])).toBe('0')
  })
})

describe('mapAccountRow', () => {
  it('maps snake_case and camelCase fields', () => {
    expect(
      mapAccountRow({
        user_id: '5.0',
        email: 'a@b.com',
        full_name: 'Alice',
        role: 'COORDINATOR',
        status: 'APPROVED'
      })
    ).toEqual({
      userId: '5',
      email: 'a@b.com',
      fullName: 'Alice',
      role: 'COORDINATOR',
      status: 'APPROVED'
    })
  })
})

describe('mapEventRow', () => {
  it('includes pendingTeams from list API payload', () => {
    expect(
      mapEventRow({
        event_id: '1',
        title: 'SEAL 2026',
        pending_teams: '3',
        status: 'BUILDING'
      })
    ).toMatchObject({
      eventId: '1',
      title: 'SEAL 2026',
      pendingTeams: '3',
      status: 'BUILDING'
    })
  })

  it('defaults pendingTeams to "0"', () => {
    expect(mapEventRow({ eventId: '2' }).pendingTeams).toBe('0')
  })
})

describe('mapEventDetailRow', () => {
  it('maps nested teams and rounds', () => {
    const detail = mapEventDetailRow({
      event_id: '10',
      title: 'Hackathon',
      teams: [{ team_id: '1', team_name: 'Alpha', status: 'APPROVED' }],
      rounds: [{ round_id: '2', name: 'Round 1', round_order: 1 }]
    })

    expect(detail.eventId).toBe('10')
    expect(detail.teams).toHaveLength(1)
    expect(detail.teams[0]).toMatchObject({ teamId: '1', teamName: 'Alpha' })
    expect(detail.rounds[0]).toMatchObject({ roundId: '2', name: 'Round 1' })
  })
})

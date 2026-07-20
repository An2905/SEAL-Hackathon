import { describe, expect, it } from 'vitest'
import { isTokenExpired, parseJwt } from './jwt'
import { makeExpiredJwt, makeFutureJwt, makeJwt } from '../test/helpers'

describe('parseJwt', () => {
  it('returns null for empty or malformed tokens', () => {
    expect(parseJwt(null)).toBeNull()
    expect(parseJwt('')).toBeNull()
    expect(parseJwt('not-a-jwt')).toBeNull()
    expect(parseJwt('a.b')).toBeNull()
  })

  it('decodes payload claims', () => {
    const token = makeJwt({ role: 'COORDINATOR', userId: '42', exp: 9999999999 })
    expect(parseJwt(token)).toMatchObject({ role: 'COORDINATOR', userId: '42' })
  })
})

describe('isTokenExpired', () => {
  it('returns true when token is missing or malformed', () => {
    expect(isTokenExpired(null)).toBe(true)
    expect(isTokenExpired('bad')).toBe(true)
  })

  it('returns true when exp is in the past', () => {
    expect(isTokenExpired(makeExpiredJwt({ role: 'STUDENT_FPT' }))).toBe(true)
  })

  it('returns false when exp is in the future', () => {
    expect(isTokenExpired(makeFutureJwt({ role: 'COORDINATOR' }))).toBe(false)
  })

  it('returns true when exp claim is missing', () => {
    expect(isTokenExpired(makeJwt({ role: 'COORDINATOR' }))).toBe(true)
  })
})

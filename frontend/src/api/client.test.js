import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest'
import { apiFetch, parseLoginResponse, parseMessageResponse, resolveAssetUrl } from './client'
import { makeFutureJwt } from '../test/helpers'

describe('resolveAssetUrl', () => {
  it('returns absolute URLs unchanged', () => {
    expect(resolveAssetUrl('https://cdn.example.com/a.png')).toBe('https://cdn.example.com/a.png')
  })

  it('prefixes relative paths with API_BASE', () => {
    expect(resolveAssetUrl('/uploads/x.png')).toMatch(/\/uploads\/x\.png$/)
  })

  it('returns falsy values as-is', () => {
    expect(resolveAssetUrl(null)).toBeNull()
    expect(resolveAssetUrl('')).toBe('')
  })
})

describe('parseMessageResponse', () => {
  it('extracts message from JSON MessageResponse', () => {
    expect(parseMessageResponse('{"message":"Phân công giám khảo thành công."}')).toBe(
      'Phân công giám khảo thành công.'
    )
  })

  it('returns plain text when not JSON', () => {
    expect(parseMessageResponse('OK')).toBe('OK')
    expect(parseMessageResponse('')).toBe('')
  })
})

describe('parseLoginResponse', () => {
  it('parses JSON login response and reads role from JWT', () => {
    const token = makeFutureJwt({ role: 'COORDINATOR', userId: '1' })
    const result = parseLoginResponse(JSON.stringify({ message: 'Login success', token }))
    expect(result).toEqual({ ok: true, token, role: 'COORDINATOR', message: null })
  })

  it('fails when JSON has no token', () => {
    const result = parseLoginResponse(JSON.stringify({ message: 'Invalid credentials' }))
    expect(result.ok).toBe(false)
    expect(result.message).toBe('Invalid credentials')
  })

  it('parses legacy plain-text login response', () => {
    const token = makeFutureJwt({ role: 'STUDENT_FPT' })
    const result = parseLoginResponse(`Login success Token: ${token} Role: STUDENT_FPT`)
    expect(result.ok).toBe(true)
    expect(result.token).toBe(token)
    expect(result.role).toBe('STUDENT_FPT')
  })

  it('fails for non-success legacy text', () => {
    const result = parseLoginResponse('Invalid email or password.')
    expect(result.ok).toBe(false)
  })
})

describe('apiFetch', () => {
  const storage = {}

  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
    vi.stubGlobal('localStorage', {
      getItem: (key) => storage[key] ?? null,
      setItem: (key, value) => {
        storage[key] = value
      },
      removeItem: (key) => {
        delete storage[key]
      }
    })
    delete storage.hh_token
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('throws TOKEN_EXPIRED and dispatches event for expired jwt', async () => {
    const { makeExpiredJwt } = await import('../test/helpers')
    storage.hh_token = makeExpiredJwt({ role: 'COORDINATOR' })

    const dispatched = vi.fn()
    vi.stubGlobal('window', { dispatchEvent: dispatched })

    await expect(apiFetch('/api/test')).rejects.toThrow('TOKEN_EXPIRED')
    expect(dispatched).toHaveBeenCalled()
    expect(fetch).not.toHaveBeenCalled()
  })

  it('returns response text on success', async () => {
    storage.hh_token = makeFutureJwt({ role: 'COORDINATOR' })
    fetch.mockResolvedValue({
      ok: true,
      text: async () => '{"message":"ok"}'
    })

    const text = await apiFetch('/api/staff/events')
    expect(text).toBe('{"message":"ok"}')
    expect(fetch).toHaveBeenCalledOnce()
  })

  it('parses JSON error message from ErrorResponse', async () => {
    fetch.mockResolvedValue({
      ok: false,
      status: 400,
      text: async () => JSON.stringify({ message: 'Event not found.' })
    })

    await expect(apiFetch('/api/x', { auth: false })).rejects.toThrow('Event not found.')
  })

  it('throws NETWORK when fetch fails', async () => {
    fetch.mockRejectedValue(new Error('offline'))
    await expect(apiFetch('/api/x', { auth: false })).rejects.toThrow('NETWORK')
  })
})

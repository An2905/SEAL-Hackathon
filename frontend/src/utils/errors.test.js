import { describe, expect, it } from 'vitest'
import { localizeError } from './errors'

describe('localizeError', () => {
  it('returns default message for empty input', () => {
    expect(localizeError()).toBe('Đã xảy ra lỗi không xác định.')
    expect(localizeError('')).toBe('Đã xảy ra lỗi không xác định.')
  })

  it('maps known error codes to Vietnamese', () => {
    expect(localizeError('NETWORK')).toContain('kết nối')
    expect(localizeError('TOKEN_EXPIRED')).toContain('hết hạn')
    expect(localizeError('Invalid captcha.')).toContain('Captcha')
  })

  it('hides technical SQL/stack trace messages', () => {
    expect(localizeError('SELECT * FROM users WHERE id = 1')).toBe('Lỗi máy chủ. Vui lòng thử lại sau.')
    expect(localizeError('java.lang.NullPointerException at com.example.Foo.java:12')).toBe(
      'Lỗi máy chủ. Vui lòng thử lại sau.'
    )
  })

  it('passes through Vietnamese server messages', () => {
    expect(localizeError('Phân công giám khảo thành công.')).toBe('Phân công giám khảo thành công.')
  })

  it('maps bare HTTP status codes', () => {
    expect(localizeError('404')).toBe('Yêu cầu không hợp lệ.')
    expect(localizeError('500')).toBe('Lỗi máy chủ. Vui lòng thử lại sau.')
  })

  it('passes through short English messages from server', () => {
    expect(localizeError('Invalid email or password.')).toBe('Invalid email or password.')
  })
})

import { describe, expect, it } from 'vitest'
import { isValidEmail } from './formValidation'

describe('form validation', () => {
  it('accepts valid email addresses and rejects malformed ones', () => {
    expect(isValidEmail('student@fpt.edu.vn')).toBe(true)
    expect(isValidEmail('invalid-email')).toBe(false)
    expect(isValidEmail('')).toBe(false)
  })
})

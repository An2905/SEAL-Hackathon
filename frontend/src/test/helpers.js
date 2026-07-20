/** Build a minimal JWT-shaped string for unit tests (signature not verified). */
export function makeJwt(payload) {
  const encode = (obj) =>
    btoa(JSON.stringify(obj))
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=+$/, '')
  return `${encode({ alg: 'HS256', typ: 'JWT' })}.${encode(payload)}.test-signature`
}

export function makeFutureJwt(claims = {}) {
  return makeJwt({
    ...claims,
    exp: Math.floor(Date.now() / 1000) + 3600
  })
}

export function makeExpiredJwt(claims = {}) {
  return makeJwt({
    ...claims,
    exp: Math.floor(Date.now() / 1000) - 60
  })
}

export const RECAPTCHA_SITE_KEY = import.meta.env.VITE_RECAPTCHA_SITE_KEY || ''

let loadPromise = null

export function loadRecaptchaScript() {
  if (!RECAPTCHA_SITE_KEY) {
    return Promise.reject(new Error('RECAPTCHA_NOT_CONFIGURED'))
  }
  if (window.grecaptcha?.render) {
    return Promise.resolve()
  }
  if (loadPromise) return loadPromise

  loadPromise = new Promise((resolve, reject) => {
    const existing = document.querySelector('script[data-recaptcha]')
    if (existing) {
      existing.addEventListener('load', () => window.grecaptcha.ready(resolve), { once: true })
      existing.addEventListener('error', reject, { once: true })
      return
    }

    const script = document.createElement('script')
    script.src = 'https://www.google.com/recaptcha/api.js?render=explicit'
    script.async = true
    script.defer = true
    script.dataset.recaptcha = '1'
    script.onload = () => window.grecaptcha.ready(resolve)
    script.onerror = () => reject(new Error('RECAPTCHA_LOAD_FAILED'))
    document.head.appendChild(script)
  })

  return loadPromise
}

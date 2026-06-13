import { forwardRef, useEffect, useImperativeHandle, useRef, useState } from 'react'
import { loadRecaptchaScript, RECAPTCHA_SITE_KEY } from '../../utils/recaptcha'

const RecaptchaWidget = forwardRef(function RecaptchaWidget(_props, ref) {
  const containerRef = useRef(null)
  const widgetIdRef = useRef(null)
  const [loadError, setLoadError] = useState(null)

  useImperativeHandle(ref, () => ({
    getToken() {
      if (widgetIdRef.current == null || !window.grecaptcha) return null
      return window.grecaptcha.getResponse(widgetIdRef.current) || null
    },
    reset() {
      if (widgetIdRef.current != null && window.grecaptcha) {
        window.grecaptcha.reset(widgetIdRef.current)
      }
    }
  }))

  useEffect(() => {
    if (!RECAPTCHA_SITE_KEY) {
      setLoadError('RECAPTCHA_NOT_CONFIGURED')
      return
    }

    let cancelled = false

    loadRecaptchaScript()
      .then(() => {
        if (cancelled || !containerRef.current || widgetIdRef.current != null) return
        widgetIdRef.current = window.grecaptcha.render(containerRef.current, {
          sitekey: RECAPTCHA_SITE_KEY
        })
      })
      .catch((err) => {
        if (!cancelled) setLoadError(err.message || 'RECAPTCHA_LOAD_FAILED')
      })

    return () => {
      cancelled = true
      widgetIdRef.current = null
    }
  }, [])

  if (!RECAPTCHA_SITE_KEY || loadError === 'RECAPTCHA_NOT_CONFIGURED') {
    return <p className='form-message error'>Captcha chưa được cấu hình (VITE_RECAPTCHA_SITE_KEY).</p>
  }

  if (loadError) {
    return <p className='form-message error'>Không tải được captcha. Vui lòng tải lại trang.</p>
  }

  return <div ref={containerRef} className='recaptcha-wrap' />
})

export default RecaptchaWidget

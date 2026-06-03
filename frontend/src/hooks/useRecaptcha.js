import { useCallback, useRef, createElement } from 'react'
import RecaptchaWidget from '../components/common/RecaptchaWidget'

export function useRecaptcha() {
  const widgetRef = useRef(null)

  const getCaptchaToken = useCallback(async () => {
    const token = widgetRef.current?.getToken?.()
    if (!token) {
      throw new Error('Vui lòng xác nhận captcha trước khi tiếp tục.')
    }
    return token
  }, [])

  const resetCaptcha = useCallback(() => {
    widgetRef.current?.reset?.()
  }, [])

  const RecaptchaField = useCallback(
    () => createElement(RecaptchaWidget, { ref: widgetRef }),
    []
  )

  return { getCaptchaToken, resetCaptcha, RecaptchaField }
}

import { useEffect, useState } from 'react'
import Modal from './Modal'
import FormField from './FormField'
import FormMessage from './FormMessage'
import LoadingButton from './LoadingButton'
import { sendResetPasswordOtp, verifyAndResetPassword } from '../../api/auth'
import { useToast } from '../../context/ToastContext'
import { localizeError } from '../../utils/errors'
import { isValidEmail } from '../../utils/formValidation'

export default function ResetPasswordModal({ isOpen, onClose, onSwitchToLogin }) {
  const { showToast } = useToast()
  const [step, setStep] = useState('email')
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState(null)
  const [email, setEmail] = useState('')
  const [otp, setOtp] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')

  useEffect(() => {
    if (!isOpen) {
      setStep('email')
      setMessage(null)
      setEmail('')
      setOtp('')
      setNewPassword('')
      setConfirmPassword('')
      setLoading(false)
    }
  }, [isOpen])

  const handleSendOtp = async (e) => {
    e.preventDefault()
    setMessage(null)
    const trimmed = email.trim()
    if (!trimmed) {
      setMessage({ text: 'Vui lòng nhập email', type: 'error' })
      return
    }
    if (!isValidEmail(trimmed)) {
      setMessage({ text: 'Email không đúng định dạng', type: 'error' })
      return
    }
    setLoading(true)
    try {
      await sendResetPasswordOtp({ email: trimmed })
      setEmail(trimmed)
      setStep('reset')
      setMessage({
        text: 'Đã gửi mã OTP tới email. Vui lòng kiểm tra hộp thư.',
        type: 'success'
      })
    } catch (err) {
      setMessage({ text: localizeError(err.message), type: 'error' })
    } finally {
      setLoading(false)
    }
  }

  const handleResetPassword = async (e) => {
    e.preventDefault()
    setMessage(null)
    const code = otp.trim()
    if (!/^\d{6}$/.test(code)) {
      setMessage({ text: 'Mã OTP gồm 6 chữ số', type: 'error' })
      return
    }
    if (newPassword.length < 6) {
      setMessage({
        text: 'Mật khẩu mới phải có ít nhất 6 ký tự',
        type: 'error'
      })
      return
    }
    if (newPassword !== confirmPassword) {
      setMessage({
        text: 'Mật khẩu mới và xác nhận không khớp',
        type: 'error'
      })
      return
    }
    setLoading(true)
    try {
      await verifyAndResetPassword({ email, otp: code, newPassword })
      setMessage({
        text: 'Đặt lại mật khẩu thành công! Hãy đăng nhập với mật khẩu mới.',
        type: 'success'
      })
      showToast('Đặt lại mật khẩu thành công!', 'success')
      setTimeout(() => {
        onClose()
        onSwitchToLogin?.()
      }, 800)
    } catch (err) {
      setMessage({ text: localizeError(err.message), type: 'error' })
    } finally {
      setLoading(false)
    }
  }

  const handleResend = async () => {
    setMessage(null)
    setLoading(true)
    try {
      await sendResetPasswordOtp({ email })
      setMessage({ text: 'Đã gửi lại mã OTP.', type: 'success' })
    } catch (err) {
      setMessage({ text: localizeError(err.message), type: 'error' })
    } finally {
      setLoading(false)
    }
  }

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title='Quên mật khẩu'
      subtitle={
        step === 'email'
          ? 'Nhập email để nhận mã OTP đặt lại mật khẩu.'
          : `Nhập mã OTP đã gửi tới ${email} và mật khẩu mới.`
      }
    >
      {step === 'email' ? (
        <form className='form' onSubmit={handleSendOtp} noValidate>
          <FormField label='Email'>
            <input
              type='email'
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              placeholder='ban@fpt.edu.vn'
              autoComplete='email'
              autoFocus
            />
          </FormField>
          <LoadingButton loading={loading} type='submit'>
            Gửi mã OTP
          </LoadingButton>
          <FormMessage message={message?.text} type={message?.type} />
          <p className='form-footer'>
            Nhớ mật khẩu rồi?{' '}
            <a
              href='#'
              onClick={(e) => {
                e.preventDefault()
                onSwitchToLogin?.()
              }}
            >
              Đăng nhập
            </a>
          </p>
        </form>
      ) : (
        <form className='form' onSubmit={handleResetPassword} noValidate>
          <FormField label='Mã OTP (6 chữ số)'>
            <input
              type='text'
              inputMode='numeric'
              pattern='\d{6}'
              maxLength={6}
              value={otp}
              onChange={(e) => setOtp(e.target.value.replace(/\D/g, '').slice(0, 6))}
              required
              placeholder='000000'
              autoFocus
            />
          </FormField>
          <FormField label='Mật khẩu mới'>
            <input
              type='password'
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              required
              minLength={6}
              placeholder='Tối thiểu 6 ký tự'
              autoComplete='new-password'
            />
          </FormField>
          <FormField label='Xác nhận mật khẩu mới'>
            <input
              type='password'
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              required
              minLength={6}
              placeholder='Nhập lại mật khẩu mới'
              autoComplete='new-password'
            />
          </FormField>
          <LoadingButton loading={loading} type='submit'>
            Đặt lại mật khẩu
          </LoadingButton>
          <FormMessage message={message?.text} type={message?.type} />
          <p className='form-footer'>
            Không nhận được mã?{' '}
            <a
              href='#'
              onClick={(e) => {
                e.preventDefault()
                if (!loading) handleResend()
              }}
            >
              Gửi lại OTP
            </a>
            {' · '}
            <a
              href='#'
              onClick={(e) => {
                e.preventDefault()
                setStep('email')
                setMessage(null)
                setOtp('')
                setNewPassword('')
                setConfirmPassword('')
              }}
            >
              Đổi email
            </a>
          </p>
        </form>
      )}
    </Modal>
  )
}

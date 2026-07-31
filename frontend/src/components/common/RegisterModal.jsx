import { useEffect, useState } from 'react'
import Modal from '../common/Modal'
import FormField from '../common/FormField'
import FormMessage from '../common/FormMessage'
import LoadingButton from '../common/LoadingButton'
import { sendRegisterOtp, verifyAndRegister } from '../../api/auth'
import { getAllUniversities } from '../../api/university'
import { useToast } from '../../context/ToastContext'
import { localizeError } from '../../utils/errors'
import { useRecaptcha } from '../../hooks/useRecaptcha'
import { isValidEmail } from '../../utils/formValidation'

const EMPTY_FORM = {
  fullName: '',
  email: '',
  university: '',
  studentId: '',
  password: ''
}

export default function RegisterModal({ isOpen, onClose, onSwitchToLogin }) {
  const { showToast } = useToast()
  const {
    getCaptchaToken: getInfoCaptcha,
    resetCaptcha: resetInfoCaptcha,
    RecaptchaField: InfoRecaptchaField
  } = useRecaptcha()
  const {
    getCaptchaToken: getResendCaptcha,
    resetCaptcha: resetResendCaptcha,
    RecaptchaField: ResendRecaptchaField
  } = useRecaptcha()
  const [step, setStep] = useState('info')
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState(null)
  const [form, setForm] = useState(EMPTY_FORM)
  const [otp, setOtp] = useState('')
  const [universities, setUniversities] = useState([])
  const [uniLoading, setUniLoading] = useState(false)

  useEffect(() => {
    if (!isOpen) {
      setStep('info')
      setMessage(null)
      setForm(EMPTY_FORM)
      setOtp('')
      setLoading(false)
      return
    }

    let cancelled = false
    setUniLoading(true)
    getAllUniversities()
      .then((list) => {
        if (!cancelled) setUniversities(list)
      })
      .catch(() => {
        if (!cancelled) setUniversities([])
      })
      .finally(() => {
        if (!cancelled) setUniLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [isOpen])

  const handleChange = (e) => setForm((f) => ({ ...f, [e.target.name]: e.target.value }))

  const handleSubmitInfo = async (e) => {
    e.preventDefault()
    setMessage(null)
    const fullName = form.fullName.trim()
    const email = form.email.trim()
    const university = form.university.trim()
    const studentId = form.studentId.trim()
    const { password } = form
    if (!fullName || !email || !university || !studentId || !password) {
      setMessage({ text: 'Vui lòng nhập đầy đủ thông tin', type: 'error' })
      return
    }
    if (!isValidEmail(email)) {
      setMessage({ text: 'Email không đúng định dạng', type: 'error' })
      return
    }
    if (password.length < 6) {
      setMessage({ text: 'Mật khẩu phải có ít nhất 6 ký tự', type: 'error' })
      return
    }
    setLoading(true)
    try {
      const captchaToken = await getInfoCaptcha()
      await sendRegisterOtp({ ...form, fullName, email, university, studentId, captchaToken })
      setStep('otp')
      setMessage({
        text: 'Đã gửi mã OTP tới email. Vui lòng kiểm tra hộp thư.',
        type: 'success'
      })
    } catch (err) {
      setMessage({ text: localizeError(err.message), type: 'error' })
    } finally {
      resetInfoCaptcha()
      setLoading(false)
    }
  }

  const handleSubmitOtp = async (e) => {
    e.preventDefault()
    setMessage(null)
    const code = otp.trim()
    if (!/^\d{6}$/.test(code)) {
      setMessage({ text: 'Mã OTP gồm 6 chữ số', type: 'error' })
      return
    }
    setLoading(true)
    try {
      await verifyAndRegister({ email: form.email, otp: code })
      setMessage({
        text: 'Tạo tài khoản thành công! Vui lòng đăng nhập.',
        type: 'success'
      })
      showToast('Tạo tài khoản thành công!', 'success')
      setTimeout(() => {
        onClose()
        onSwitchToLogin()
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
      const captchaToken = await getResendCaptcha()
      await sendRegisterOtp({ ...form, captchaToken })
      setMessage({ text: 'Đã gửi lại mã OTP.', type: 'success' })
    } catch (err) {
      setMessage({ text: localizeError(err.message), type: 'error' })
    } finally {
      resetResendCaptcha()
      setLoading(false)
    }
  }

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={step === 'info' ? 'Tạo tài khoản' : 'Xác thực email'}
      subtitle={step === 'info' ? 'Tham gia SEAL Hackathon ngay hôm nay.' : `Nhập mã OTP đã gửi tới ${form.email}.`}
    >
      {step === 'info' ? (
        <form className='form' onSubmit={handleSubmitInfo} noValidate>
          <FormField label='Họ và tên'>
            <input
              type='text'
              name='fullName'
              value={form.fullName}
              onChange={handleChange}
              required
              placeholder='Nguyễn Văn A'
            />
          </FormField>
          <FormField label='Email'>
            <input
              type='email'
              name='email'
              value={form.email}
              onChange={handleChange}
              required
              placeholder='ban@fpt.edu.vn'
              autoComplete='email'
            />
          </FormField>
          <div className='field-row'>
            <FormField label='Trường'>
              <select
                name='university'
                value={form.university}
                onChange={handleChange}
                required
                disabled={uniLoading || universities.length === 0}
              >
                <option value='' disabled>
                  {uniLoading
                    ? 'Đang tải danh sách trường...'
                    : universities.length === 0
                      ? 'Không có trường nào'
                      : '-- Chọn trường --'}
                </option>
                {universities.map((u) => (
                  <option key={u.universityId} value={u.universityName}>
                    {u.universityName}
                  </option>
                ))}
              </select>
            </FormField>
            <FormField label='Mã sinh viên'>
              <input
                type='text'
                name='studentId'
                value={form.studentId}
                onChange={handleChange}
                required
                placeholder='SE123456'
              />
            </FormField>
          </div>
          <FormField label='Mật khẩu'>
            <input
              type='password'
              name='password'
              value={form.password}
              onChange={handleChange}
              required
              minLength={6}
              placeholder='Tối thiểu 6 ký tự'
              autoComplete='new-password'
            />
          </FormField>
          <InfoRecaptchaField />
          <LoadingButton loading={loading} type='submit'>
            Gửi mã OTP
          </LoadingButton>
          <FormMessage message={message?.text} type={message?.type} />
          <p className='form-footer'>
            Đã có tài khoản?{' '}
            <a
              href='#'
              onClick={(e) => {
                e.preventDefault()
                onSwitchToLogin()
              }}
            >
              Đăng nhập
            </a>
          </p>
        </form>
      ) : (
        <form className='form' onSubmit={handleSubmitOtp} noValidate>
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
          <LoadingButton loading={loading} type='submit'>
            Xác thực &amp; tạo tài khoản
          </LoadingButton>
          <FormMessage message={message?.text} type={message?.type} />
          <ResendRecaptchaField />
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
                setStep('info')
                setMessage(null)
              }}
            >
              Sửa thông tin
            </a>
          </p>
        </form>
      )}
    </Modal>
  )
}

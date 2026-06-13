import { useState } from 'react'
import Modal from '../common/Modal'
import FormField from '../common/FormField'
import FormMessage from '../common/FormMessage'
import LoadingButton from '../common/LoadingButton'
import { login } from '../../api/auth'
import { useAuth } from '../../context/AuthContext'
import { useToast } from '../../context/ToastContext'
import { useNavigate } from 'react-router-dom'
import { localizeError } from '../../utils/errors'
import { useRecaptcha } from '../../hooks/useRecaptcha'

export default function LoginModal({ isOpen, onClose, onSwitchToRegister, onSwitchToReset }) {
  const { saveAuth, pathForRole } = useAuth()
  const { showToast } = useToast()
  const navigate = useNavigate()
  const { getCaptchaToken, resetCaptcha, RecaptchaField } = useRecaptcha()
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState(null)
  const [form, setForm] = useState({ email: '', password: '' })

  const handleChange = (e) => setForm(f => ({ ...f, [e.target.name]: e.target.value }))

  const handleSubmit = async (e) => {
    e.preventDefault()
    setMessage(null)
    if (!form.email || !form.password) {
      setMessage({ text: 'Vui lòng nhập đầy đủ email và mật khẩu', type: 'error' })
      return
    }
    setLoading(true)
    try {
      const captchaToken = await getCaptchaToken()
      const result = await login({ ...form, captchaToken })
      if (!result.ok) {
        setMessage({ text: localizeError(result.message), type: 'error' })
        return
      }
      saveAuth({ token: result.token, email: form.email, role: result.role, avatarUrl: result.avatarUrl })
      setMessage({ text: 'Đăng nhập thành công, đang chuyển hướng...', type: 'success' })
      const displayName = localStorage.getItem('hh_full_name') || form.email
      showToast(`Chào mừng ${displayName}!`, 'success')
      setTimeout(() => {
        onClose()
        navigate(pathForRole(result.role))
      }, 500)
    } catch (err) {
      setMessage({ text: localizeError(err.message), type: 'error' })
    } finally {
      resetCaptcha()
      setLoading(false)
    }
  }

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Đăng nhập" subtitle="Chào mừng quay lại! Hãy đăng nhập để tiếp tục.">
      <form className="form" onSubmit={handleSubmit} noValidate>
        <FormField label="Email">
          <input type="email" name="email" value={form.email} onChange={handleChange}
            required placeholder="ban@fpt.edu.vn" autoComplete="email" />
        </FormField>
        <FormField label="Mật khẩu">
          <input type="password" name="password" value={form.password} onChange={handleChange}
            required placeholder="••••••••" autoComplete="current-password" />
        </FormField>
        <RecaptchaField />
        <LoadingButton loading={loading} type="submit">Đăng nhập</LoadingButton>
        <FormMessage message={message?.text} type={message?.type} />
        <p className="form-footer">
          <a href="#" onClick={(e) => { e.preventDefault(); onSwitchToReset?.() }}>Quên mật khẩu?</a>
        </p>
        <p className="form-footer">
          Chưa có tài khoản?{' '}
          <a href="#" onClick={(e) => { e.preventDefault(); onSwitchToRegister() }}>Đăng ký ngay</a>
        </p>
      </form>
    </Modal>
  )
}

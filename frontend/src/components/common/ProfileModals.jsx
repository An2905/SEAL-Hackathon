import { useState } from 'react'
import Modal from './Modal'
import FormField from './FormField'
import FormMessage from './FormMessage'
import LoadingButton from './LoadingButton'
import { updateProfile, updatePassword } from '../../api/auth'
import { useAuth } from '../../context/AuthContext'
import { useToast } from '../../context/ToastContext'
import { localizeError } from '../../utils/errors'

export function ProfileModal({ isOpen, onClose, showStudentFields = false, showStaffFields = false }) {
  const { auth, saveAuth } = useAuth()
  const { showToast } = useToast()
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState(null)
  const [form, setForm] = useState({
    fullName: auth.fullName || '',
    email: auth.email,
    university: '',
    studentId: '',
    phone: '',
    avatarUrl: ''
  })

  const handleChange = (e) => setForm((f) => ({ ...f, [e.target.name]: e.target.value }))

  const handleSubmit = async (e) => {
    e.preventDefault()
    setMessage(null)
    setLoading(true)
    try {
      const { newToken } = await updateProfile(form)
      // saveAuth auto-decodes fullName/email from new token; pass email/fullName as fallbacks
      saveAuth({
        ...(newToken ? { token: newToken } : {}),
        fullName: form.fullName
      })
      setMessage({ text: 'Cập nhật hồ sơ thành công!', type: 'success' })
      showToast('Đã cập nhật hồ sơ', 'success')
      setTimeout(onClose, 600)
    } catch (err) {
      setMessage({ text: localizeError(err.message), type: 'error' })
    } finally {
      setLoading(false)
    }
  }

  return (
    <Modal isOpen={isOpen} onClose={onClose} title='Cập nhật hồ sơ' subtitle='Cập nhật thông tin cá nhân của bạn.'>
      <form className='form' onSubmit={handleSubmit}>
        <FormField label='Họ và tên'>
          <input name='fullName' value={form.fullName} onChange={handleChange} required placeholder='Nguyễn Văn A' />
        </FormField>
        <FormField label='Email — để trống nếu giữ nguyên'>
          <input
            name='email'
            type='email'
            value={form.email}
            onChange={handleChange}
            placeholder={auth.email || 'email@example.com'}
          />
        </FormField>
        {showStudentFields && (
          <div className='field-row'>
            <FormField label='Trường'>
              <input name='university' value={form.university} onChange={handleChange} />
            </FormField>
            <FormField label='Mã sinh viên'>
              <input name='studentId' value={form.studentId} onChange={handleChange} />
            </FormField>
          </div>
        )}
        {showStaffFields && (
          <>
            <FormField label='Số điện thoại'>
              <input
                name='phone'
                type='tel'
                value={form.phone}
                onChange={handleChange}
                required
                placeholder='09xxxxxxxx'
                autoComplete='tel'
              />
            </FormField>
            <FormField label='Ảnh đại diện (URL) — tùy chọn'>
              <input
                name='avatarUrl'
                type='text'
                value={form.avatarUrl}
                onChange={handleChange}
                placeholder='Để trống nếu không dùng'
              />
            </FormField>
          </>
        )}
        <LoadingButton loading={loading} type='submit'>
          Cập nhật
        </LoadingButton>
        <FormMessage message={message?.text} type={message?.type} />
      </form>
    </Modal>
  )
}

export function PasswordModal({ isOpen, onClose }) {
  const { showToast } = useToast()
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState(null)
  const [form, setForm] = useState({
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  })

  const handleChange = (e) => setForm((f) => ({ ...f, [e.target.name]: e.target.value }))

  const handleSubmit = async (e) => {
    e.preventDefault()
    setMessage(null)
    if (form.newPassword !== form.confirmPassword) {
      setMessage({
        text: 'Mật khẩu mới và xác nhận không khớp',
        type: 'error'
      })
      return
    }
    setLoading(true)
    try {
      await updatePassword(form)
      setMessage({ text: 'Đổi mật khẩu thành công!', type: 'success' })
      showToast('Đã đổi mật khẩu', 'success')
      setTimeout(() => {
        onClose()
        setForm({ oldPassword: '', newPassword: '', confirmPassword: '' })
      }, 600)
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
      title='Đổi mật khẩu'
      subtitle='Để bảo mật, bạn cần nhập mật khẩu hiện tại.'
    >
      <form className='form' onSubmit={handleSubmit}>
        <FormField label='Mật khẩu cũ'>
          <input name='oldPassword' type='password' value={form.oldPassword} onChange={handleChange} required />
        </FormField>
        <FormField label='Mật khẩu mới'>
          <input
            name='newPassword'
            type='password'
            value={form.newPassword}
            onChange={handleChange}
            required
            minLength={6}
          />
        </FormField>
        <FormField label='Xác nhận mật khẩu mới'>
          <input
            name='confirmPassword'
            type='password'
            value={form.confirmPassword}
            onChange={handleChange}
            required
            minLength={6}
          />
        </FormField>
        <LoadingButton loading={loading} type='submit'>
          Cập nhật
        </LoadingButton>
        <FormMessage message={message?.text} type={message?.type} />
      </form>
    </Modal>
  )
}

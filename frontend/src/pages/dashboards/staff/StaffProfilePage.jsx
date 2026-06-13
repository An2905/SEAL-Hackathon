import { useState } from 'react'
import { useAuth } from '../../../context/AuthContext'
import { useToast } from '../../../context/ToastContext'
import DashboardLayout from '../../../components/layout/DashboardLayout'
import Avatar from '../../../components/common/Avatar'
import { ProfileModal, PasswordModal } from '../../../components/common/ProfileModals'
import { vietnameseRoleLabel, STUDENT_ROLES } from '../../../utils/roleLabels'
import { uploadAvatar } from '../../../api/auth'
import { localizeError } from '../../../utils/errors'

export default function StaffProfilePage() {
  const { auth, saveAuth } = useAuth()
  const { showToast } = useToast()
  const [profileOpen, setProfileOpen] = useState(false)
  const [passwordOpen, setPasswordOpen] = useState(false)
  const [avatarUploading, setAvatarUploading] = useState(false)

  const isStudent = STUDENT_ROLES.includes(auth.role)
  const roleLabel = vietnameseRoleLabel(auth.role)
  const displayName = auth.fullName || auth.email || '—'

  const handleAvatarChange = async (e) => {
    const file = e.target.files?.[0]
    e.target.value = ''
    if (!file) return

    setAvatarUploading(true)
    try {
      const avatarUrl = await uploadAvatar(file)
      saveAuth({ avatarUrl })
      showToast('Đã cập nhật ảnh đại diện', 'success')
    } catch (err) {
      showToast(localizeError(err.message), 'error')
    } finally {
      setAvatarUploading(false)
    }
  }

  return (
    <DashboardLayout
      roleLabel={roleLabel}
      moduleTitle='Hồ sơ của tôi'
      moduleSubtitle='Quản lý thông tin tài khoản và bảo mật'
      showStudentFields={isStudent}
      showStaffFields={!isStudent}
    >
      {/* ── Identity card ── */}
      <div style={sectionStyle}>
        <div style={sectionHeaderStyle}>
          <span style={sectionTitleStyle}>Thông tin cá nhân</span>
          <button
            type='button'
            className='btn btn-outline'
            style={{ fontSize: 12, padding: '5px 12px' }}
            onClick={() => setProfileOpen(true)}
          >
            Chỉnh sửa hồ sơ
          </button>
        </div>

        <div style={avatarRowStyle}>
          <label className={`avatar-upload${avatarUploading ? ' is-uploading' : ''}`} title='Đổi ảnh đại diện'>
            <Avatar name={displayName} avatarUrl={auth.avatarUrl} size={52} fontSize={20} />
            <span className='avatar-upload-overlay'>
              {avatarUploading ? <span className='spinner' /> : <IconCamera />}
            </span>
            <input
              type='file'
              accept='image/png,image/jpeg,image/gif'
              onChange={handleAvatarChange}
              disabled={avatarUploading}
            />
          </label>
          <div>
            <div style={{ fontSize: 16, fontWeight: 600, color: 'var(--text,#1a202c)' }}>{displayName}</div>
            <span style={rolePillStyle}>{roleLabel}</span>
            <div style={{ fontSize: 13, color: 'var(--text-dim,#718096)', marginTop: 4 }}>{auth.email}</div>
          </div>
        </div>
      </div>

      {/* ── Contact info & security ── */}
      <div className='cards'>
        <div style={sectionStyle}>
          <div style={sectionHeaderStyle}>
            <span style={sectionTitleStyle}>Thông tin liên hệ</span>
          </div>
          <div style={{ padding: '4px 0' }}>
            <KvRow label='Email' value={auth.email || '—'} />
            {isStudent ? (
              <>
                <KvRow label='Trường' value={auth.university || '—'} />
                <KvRow label='Mã sinh viên' value={auth.studentId || '—'} last />
              </>
            ) : (
              <>
                <KvRow label='Số điện thoại' value={auth.phone || '—'} />
                <KvRow label='Trường' value={auth.university || '—'} />
                <KvRow label='Khoa / Phòng' value={auth.department || '—'} last />
              </>
            )}
          </div>
        </div>

        <div style={sectionStyle}>
          <div style={sectionHeaderStyle}>
            <span style={sectionTitleStyle}>Bảo mật</span>
          </div>
          <div style={{ padding: '16px 18px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <div>
              <div style={{ fontSize: 13, fontWeight: 500, color: 'var(--text,#1a202c)' }}>Mật khẩu</div>
              <div style={{ fontSize: 12, color: 'var(--text-dim,#718096)', marginTop: 2 }}>
                Thay đổi mật khẩu đăng nhập
              </div>
            </div>
            <button
              type='button'
              className='btn btn-outline'
              style={{ fontSize: 12, padding: '5px 12px' }}
              onClick={() => setPasswordOpen(true)}
            >
              Đổi mật khẩu
            </button>
          </div>
        </div>
      </div>

      {/* ── Modals ── */}
      <ProfileModal
        isOpen={profileOpen}
        onClose={() => setProfileOpen(false)}
        showStudentFields={isStudent}
        showStaffFields={!isStudent}
      />
      <PasswordModal
        isOpen={passwordOpen}
        onClose={() => setPasswordOpen(false)}
      />
    </DashboardLayout>
  )
}

function IconCamera() {
  return (
    <svg width='18' height='18' viewBox='0 0 24 24' fill='none' stroke='currentColor' strokeWidth='1.8' strokeLinecap='round' strokeLinejoin='round'>
      <path d='M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z' />
      <circle cx='12' cy='13' r='4' />
    </svg>
  )
}

function KvRow({ label, value, last }) {
  return (
    <div style={{
      display: 'flex', alignItems: 'baseline', justifyContent: 'space-between',
      padding: '9px 18px',
      borderBottom: last ? 'none' : '1px solid var(--border-soft,#f5f5f5)',
      gap: 16,
    }}>
      <span style={{ fontSize: 12, color: 'var(--text-dim,#718096)', minWidth: 120, flexShrink: 0 }}>{label}</span>
      <span style={{ fontSize: 13, fontWeight: 500, color: 'var(--text,#1a202c)', textAlign: 'right' }}>{value}</span>
    </div>
  )
}

// ─── Styles ───────────────────────────────────────────────────────────────────

const sectionStyle = {
  background: 'var(--card-bg,#fff)',
  border: '1px solid var(--border,#e2e8f0)',
  borderRadius: 10, overflow: 'hidden', marginBottom: 16,
}

const sectionHeaderStyle = {
  display: 'flex', alignItems: 'center', justifyContent: 'space-between',
  padding: '12px 18px',
  borderBottom: '1px solid var(--border,#e2e8f0)',
  background: 'var(--surface-alt,#f7f8fa)',
}

const sectionTitleStyle = { fontSize: 13, fontWeight: 600, color: 'var(--text,#1a202c)' }

const avatarRowStyle = {
  display: 'flex', alignItems: 'center', gap: 16,
  padding: '16px 18px',
}

const rolePillStyle = {
  display: 'inline-flex', alignItems: 'center',
  marginTop: 4,
  padding: '2px 10px',
  borderRadius: 20,
  fontSize: 12, fontWeight: 500,
  background: 'var(--accent-soft,#dbeafe)', color: 'var(--accent,#2563eb)',
}

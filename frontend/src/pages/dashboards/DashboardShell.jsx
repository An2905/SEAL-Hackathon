import { useState } from 'react'
import DashboardNavbar from '../../components/layout/DashboardNavbar'
import SiteFooter from '../../components/layout/SiteFooter'
import { ProfileModal, PasswordModal } from '../../components/common/ProfileModals'
import { useAuth } from '../../context/AuthContext'

export default function DashboardShell({ roleLabel, title, subtitle, role, showStudentFields = false, children }) {
  const { auth } = useAuth()
  const [profileOpen, setProfileOpen] = useState(false)
  const [passwordOpen, setPasswordOpen] = useState(false)

  return (
    <>
      <DashboardNavbar roleLabel={roleLabel} />

      <main className="dashboard">
        <div className="welcome-banner">
          <h2>Xin chào, <span>{auth.email}</span>!</h2>
          <p>{subtitle}</p>
        </div>

        <div className="dashboard-header">
          <div className="dashboard-title">
            <h1>{title}</h1>
            <p>Vai trò: <strong>{role}</strong></p>
          </div>
          <div className="action-row">
            <button className="btn btn-outline" onClick={() => setProfileOpen(true)}>Cập nhật hồ sơ</button>
            <button className="btn btn-outline" onClick={() => setPasswordOpen(true)}>Đổi mật khẩu</button>
          </div>
        </div>

        {children}
      </main>

      <SiteFooter />

      <ProfileModal isOpen={profileOpen} onClose={() => setProfileOpen(false)} showStudentFields={showStudentFields} />
      <PasswordModal isOpen={passwordOpen} onClose={() => setPasswordOpen(false)} />
    </>
  )
}

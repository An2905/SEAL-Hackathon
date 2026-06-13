import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { useToast } from '../../context/ToastContext'
import fptLogo from '../../assets/images/fpt-logo.png'

export default function DashboardNavbar({ roleLabel }) {
  const { auth, clearAuth } = useAuth()
  const { showToast } = useToast()
  const navigate = useNavigate()

  const handleLogout = () => {
    clearAuth()
    showToast('Đã đăng xuất', 'success')
    navigate('/')
  }

  const displayName = auth.fullName || auth.email
  const initial = (displayName?.[0] || 'U').toUpperCase()
  const pillClass =
    {
      COORDINATOR: 'staff',
      EXPERT_INTERNAL: 'guest',
      EXPERT_EXTERNAL: 'guest',
      STUDENT_FPT: 'student',
      STUDENT_EXTERNAL: 'student'
    }[auth.role] || 'guest'

  return (
    <nav className='navbar'>
      <div className='nav-container'>
        <a onClick={() => navigate('/')} className='brand' style={{ cursor: 'pointer' }}>
          <img src={fptLogo} alt='FPT University' className='brand-logo' />
          <span className='brand-divider' />
          <span className='brand-text'>
            <strong>SEAL Hackathon</strong>
            <small>Spring 2026</small>
          </span>
        </a>

        <div className='nav-user'>
          <span className={`role-pill role-${pillClass}`}>{roleLabel}</span>
          <div className='user-chip'>
            <div className='avatar'>{initial}</div>
            <div className='user-meta'>
              <span className='user-email'>{displayName}</span>
            </div>
          </div>
          <button className='btn btn-ghost' onClick={handleLogout}>
            Đăng xuất
          </button>
        </div>
      </div>
    </nav>
  )
}

import { useAuth } from '../../context/AuthContext'
import { useToast } from '../../context/ToastContext'
import { useNavigate } from 'react-router-dom'
import fptLogo from '../../assets/images/fpt-logo.png'

export default function HomeNavbar({ onOpenLogin, onOpenRegister }) {
  const { auth, isLoggedIn, clearAuth, pathForRole, labelForRole } = useAuth()
  const { showToast } = useToast()
  const navigate = useNavigate()

  const handleLogout = () => {
    clearAuth()
    showToast('Đã đăng xuất thành công', 'success')
  }

  return (
    <nav className='navbar'>
      <div className='nav-container'>
        <a href='#' className='brand'>
          <img src={fptLogo} alt='FPT University' className='brand-logo' />
          <span className='brand-divider' />
          <span className='brand-text'>
            <strong>SEAL Hackathon</strong>
            <small>Spring 2026</small>
          </span>
        </a>

        <div className='nav-links'>
          <a href='#about' className='nav-link'>
            Giới thiệu
          </a>
          <a href='#schedule' className='nav-link'>
            Lịch trình
          </a>
          <a href='#gallery' className='nav-link'>
            Khoảnh khắc
          </a>
          <a href='#contact' className='nav-link'>
            Liên hệ
          </a>
        </div>

        {!isLoggedIn ? (
          <div className='nav-actions'>
            <button className='btn btn-ghost btn-sm' onClick={onOpenLogin}>
              Đăng nhập
            </button>
            <button className='btn btn-primary btn-sm' onClick={onOpenRegister}>
              Đăng ký
            </button>
          </div>
        ) : (
          <div className='nav-user'>
            <button className='btn btn-primary btn-sm' onClick={() => navigate(pathForRole(auth.role))}>
              {labelForRole(auth.role)}
            </button>
            <div className='user-chip'>
              <div className='avatar'>{((auth.fullName || auth.email)?.[0] || 'U').toUpperCase()}</div>
              <div className='user-meta'>
                <span className='user-email'>{auth.fullName || auth.email}</span>
              </div>
            </div>
            <button className='btn btn-ghost btn-sm logout-btn' onClick={handleLogout}>
              Đăng xuất
            </button>
          </div>
        )}
      </div>
    </nav>
  )
}

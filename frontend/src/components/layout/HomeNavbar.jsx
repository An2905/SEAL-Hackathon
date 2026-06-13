import { useAuth } from '../../context/AuthContext'
import fptLogo from '../../assets/images/fpt-logo.png'
import AccountDropdown from '../common/AccountDropdown'
import { vietnameseRoleLabel, STUDENT_ROLES } from '../../utils/roleLabels'

export default function HomeNavbar({ onOpenLogin, onOpenRegister }) {
  const { auth, isLoggedIn } = useAuth()
  const isStudentRole = STUDENT_ROLES.includes(auth.role)

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
            <AccountDropdown
              roleLabel={vietnameseRoleLabel(auth.role)}
              showStudentFields={isStudentRole}
              showStaffFields={!isStudentRole}
            />
          </div>
        )}
      </div>
    </nav>
  )
}

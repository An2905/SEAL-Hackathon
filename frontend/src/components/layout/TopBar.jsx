import { useNavigate } from 'react-router-dom'
import fptLogo from '../../assets/images/fpt-logo.png'
import AccountDropdown from '../common/AccountDropdown'

export default function TopBar({ roleLabel, onNavigateProfile, showStudentFields = false, showStaffFields = false }) {
  const navigate = useNavigate()

  return (
    <nav className='navbar' style={{ position: 'sticky', top: 0, zIndex: 10 }}>
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
          <AccountDropdown
            roleLabel={roleLabel}
            onNavigateProfile={onNavigateProfile}
            showStudentFields={showStudentFields}
            showStaffFields={showStaffFields}
          />
        </div>
      </div>
    </nav>
  )
}

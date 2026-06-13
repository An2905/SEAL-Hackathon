import { useAuth } from '../../../context/AuthContext'

export default function StaffOverviewPage() {
  const { auth } = useAuth()
  return (
    <>
      <div className='section-title'>
        <h2>Tổng quan</h2>
        <span className='hint'>Chọn một chức năng ở thanh điều hướng phía trên</span>
      </div>

      <div className='card'>
        <div className='card-head'>
          <div className='card-title'>Thông tin tài khoản</div>
        </div>
        <div className='kv-list'>
          <div className='kv'>
            <span>Họ tên</span>
            <span>{auth.fullName || '—'}</span>
          </div>
          <div className='kv'>
            <span>Email</span>
            <span>{auth.email}</span>
          </div>
          <div className='kv'>
            <span>Vai trò</span>
            <span>Staff</span>
          </div>
          <div className='kv'>
            <span>Trạng thái phiên</span>
            <span>Đã đăng nhập</span>
          </div>
        </div>
      </div>
    </>
  )
}

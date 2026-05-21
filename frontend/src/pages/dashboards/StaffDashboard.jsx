import DashboardShell from './DashboardShell'
import ComingSoonCards from '../../components/common/ComingSoonCards'
import { useAuth } from '../../context/AuthContext'

const CARDS = [
  { title: 'Quản lý sự kiện', desc: 'Tạo, sửa, xóa các event hackathon và danh mục thi đấu.' },
  { title: 'Phê duyệt đội', desc: 'Xem danh sách đội đăng ký event, duyệt hoặc từ chối hồ sơ.' },
  { title: 'Quản lý thí sinh', desc: 'Xem danh sách user, phê duyệt tài khoản pending, đổi role.' },
  { title: 'Thống kê tổng quan', desc: 'Số đội, thí sinh, event đang diễn ra, tỷ lệ tham gia...' },
]

export default function StaffDashboard() {
  const { auth } = useAuth()
  return (
    <DashboardShell
      roleLabel="Staff"
      title="Tài khoản nhân viên"
      subtitle="Bảng điều khiển dành cho Coordinator — quản lý sự kiện, đội thi và thí sinh."
      role="COORDINATOR"
    >
      <div className="section-title">
        <h2>Công cụ quản trị</h2>
        <span className="hint">Cần BE bổ sung endpoint để hoạt động đầy đủ</span>
      </div>
      <ComingSoonCards cards={CARDS} />

      <div className="section-title"><h2>Thông tin tài khoản</h2></div>
      <div className="card">
        <div className="kv-list">
          <div className="kv"><span>Email</span><span>{auth.email}</span></div>
          <div className="kv"><span>Vai trò</span><span>Staff (COORDINATOR)</span></div>
          <div className="kv"><span>Trạng thái phiên</span><span>Đã đăng nhập</span></div>
        </div>
      </div>
    </DashboardShell>
  )
}

import DashboardShell from './DashboardShell'
import ComingSoonCards from '../../components/common/ComingSoonCards'
import { useAuth } from '../../context/AuthContext'

const CARDS = [
  { title: 'Đội tôi đang mentor', desc: 'Xem các đội được phân công, truy cập project và lịch họp.' },
  { title: 'Phản hồi & ghi chú', desc: 'Gửi feedback cho đội, ghi chú cuộc họp 1-1 hoặc nhóm.' },
  { title: 'Lịch mentor', desc: 'Quản lý lịch slot mentor, đăng ký rảnh hỗ trợ.' },
  { title: 'Tài liệu kỹ thuật', desc: 'Kho tài liệu hướng dẫn dành riêng cho mentor.' },
]

export default function MentorDashboard() {
  const { auth } = useAuth()
  return (
    <DashboardShell
      roleLabel="Mentor"
      title="Tài khoản Mentor"
      subtitle="Bảng điều khiển dành cho Mentor — đồng hành cùng các đội thí sinh trong hackathon."
      role="MENTOR"
    >
      <div className="section-title">
        <h2>Hỗ trợ đội thi</h2>
        <span className="hint">Cần BE bổ sung endpoint để hoạt động đầy đủ</span>
      </div>
      <ComingSoonCards cards={CARDS} />

      <div className="section-title"><h2>Thông tin tài khoản</h2></div>
      <div className="card">
        <div className="kv-list">
          <div className="kv"><span>Email</span><span>{auth.email}</span></div>
          <div className="kv"><span>Vai trò</span><span>Mentor</span></div>
          <div className="kv"><span>Trạng thái phiên</span><span>Đã đăng nhập</span></div>
        </div>
      </div>
    </DashboardShell>
  )
}

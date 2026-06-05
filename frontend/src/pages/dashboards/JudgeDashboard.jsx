import { Link } from 'react-router-dom'
import DashboardShell from './DashboardShell'
import ComingSoonCards from '../../components/common/ComingSoonCards'
import { useAuth } from '../../context/AuthContext'

const CARDS = [
  { title: 'Đội cần chấm', desc: 'Danh sách đội được giao chấm, theo từng vòng / bảng.' },
  { title: 'Form chấm điểm', desc: 'Điền tiêu chí, nhận xét và submit điểm.' },
  { title: 'Lịch sử chấm', desc: 'Xem lại các bài đã chấm, sửa nếu vòng chưa khóa.' },
  { title: 'Tiêu chí đánh giá', desc: 'Đọc rubric chấm điểm chi tiết cho từng bảng.' },
]

export default function JudgeDashboard() {
  const { auth, pillLabelForRole } = useAuth()
  const guestLabel = pillLabelForRole(auth.role) || 'Khách'
  return (
    <DashboardShell
      roleLabel={guestLabel}
      title="Khu vực Judge"
      subtitle="Khách được phân công giám khảo — chấm điểm các đội thi. Cùng tài khoản có thể vào khu Mentor nếu được gán hướng dẫn."
      role={guestLabel}
      showStaffFields
    >
      <div className="action-row" style={{ marginBottom: '1rem' }}>
        <Link className="btn btn-outline" to="/mentor">Chuyển sang khu Mentor</Link>
      </div>
      <div className="section-title">
        <h2>Khu vực chấm thi</h2>
        <span className="hint">Cần BE bổ sung endpoint để hoạt động đầy đủ</span>
      </div>
      <ComingSoonCards cards={CARDS} />

      <div className="section-title"><h2>Thông tin tài khoản</h2></div>
      <div className="card">
        <div className="kv-list">
          <div className="kv"><span>Họ tên</span><span>{auth.fullName || '—'}</span></div>
          <div className="kv"><span>Email</span><span>{auth.email}</span></div>
          <div className="kv"><span>Vai trò tài khoản</span><span>{guestLabel}</span></div>
          <div className="kv"><span>Trạng thái phiên</span><span>Đã đăng nhập</span></div>
        </div>
      </div>
    </DashboardShell>
  )
}

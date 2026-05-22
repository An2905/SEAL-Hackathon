import DashboardShell from './DashboardShell'
import ComingSoonCards from '../../components/common/ComingSoonCards'
import { useAuth } from '../../context/AuthContext'

const CARDS = [
  { title: 'Đội cần chấm', desc: 'Danh sách đội được giao chấm, theo từng vòng / category.' },
  { title: 'Form chấm điểm', desc: 'Điền tiêu chí, nhận xét và submit điểm.' },
  { title: 'Lịch sử chấm', desc: 'Xem lại các bài đã chấm, sửa nếu vòng chưa khóa.' },
  { title: 'Tiêu chí đánh giá', desc: 'Đọc rubric chấm điểm chi tiết cho từng category.' },
]

export default function JudgeDashboard() {
  const { auth } = useAuth()
  return (
    <DashboardShell
      roleLabel="Judge"
      title="Tài khoản Giám khảo"
      subtitle="Bảng điều khiển dành cho Judge — chấm điểm các đội thi."
      role="JUDGE_INTERNAL"
    >
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
          <div className="kv"><span>Vai trò</span><span>Judge (JUDGE_INTERNAL)</span></div>
          <div className="kv"><span>Trạng thái phiên</span><span>Đã đăng nhập</span></div>
        </div>
      </div>
    </DashboardShell>
  )
}

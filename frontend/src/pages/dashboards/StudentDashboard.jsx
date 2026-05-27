import { useState, useEffect, useCallback } from 'react'
import DashboardShell from './DashboardShell'
import FormField from '../../components/common/FormField'
import FormMessage from '../../components/common/FormMessage'
import LoadingButton from '../../components/common/LoadingButton'
import { getMyTeam, createTeam, joinTeam, joinEvent, deleteMember } from '../../api/team'
import { useToast } from '../../context/ToastContext'
import { localizeError } from '../../utils/errors'

// ─── Team Info Card ───────────────────────────────────────────────────────────
function TeamInfoCard({ data, onRefresh }) {
  const { showToast } = useToast()

  const handleCopyEnroll = async () => {
    const code = data.enrollCode
    try {
      await navigator.clipboard.writeText(code)
      showToast('Đã sao chép mã enroll: ' + code, 'success')
    } catch {
      showToast('Mã enroll: ' + code, 'success')
    }
  }

  return (
    <div className="card team-info-card">
      <div className="card-head">
        <div>
          <div className="card-title">{data.teamName}</div>
          <div className="card-sub" style={{ margin: 0 }}>
            {data.isLeader ? 'Bạn là leader của đội này' : 'Bạn đang là thành viên của đội'}
          </div>
        </div>
        <span className={`role-pill ${data.isLeader ? 'role-judge' : 'role-student'}`} style={{ marginLeft: 'auto' }}>
          {data.isLeader ? 'Leader' : 'Thành viên'}
        </span>
      </div>

      <div className="kv-list">
        <div className="kv"><span>Tên đội</span><span>{data.teamName}</span></div>
        <div className="kv"><span>Mã enroll</span><span><code>{data.enrollCode}</code></span></div>
        <div className="kv"><span>Leader</span><span>{data.leaderName} ({data.leaderEmail})</span></div>
        <div className="kv"><span>Trạng thái</span><span>{data.status}</span></div>
        <div className="kv"><span>Số thành viên</span><span>{data.memberCount} / 5</span></div>
      </div>

      <div className="section-title" style={{ margin: '22px 0 10px' }}>
        <h2 style={{ fontSize: 16 }}>Thành viên</h2>
      </div>
      <div className="kv-list">
        {(data.members || []).map((m) => (
          <div className="member-row" key={m.userId}>
            <div className="avatar">{(m.fullName?.[0] || m.email?.[0] || 'U').toUpperCase()}</div>
            <div className="member-info">
              <div className="member-name">
                {m.fullName || '(Chưa có tên)'}
                {m.isLeader && <span className="leader-tag">Leader</span>}
              </div>
              <div className="member-meta">{m.email || ''}</div>
            </div>
            <span className="member-id-chip" title="user_id">#{m.userId}</span>
          </div>
        ))}
      </div>

      <div className="card-actions" style={{ marginTop: 18 }}>
        <button className="btn btn-outline" onClick={handleCopyEnroll}>Sao chép mã enroll</button>
        <button className="btn btn-ghost" onClick={onRefresh}>Làm mới</button>
      </div>
    </div>
  )
}

// ─── Create Team Form ─────────────────────────────────────────────────────────
function CreateTeamForm({ onSuccess }) {
  const { showToast } = useToast()
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState(null)
  const [teamName, setTeamName] = useState('')

  const handleSubmit = async (e) => {
    e.preventDefault()
    setMessage(null)
    setLoading(true)
    try {
      const { enrollCode } = await createTeam({ teamName: teamName.trim() })
      setMessage({ text: `Tạo đội thành công! Mã enroll: ${enrollCode}`, type: 'success' })
      showToast('Đã tạo đội — mã enroll: ' + enrollCode, 'success')
      setTeamName('')
      setTimeout(onSuccess, 400)
    } catch (err) {
      setMessage({ text: localizeError(err.message), type: 'error' })
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="card">
      <div className="card-head"><div className="card-title">Tạo đội mới</div></div>
      <p className="card-sub">Tạo đội của riêng bạn. Hệ thống sẽ sinh mã <strong>enrollCode</strong> để mời thành viên khác.</p>
      <form className="form" onSubmit={handleSubmit}>
        <FormField label="Tên đội">
          <input name="teamName" value={teamName} onChange={e => setTeamName(e.target.value)}
            required placeholder="VD: Code Hunters" />
        </FormField>
        <LoadingButton loading={loading} type="submit">Tạo đội</LoadingButton>
        <FormMessage message={message?.text} type={message?.type} />
      </form>
    </div>
  )
}

// ─── Join Team Form ───────────────────────────────────────────────────────────
function JoinTeamForm({ onSuccess }) {
  const { showToast } = useToast()
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState(null)
  const [enrollCode, setEnrollCode] = useState('')

  const handleSubmit = async (e) => {
    e.preventDefault()
    setMessage(null)
    setLoading(true)
    try {
      await joinTeam({ enrollCode: enrollCode.trim() })
      setMessage({ text: 'Đã tham gia đội thành công!', type: 'success' })
      showToast('Đã tham gia đội', 'success')
      setEnrollCode('')
      setTimeout(onSuccess, 400)
    } catch (err) {
      setMessage({ text: localizeError(err.message), type: 'error' })
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="card">
      <div className="card-head"><div className="card-title">Tham gia đội</div></div>
      <p className="card-sub">Nhập mã <strong>enrollCode</strong> mà leader cung cấp cho bạn.</p>
      <form className="form" onSubmit={handleSubmit}>
        <FormField label="Mã enroll">
          <input name="enrollCode" value={enrollCode} onChange={e => setEnrollCode(e.target.value)}
            required placeholder="VD: 12345678" maxLength={16} />
        </FormField>
        <LoadingButton loading={loading} type="submit">Tham gia</LoadingButton>
        <FormMessage message={message?.text} type={message?.type} />
      </form>
    </div>
  )
}

// ─── Join Event Form ──────────────────────────────────────────────────────────
function JoinEventForm() {
  const { showToast } = useToast()
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState(null)
  const [form, setForm] = useState({ eventId: '', categoryId: '' })
  const handle = e => setForm(f => ({ ...f, [e.target.name]: e.target.value }))

  const handleSubmit = async (e) => {
    e.preventDefault()
    setMessage(null)
    setLoading(true)
    try {
      await joinEvent({ eventId: form.eventId.trim(), categoryId: form.categoryId.trim() })
      setMessage({ text: 'Đăng ký event thành công!', type: 'success' })
      showToast('Đăng ký event thành công', 'success')
      setForm({ eventId: '', categoryId: '' })
    } catch (err) {
      setMessage({ text: localizeError(err.message), type: 'error' })
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="card">
      <div className="card-head"><div className="card-title">Đăng ký sự kiện</div></div>
      <p className="card-sub">Dẫn đội tham gia event hackathon. Nhập <strong>Event ID</strong> và <strong>Category ID</strong>.</p>
      <form className="form" onSubmit={handleSubmit}>
        <FormField label="Event ID">
          <input name="eventId" value={form.eventId} onChange={handle} required placeholder="VD: 1" />
        </FormField>
        <FormField label="Category ID">
          <input name="categoryId" value={form.categoryId} onChange={handle} required placeholder="VD: 2" />
        </FormField>
        <LoadingButton loading={loading} type="submit">Đăng ký event</LoadingButton>
        <FormMessage message={message?.text} type={message?.type} />
      </form>
    </div>
  )
}

// ─── Delete Member Form ───────────────────────────────────────────────────────
function DeleteMemberForm({ onSuccess }) {
  const { showToast } = useToast()
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState(null)
  const [memberId, setMemberId] = useState('')

  const handleSubmit = async (e) => {
    e.preventDefault()
    setMessage(null)
    setLoading(true)
    try {
      await deleteMember({ memberId: memberId.trim() })
      setMessage({ text: 'Đã xóa thành viên', type: 'success' })
      showToast('Đã xóa thành viên khỏi đội', 'success')
      setMemberId('')
      setTimeout(onSuccess, 400)
    } catch (err) {
      setMessage({ text: localizeError(err.message), type: 'error' })
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="card">
      <div className="card-head"><div className="card-title">Xóa thành viên</div></div>
      <p className="card-sub">Loại một thành viên ra khỏi đội. Chỉ leader mới có quyền này.</p>
      <form className="form" onSubmit={handleSubmit}>
        <FormField label="Member ID">
          <input name="memberId" value={memberId} onChange={e => setMemberId(e.target.value)}
            required placeholder="Nhập user_id của thành viên" />
        </FormField>
        <LoadingButton loading={loading} type="submit">Xóa thành viên</LoadingButton>
        <FormMessage message={message?.text} type={message?.type} />
      </form>
    </div>
  )
}

// ─── Activity Log ─────────────────────────────────────────────────────────────
function ActivityLog({ activities }) {
  if (!activities.length) {
    return (
      <div className="empty-state">
        Chưa có hoạt động nào trong phiên này. Hãy thử tạo đội hoặc tham gia một đội ở trên.
      </div>
    )
  }
  return (
    <div className="kv-list">
      {activities.map((a, i) => (
        <div className="kv" key={i}>
          <span>{a.at.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}</span>
          <span>{a.text}</span>
        </div>
      ))}
    </div>
  )
}

// ─── Main Page ────────────────────────────────────────────────────────────────
export default function StudentDashboard() {
  const { showToast } = useToast()
  const [teamState, setTeamState] = useState('loading') // 'loading' | 'no-team' | 'has-team'
  const [teamData, setTeamData] = useState(null)
  const [activities, setActivities] = useState([])

  const logActivity = (text) => setActivities(prev => [{ text, at: new Date() }, ...prev])

  const loadMyTeam = useCallback(async () => {
    setTeamState('loading')
    try {
      const result = await getMyTeam()
      if (result.hasTeam) {
        setTeamData(result.data)
        setTeamState('has-team')
      } else {
        setTeamState('no-team')
      }
    } catch (err) {
      showToast(localizeError(err.message), 'error')
      setTeamState('no-team')
    }
  }, [showToast])

  useEffect(() => { loadMyTeam() }, [loadMyTeam])

  const handleTeamCreated = () => { logActivity('Tạo đội mới'); loadMyTeam() }
  const handleTeamJoined = () => { logActivity('Tham gia đội thành công'); loadMyTeam() }
  const handleMemberDeleted = () => { logActivity('Xóa thành viên'); loadMyTeam() }
  const handleRefresh = () => { showToast('Đang làm mới...', 'success'); loadMyTeam() }

  return (
    <DashboardShell
      roleLabel="Student"
      title="Tài khoản sinh viên"
      subtitle="Quản lý đội thi và đăng ký sự kiện hackathon ngay tại đây."
      role="STUDENT"
      showStudentFields
    >
      <div className="section-title">
        <h2>Đội của tôi</h2>
        <span className="hint">Mỗi sinh viên chỉ có thể tham gia 1 đội</span>
      </div>

      {teamState === 'loading' && (
        <div className="empty-state">Đang tải thông tin đội...</div>
      )}

      {teamState === 'has-team' && teamData && (
        <TeamInfoCard data={teamData} onRefresh={handleRefresh} />
      )}

      {teamState === 'no-team' && (
        <div className="cards">
          <CreateTeamForm onSuccess={handleTeamCreated} />
          <JoinTeamForm onSuccess={handleTeamJoined} />
        </div>
      )}

      {teamState === 'has-team' && teamData?.isLeader && (
        <>
          <div className="section-title">
            <h2>Quản lý leader</h2>
            <span className="hint">Chỉ leader mới thực hiện được các thao tác bên dưới</span>
          </div>
          <div className="cards">
            <JoinEventForm />
            <DeleteMemberForm onSuccess={handleMemberDeleted} />
          </div>
        </>
      )}

      <div className="section-title"><h2>Hoạt động gần đây</h2></div>
      <ActivityLog activities={activities} />
    </DashboardShell>
  )
}

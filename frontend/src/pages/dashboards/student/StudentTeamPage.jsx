import { useState } from 'react'
import FormField from '../../../components/common/FormField'
import FormMessage from '../../../components/common/FormMessage'
import LoadingButton from '../../../components/common/LoadingButton'
import { createTeam, joinTeam, deleteMember } from '../../../api/team'
import { useToast } from '../../../context/ToastContext'
import { localizeError } from '../../../utils/errors'
import Pagination from '../../../components/common/Pagination'
import LoadingState from '../../../components/common/LoadingState'
import {
  PAGE_SIZE,
  DashboardSection,
  DetailMeta,
  StatusStack,
  StatusBadge,
  GithubRequiredBanner,
  GithubLinkedBadge
} from './shared'

// ─── Team Info Card ───────────────────────────────────────────────────────────
function TeamInfoCard({ data, onRefresh, onMemberDeleted }) {
  const { showToast } = useToast()
  const [membersPage, setMembersPage] = useState(1)
  const [deletingId, setDeletingId] = useState(null)
  const members = data.members || []
  const isLeader = Boolean(data.isLeader)

  const handleCopyEnroll = async () => {
    const code = data.enrollCode
    try {
      await navigator.clipboard.writeText(code)
      showToast('Đã sao chép mã enroll: ' + code, 'success')
    } catch {
      showToast('Mã enroll: ' + code, 'success')
    }
  }

  const handleDeleteMember = async (member) => {
    const label = member.fullName || member.email || 'thành viên này'
    if (!window.confirm(`Xóa ${label} khỏi đội?`)) return

    const memberId = (member.email || member.userId || '').trim()
    if (!memberId) return

    setDeletingId(member.userId || memberId)
    try {
      await deleteMember({ memberId })
      showToast('Đã xóa thành viên khỏi đội', 'success')
      onMemberDeleted?.()
    } catch (err) {
      showToast(localizeError(err.message), 'error')
    } finally {
      setDeletingId(null)
    }
  }

  return (
    <div className='card'>
      <div className='mentor-team-card student-team-summary'>
        <div className='kv student-kv-row'>
          <span className='student-kv-main'>
            <div className='student-kv-title'>{data.teamName || '—'}</div>
            <DetailMeta>
              Trưởng nhóm: {data.leaderName || '—'}
              {data.leaderEmail ? ` · ${data.leaderEmail}` : ''}
            </DetailMeta>
            {data.enrollCode && (
              <DetailMeta>
                Mã đội: <code>{data.enrollCode}</code>
              </DetailMeta>
            )}
            <DetailMeta>Thành viên: {data.memberCount ?? members.length} / 5</DetailMeta>
          </span>
          <StatusStack>
            <span className={`role-pill ${isLeader ? 'role-judge' : 'role-student'}`}>
              {isLeader ? 'Leader' : 'Thành viên'}
            </span>
            {data.status ? <StatusBadge status={data.status} className='status-default' /> : null}
          </StatusStack>
        </div>
      </div>

      <div className='student-panel-divider'>
        <span className='student-panel-divider__label'>Thành viên</span>
      </div>
      <div className='kv-list student-members-list'>
        {members.slice((membersPage - 1) * PAGE_SIZE, membersPage * PAGE_SIZE).map((m) => {
          const canDelete = isLeader && !m.isLeader
          const isDeleting = deletingId === (m.userId || m.email)
          return (
            <div className='member-row' key={m.userId}>
              <div className='avatar'>{(m.fullName?.[0] || m.email?.[0] || 'U').toUpperCase()}</div>
              <div className='member-info'>
                <div className='member-name-row'>
                  <div className='member-name'>
                    {m.fullName || '(Chưa có tên)'}
                    {m.isLeader && <span className='leader-tag'>Leader</span>}
                  </div>
                  {canDelete && (
                    <button
                      type='button'
                      className='member-delete-btn'
                      onClick={() => handleDeleteMember(m)}
                      disabled={isDeleting}
                      aria-label={`Xóa ${m.fullName || m.email}`}
                      title='Xóa thành viên'
                    >
                      {isDeleting ? (
                        <span className='spinner spinner-dark spinner--sm' aria-hidden='true' />
                      ) : (
                        <svg
                          width='14'
                          height='14'
                          viewBox='0 0 24 24'
                          fill='none'
                          stroke='currentColor'
                          strokeWidth='2'
                          aria-hidden='true'
                        >
                          <path d='M3 6h18M8 6V4h8v2M19 6l-1 14H6L5 6' strokeLinecap='round' strokeLinejoin='round' />
                          <path d='M10 11v6M14 11v6' strokeLinecap='round' />
                        </svg>
                      )}
                    </button>
                  )}
                </div>
                <div className='member-meta'>{m.email || ''}</div>
              </div>
            </div>
          )
        })}
      </div>
      <Pagination total={members.length} pageSize={PAGE_SIZE} currentPage={membersPage} onChange={setMembersPage} />

      <div className='card-actions student-card-actions'>
        <button type='button' className='btn btn-outline' onClick={handleCopyEnroll}>
          Sao chép mã enroll
        </button>
        <button type='button' className='btn btn-ghost' onClick={onRefresh}>
          Làm mới
        </button>
      </div>
    </div>
  )
}

// ─── Create Team Form ─────────────────────────────────────────────────────────
function CreateTeamForm({ onSuccess, githubLinked }) {
  const { showToast } = useToast()
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState(null)
  const [teamName, setTeamName] = useState('')

  const handleSubmit = async (e) => {
    e.preventDefault()
    setMessage(null)
    const normalized = teamName.trim().replace(/\s+/g, ' ')
    if (!normalized) {
      setMessage({ text: 'Tên đội không được để trống', type: 'error' })
      return
    }
    if (normalized.length > 100) {
      setMessage({ text: 'Tên đội tối đa 100 ký tự', type: 'error' })
      return
    }
    setLoading(true)
    try {
      const { enrollCode } = await createTeam({ teamName: normalized })
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
    <div className={`card student-team-card${!githubLinked ? ' student-team-card--locked' : ''}`}>
      <div className='student-team-card__head'>
        <span className='student-team-card__icon student-team-card__icon--create' aria-hidden='true'>
          <svg width='20' height='20' viewBox='0 0 24 24' fill='none' stroke='currentColor' strokeWidth='1.8'>
            <path d='M12 5v14M5 12h14' strokeLinecap='round' />
          </svg>
        </span>
        <div className='student-team-card__intro'>
          <div className='card-title'>Tạo đội mới</div>
          <p className='student-team-card__lead'>
            Khởi tạo đội thi của bạn. Hệ thống tự sinh mã <strong>enrollCode</strong> để mời thành viên.
          </p>
        </div>
      </div>
      <form className='form student-team-form' onSubmit={handleSubmit}>
        <FormField label='Tên đội'>
          <input
            name='teamName'
            value={teamName}
            onChange={(e) => setTeamName(e.target.value)}
            required
            maxLength={100}
            placeholder='VD: Code Hunters'
            disabled={!githubLinked || loading}
          />
        </FormField>
        <p className='student-team-form__hint'>
          Tên đội phải duy nhất trên toàn hệ thống (không phân biệt hoa thường).
        </p>
        <LoadingButton
          loading={loading}
          type='submit'
          disabled={!githubLinked}
          className='btn btn-primary btn-block student-team-form__submit'
        >
          Tạo đội
        </LoadingButton>
        <FormMessage message={message?.text} type={message?.type} />
      </form>
    </div>
  )
}

// ─── Join Team Form ───────────────────────────────────────────────────────────
function JoinTeamForm({ onSuccess, githubLinked }) {
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
    <div className={`card student-team-card${!githubLinked ? ' student-team-card--locked' : ''}`}>
      <div className='student-team-card__head'>
        <span className='student-team-card__icon student-team-card__icon--join' aria-hidden='true'>
          <svg width='20' height='20' viewBox='0 0 24 24' fill='none' stroke='currentColor' strokeWidth='1.8'>
            <path d='M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2' strokeLinecap='round' />
            <circle cx='9' cy='7' r='4' />
            <path d='M19 8v6M22 11h-6' strokeLinecap='round' />
          </svg>
        </span>
        <div className='student-team-card__intro'>
          <div className='card-title'>Tham gia đội</div>
          <p className='student-team-card__lead'>
            Nhập mã <strong>enrollCode</strong> do leader cung cấp để gia nhập đội có sẵn.
          </p>
        </div>
      </div>
      <form className='form student-team-form' onSubmit={handleSubmit}>
        <FormField label='Mã enroll'>
          <input
            name='enrollCode'
            value={enrollCode}
            onChange={(e) => setEnrollCode(e.target.value)}
            required
            placeholder='VD: 12345678'
            maxLength={16}
            disabled={!githubLinked || loading}
          />
        </FormField>
        <LoadingButton
          loading={loading}
          type='submit'
          disabled={!githubLinked}
          className='btn btn-primary btn-block student-team-form__submit'
        >
          Tham gia
        </LoadingButton>
        <FormMessage message={message?.text} type={message?.type} />
      </form>
    </div>
  )
}

// ─── Page ─────────────────────────────────────────────────────────────────────
export default function StudentTeamPage({
  teamState,
  teamData,
  githubStatus,
  oauthLoading,
  onConnectGithub,
  onTeamCreated,
  onTeamJoined,
  onMemberDeleted,
  onRefresh
}) {
  return (
    <>
      {!githubStatus.loading && (!githubStatus.linked || !githubStatus.username) ? (
        <GithubRequiredBanner
          onConnect={onConnectGithub}
          loading={oauthLoading}
          isWarning={teamState === 'has-team'}
        />
      ) : null}

      {teamState === 'loading' && (
        <DashboardSection title='Đội của tôi' hint='Mỗi sinh viên chỉ có thể tham gia 1 đội'>
          <div className='card'>
            <LoadingState text='Đang tải thông tin đội…' />
          </div>
        </DashboardSection>
      )}

      {teamState !== 'loading' && (
        <DashboardSection title='Đội của tôi' hint='Mỗi sinh viên chỉ có thể tham gia 1 đội'>
          {teamState === 'has-team' && teamData ? (
            <TeamInfoCard data={teamData} onRefresh={onRefresh} onMemberDeleted={onMemberDeleted} />
          ) : null}

          {teamState === 'no-team' ? (
            <>
              {!githubStatus.loading && githubStatus.linked ? (
                <GithubLinkedBadge username={githubStatus.username} />
              ) : null}
              <div className='cards student-team-cards'>
                <CreateTeamForm onSuccess={onTeamCreated} githubLinked={githubStatus.linked} />
                <JoinTeamForm onSuccess={onTeamJoined} githubLinked={githubStatus.linked} />
              </div>
            </>
          ) : null}
        </DashboardSection>
      )}
    </>
  )
}

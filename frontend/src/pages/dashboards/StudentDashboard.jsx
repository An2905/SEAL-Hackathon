import { useState, useEffect, useCallback, useRef } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import DashboardLayout from '../../components/layout/DashboardLayout'
import { getMyTeam } from '../../api/team'
import { useToast } from '../../context/ToastContext'
import { localizeError } from '../../utils/errors'
import { getGithubLinkStatus, getGithubLinkUrl } from '../../api/auth'
import Modal from '../../components/common/Modal'
import StudentTeamPage from './student/StudentTeamPage'
import StudentEventsPage from './student/StudentEventsPage'

// ─── Main Page ────────────────────────────────────────────────────────────────
export default function StudentDashboard() {
  const { showToast } = useToast()
  const navigate = useNavigate()
  const location = useLocation()
  const [teamState, setTeamState] = useState('loading')
  const [teamData, setTeamData] = useState(null)
  const [registrationsRefreshKey, setRegistrationsRefreshKey] = useState(0)
  const [githubStatus, setGithubStatus] = useState({ loading: true, linked: false, username: '' })
  const [oauthLoading, setOauthLoading] = useState(false)
  const handledOauthSearchRef = useRef('')
  const [oauthConflictError, setOauthConflictError] = useState(null)

  const loadGithubStatus = useCallback(async () => {
    setGithubStatus((prev) => ({ ...prev, loading: true }))
    try {
      const status = await getGithubLinkStatus()
      setGithubStatus({
        loading: false,
        linked: status.githubLinked,
        username: status.githubUsername
      })
    } catch {
      setGithubStatus({ loading: false, linked: false, username: '' })
    }
  }, [])

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

  useEffect(() => {
    loadMyTeam()
  }, [loadMyTeam])

  useEffect(() => {
    loadGithubStatus()
  }, [loadGithubStatus])

  useEffect(() => {
    const params = new URLSearchParams(location.search)
    const status = params.get('github_oauth')
    if (!status) return
    if (handledOauthSearchRef.current === location.search) return
    handledOauthSearchRef.current = location.search

    if (status === 'success') {
      const username = params.get('github_username')
      showToast(username ? `Đã liên kết GitHub: @${username}` : 'Đã liên kết GitHub thành công.', 'success')
      loadGithubStatus()
    } else {
      const errorCode = params.get('code')
      const rawMessage = params.get('message') || 'Liên kết GitHub thất bại.'
      let message = rawMessage
      try {
        message = decodeURIComponent(rawMessage.replace(/\+/g, ' '))
      } catch {
        message = rawMessage
      }

      if (errorCode === 'ALREADY_LINKED' || message.includes('already linked to another user')) {
        setOauthConflictError(message)
      } else {
        showToast(localizeError(message), 'error')
      }
    }

    navigate('/student', { replace: true })
  }, [location.search, navigate, showToast, loadGithubStatus])

  const handleConnectGithub = async (prompt) => {
    setOauthLoading(true)
    try {
      const authorizeUrl = await getGithubLinkUrl(prompt)
      window.location.href = authorizeUrl
    } catch (err) {
      showToast(localizeError(err.message), 'error')
      setOauthLoading(false)
    }
  }

  const handleTeamCreated = () => {
    loadMyTeam()
  }
  const handleTeamJoined = () => {
    loadMyTeam()
  }
  const handleMemberDeleted = () => {
    loadMyTeam()
  }
  const handleRefresh = () => {
    showToast('Đang làm mới...', 'success')
    loadMyTeam()
  }
  const refreshRegistrations = () => setRegistrationsRefreshKey((k) => k + 1)

  const teamPage = (
    <StudentTeamPage
      teamState={teamState}
      teamData={teamData}
      githubStatus={githubStatus}
      oauthLoading={oauthLoading}
      onConnectGithub={handleConnectGithub}
      onTeamCreated={handleTeamCreated}
      onTeamJoined={handleTeamJoined}
      onMemberDeleted={handleMemberDeleted}
      onRefresh={handleRefresh}
    />
  )

  const tabs =
    teamState === 'has-team'
      ? [
          { key: 'doi', label: 'Đội của tôi', content: teamPage },
          {
            key: 'sukien',
            label: 'Sự kiện',
            content: (
              <StudentEventsPage
                isLeader={Boolean(teamData?.isLeader)}
                teamId={teamData?.teamId}
                teamName={teamData?.teamName}
                refreshKey={registrationsRefreshKey}
                onRegisterSuccess={refreshRegistrations}
                githubStatus={githubStatus}
                oauthLoading={oauthLoading}
                onConnectGithub={handleConnectGithub}
              />
            )
          }
        ]
      : [{ key: 'doi', label: 'Đội của tôi', content: teamPage }]

  return (
    <>
      <DashboardLayout
        roleLabel='Sinh viên'
        moduleTitle='Khu vực Sinh viên'
        moduleSubtitle='Quản lý đội thi, đăng ký sự kiện hackathon và liên kết GitHub.'
        showStudentFields
        className='dashboard-shell--student-zone'
        tabs={tabs}
      />

      {oauthConflictError && (
        <Modal isOpen={true} onClose={() => setOauthConflictError(null)} title='Liên kết GitHub thất bại'>
          <div className='oauth-conflict-modal' style={{ padding: '0.5rem' }}>
            <div
              className='oauth-conflict-modal__icon'
              style={{ color: '#ef4444', fontSize: '2.5rem', marginBottom: '1rem', textAlign: 'center' }}
            >
              ⚠️
            </div>
            <p
              style={{
                marginBottom: '1.5rem',
                lineHeight: '1.6',
                fontSize: '1rem',
                color: 'var(--text-color, #1f2937)',
                textAlign: 'center'
              }}
            >
              <strong>Mỗi tài khoản GitHub chỉ có thể liên kết với một tài khoản SEAL Hackathon duy nhất.</strong>
            </p>
            <div
              className='form-actions'
              style={{ display: 'flex', justifyContent: 'center', gap: '1rem', marginTop: '1.5rem' }}
            >
              <button
                type='button'
                className='btn btn-primary'
                onClick={() => {
                  setOauthConflictError(null)
                  handleConnectGithub('select_account')
                }}
                disabled={oauthLoading}
              >
                {oauthLoading ? 'Đang chuyển hướng...' : 'Chọn tài khoản liên kết khác'}
              </button>
            </div>
          </div>
        </Modal>
      )}
    </>
  )
}

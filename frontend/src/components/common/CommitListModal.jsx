import { useState, useCallback, useEffect } from 'react'
import Modal from './Modal'
import { listCommits, getCommit } from '../../api/githubRepo'
import { useToast } from '../../context/ToastContext'
import { localizeError } from '../../utils/errors'

const PER_PAGE = 20

function shortSha(sha) {
  return String(sha || '').slice(0, 7)
}

function firstLine(msg) {
  return String(msg || '').split('\n')[0]
}

function formatDate(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function fileStatusColor(status) {
  if (status === 'added') return '#16a34a'
  if (status === 'removed') return '#dc2626'
  if (status === 'renamed') return '#d97706'
  return '#2563eb'
}

function CommitDetail({ owner, repo, sha, onBack }) {
  const { showToast } = useToast()
  const [detail, setDetail] = useState(null)
  const [loading, setLoading] = useState(true)
  const [filePage, setFilePage] = useState(1)
  const [loadingMore, setLoadingMore] = useState(false)

  const load = useCallback(async () => {
    try {
      const data = await getCommit({ owner, repo, ref: sha, perPage: 30, page: 1 })
      setDetail(data)
    } catch (err) {
      showToast(localizeError(err.message), 'error')
    } finally {
      setLoading(false)
    }
  }, [owner, repo, sha, showToast])

  useEffect(() => { load() }, [load])

  const loadMoreFiles = async () => {
    setLoadingMore(true)
    try {
      const next = filePage + 1
      const data = await getCommit({ owner, repo, ref: sha, perPage: 30, page: next })
      const newFiles = data?.files ?? []
      setDetail((prev) => ({ ...prev, files: [...(prev?.files ?? []), ...newFiles] }))
      setFilePage(next)
    } catch (err) {
      showToast(localizeError(err.message), 'error')
    } finally {
      setLoadingMore(false)
    }
  }

  const commitData = detail?.commit ?? {}
  const stats = detail?.stats ?? {}
  const files = detail?.files ?? []

  return (
    <div>
      <button type='button' className='btn btn-outline btn-sm' onClick={onBack} style={{ marginBottom: 12 }}>
        ← Danh sách commit
      </button>

      {loading && <p className='hint'>Đang tải...</p>}

      {!loading && detail && (
        <>
          <div style={{ marginBottom: 12 }}>
            <div style={{ fontWeight: 600, fontSize: 14, marginBottom: 4 }}>
              {commitData.message || '(no message)'}
            </div>
            <div style={{ fontSize: 12, color: 'var(--text-dim)' }}>
              {commitData.author?.name} · {formatDate(commitData.author?.date)}
            </div>
            <code style={{ fontSize: 11, color: 'var(--text-dim)', marginTop: 4, display: 'block' }}>
              {detail.sha}
            </code>
          </div>

          <div style={{ display: 'flex', gap: 16, marginBottom: 12 }}>
            <span style={{ fontSize: 12, color: '#16a34a', fontWeight: 600 }}>+{stats.additions ?? 0}</span>
            <span style={{ fontSize: 12, color: '#dc2626', fontWeight: 600 }}>−{stats.deletions ?? 0}</span>
            <span style={{ fontSize: 12, color: 'var(--text-dim)' }}>{stats.total ?? 0} thay đổi · {files.length} file</span>
          </div>

          <div className='kv-list'>
            {files.map((f) => (
              <div key={f.filename} style={{ padding: '6px 0', borderBottom: '1px solid var(--border-soft, #f5f5f5)' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <span
                    style={{
                      fontSize: 10,
                      fontWeight: 700,
                      padding: '1px 6px',
                      borderRadius: 4,
                      background: fileStatusColor(f.status) + '1a',
                      color: fileStatusColor(f.status),
                      flexShrink: 0
                    }}
                  >
                    {f.status}
                  </span>
                  <span style={{ fontSize: 12, wordBreak: 'break-all', flex: 1 }}>{f.filename}</span>
                  <span style={{ fontSize: 11, color: '#16a34a', flexShrink: 0 }}>+{f.additions}</span>
                  <span style={{ fontSize: 11, color: '#dc2626', flexShrink: 0 }}>−{f.deletions}</span>
                </div>
              </div>
            ))}
          </div>

          {files.length === 30 * filePage && (
            <button
              type='button'
              className='btn btn-outline btn-sm'
              style={{ marginTop: 10, width: '100%' }}
              onClick={loadMoreFiles}
              disabled={loadingMore}
            >
              {loadingMore ? 'Đang tải...' : 'Tải thêm file'}
            </button>
          )}
        </>
      )}
    </div>
  )
}

function CommitList({ owner, repo, onSelect }) {
  const { showToast } = useToast()
  const [commits, setCommits] = useState([])
  const [page, setPage] = useState(1)
  const [loading, setLoading] = useState(false)
  const [hasMore, setHasMore] = useState(true)
  const [initialLoaded, setInitialLoaded] = useState(false)

  const loadPage = useCallback(async (pageNum) => {
    setLoading(true)
    try {
      const data = await listCommits({ owner, repo, perPage: PER_PAGE, page: pageNum })
      const rows = Array.isArray(data) ? data : []
      setCommits((prev) => [...prev, ...rows])
      setPage(pageNum)
      setHasMore(rows.length === PER_PAGE)
    } catch (err) {
      showToast(localizeError(err.message), 'error')
    } finally {
      setLoading(false)
      setInitialLoaded(true)
    }
  }, [owner, repo, showToast])

  useEffect(() => { loadPage(1) }, [owner, repo])

  return (
    <div>
      {!initialLoaded && <p className='hint'>Đang tải commit...</p>}

      {commits.length === 0 && initialLoaded && (
        <p className='hint'>Không có commit nào.</p>
      )}

      <div className='kv-list'>
        {commits.map((c) => {
          const sha = c.sha ?? ''
          const msg = c.commit?.message ?? ''
          const authorName = c.commit?.author?.name ?? c.author?.login ?? '—'
          const date = c.commit?.author?.date ?? ''
          return (
            <button
              key={sha}
              type='button'
              onClick={() => onSelect(sha)}
              style={{
                display: 'block',
                width: '100%',
                textAlign: 'left',
                background: 'none',
                border: 'none',
                borderBottom: '1px solid var(--border-soft, #f5f5f5)',
                padding: '8px 0',
                cursor: 'pointer'
              }}
            >
              <div style={{ display: 'flex', alignItems: 'flex-start', gap: 10 }}>
                <code style={{ fontSize: 11, color: 'var(--accent)', flexShrink: 0, marginTop: 2 }}>
                  {shortSha(sha)}
                </code>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: 13, fontWeight: 500, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {firstLine(msg) || '(no message)'}
                  </div>
                  <div style={{ fontSize: 11, color: 'var(--text-dim)', marginTop: 2 }}>
                    {authorName} · {formatDate(date)}
                  </div>
                </div>
              </div>
            </button>
          )
        })}
      </div>

      {hasMore && (
        <button
          type='button'
          className='btn btn-outline btn-sm'
          style={{ marginTop: 10, width: '100%' }}
          onClick={() => loadPage(page + 1)}
          disabled={loading}
        >
          {loading ? 'Đang tải...' : `Tải thêm ${PER_PAGE} commit`}
        </button>
      )}

      {!hasMore && commits.length > 0 && (
        <p className='hint' style={{ textAlign: 'center', marginTop: 8 }}>
          Đã tải hết {commits.length} commit
        </p>
      )}
    </div>
  )
}

export default function CommitListModal({ isOpen, onClose, owner, repo, teamName }) {
  const [selectedSha, setSelectedSha] = useState(null)

  const handleClose = () => {
    setSelectedSha(null)
    onClose()
  }

  return (
    <Modal
      isOpen={isOpen}
      onClose={handleClose}
      title={selectedSha ? `Commit ${shortSha(selectedSha)}` : 'Danh sách commit'}
      subtitle={`${owner}/${repo}${teamName ? ` · ${teamName}` : ''}`}
      className='modal-wide'
    >
      <div style={{ maxHeight: '65vh', overflowY: 'auto', paddingRight: 4 }}>
        {selectedSha ? (
          <CommitDetail
            owner={owner}
            repo={repo}
            sha={selectedSha}
            onBack={() => setSelectedSha(null)}
          />
        ) : (
          <CommitList owner={owner} repo={repo} onSelect={setSelectedSha} />
        )}
      </div>
    </Modal>
  )
}

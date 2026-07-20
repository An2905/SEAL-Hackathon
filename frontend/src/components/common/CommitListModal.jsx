import { useState, useCallback, useEffect, useRef, useMemo } from 'react'
import Modal from './Modal'
import { listCommits, getCommit } from '../../api/githubRepo'
import { useToast } from '../../context/ToastContext'
import { localizeError } from '../../utils/errors'

const PER_PAGE = 20
const FILES_PER_PAGE = 30

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

function commitsFromPageCache(pageCache) {
  return Array.from(pageCache.keys())
    .sort((a, b) => a - b)
    .flatMap((page) => pageCache.get(page) ?? [])
}

function CommitDetail({ owner, repo, sha, detailCacheRef, onBack }) {
  const { showToast } = useToast()
  const cached = detailCacheRef.current.get(sha)
  const [detail, setDetail] = useState(cached?.detail ?? null)
  const [loading, setLoading] = useState(!cached?.detail)
  const [error, setError] = useState(null)
  const [filePage, setFilePage] = useState(cached?.filePage ?? 1)
  const [loadingMore, setLoadingMore] = useState(false)

  const updateCache = useCallback(
    (nextDetail, nextFilePage) => {
      detailCacheRef.current.set(sha, { detail: nextDetail, filePage: nextFilePage })
    },
    [detailCacheRef, sha]
  )

  const loadDetail = useCallback(async () => {
    const hit = detailCacheRef.current.get(sha)
    if (hit?.detail) {
      setDetail(hit.detail)
      setFilePage(hit.filePage ?? 1)
      setLoading(false)
      setError(null)
      return
    }

    setLoading(true)
    setError(null)
    try {
      const data = await getCommit({ owner, repo, ref: sha, perPage: FILES_PER_PAGE, page: 1 })
      setDetail(data)
      setFilePage(1)
      updateCache(data, 1)
    } catch (err) {
      const message = localizeError(err.message)
      setError(message)
      showToast(message, 'error')
    } finally {
      setLoading(false)
    }
  }, [owner, repo, sha, showToast, detailCacheRef, updateCache])

  useEffect(() => {
    loadDetail()
  }, [loadDetail])

  const loadMoreFiles = async () => {
    const next = filePage + 1
    const fileCacheKey = `${sha}:files:${next}`
    const filePageCache = detailCacheRef.current.get(fileCacheKey)
    if (filePageCache) {
      const merged = {
        ...detail,
        files: [...(detail?.files ?? []), ...filePageCache]
      }
      setDetail(merged)
      setFilePage(next)
      updateCache(merged, next)
      return
    }

    setLoadingMore(true)
    try {
      const data = await getCommit({ owner, repo, ref: sha, perPage: FILES_PER_PAGE, page: next })
      const newFiles = data?.files ?? []
      detailCacheRef.current.set(fileCacheKey, newFiles)
      const merged = {
        ...detail,
        files: [...(detail?.files ?? []), ...newFiles]
      }
      setDetail(merged)
      setFilePage(next)
      updateCache(merged, next)
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

      {loading && <p className='hint'>Đang tải chi tiết commit...</p>}
      {error && !loading && <p className='field-error'>{error}</p>}

      {!loading && !error && detail && (
        <>
          <div style={{ marginBottom: 12 }}>
            <div style={{ fontWeight: 600, fontSize: 14, marginBottom: 4 }}>{commitData.message || '(no message)'}</div>
            <div style={{ fontSize: 12, color: 'var(--text-dim)' }}>
              {commitData.author?.name} · {formatDate(commitData.author?.date)}
            </div>
            <code style={{ fontSize: 11, color: 'var(--text-dim)', marginTop: 4, display: 'block' }}>{detail.sha}</code>
          </div>

          <div style={{ display: 'flex', gap: 16, marginBottom: 12 }}>
            <span style={{ fontSize: 12, color: '#16a34a', fontWeight: 600 }}>+{stats.additions ?? 0}</span>
            <span style={{ fontSize: 12, color: '#dc2626', fontWeight: 600 }}>−{stats.deletions ?? 0}</span>
            <span style={{ fontSize: 12, color: 'var(--text-dim)' }}>
              {stats.total ?? 0} thay đổi · {files.length} file
            </span>
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

          {files.length === FILES_PER_PAGE * filePage && (
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

function CommitList({ commits, page, hasMore, loading, error, initialLoaded, onSelect, onLoadMore }) {
  return (
    <div>
      {!initialLoaded && loading && <p className='hint'>Đang tải commit...</p>}
      {error && <p className='field-error'>{error}</p>}

      {commits.length === 0 && initialLoaded && !loading && !error && <p className='hint'>Không có commit nào.</p>}

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
                  <div
                    style={{
                      fontSize: 13,
                      fontWeight: 500,
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      whiteSpace: 'nowrap'
                    }}
                  >
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
          onClick={onLoadMore}
          disabled={loading}
        >
          {loading ? 'Đang tải...' : `Tải thêm ${PER_PAGE} commit`}
        </button>
      )}

      {!hasMore && commits.length > 0 && (
        <p className='hint' style={{ textAlign: 'center', marginTop: 8 }}>
          Đã tải hết {commits.length} commit{page > 1 ? ` (${page} trang)` : ''}
        </p>
      )}
    </div>
  )
}

export default function CommitListModal({
  isOpen,
  onClose,
  owner,
  repo,
  teamName,
  sha: filterSha,
  author,
  since,
  until
}) {
  const { showToast } = useToast()
  const [selectedSha, setSelectedSha] = useState(null)

  const pageCacheRef = useRef(new Map())
  const detailCacheRef = useRef(new Map())

  const [commits, setCommits] = useState([])
  const [page, setPage] = useState(0)
  const [hasMore, setHasMore] = useState(true)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [initialLoaded, setInitialLoaded] = useState(false)

  const filters = useMemo(
    () => ({
      sha: filterSha || undefined,
      author: author || undefined,
      since: since || undefined,
      until: until || undefined
    }),
    [filterSha, author, since, until]
  )

  const filterKey = useMemo(() => JSON.stringify({ owner, repo, ...filters }), [owner, repo, filters])

  const resetListState = useCallback(() => {
    pageCacheRef.current.clear()
    detailCacheRef.current.clear()
    setSelectedSha(null)
    setCommits([])
    setPage(0)
    setHasMore(true)
    setLoading(false)
    setError(null)
    setInitialLoaded(false)
  }, [])

  const syncCommitsFromCache = useCallback(() => {
    setCommits(commitsFromPageCache(pageCacheRef.current))
  }, [])

  const loadPage = useCallback(
    async (pageNum) => {
      if (pageCacheRef.current.has(pageNum)) {
        syncCommitsFromCache()
        setPage(pageNum)
        setHasMore((pageCacheRef.current.get(pageNum)?.length ?? 0) === PER_PAGE)
        setInitialLoaded(true)
        return
      }

      setLoading(true)
      setError(null)
      try {
        const data = await listCommits({
          owner,
          repo,
          ...filters,
          perPage: PER_PAGE,
          page: pageNum
        })
        const rows = Array.isArray(data) ? data : []
        pageCacheRef.current.set(pageNum, rows)
        syncCommitsFromCache()
        setPage(pageNum)
        setHasMore(rows.length === PER_PAGE)
      } catch (err) {
        const message = localizeError(err.message)
        setError(message)
        showToast(message, 'error')
      } finally {
        setLoading(false)
        setInitialLoaded(true)
      }
    },
    [owner, repo, filters, showToast, syncCommitsFromCache]
  )

  useEffect(() => {
    if (!isOpen) {
      resetListState()
      return undefined
    }

    resetListState()

    let cancelled = false
    ;(async () => {
      setLoading(true)
      setError(null)
      try {
        const data = await listCommits({
          owner,
          repo,
          ...filters,
          perPage: PER_PAGE,
          page: 1
        })
        if (cancelled) return
        const rows = Array.isArray(data) ? data : []
        pageCacheRef.current.set(1, rows)
        setCommits(rows)
        setPage(1)
        setHasMore(rows.length === PER_PAGE)
      } catch (err) {
        if (cancelled) return
        const message = localizeError(err.message)
        setError(message)
        showToast(message, 'error')
      } finally {
        if (!cancelled) {
          setLoading(false)
          setInitialLoaded(true)
        }
      }
    })()

    return () => {
      cancelled = true
    }
  }, [isOpen, filterKey, owner, repo, filters, resetListState, showToast])

  const handleClose = () => {
    resetListState()
    onClose()
  }

  const handleLoadMore = () => {
    if (loading || !hasMore) return
    loadPage(page + 1)
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
            detailCacheRef={detailCacheRef}
            onBack={() => setSelectedSha(null)}
          />
        ) : (
          <CommitList
            commits={commits}
            page={page}
            hasMore={hasMore}
            loading={loading}
            error={error}
            initialLoaded={initialLoaded}
            onSelect={setSelectedSha}
            onLoadMore={handleLoadMore}
          />
        )}
      </div>
    </Modal>
  )
}

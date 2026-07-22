import { useState, useCallback, useEffect, useRef, useMemo } from 'react'
import Modal from './Modal'
import Pagination from './Pagination'
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

/** Exact total once a short page is known; otherwise total is unknown. */
function resolvePagerState(pageCache, currentPage, currentRows) {
  let lastShortPage = null
  let maxPage = 0
  for (const [p, rows] of pageCache.entries()) {
    maxPage = Math.max(maxPage, p)
    if ((rows?.length ?? 0) < PER_PAGE) {
      lastShortPage = lastShortPage == null ? p : Math.max(lastShortPage, p)
    }
  }
  if ((currentRows?.length ?? 0) < PER_PAGE) {
    lastShortPage = lastShortPage == null ? currentPage : Math.max(lastShortPage, currentPage)
  }

  if (lastShortPage != null) {
    let total = 0
    for (let p = 1; p <= lastShortPage; p++) {
      total += pageCache.get(p)?.length ?? 0
    }
    return { total, totalKnown: true, hasNext: false }
  }

  return {
    total: Math.max(maxPage, currentPage) * PER_PAGE,
    totalKnown: false,
    hasNext: true
  }
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
            <div style={{ fontWeight: 600, fontSize: 14, marginBottom: 4 }}>
              {commitData.message || '(Không có nội dung)'}
            </div>
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

function CommitList({
  commits,
  page,
  pagerTotal,
  totalKnown,
  hasNext,
  loading,
  error,
  initialLoaded,
  onSelect,
  onPageChange
}) {
  return (
    <div>
      {!initialLoaded && loading && <p className='hint'>Đang tải commit...</p>}
      {error && <p className='field-error'>{error}</p>}

      {commits.length === 0 && initialLoaded && !loading && !error && <p className='hint'>Không có commit nào.</p>}

      <div className='kv-list' style={{ opacity: loading && initialLoaded ? 0.55 : 1 }}>
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
                    {firstLine(msg) || '(Không có nội dung)'}
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

      {initialLoaded && !error && (commits.length > 0 || pagerTotal > 0) && (
        <Pagination
          total={pagerTotal}
          pageSize={PER_PAGE}
          currentPage={Math.max(1, page)}
          onChange={onPageChange}
          itemLabel='commit'
          showSinglePageSummary
          totalKnown={totalKnown}
          hasNext={hasNext}
          pageItemCount={commits.length}
        />
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
  const [page, setPage] = useState(1)
  const [pagerTotal, setPagerTotal] = useState(0)
  const [totalKnown, setTotalKnown] = useState(false)
  const [hasNext, setHasNext] = useState(false)
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
    setPage(1)
    setPagerTotal(0)
    setTotalKnown(false)
    setHasNext(false)
    setLoading(false)
    setError(null)
    setInitialLoaded(false)
  }, [])

  const applyPageState = useCallback((pageNum, rows) => {
    setCommits(rows)
    setPage(pageNum)
    const pager = resolvePagerState(pageCacheRef.current, pageNum, rows)
    setPagerTotal(pager.total)
    setTotalKnown(pager.totalKnown)
    setHasNext(pager.hasNext)
  }, [])

  const loadPage = useCallback(
    async (pageNum) => {
      if (pageNum < 1) return

      if (pageCacheRef.current.has(pageNum)) {
        applyPageState(pageNum, pageCacheRef.current.get(pageNum) ?? [])
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
        applyPageState(pageNum, rows)
      } catch (err) {
        const message = localizeError(err.message)
        setError(message)
        showToast(message, 'error')
      } finally {
        setLoading(false)
        setInitialLoaded(true)
      }
    },
    [owner, repo, filters, showToast, applyPageState]
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
        applyPageState(1, rows)
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
  }, [isOpen, filterKey, owner, repo, filters, resetListState, showToast, applyPageState])

  const handleClose = () => {
    resetListState()
    onClose()
  }

  const handlePageChange = (nextPage) => {
    if (loading || nextPage === page) return
    loadPage(nextPage)
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
            pagerTotal={pagerTotal}
            totalKnown={totalKnown}
            hasNext={hasNext}
            loading={loading}
            error={error}
            initialLoaded={initialLoaded}
            onSelect={setSelectedSha}
            onPageChange={handlePageChange}
          />
        )}
      </div>
    </Modal>
  )
}

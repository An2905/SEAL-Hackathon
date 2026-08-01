import { useState } from 'react'

/**
 * Pagination
 *
 * Props:
 *   total            — total items (used when totalKnown)
 *   pageSize         — items per page (default 5)
 *   currentPage      — 1-based current page
 *   onChange         — (page: number) => void
 *   totalKnown       — false to hide "/ N" (e.g. GitHub list commits)
 *   hasNext          — enable next when totalKnown is false
 *   pageItemCount    — items on current page (accurate from–to)
 *
 * Usage:
 *   <Pagination total={22} pageSize={5} currentPage={page} onChange={setPage} />
 */
export default function Pagination({
  total,
  pageSize = 5,
  currentPage,
  onChange,
  className,
  itemLabel = 'mục',
  showSinglePageSummary = false,
  totalKnown = true,
  hasNext = false,
  pageItemCount
}) {
  const totalPages = totalKnown
    ? Math.max(1, Math.ceil((total || 0) / pageSize))
    : Math.max(1, currentPage + (hasNext ? 1 : 0))

  const itemsOnPage =
    pageItemCount != null
      ? pageItemCount
      : totalKnown
        ? Math.max(0, Math.min(pageSize, (total || 0) - (currentPage - 1) * pageSize))
        : pageSize

  const from = itemsOnPage === 0 ? 0 : (currentPage - 1) * pageSize + 1
  const to = itemsOnPage === 0 ? 0 : from + itemsOnPage - 1

  const summary =
    totalKnown && (total || 0) > 0
      ? `Hiển thị ${from}–${to} / ${total} ${itemLabel}`
      : from === 0
        ? `Không có ${itemLabel}`
        : `Hiển thị ${from}–${to} ${itemLabel}`

  const canGoNext = totalKnown ? currentPage < totalPages : Boolean(hasNext)
  const showControls = totalPages > 1 || (!totalKnown && hasNext)

  if (!showControls && !showSinglePageSummary) return null
  if (!showControls && showSinglePageSummary && from === 0 && !(totalKnown && total > 0)) return null

  return (
    <div className={className} style={wrapStyle}>
      <span style={infoStyle}>{summary}</span>

      {showControls && (
        <div style={btnGroupStyle}>
          <PageBtn onClick={() => onChange(currentPage - 1)} disabled={currentPage === 1} label='←' />

          {buildPages(currentPage, totalPages).map((p, i) =>
            p === '...' ? (
              <span key={`ellipsis-${i}`} style={ellipsisStyle}>
                …
              </span>
            ) : (
              <PageBtn key={p} onClick={() => onChange(p)} active={p === currentPage} label={String(p)} />
            )
          )}

          <PageBtn onClick={() => onChange(currentPage + 1)} disabled={!canGoNext} label='→' />
        </div>
      )}
    </div>
  )
}

function PageBtn({ onClick, disabled, active, label }) {
  const [hovered, setHovered] = useState(false)
  return (
    <button
      type='button'
      onClick={onClick}
      disabled={disabled}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      style={{
        minWidth: 32,
        height: 32,
        padding: '0 8px',
        border: '1px solid var(--border,#e2e8f0)',
        borderRadius: 6,
        fontSize: 13,
        cursor: disabled ? 'default' : 'pointer',
        background: active
          ? 'var(--accent,#2563eb)'
          : hovered && !disabled
            ? 'var(--surface-alt,#f7f8fa)'
            : 'var(--card-bg,#fff)',
        color: active ? '#fff' : disabled ? 'var(--text-mute,#a0aec0)' : 'var(--text,#1a202c)',
        fontWeight: active ? 600 : 400,
        opacity: disabled ? 0.45 : 1,
        transition: 'background .12s, color .12s'
      }}
    >
      {label}
    </button>
  )
}

// Build page list: [1, 2, 3, ..., current, ..., N].
// Current page is always visible. Show all if total <= 4.
function buildPages(current, total) {
  if (total <= 4) return Array.from({ length: total }, (_, i) => i + 1)
  // Current page is within first 3 or is the last page → no mid section needed
  if (current <= 3 || current === total) return [1, 2, 3, '...', total]
  // Current page is page 4 and last page is 5 → [1, 2, 3, 4, 5], no ellipsis
  if (current === 4 && total === 5) return [1, 2, 3, 4, 5]
  // Current page sits between first 3 and last page
  return [1, 2, 3, '...', current, '...', total]
}

const wrapStyle = {
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'space-between',
  flexWrap: 'wrap',
  gap: 12,
  marginTop: 16,
  padding: '12px 0'
}
const infoStyle = { fontSize: 12, color: 'var(--text-dim,#718096)' }
const btnGroupStyle = { display: 'flex', gap: 4, alignItems: 'center' }
const ellipsisStyle = { fontSize: 13, color: 'var(--text-mute,#a0aec0)', padding: '0 4px' }

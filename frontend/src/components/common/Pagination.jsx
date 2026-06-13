import { useState } from 'react'

/**
 * Pagination
 *
 * Props:
 *   total        — total number of items
 *   pageSize     — items per page (default 5)
 *   currentPage  — 1-based current page
 *   onChange     — (page: number) => void
 *
 * Usage:
 *   <Pagination total={22} pageSize={5} currentPage={page} onChange={setPage} />
 */
export default function Pagination({ total, pageSize = 5, currentPage, onChange }) {
  const totalPages = Math.ceil(total / pageSize)
  if (totalPages <= 1) return null

  const from = (currentPage - 1) * pageSize + 1
  const to = Math.min(currentPage * pageSize, total)

  // Build page number array with ellipsis
  const pages = buildPages(currentPage, totalPages)

  return (
    <div style={wrapStyle}>
      <span style={infoStyle}>
        Hiển thị {from}–{to} / {total} mục
      </span>

      <div style={btnGroupStyle}>
        <PageBtn onClick={() => onChange(currentPage - 1)} disabled={currentPage === 1} label='←' />

        {pages.map((p, i) =>
          p === '...' ? (
            <span key={`ellipsis-${i}`} style={ellipsisStyle}>…</span>
          ) : (
            <PageBtn
              key={p}
              onClick={() => onChange(p)}
              active={p === currentPage}
              label={String(p)}
            />
          )
        )}

        <PageBtn onClick={() => onChange(currentPage + 1)} disabled={currentPage === totalPages} label='→' />
      </div>
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
        minWidth: 32, height: 32, padding: '0 8px',
        border: '1px solid var(--border,#e2e8f0)',
        borderRadius: 6, fontSize: 13, cursor: disabled ? 'default' : 'pointer',
        background: active
          ? 'var(--accent,#2563eb)'
          : hovered && !disabled
            ? 'var(--surface-alt,#f7f8fa)'
            : 'var(--card-bg,#fff)',
        color: active ? '#fff' : disabled ? 'var(--text-mute,#a0aec0)' : 'var(--text,#1a202c)',
        fontWeight: active ? 600 : 400,
        opacity: disabled ? 0.45 : 1,
        transition: 'background .12s, color .12s',
      }}
    >
      {label}
    </button>
  )
}

// Build page list with ellipsis: [1, '...', 4, 5, 6, '...', 12]
function buildPages(current, total) {
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1)
  const pages = []
  pages.push(1)
  if (current > 3) pages.push('...')
  for (let p = Math.max(2, current - 1); p <= Math.min(total - 1, current + 1); p++) {
    pages.push(p)
  }
  if (current < total - 2) pages.push('...')
  pages.push(total)
  return pages
}

const wrapStyle = {
  display: 'flex', alignItems: 'center', justifyContent: 'space-between',
  flexWrap: 'wrap', gap: 12, marginTop: 16, padding: '12px 0',
}
const infoStyle = { fontSize: 12, color: 'var(--text-dim,#718096)' }
const btnGroupStyle = { display: 'flex', gap: 4, alignItems: 'center' }
const ellipsisStyle = { fontSize: 13, color: 'var(--text-mute,#a0aec0)', padding: '0 4px' }

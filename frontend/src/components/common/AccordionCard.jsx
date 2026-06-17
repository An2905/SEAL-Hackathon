import { useId, useState } from 'react'

/**
 * AccordionCard
 *
 * Props:
 *   title      — string, shown in header always
 *   badge      — optional ReactNode (status pill, count, etc.)
 *   children   — expanded content
 *   defaultOpen — boolean (default false)
 *
 * Usage:
 *   <AccordionCard title="SEAL Hackathon Spring 2026" badge={<StatusPill status="ONGOING" />}>
 *     <p>Start: ...</p>
 *   </AccordionCard>
 */
export default function AccordionCard({ title, badge, children, defaultOpen = false }) {
  const [open, setOpen] = useState(defaultOpen)
  const headerId = useId()
  const bodyId = useId()

  return (
    <div style={cardStyle}>
      {/* Header row — always visible */}
      <button
        type='button'
        id={headerId}
        onClick={() => setOpen((v) => !v)}
        style={headerStyle(open)}
        onMouseEnter={(e) => {
          if (!open) e.currentTarget.style.background = 'var(--surface-alt,#f7f8fa)'
        }}
        onMouseLeave={(e) => {
          if (!open) e.currentTarget.style.background = 'var(--card-bg,#fff)'
        }}
        aria-expanded={open}
        aria-controls={bodyId}
      >
        {/* Chevron */}
        <svg
          width='14'
          height='14'
          viewBox='0 0 14 14'
          fill='none'
          style={{
            flexShrink: 0,
            transform: open ? 'rotate(90deg)' : 'none',
            transition: 'transform .18s',
            color: 'var(--text-dim,#718096)'
          }}
        >
          <path d='M4 2l6 5-6 5' stroke='currentColor' strokeWidth='1.6' strokeLinecap='round' strokeLinejoin='round' />
        </svg>

        <span style={{ flex: 1, fontSize: 14, fontWeight: 500, color: 'var(--text,#1a202c)', textAlign: 'left' }}>
          {title}
        </span>

        {badge && <span style={{ flexShrink: 0 }}>{badge}</span>}
      </button>

      {/* Expanded body — height-animated via CSS grid trick, kept mounted */}
      <div
        id={bodyId}
        role='region'
        aria-labelledby={headerId}
        aria-hidden={!open}
        className={`accordion-body${open ? ' is-open' : ''}`}
      >
        <div className='accordion-body-inner'>
          <div style={bodyStyle}>{children}</div>
        </div>
      </div>
    </div>
  )
}

const cardStyle = {
  border: '1px solid var(--border,#e2e8f0)',
  borderRadius: 8,
  overflow: 'hidden',
  background: 'var(--card-bg,#fff)',
  marginBottom: 8
}

const headerStyle = (open) => ({
  display: 'flex',
  alignItems: 'center',
  gap: 10,
  width: '100%',
  padding: '12px 16px',
  background: open ? 'var(--surface-alt,#f7f8fa)' : 'var(--card-bg,#fff)',
  border: 'none',
  borderBottom: open ? '1px solid var(--border-soft,#f0f0f0)' : 'none',
  cursor: 'pointer',
  transition: 'background .15s'
})

const bodyStyle = {
  padding: '14px 16px'
}

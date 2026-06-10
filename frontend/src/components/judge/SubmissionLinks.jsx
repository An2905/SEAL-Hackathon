const LINKS = [
  { key: 'githubUrl', label: 'GitHub' },
  { key: 'demoUrl', label: 'Demo' },
  { key: 'reportUrl', label: 'Báo cáo' },
  { key: 'slideUrl', label: 'Slide' }
]

export default function SubmissionLinks({ team, className = '' }) {
  const items = LINKS.filter((l) => team?.[l.key]?.trim())
  if (!items.length) return null

  return (
    <div
      className={`submission-links ${className}`.trim()}
      style={{ marginTop: 6, display: 'flex', flexWrap: 'wrap', gap: 6 }}
    >
      {items.map((l) => (
        <a
          key={l.key}
          href={team[l.key]}
          target='_blank'
          rel='noopener noreferrer'
          className='btn btn-outline btn-sm'
          style={{ fontSize: 11, padding: '2px 8px' }}
        >
          {l.label}
        </a>
      ))}
    </div>
  )
}

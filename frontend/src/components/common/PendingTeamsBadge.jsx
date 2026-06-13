export default function PendingTeamsBadge({ count }) {
  const pending = Number(count) || 0
  if (pending <= 0) return null

  return (
    <span className='pending-teams-badge' title={`${pending} đội chờ duyệt`} aria-label={`${pending} đội chờ duyệt`}>
      {pending > 99 ? '99+' : pending}
    </span>
  )
}

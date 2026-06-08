function displayName(item) {
  return item?.fullName || item?.email || item?.userId || '—'
}

function ExpertStaffChip({ person, roleLabel }) {
  const label = displayName(person)
  const title = person.email && person.fullName ? `${label} · ${person.email}` : label

  return (
    <span
      className={`expert-staff-chip expert-staff-chip--${roleLabel.toLowerCase()}${person.self ? ' is-self' : ''}`}
      title={title}
    >
      {label}
      {person.self ? <span className='expert-staff-chip-you'>Bạn</span> : null}
    </span>
  )
}

function ExpertStaffRow({ title, count, people, roleLabel, emptyText }) {
  return (
    <div className='expert-staff-row'>
      <span className='expert-staff-row-label'>
        {title} <span className='expert-staff-row-count'>{count}</span>
      </span>
      <div className='expert-staff-chips'>
        {people.length === 0 ? (
          <span className='expert-staff-empty'>{emptyText}</span>
        ) : (
          people.map((person) => (
            <ExpertStaffChip key={`${roleLabel}-${person.userId}`} person={person} roleLabel={roleLabel} />
          ))
        )}
      </div>
    </div>
  )
}

export default function ExpertGroupColleaguesBoard({ colleagues, loading, error }) {
  if (loading) {
    return <div className='expert-staff-compact expert-staff-compact--loading'>Đang tải…</div>
  }

  if (error) {
    return <div className='expert-staff-compact expert-staff-compact--error'>{error}</div>
  }

  if (!colleagues) {
    return null
  }

  const mentors = colleagues.mentors ?? []
  const judges = colleagues.judges ?? []

  return (
    <div className='expert-staff-compact'>
      <ExpertStaffRow
        title='Mentor'
        count={mentors.length}
        people={mentors}
        roleLabel='Mentor'
        emptyText='Chưa có'
      />
      <ExpertStaffRow
        title='Judge'
        count={judges.length}
        people={judges}
        roleLabel='Judge'
        emptyText='Chưa có'
      />
    </div>
  )
}

function displayName(item) {
  return item?.fullName || item?.email || item?.userId || '—'
}

function ExpertColleagueCard({ person, roleLabel }) {
  const name = displayName(person)
  const email = person?.email?.trim() || ''

  return (
    <div
      className={`expert-colleague-card expert-colleague-card--${roleLabel.toLowerCase()}${person.self ? ' is-self' : ''}`}
    >
      <div className='expert-colleague-card-head'>
        <span className='expert-colleague-card-name'>{name}</span>
        {person.self ? <span className='expert-colleague-card-you'>Bạn</span> : null}
      </div>
      <div className='expert-colleague-card-email'>{email || '—'}</div>
    </div>
  )
}

function ExpertColleaguesRow({ title, count, people, roleLabel, emptyText }) {
  return (
    <div className={`expert-colleagues-row expert-colleagues-row--${roleLabel.toLowerCase()}`}>
      <div className='expert-colleagues-row-label'>
        <span className={`expert-colleagues-pill expert-colleagues-pill--${roleLabel.toLowerCase()}`}>
          {title}
        </span>
        <span className='expert-colleagues-count'>{count}</span>
      </div>
      <div className='expert-colleagues-track'>
        {people.length === 0 ? (
          <span className='expert-colleagues-empty'>{emptyText}</span>
        ) : (
          people.map((person) => (
            <ExpertColleagueCard
              key={`${roleLabel}-${person.userId}`}
              person={person}
              roleLabel={roleLabel}
            />
          ))
        )}
      </div>
    </div>
  )
}

export default function ExpertGroupColleaguesBoard({ colleagues, loading, error }) {
  if (loading) {
    return <div className='expert-colleagues-board expert-colleagues-board--loading'>Đang tải…</div>
  }

  if (error) {
    return <div className='expert-colleagues-board expert-colleagues-board--error'>{error}</div>
  }

  if (!colleagues) {
    return null
  }

  const mentors = colleagues.mentors ?? []
  const judges = colleagues.judges ?? []

  return (
    <div className='expert-colleagues-board'>
      <ExpertColleaguesRow
        title='Mentor'
        count={mentors.length}
        people={mentors}
        roleLabel='Mentor'
        emptyText='Chưa có mentor'
      />
      <ExpertColleaguesRow
        title='Judge'
        count={judges.length}
        people={judges}
        roleLabel='Judge'
        emptyText='Chưa có judge'
      />
    </div>
  )
}

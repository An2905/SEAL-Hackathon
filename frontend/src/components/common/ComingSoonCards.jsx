export default function ComingSoonCards({ cards }) {
  return (
    <div className='cards'>
      {cards.map((card) => (
        <div className='card' key={card.title}>
          <div className='card-head'>
            <div className='card-title'>{card.title}</div>
          </div>
          <p className='card-sub'>{card.desc}</p>
          <div className='card-actions'>
            <span className='card-badge'>Sắp ra mắt</span>
          </div>
        </div>
      ))}
    </div>
  )
}

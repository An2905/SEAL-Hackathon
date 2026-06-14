export default function DashboardHeader({ title, subtitle }) {
  if (!title && !subtitle) return null

  return (
    <div className='dashboard-header'>
      <div className='dashboard-title'>
        {title && <h1>{title}</h1>}
        {subtitle && <p>{subtitle}</p>}
      </div>
    </div>
  )
}

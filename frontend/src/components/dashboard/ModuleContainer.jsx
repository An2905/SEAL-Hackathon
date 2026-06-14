export default function ModuleContainer({ children }) {
  return (
    <main className='dashboard'>
      <div className='dashboard-frame'>{children}</div>
    </main>
  )
}

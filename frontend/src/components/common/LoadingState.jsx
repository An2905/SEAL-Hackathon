/**
 * LoadingState
 *
 * Inline loading indicator: a small dark spinner next to a text label.
 * Drop-in replacement for bare "Đang tải…" text nodes.
 *
 * Props:
 *   text      — label shown next to the spinner (default 'Đang tải…')
 *   className — wrapper class(es), combined with 'loading-state' (default 'empty-state')
 *   style     — optional inline style passed through to the wrapper
 */
export default function LoadingState({ text = 'Đang tải…', className = 'empty-state', style }) {
  return (
    <div className={`${className} loading-state`} style={style}>
      <span className='spinner spinner-dark' />
      {text}
    </div>
  )
}

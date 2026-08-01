import { useEffect } from 'react'
import { createPortal } from 'react-dom'

let modalStackDepth = 0

export default function Modal({ isOpen, onClose, title, subtitle, children, className }) {
  useEffect(() => {
    if (!isOpen) return undefined
    modalStackDepth += 1
    document.body.style.overflow = 'hidden'
    return () => {
      modalStackDepth = Math.max(0, modalStackDepth - 1)
      if (modalStackDepth === 0) {
        document.body.style.overflow = ''
      }
    }
  }, [isOpen])

  useEffect(() => {
    if (!isOpen) return undefined
    const handler = (e) => {
      if (e.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', handler)
    return () => document.removeEventListener('keydown', handler)
  }, [isOpen, onClose])

  if (!isOpen) return null

  return createPortal(
    <div
      className='modal-overlay'
      // Nested modals need to sit above the parent overlay.
      style={{ zIndex: 100 + modalStackDepth }}
      onMouseDown={(e) => {
        // Use mousedown so a click that closes this modal cannot "fall through"
        // to a parent overlay underneath after unmount.
        if (e.target === e.currentTarget) onClose()
      }}
    >
      <div
        className={className ? `modal ${className}` : 'modal'}
        onMouseDown={(e) => e.stopPropagation()}
      >
        <button className='modal-close' onClick={onClose} aria-label='Đóng'>
          &times;
        </button>
        <h2>{title}</h2>
        {subtitle && <p className='modal-sub'>{subtitle}</p>}
        {children}
      </div>
    </div>,
    document.body
  )
}

import Modal from './Modal'
import LoadingButton from './LoadingButton'

/**
 * ConfirmModal
 *
 * Styled replacement for window.confirm(). Renders a small modal with a
 * message and Hủy/confirm action pair.
 *
 * Props:
 *   isOpen       — boolean
 *   onClose      — () => void, called on Hủy / overlay click / Escape
 *   onConfirm    — () => void | Promise<void>, called when the confirm button is clicked
 *   title        — modal title (default 'Xác nhận')
 *   message      — body text
 *   confirmLabel — confirm button label (default 'Xác nhận')
 *   cancelLabel  — cancel button label (default 'Hủy')
 *   loading      — shows a spinner on the confirm button and disables both buttons
 *   danger       — styles the confirm button as btn-danger instead of btn-primary
 */
export default function ConfirmModal({
  isOpen,
  onClose,
  onConfirm,
  title = 'Xác nhận',
  message,
  confirmLabel = 'Xác nhận',
  cancelLabel = 'Hủy',
  loading = false,
  danger = false
}) {
  return (
    <Modal isOpen={isOpen} onClose={onClose} title={title}>
      {message && <p className='modal-sub'>{message}</p>}
      <div className='form-actions'>
        <button type='button' className='btn btn-outline' onClick={onClose} disabled={loading}>
          {cancelLabel}
        </button>
        <LoadingButton
          loading={loading}
          type='button'
          className={`btn ${danger ? 'btn-danger' : 'btn-primary'}`}
          onClick={onConfirm}
        >
          {confirmLabel}
        </LoadingButton>
      </div>
    </Modal>
  )
}

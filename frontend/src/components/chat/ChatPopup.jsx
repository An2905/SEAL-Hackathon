import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import FormMessage from '../common/FormMessage'
import LoadingButton from '../common/LoadingButton'
import { chatErrorMessage, getChatMessages, listChatRooms, openChatRoom } from '../../api/chat'
import { useAuth } from '../../context/AuthContext'
import { useChatStomp } from '../../hooks/useChatStomp'
import { localizeError } from '../../utils/errors'
import { parseJwt } from '../../utils/jwt'

function formatDateTime(value) {
  if (!value) return ''
  const d = new Date(String(value).replace(' ', 'T'))
  if (Number.isNaN(d.getTime())) return String(value)
  return d.toLocaleString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function renderMessageContent(content) {
  return String(content || '')
    .split(/(https?:\/\/\S+)/g)
    .map((part, i) => {
      if (/^https?:\/\//.test(part)) {
        return (
          <a key={i} href={part} target='_blank' rel='noopener noreferrer'>
            {part}
          </a>
        )
      }
      return part
    })
}

function ChatIcon() {
  return (
    <svg width='18' height='18' viewBox='0 0 24 24' fill='none' aria-hidden='true'>
      <path
        d='M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z'
        stroke='currentColor'
        strokeWidth='1.75'
        strokeLinecap='round'
        strokeLinejoin='round'
      />
    </svg>
  )
}

export function ChatOpenButton({ onClick, title = 'Nhắn tin' }) {
  return (
    <button type='button' className='chat-open-btn' onClick={onClick} title={title} aria-label={title}>
      <ChatIcon />
    </button>
  )
}

export default function ChatPopup({
  open,
  onClose,
  eventId,
  eventTitle,
  mentorId,
  mentorName,
  mode = 'student',
  teamName: teamNameProp = ''
}) {
  const { auth } = useAuth()
  const currentUserId = useMemo(() => {
    const claims = parseJwt(auth.token)
    return claims?.userId ? String(claims.userId) : ''
  }, [auth.token])

  const [rooms, setRooms] = useState([])
  const [selectedRoomId, setSelectedRoomId] = useState('')
  const [selectedRoom, setSelectedRoom] = useState(null)
  const [messages, setMessages] = useState([])
  const [loading, setLoading] = useState(false)
  const [loadingMessages, setLoadingMessages] = useState(false)
  const [messageText, setMessageText] = useState('')
  const [sending, setSending] = useState(false)
  const [error, setError] = useState(null)

  const messagesEndRef = useRef(null)
  const roomClosed = selectedRoom?.status === 'CLOSED'
  const isMentorMode = mode === 'mentor'

  const scrollToBottom = useCallback(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [])

  const handleIncoming = useCallback((msg) => {
    setMessages((prev) => {
      if (prev.some((m) => m.messageId === msg.messageId)) return prev
      return [...prev, msg]
    })
  }, [])

  const { connected, sendMessage } = useChatStomp({
    roomId: open && selectedRoomId ? selectedRoomId : '',
    onMessage: handleIncoming
  })

  useEffect(() => {
    if (!open) return undefined

    setMessages([])
    setMessageText('')
    setError(null)
    setSelectedRoomId('')
    setSelectedRoom(null)
    setRooms([])

    let cancelled = false
    ;(async () => {
      setLoading(true)
      try {
        if (isMentorMode) {
          const data = await listChatRooms()
          if (cancelled) return
          setRooms(data)
          if (data.length > 0) {
            setSelectedRoomId(data[0].roomId)
            setSelectedRoom(data[0])
          }
        } else {
          const room = await openChatRoom({ eventId, mentorId })
          if (cancelled) return
          setSelectedRoomId(room.roomId)
          setSelectedRoom(room)
          setRooms([room])
        }
      } catch (err) {
        if (!cancelled) setError(localizeError(chatErrorMessage(err)))
      } finally {
        if (!cancelled) setLoading(false)
      }
    })()

    return () => {
      cancelled = true
    }
  }, [open, eventId, mentorId, isMentorMode])

  useEffect(() => {
    if (!open || !selectedRoomId) {
      setMessages([])
      return undefined
    }

    let cancelled = false
    ;(async () => {
      setLoadingMessages(true)
      setError(null)
      try {
        const data = await getChatMessages(selectedRoomId)
        if (!cancelled) {
          setMessages(data)
          setTimeout(scrollToBottom, 50)
        }
      } catch (err) {
        if (!cancelled) setError(localizeError(chatErrorMessage(err)))
      } finally {
        if (!cancelled) setLoadingMessages(false)
      }
    })()

    return () => {
      cancelled = true
    }
  }, [open, selectedRoomId, scrollToBottom])

  useEffect(() => {
    scrollToBottom()
  }, [messages, scrollToBottom])

  const handleRoomChange = (roomId) => {
    setSelectedRoomId(roomId)
    setSelectedRoom(rooms.find((r) => r.roomId === roomId) || null)
  }

  const handleSend = async (e) => {
    e.preventDefault()
    const text = messageText.trim()
    if (!text || roomClosed) return
    setSending(true)
    setError(null)
    try {
      sendMessage(text)
      setMessageText('')
    } catch (err) {
      setError(localizeError(err.message))
    } finally {
      setSending(false)
    }
  }

  if (!open) return null

  const displayTeamName = teamNameProp || selectedRoom?.teamName || ''
  const displayMentorName = selectedRoom?.mentorName || mentorName || ''
  const title =
    displayTeamName && displayMentorName
      ? `${displayTeamName} · ${displayMentorName}`
      : displayTeamName || displayMentorName || 'Chat'
  const subtitle = selectedRoom?.eventTitle || eventTitle || ''

  return (
    <div className='chat-popup'>
      <div className='chat-popup-header'>
        <div className='chat-popup-header-text'>
          <div className='chat-popup-title'>{title}</div>
          <div className='chat-popup-sub'>
            {subtitle}
            {selectedRoomId && <span>{connected ? ' · Đã kết nối' : ' · Đang kết nối...'}</span>}
          </div>
        </div>
        <button type='button' className='chat-popup-close' onClick={onClose} aria-label='Đóng'>
          ×
        </button>
      </div>

      {rooms.length > 1 && (
        <div className='chat-popup-round'>
          <select value={selectedRoomId} onChange={(e) => handleRoomChange(e.target.value)}>
            {rooms.map((room) => (
              <option key={room.roomId} value={room.roomId}>
                {[room.teamName, room.mentorName, room.roundName].filter(Boolean).join(' · ')}
                {room.status === 'CLOSED' ? ' (đã đóng)' : ''}
              </option>
            ))}
          </select>
        </div>
      )}

      <div className='chat-popup-body'>
        {loading && <div className='empty-state'>Đang mở phòng chat...</div>}

        {!loading && !selectedRoomId && (
          <div className='chat-popup-empty'>
            <p>{isMentorMode ? 'Chưa có phòng chat nào.' : 'Không thể mở phòng chat.'}</p>
          </div>
        )}

        {!loading && selectedRoomId && (
          <>
            {roomClosed && (
              <div className='form-message error chat-popup-closed'>Phòng đã đóng — chỉ xem tin nhắn.</div>
            )}
            <div className='chat-messages chat-popup-messages'>
              {loadingMessages && <div className='empty-state'>Đang tải tin nhắn...</div>}
              {!loadingMessages && messages.length === 0 && (
                <div className='empty-state'>Gửi tin nhắn hoặc link cho mentor.</div>
              )}
              {!loadingMessages &&
                messages.map((msg) => {
                  const isMentorMsg = String(msg.senderId) === String(selectedRoom?.mentorId || mentorId)
                  const isRight = isMentorMode ? isMentorMsg : !isMentorMsg
                  const isOwn = currentUserId && String(msg.senderId) === currentUserId
                  const showSender = !isRight || (!isOwn && !isMentorMsg)
                  return (
                    <div
                      key={msg.messageId}
                      className={`chat-message-row${isRight ? ' chat-message-row--own' : ' chat-message-row--other'}`}
                    >
                      {showSender && <div className='chat-message-sender'>{msg.senderName || msg.senderId}</div>}
                      <div className='chat-message-bubble'>{renderMessageContent(msg.content)}</div>
                      <div className='chat-message-time'>{formatDateTime(msg.createdAt)}</div>
                    </div>
                  )
                })}
              <div ref={messagesEndRef} />
            </div>
            {!roomClosed && (
              <form className='chat-send-form chat-popup-send' onSubmit={handleSend}>
                <input
                  type='text'
                  value={messageText}
                  onChange={(e) => setMessageText(e.target.value)}
                  placeholder='Nhập tin nhắn hoặc dán link...'
                  maxLength={2000}
                />
                <LoadingButton loading={sending} type='submit' className='btn btn-primary chat-send-btn'>
                  Gửi
                </LoadingButton>
              </form>
            )}
            <FormMessage message={error} type='error' />
          </>
        )}
      </div>
    </div>
  )
}

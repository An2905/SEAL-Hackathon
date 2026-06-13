import { useCallback, useEffect, useRef, useState } from 'react'
import FormField from '../common/FormField'
import FormMessage from '../common/FormMessage'
import LoadingButton from '../common/LoadingButton'
import { chatErrorMessage, createChatRoom, getChatMessages, listChatRooms } from '../../api/chat'
import { getTeamRegistrations, getTeamTrackMentors, getTeamRounds } from '../../api/team'
import { useChatStomp } from '../../hooks/useChatStomp'
import { localizeError } from '../../utils/errors'

function formatDateTime(value) {
  if (!value) return ''
  const d = new Date(value.replace(' ', 'T'))
  if (Number.isNaN(d.getTime())) return String(value)
  return d.toLocaleString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

export default function TeamChatPanel({ isLeader }) {
  const [rooms, setRooms] = useState([])
  const [selectedRoomId, setSelectedRoomId] = useState('')
  const [messages, setMessages] = useState([])
  const [loadingRooms, setLoadingRooms] = useState(true)
  const [loadingMessages, setLoadingMessages] = useState(false)
  const [messageText, setMessageText] = useState('')
  const [sending, setSending] = useState(false)
  const [error, setError] = useState(null)
  const [formError, setFormError] = useState(null)
  const [creating, setCreating] = useState(false)

  const [registrations, setRegistrations] = useState([])
  const [rounds, setRounds] = useState([])
  const [mentors, setMentors] = useState([])
  const [form, setForm] = useState({ eventId: '', roundId: '', mentorId: '' })

  const messagesEndRef = useRef(null)
  const selectedRoom = rooms.find((r) => r.roomId === selectedRoomId)
  const roomClosed = selectedRoom?.status === 'CLOSED'

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
    roomId: selectedRoomId,
    onMessage: handleIncoming
  })

  const loadRooms = useCallback(async () => {
    setLoadingRooms(true)
    setError(null)
    try {
      const data = await listChatRooms()
      setRooms(data)
      if (data.length && !selectedRoomId) {
        setSelectedRoomId(data[0].roomId)
      }
    } catch (err) {
      setError(localizeError(chatErrorMessage(err)))
    } finally {
      setLoadingRooms(false)
    }
  }, [selectedRoomId])

  useEffect(() => {
    loadRooms()
  }, [loadRooms])

  useEffect(() => {
    if (!selectedRoomId) {
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
  }, [selectedRoomId, scrollToBottom])

  useEffect(() => {
    scrollToBottom()
  }, [messages, scrollToBottom])

  useEffect(() => {
    if (!isLeader) return undefined
    let cancelled = false
    ;(async () => {
      try {
        const regs = await getTeamRegistrations()
        if (!cancelled) setRegistrations(regs.filter((r) => r.registrationStatus === 'APPROVED'))
      } catch {
        // optional for create form
      }
    })()
    return () => {
      cancelled = true
    }
  }, [isLeader])

  useEffect(() => {
    if (!form.eventId) {
      setRounds([])
      setMentors([])
      return undefined
    }
    let cancelled = false
    ;(async () => {
      try {
        const [roundData, mentorData] = await Promise.all([
          getTeamRounds(form.eventId),
          getTeamTrackMentors(form.eventId)
        ])
        if (!cancelled) {
          setRounds(roundData)
          setMentors(mentorData.mentors || [])
        }
      } catch {
        if (!cancelled) {
          setRounds([])
          setMentors([])
        }
      }
    })()
    return () => {
      cancelled = true
    }
  }, [form.eventId])

  const handleCreateRoom = async (e) => {
    e.preventDefault()
    setFormError(null)
    if (!form.eventId || !form.roundId || !form.mentorId) {
      setFormError('Vui lòng chọn event, round và mentor.')
      return
    }
    setCreating(true)
    try {
      const room = await createChatRoom(form)
      await loadRooms()
      setSelectedRoomId(room.roomId)
      setForm({ eventId: '', roundId: '', mentorId: '' })
    } catch (err) {
      setFormError(localizeError(chatErrorMessage(err)))
    } finally {
      setCreating(false)
    }
  }

  const handleSend = async (e) => {
    e.preventDefault()
    if (!messageText.trim() || roomClosed) return
    setSending(true)
    setError(null)
    try {
      sendMessage(messageText.trim())
      setMessageText('')
    } catch (err) {
      setError(localizeError(err.message))
    } finally {
      setSending(false)
    }
  }

  return (
    <div className='card team-chat-panel'>
      <div className='card-head'>
        <div>
          <div className='card-title'>Chat mentor &amp; đội</div>
          <div className='card-sub'>{connected ? 'Realtime đã kết nối' : 'Đang kết nối WebSocket...'}</div>
        </div>
      </div>

      {isLeader && (
        <form className='chat-create-form' onSubmit={handleCreateRoom}>
          <div className='chat-create-grid'>
            <FormField label='Sự kiện'>
              <select
                value={form.eventId}
                onChange={(e) => setForm({ eventId: e.target.value, roundId: '', mentorId: '' })}
              >
                <option value=''>-- Chọn event --</option>
                {registrations.map((r) => (
                  <option key={r.eventId} value={r.eventId}>
                    {r.eventTitle}
                  </option>
                ))}
              </select>
            </FormField>
            <FormField label='Round'>
              <select
                value={form.roundId}
                onChange={(e) => setForm((f) => ({ ...f, roundId: e.target.value }))}
                disabled={!form.eventId}
              >
                <option value=''>-- Chọn round --</option>
                {rounds.map((r) => (
                  <option key={r.roundId} value={r.roundId}>
                    {r.name}
                  </option>
                ))}
              </select>
            </FormField>
            <FormField label='Mentor'>
              <select
                value={form.mentorId}
                onChange={(e) => setForm((f) => ({ ...f, mentorId: e.target.value }))}
                disabled={!form.eventId}
              >
                <option value=''>-- Chọn mentor --</option>
                {mentors.map((m) => (
                  <option key={m.mentorId} value={m.mentorId}>
                    {m.mentorName}
                  </option>
                ))}
              </select>
            </FormField>
          </div>
          <LoadingButton loading={creating} type='submit'>
            Tạo phòng chat
          </LoadingButton>
          <FormMessage message={formError} type='error' />
        </form>
      )}

      <div className='chat-layout'>
        <div className='chat-room-list'>
          {loadingRooms && <div className='empty-state'>Đang tải phòng...</div>}
          {!loadingRooms && rooms.length === 0 && <div className='empty-state'>Chưa có phòng chat.</div>}
          {rooms.map((room) => (
            <button
              key={room.roomId}
              type='button'
              className={`chat-room-item${room.roomId === selectedRoomId ? ' active' : ''}`}
              onClick={() => setSelectedRoomId(room.roomId)}
            >
              <div className='chat-room-item-title'>
                {room.eventTitle || 'Sự kiện'} · {room.roundName || 'Vòng thi'}
              </div>
              <div className='chat-room-item-sub'>
                Mentor: {room.mentorName || room.mentorId}
                {room.status === 'CLOSED' ? ' · Đã đóng' : ''}
              </div>
            </button>
          ))}
        </div>

        <div className='chat-main'>
          {!selectedRoomId && <div className='empty-state'>Chọn một phòng chat.</div>}
          {selectedRoomId && (
            <>
              {roomClosed && <div className='form-message error'>Phòng đã đóng — chỉ xem, không gửi tin.</div>}
              <div className='chat-messages'>
                {loadingMessages && <div className='empty-state'>Đang tải tin nhắn...</div>}
                {!loadingMessages &&
                  messages.map((msg) => (
                    <div key={msg.messageId} className='chat-message-row'>
                      <div className='chat-message-meta'>
                        <strong>{msg.senderName || msg.senderId}</strong>
                        <span>{formatDateTime(msg.createdAt)}</span>
                      </div>
                      <div className='chat-message-content'>{msg.content}</div>
                    </div>
                  ))}
                <div ref={messagesEndRef} />
              </div>
              {!roomClosed && (
                <form className='chat-send-form' onSubmit={handleSend}>
                  <input
                    type='text'
                    value={messageText}
                    onChange={(e) => setMessageText(e.target.value)}
                    placeholder='Nhập tin nhắn...'
                    maxLength={2000}
                  />
                  <LoadingButton loading={sending} type='submit'>
                    Gửi
                  </LoadingButton>
                </form>
              )}
              <FormMessage message={error} type='error' />
            </>
          )}
        </div>
      </div>
    </div>
  )
}

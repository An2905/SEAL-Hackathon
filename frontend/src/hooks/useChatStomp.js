import { useCallback, useEffect, useRef, useState } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { getWebSocketUrl } from '../api/chat'
import { isTokenExpired } from '../utils/jwt'

function readValidToken() {
  const token = localStorage.getItem('hh_token')
  if (!token || token === 'null' || token === 'undefined') return null
  if (isTokenExpired(token)) {
    window.dispatchEvent(new Event('auth:token-expired'))
    return null
  }
  return token
}

export function useChatStomp({ roomId, onMessage }) {
  const clientRef = useRef(null)
  const subscriptionRef = useRef(null)
  const [connected, setConnected] = useState(false)
  const onMessageRef = useRef(onMessage)

  useEffect(() => {
    onMessageRef.current = onMessage
  }, [onMessage])

  useEffect(() => {
    if (!roomId) return undefined

    const token = readValidToken()
    if (!token) return undefined

    const client = new Client({
      webSocketFactory: () => new SockJS(getWebSocketUrl()),
      connectHeaders: {
        Authorization: `Bearer ${token}`
      },
      reconnectDelay: 5000,
      onConnect: () => {
        const reconnectToken = readValidToken()
        if (!reconnectToken) {
          client.deactivate()
          setConnected(false)
          return
        }

        setConnected(true)
        subscriptionRef.current?.unsubscribe()
        subscriptionRef.current = client.subscribe(`/topic/chat/${roomId}`, (frame) => {
          try {
            const payload = JSON.parse(frame.body)
            onMessageRef.current?.(payload)
          } catch {
            // ignore malformed payloads
          }
        })
      },
      onDisconnect: () => setConnected(false),
      onStompError: () => setConnected(false),
      onWebSocketClose: () => setConnected(false)
    })

    client.activate()
    clientRef.current = client

    return () => {
      subscriptionRef.current?.unsubscribe()
      subscriptionRef.current = null
      client.deactivate()
      clientRef.current = null
      setConnected(false)
    }
  }, [roomId])

  const sendMessage = useCallback(
    (content) => {
      const client = clientRef.current
      if (!client?.connected) {
        throw new Error('WebSocket chưa kết nối')
      }
      client.publish({
        destination: '/app/chat.send',
        body: JSON.stringify({ roomId, content })
      })
    },
    [roomId]
  )

  return { connected, sendMessage }
}

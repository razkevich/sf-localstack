import { useState, useEffect, useRef } from 'react'
import type { RequestLogEntry } from '../types'

interface UseSseResult {
  entries: RequestLogEntry[]
  connected: boolean
  clear: () => void
}

export function useSse(): UseSseResult {
  const [entries, setEntries] = useState<RequestLogEntry[]>([])
  const [connected, setConnected] = useState(false)
  const retryTimer = useRef<ReturnType<typeof setTimeout> | null>(null)
  const esRef = useRef<EventSource | null>(null)

  useEffect(() => {
    function connect() {
      const es = new EventSource('/api/dashboard/events')
      esRef.current = es

      es.onopen = () => setConnected(true)

      es.onmessage = (event) => {
        try {
          const entry: RequestLogEntry = JSON.parse(event.data as string)
          setEntries((prev) => [entry, ...prev])
        } catch {
          // ignore malformed messages
        }
      }

      es.onerror = () => {
        setConnected(false)
        es.close()
        retryTimer.current = setTimeout(connect, 3000)
      }
    }

    connect()

    return () => {
      esRef.current?.close()
      if (retryTimer.current !== null) {
        clearTimeout(retryTimer.current)
      }
    }
  }, [])

  return {
    entries,
    connected,
    clear: () => setEntries([]),
  }
}

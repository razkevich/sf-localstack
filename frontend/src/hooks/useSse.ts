import { useState, useEffect, useRef } from 'react'
import { eventStreamUrl, fetchRecentRequests } from '../services/api'
import type { RequestLogEntry } from '../types'

interface UseSseResult {
  entries: RequestLogEntry[]
  connected: boolean
  clear: () => void
}

export function useSse(refreshToken = 0): UseSseResult {
  const [entries, setEntries] = useState<RequestLogEntry[]>([])
  const [connected, setConnected] = useState(false)
  const retryTimer = useRef<ReturnType<typeof setTimeout> | null>(null)
  const esRef = useRef<EventSource | null>(null)

  useEffect(() => {
    let cancelled = false

    async function loadRecent() {
      try {
        const recentEntries = await fetchRecentRequests()
        if (!cancelled) {
          setEntries(recentEntries)
        }
      } catch {
        if (!cancelled) {
          setEntries([])
        }
      }
    }

    function connect() {
      const es = new EventSource(eventStreamUrl('/api/dashboard/events'))
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

    void loadRecent()
    connect()

    return () => {
      cancelled = true
      esRef.current?.close()
      if (retryTimer.current !== null) {
        clearTimeout(retryTimer.current)
      }
    }
  }, [refreshToken])

  return {
    entries,
    connected,
    clear: () => setEntries([]),
  }
}

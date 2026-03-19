import { useCallback, useEffect, useState } from 'react'
import { fetchDashboardOverview } from '../services/api'
import type { DashboardOverview } from '../types'

interface UseDashboardOverviewResult {
  overview: DashboardOverview | null
  loading: boolean
  error: string | null
  refresh: () => Promise<void>
}

export function useDashboardOverview(refreshToken: number): UseDashboardOverviewResult {
  const [overview, setOverview] = useState<DashboardOverview | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const refresh = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      setOverview(await fetchDashboardOverview())
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load overview')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void refresh()
  }, [refresh, refreshToken])

  return { overview, loading, error, refresh }
}

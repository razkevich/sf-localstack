import type { DashboardOverview, RequestLogEntry } from '../types'

const DEFAULT_API_BASE = import.meta.env.VITE_API_BASE ?? ''

function url(path: string): string {
  return `${DEFAULT_API_BASE}${path}`
}

export async function fetchRecentRequests(limit = 100): Promise<RequestLogEntry[]> {
  const response = await fetch(url(`/api/dashboard/requests?limit=${limit}`))
  if (!response.ok) {
    throw new Error('Failed to load requests')
  }

  const entries = await response.json() as RequestLogEntry[]
  return [...entries].reverse()
}

export async function fetchDashboardOverview(): Promise<DashboardOverview> {
  const response = await fetch(url('/api/dashboard/overview'))
  if (!response.ok) {
    throw new Error('Failed to load overview')
  }

  return response.json() as Promise<DashboardOverview>
}

export async function resetOrg(): Promise<void> {
  const response = await fetch(url('/reset'), { method: 'POST' })
  if (!response.ok) {
    throw new Error('Failed to reset org')
  }
}

export function eventStreamUrl(path: string): string {
  return url(path)
}

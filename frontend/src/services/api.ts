import type { DashboardOverview, DescribeResult, QueryResult, RequestLogEntry, SObjectListResult } from '../types'

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

export async function runSoqlQuery(soql: string): Promise<QueryResult> {
  const response = await fetch(url(`/services/data/v60.0/query?q=${encodeURIComponent(soql)}`))
  if (!response.ok) {
    const errorBody = await response.json().catch(() => null)
    const message = Array.isArray(errorBody) && errorBody[0]?.message
      ? String(errorBody[0].message)
      : 'Failed to run query'
    throw new Error(message)
  }

  return response.json() as Promise<QueryResult>
}

export async function fetchObjectRecords(objectType: string): Promise<SObjectListResult> {
  const response = await fetch(url(`/services/data/v60.0/sobjects/${objectType}`))
  if (!response.ok) {
    throw new Error(`Failed to load ${objectType} records`)
  }

  return response.json() as Promise<SObjectListResult>
}

export async function fetchDescribe(objectType: string): Promise<DescribeResult> {
  const response = await fetch(url(`/services/data/v60.0/sobjects/${objectType}/describe`))
  if (!response.ok) {
    throw new Error(`Failed to describe ${objectType}`)
  }

  return response.json() as Promise<DescribeResult>
}

export function eventStreamUrl(path: string): string {
  return url(path)
}

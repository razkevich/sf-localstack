export interface RequestLogEntry {
  id: string
  timestamp: string
  method: string
  path: string
  statusCode: number
  durationMs: number
  requestBody: string
  responseBody: string
}

export interface ObjectCount {
  objectType: string
  count: number
}

export interface DashboardOverview {
  service: string
  status: string
  apiVersion: string
  totalRecords: number
  recentRequestCount: number
  objectCounts: ObjectCount[]
}

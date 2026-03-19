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

export interface SalesforceAttributes {
  type: string
  url: string
}

export interface SalesforceRecord {
  attributes?: SalesforceAttributes
  [key: string]: unknown
}

export interface QueryResult {
  totalSize: number
  done: boolean
  records: SalesforceRecord[]
}

export interface DescribeField {
  name: string
  label: string
  type: string
  filterable: boolean
  sortable: boolean
  nillable: boolean
}

export interface DescribeResult {
  name: string
  label: string
  queryable: boolean
  createable: boolean
  updateable: boolean
  deleteable: boolean
  fields: DescribeField[]
}

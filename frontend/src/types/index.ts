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

export interface SObjectSummary {
  name: string
  label: string
  labelPlural: string
  keyPrefix: string
  queryable: boolean
  createable: boolean
  updateable: boolean
  urls: Record<string, string>
}

export interface SObjectListResult {
  objectDescribe: SObjectSummary
  recentItems: SalesforceRecord[]
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
  actionOverrides: unknown[]
  childRelationships: unknown[]
  name: string
  label: string
  labelPlural: string
  keyPrefix: string
  queryable: boolean
  createable: boolean
  updateable: boolean
  deletable: boolean
  urls: Record<string, string>
  fields: DescribeField[]
}

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
  custom?: boolean
  createable?: boolean
  updateable?: boolean
  deprecatedAndHidden?: boolean
  filterable: boolean
  sortable: boolean
  soapType?: string
  nillable: boolean
}

export interface MutationResult {
  id?: string
  success: boolean
  errors: unknown[]
  status: number
  created?: boolean
  location?: string | null
}

export interface BulkJob {
  id: string
  operation: string
  object: string
  state: string
  externalIdFieldName?: string | null
  contentType: string
  numberRecordsProcessed: number
  numberRecordsFailed: number
  apiVersion: number
  lineEnding: string
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

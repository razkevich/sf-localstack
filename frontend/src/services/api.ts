import type {
  BulkJob,
  DashboardOverview,
  DescribeResult,
  MutationResult,
  QueryResult,
  RequestLogEntry,
  SObjectListResult,
} from '../types'

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

export async function upsertByExternalId(
  objectType: string,
  externalIdField: string,
  externalIdValue: string,
  fields: Record<string, unknown>,
): Promise<MutationResult> {
  const response = await fetch(
    url(`/services/data/v60.0/sobjects/${objectType}/${externalIdField}/${encodeURIComponent(externalIdValue)}`),
    {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(fields),
    },
  )

  const body = await response.json().catch(() => null)
  if (!response.ok) {
    const message = Array.isArray(body) && body[0]?.message
      ? String(body[0].message)
      : 'Upsert failed'
    throw new Error(message)
  }

  return {
    id: body?.id,
    success: Boolean(body?.success),
    errors: Array.isArray(body?.errors) ? body.errors : [],
    status: response.status,
    created: typeof body?.created === 'boolean' ? body.created : undefined,
    location: response.headers.get('Location'),
  }
}

export async function createBulkJob(payload: {
  object: string
  operation: string
  externalIdFieldName?: string
}): Promise<BulkJob> {
  const response = await fetch(url('/services/data/v60.0/jobs/ingest'), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ ...payload, contentType: 'CSV' }),
  })
  if (!response.ok) {
    throw new Error('Failed to create bulk job')
  }
  return response.json() as Promise<BulkJob>
}

export async function uploadBulkCsv(jobId: string, csv: string): Promise<void> {
  const response = await fetch(url(`/services/data/v60.0/jobs/ingest/${jobId}/batches`), {
    method: 'PUT',
    headers: { 'Content-Type': 'text/csv' },
    body: csv,
  })
  if (!response.ok) {
    throw new Error('Failed to upload CSV batch')
  }
}

export async function closeBulkJob(jobId: string): Promise<BulkJob> {
  const response = await fetch(url(`/services/data/v60.0/jobs/ingest/${jobId}`), {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ state: 'UploadComplete' }),
  })
  if (!response.ok) {
    throw new Error('Failed to close bulk job')
  }
  return response.json() as Promise<BulkJob>
}

export async function fetchBulkJob(jobId: string): Promise<BulkJob> {
  const response = await fetch(url(`/services/data/v60.0/jobs/ingest/${jobId}`))
  if (!response.ok) {
    throw new Error('Failed to load bulk job')
  }
  return response.json() as Promise<BulkJob>
}

export async function fetchBulkCsvResult(
  jobId: string,
  kind: 'successfulResults' | 'failedResults' | 'unprocessedrecords',
): Promise<string> {
  const response = await fetch(url(`/services/data/v60.0/jobs/ingest/${jobId}/${kind}`))
  if (!response.ok) {
    throw new Error(`Failed to load ${kind}`)
  }
  return response.text()
}

export function eventStreamUrl(path: string): string {
  return url(path)
}

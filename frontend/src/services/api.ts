import type {
  BulkJob,
  DashboardOverview,
  DescribeResult,
  MetadataResource,
  MutationResult,
  QueryResult,
  RequestLogEntry,
  SObjectListResult,
  ToolingQueryResult,
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

export async function createRecord(objectType: string, fields: Record<string, unknown>): Promise<MutationResult> {
  const response = await fetch(url(`/services/data/v60.0/sobjects/${objectType}`), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(fields),
  })
  const body = await response.json().catch(() => null)
  if (!response.ok) {
    throw new Error(Array.isArray(body) && body[0]?.message ? String(body[0].message) : `Failed to create ${objectType}`)
  }
  return {
    id: body?.id,
    success: Boolean(body?.success),
    errors: Array.isArray(body?.errors) ? body.errors : [],
    status: response.status,
  }
}

export async function replaceRecord(objectType: string, id: string, fields: Record<string, unknown>): Promise<MutationResult> {
  const response = await fetch(url(`/services/data/v60.0/sobjects/${objectType}/${id}`), {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(fields),
  })
  const body = await response.json().catch(() => null)
  if (!response.ok) {
    throw new Error(Array.isArray(body) && body[0]?.message ? String(body[0].message) : `Failed to update ${objectType}`)
  }
  return {
    id: body?.id,
    success: Boolean(body?.success),
    errors: Array.isArray(body?.errors) ? body.errors : [],
    status: response.status,
  }
}

export async function deleteRecord(objectType: string, id: string): Promise<void> {
  const response = await fetch(url(`/services/data/v60.0/sobjects/${objectType}/${id}`), { method: 'DELETE' })
  if (!response.ok) {
    const body = await response.json().catch(() => null)
    throw new Error(Array.isArray(body) && body[0]?.message ? String(body[0].message) : `Failed to delete ${objectType}`)
  }
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

export async function runToolingQuery(soql: string): Promise<ToolingQueryResult> {
  const response = await fetch(url(`/services/data/v60.0/tooling/query?q=${encodeURIComponent(soql)}`))
  if (!response.ok) {
    const body = await response.json().catch(() => null)
    const message = Array.isArray(body) && body[0]?.message ? String(body[0].message) : 'Failed to run tooling query'
    throw new Error(message)
  }
  return response.json() as Promise<ToolingQueryResult>
}

export async function callMetadataSoap(operationBody: string): Promise<string> {
  const envelope = `<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:met="http://soap.sforce.com/2006/04/metadata">
  <soapenv:Body>
    ${operationBody}
  </soapenv:Body>
</soapenv:Envelope>`

  const response = await fetch(url('/services/Soap/m/60.0'), {
    method: 'POST',
    headers: { 'Content-Type': 'text/xml' },
    body: envelope,
  })
  if (!response.ok) {
    throw new Error('Failed to call metadata SOAP endpoint')
  }
  return response.text()
}

export async function fetchMetadataResources(): Promise<MetadataResource[]> {
  const response = await fetch(url('/api/admin/metadata/resources'))
  if (!response.ok) {
    throw new Error('Failed to load metadata resources')
  }
  return response.json() as Promise<MetadataResource[]>
}

export async function createMetadataResource(resource: MetadataResource): Promise<MetadataResource> {
  const response = await fetch(url('/api/admin/metadata/resources'), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(resource),
  })
  if (!response.ok) {
    throw new Error('Failed to create metadata resource')
  }
  return response.json() as Promise<MetadataResource>
}

export async function updateMetadataResource(originalType: string, originalFullName: string, resource: MetadataResource): Promise<MetadataResource> {
  const response = await fetch(url(`/api/admin/metadata/resources/${encodeURIComponent(originalType)}/${encodeURIComponent(originalFullName)}`), {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(resource),
  })
  if (!response.ok) {
    throw new Error('Failed to update metadata resource')
  }
  return response.json() as Promise<MetadataResource>
}

export async function deleteMetadataResource(type: string, fullName: string): Promise<void> {
  const response = await fetch(url(`/api/admin/metadata/resources/${encodeURIComponent(type)}/${encodeURIComponent(fullName)}`), {
    method: 'DELETE',
  })
  if (!response.ok) {
    throw new Error('Failed to delete metadata resource')
  }
}

export interface RetrieveTypeRequest {
  type: string
  members: string[]
}

export interface RetrieveJobStatus {
  id: string
  done: boolean
  success: boolean
  status: string
  numberComponentsTotal: number
  zipFileBase64?: string
}

export async function triggerRetrieve(typeRequests: RetrieveTypeRequest[]): Promise<{ id: string }> {
  const typesXml = typeRequests
    .map(
      (req) =>
        `<met:types>${req.members.map((m) => `<met:members>${m}</met:members>`).join('')}<met:name>${req.type}</met:name></met:types>`,
    )
    .join('')
  const operationBody = `<met:retrieve><met:retrieveRequest><met:apiVersion>60.0</met:apiVersion><met:unpackaged>${typesXml}</met:unpackaged></met:retrieveRequest></met:retrieve>`
  const xml = await callMetadataSoap(operationBody)
  const doc = new DOMParser().parseFromString(xml, 'text/xml')
  const id = doc.querySelector('id')?.textContent ?? ''
  if (!id) throw new Error('No retrieve ID in response')
  return { id }
}

export async function pollRetrieveStatus(jobId: string): Promise<RetrieveJobStatus> {
  const operationBody = `<met:checkRetrieveStatus><met:asyncProcessId>${jobId}</met:asyncProcessId><met:includeZip>true</met:includeZip></met:checkRetrieveStatus>`
  const xml = await callMetadataSoap(operationBody)
  const doc = new DOMParser().parseFromString(xml, 'text/xml')
  const get = (tag: string) => doc.querySelector(tag)?.textContent ?? ''
  return {
    id: get('id'),
    done: get('done') === 'true',
    success: get('success') === 'true',
    status: get('status'),
    numberComponentsTotal: parseInt(get('numberComponentsTotal') || '0', 10),
    zipFileBase64: get('zipFile') || undefined,
  }
}

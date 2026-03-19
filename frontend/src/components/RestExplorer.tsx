import { useEffect, useMemo, useState } from 'react'
import { fetchDescribe, fetchObjectRecords, runSoqlQuery, upsertByExternalId } from '../services/api'
import type {
  DashboardOverview,
  DescribeResult,
  MutationResult,
  QueryResult,
  SalesforceRecord,
  SObjectListResult,
} from '../types'

interface Props {
  overview: DashboardOverview | null
}

const DEFAULT_QUERY = "SELECT Id, FirstName, Account.Name FROM Contact WHERE LastName = 'Doe'"
const DEFAULT_UPSERT_PAYLOAD = '{\n  "Name": "Upsert Test Account",\n  "Industry": "Testing"\n}'

export function RestExplorer({ overview }: Props) {
  const [query, setQuery] = useState(DEFAULT_QUERY)
  const [queryResult, setQueryResult] = useState<QueryResult | null>(null)
  const [queryError, setQueryError] = useState<string | null>(null)
  const [queryLoading, setQueryLoading] = useState(false)

  const objectOptions = useMemo(
    () => overview?.objectCounts.map((item) => item.objectType) ?? ['Account', 'Contact'],
    [overview],
  )
  const [selectedObject, setSelectedObject] = useState('Account')
  const [recordsResult, setRecordsResult] = useState<SObjectListResult | null>(null)
  const [describeResult, setDescribeResult] = useState<DescribeResult | null>(null)
  const [objectLoading, setObjectLoading] = useState(false)
  const [objectError, setObjectError] = useState<string | null>(null)
  const [externalIdField, setExternalIdField] = useState('External_Id__c')
  const [externalIdValue, setExternalIdValue] = useState('EXT-001')
  const [upsertPayload, setUpsertPayload] = useState(DEFAULT_UPSERT_PAYLOAD)
  const [upsertLoading, setUpsertLoading] = useState(false)
  const [upsertError, setUpsertError] = useState<string | null>(null)
  const [upsertResult, setUpsertResult] = useState<MutationResult | null>(null)

  useEffect(() => {
    if (objectOptions.length > 0 && !objectOptions.includes(selectedObject)) {
      setSelectedObject(objectOptions[0])
    }
  }, [objectOptions, selectedObject])

  useEffect(() => {
    let cancelled = false

    async function loadObjectDetails() {
      setObjectLoading(true)
      setObjectError(null)
      try {
        const [records, describe] = await Promise.all([
          fetchObjectRecords(selectedObject),
          fetchDescribe(selectedObject),
        ])
        if (!cancelled) {
          setRecordsResult(records)
          setDescribeResult(describe)
        }
      } catch (err) {
        if (!cancelled) {
          setObjectError(err instanceof Error ? err.message : 'Failed to load object details')
        }
      } finally {
        if (!cancelled) {
          setObjectLoading(false)
        }
      }
    }

    void loadObjectDetails()
    return () => {
      cancelled = true
    }
  }, [selectedObject])

  async function handleRunQuery() {
    setQueryLoading(true)
    setQueryError(null)
    try {
      setQueryResult(await runSoqlQuery(query))
    } catch (err) {
      setQueryResult(null)
      setQueryError(err instanceof Error ? err.message : 'Failed to run query')
    } finally {
      setQueryLoading(false)
    }
  }

  async function handleUpsert() {
    setUpsertLoading(true)
    setUpsertError(null)
    try {
      const parsedPayload = JSON.parse(upsertPayload) as Record<string, unknown>
      const result = await upsertByExternalId(selectedObject, externalIdField, externalIdValue, parsedPayload)
      setUpsertResult(result)
      const [records, describe] = await Promise.all([
        fetchObjectRecords(selectedObject),
        fetchDescribe(selectedObject),
      ])
      setRecordsResult(records)
      setDescribeResult(describe)
      setQuery(`SELECT Id, Name FROM ${selectedObject} WHERE ${externalIdField} = '${externalIdValue}'`)
    } catch (err) {
      setUpsertResult(null)
      setUpsertError(err instanceof Error ? err.message : 'Failed to run upsert')
    } finally {
      setUpsertLoading(false)
    }
  }

  return (
    <div className="grid h-full grid-cols-[1.1fr,0.9fr] overflow-hidden">
      <div className="overflow-auto border-r border-slate-800 bg-slate-950">
        <div className="border-b border-slate-800 px-6 py-5">
          <div className="text-xs uppercase tracking-[0.2em] text-cyan-300">Features 1-2</div>
          <h2 className="mt-2 text-2xl font-semibold text-white">REST Explorer + Upsert</h2>
          <p className="mt-1 text-sm text-slate-400">
            Run supported SOQL, inspect object metadata, and exercise external-ID upsert flows from the same dashboard surface.
          </p>
        </div>

        <div className="p-6">
          <label className="text-xs uppercase tracking-[0.18em] text-slate-500">SOQL query</label>
          <textarea
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            className="mt-3 h-32 w-full rounded-2xl border border-slate-800 bg-slate-900 px-4 py-3 font-mono text-sm text-slate-100 outline-none transition focus:border-cyan-400"
          />
          <div className="mt-4 flex items-center gap-3">
            <button
              type="button"
              onClick={() => void handleRunQuery()}
              disabled={queryLoading}
              className="rounded-xl bg-cyan-400 px-4 py-2 text-sm font-medium text-slate-950 transition hover:bg-cyan-300 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {queryLoading ? 'Running...' : 'Run query'}
            </button>
            <span className="text-xs text-slate-500">Current baseline: {overview?.totalRecords ?? '--'} records</span>
          </div>

          {queryError ? (
            <div className="mt-4 rounded-xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-200">
              {queryError}
            </div>
          ) : null}

          <div className="mt-6 rounded-2xl border border-slate-800 bg-slate-900/70 p-4">
            <div className="flex items-center justify-between">
              <h3 className="text-sm font-semibold uppercase tracking-[0.18em] text-slate-400">Query result</h3>
              <span className="text-xs text-slate-500">{queryResult?.totalSize ?? 0} records</span>
            </div>
            <pre className="mt-4 max-h-[26rem] overflow-auto rounded-xl bg-slate-950 p-4 text-xs text-slate-200">
              {queryResult ? JSON.stringify(queryResult, null, 2) : 'Run a query to inspect the response envelope.'}
            </pre>
          </div>

          <div className="mt-6 rounded-2xl border border-slate-800 bg-slate-900/70 p-4">
            <div className="flex items-center justify-between">
              <h3 className="text-sm font-semibold uppercase tracking-[0.18em] text-slate-400">External-ID upsert</h3>
              <span className="text-xs text-slate-500">Feature 2</span>
            </div>

            <div className="mt-4 grid gap-3 md:grid-cols-2">
              <label className="text-sm text-slate-300">
                <span className="mb-2 block text-xs uppercase tracking-[0.16em] text-slate-500">Field</span>
                <input
                  value={externalIdField}
                  onChange={(event) => setExternalIdField(event.target.value)}
                  className="w-full rounded-xl border border-slate-800 bg-slate-950 px-3 py-2 text-sm text-white outline-none focus:border-cyan-400"
                />
              </label>
              <label className="text-sm text-slate-300">
                <span className="mb-2 block text-xs uppercase tracking-[0.16em] text-slate-500">Value</span>
                <input
                  value={externalIdValue}
                  onChange={(event) => setExternalIdValue(event.target.value)}
                  className="w-full rounded-xl border border-slate-800 bg-slate-950 px-3 py-2 text-sm text-white outline-none focus:border-cyan-400"
                />
              </label>
            </div>

            <label className="mt-4 block text-sm text-slate-300">
              <span className="mb-2 block text-xs uppercase tracking-[0.16em] text-slate-500">Payload</span>
              <textarea
                value={upsertPayload}
                onChange={(event) => setUpsertPayload(event.target.value)}
                className="h-32 w-full rounded-2xl border border-slate-800 bg-slate-950 px-4 py-3 font-mono text-sm text-slate-100 outline-none focus:border-cyan-400"
              />
            </label>

            <div className="mt-4 flex items-center gap-3">
              <button
                type="button"
                onClick={() => void handleUpsert()}
                disabled={upsertLoading}
                className="rounded-xl bg-emerald-400 px-4 py-2 text-sm font-medium text-slate-950 transition hover:bg-emerald-300 disabled:cursor-not-allowed disabled:opacity-60"
              >
                {upsertLoading ? 'Upserting...' : 'Run upsert'}
              </button>
              <span className="text-xs text-slate-500">Creates on first request, updates on the second.</span>
            </div>

            {upsertError ? (
              <div className="mt-4 rounded-xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-200">
                {upsertError}
              </div>
            ) : null}

            <pre className="mt-4 max-h-52 overflow-auto rounded-xl bg-slate-950 p-4 text-xs text-slate-200">
              {upsertResult
                ? JSON.stringify(upsertResult, null, 2)
                : 'Run an upsert to inspect the create vs update response shape.'}
            </pre>
          </div>
        </div>
      </div>

      <div className="overflow-auto bg-slate-950">
        <div className="border-b border-slate-800 px-6 py-5">
          <div className="text-xs uppercase tracking-[0.18em] text-slate-500">Object browser</div>
          <div className="mt-3 flex items-center gap-3">
            <select
              value={selectedObject}
              onChange={(event) => setSelectedObject(event.target.value)}
              className="rounded-xl border border-slate-800 bg-slate-900 px-3 py-2 text-sm text-white outline-none focus:border-cyan-400"
            >
              {objectOptions.map((option) => (
                <option key={option} value={option}>{option}</option>
              ))}
            </select>
            <span className="text-xs text-slate-500">Describe + list from the current local org state</span>
          </div>
        </div>

        <div className="space-y-6 p-6">
          {objectError ? (
            <div className="rounded-xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-200">
              {objectError}
            </div>
          ) : null}

          <section className="rounded-2xl border border-slate-800 bg-slate-900/70 p-4">
            <div className="flex items-center justify-between gap-3">
              <h3 className="text-sm font-semibold uppercase tracking-[0.18em] text-slate-400">SObject summary</h3>
              <span className="text-xs text-slate-500">{recordsResult?.objectDescribe.keyPrefix ?? '---'}</span>
            </div>
            {recordsResult?.objectDescribe ? (
              <div className="mt-4 grid grid-cols-2 gap-3 text-sm">
                <SummaryPill label="Label" value={recordsResult.objectDescribe.labelPlural} />
                <SummaryPill label="Queryable" value={String(recordsResult.objectDescribe.queryable)} />
                <SummaryPill label="Createable" value={String(recordsResult.objectDescribe.createable)} />
                <SummaryPill label="Updateable" value={String(recordsResult.objectDescribe.updateable)} />
              </div>
            ) : null}
          </section>

          <section className="rounded-2xl border border-slate-800 bg-slate-900/70 p-4">
            <h3 className="text-sm font-semibold uppercase tracking-[0.18em] text-slate-400">Describe fields</h3>
            <div className="mt-4 grid gap-3">
              {(describeResult?.fields ?? []).map((field) => (
                <div key={field.name} className="rounded-xl border border-slate-800 bg-slate-950/70 px-4 py-3">
                  <div className="flex items-center justify-between gap-3">
                    <div>
                      <div className="text-sm font-medium text-white">{field.name}</div>
                      <div className="text-xs text-slate-500">{field.label}</div>
                    </div>
                    <div className="text-xs uppercase tracking-[0.16em] text-cyan-300">{field.type}</div>
                  </div>
                </div>
              ))}
              {objectLoading && !describeResult ? <div className="text-sm text-slate-400">Loading describe...</div> : null}
            </div>
          </section>

          <section className="rounded-2xl border border-slate-800 bg-slate-900/70 p-4">
            <div className="flex items-center justify-between">
              <h3 className="text-sm font-semibold uppercase tracking-[0.18em] text-slate-400">Recent items</h3>
              <span className="text-xs text-slate-500">{recordsResult?.recentItems.length ?? 0} rows</span>
            </div>
            <div className="mt-4 space-y-3">
              {(recordsResult?.recentItems ?? []).map((record, index) => (
                <RecordCard key={`${selectedObject}-${index}`} record={record} />
              ))}
              {objectLoading && !recordsResult ? <div className="text-sm text-slate-400">Loading records...</div> : null}
            </div>
          </section>
        </div>
      </div>
    </div>
  )
}

function SummaryPill({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-xl border border-slate-800 bg-slate-950/70 px-4 py-3">
      <div className="text-xs uppercase tracking-[0.16em] text-slate-500">{label}</div>
      <div className="mt-1 text-sm font-medium text-white">{value}</div>
    </div>
  )
}

function RecordCard({ record }: { record: SalesforceRecord }) {
  const entries = Object.entries(record).filter(([key]) => key !== 'attributes')

  return (
    <div className="rounded-xl border border-slate-800 bg-slate-950/70 px-4 py-3">
      <div className="mb-3 text-xs uppercase tracking-[0.16em] text-slate-500">
        {record.attributes?.type ?? 'Record'}
      </div>
      <div className="space-y-2 text-sm">
        {entries.map(([key, value]) => (
          <div key={key} className="grid grid-cols-[8rem,1fr] gap-3">
            <div className="font-medium text-slate-400">{key}</div>
            <div className="break-words text-slate-100">{formatValue(value)}</div>
          </div>
        ))}
      </div>
    </div>
  )
}

function formatValue(value: unknown): string {
  if (value === null || value === undefined) {
    return 'null'
  }
  if (typeof value === 'object') {
    return JSON.stringify(value)
  }
  return String(value)
}

import { useMemo, useState } from 'react'
import { Activity, X } from 'lucide-react'
import type { RequestLogEntry } from '../types'
import { DataTable } from '../components/ui/DataTable'
import type { Column } from '../components/ui/DataTable'
import { PageHeader } from '../components/ui/PageHeader'
import { Badge } from '../components/ui/Badge'

interface Props {
  entries: RequestLogEntry[]
  connected: boolean
  onClear: () => void
}

type MethodFilter = 'ALL' | 'GET' | 'POST' | 'PATCH' | 'PUT' | 'DELETE'

function methodVariant(method: string): 'success' | 'warning' | 'error' | 'info' | 'neutral' {
  switch (method) {
    case 'GET': return 'info'
    case 'POST': return 'success'
    case 'PATCH': case 'PUT': return 'warning'
    case 'DELETE': return 'error'
    default: return 'neutral'
  }
}

function statusVariant(code: number): 'success' | 'warning' | 'error' | 'info' | 'neutral' {
  if (code >= 200 && code < 300) return 'success'
  if (code >= 400 && code < 500) return 'warning'
  if (code >= 500) return 'error'
  return 'neutral'
}

function formatTime(timestamp: string): string {
  try { return new Date(timestamp).toLocaleTimeString() } catch { return timestamp }
}

function formatBody(body: string): string {
  if (!body) return ''
  try { return JSON.stringify(JSON.parse(body), null, 2) } catch { return body }
}

export function RequestLogView({ entries, connected, onClear }: Props) {
  const [methodFilter, setMethodFilter] = useState<MethodFilter>('ALL')
  const [pathSearch, setPathSearch] = useState('')
  const [selectedEntry, setSelectedEntry] = useState<RequestLogEntry | null>(null)
  const [detailTab, setDetailTab] = useState<'request' | 'response'>('response')

  const filtered = useMemo(() => {
    return entries.filter((e) => {
      if (methodFilter !== 'ALL' && e.method !== methodFilter) return false
      if (pathSearch && !e.path.toLowerCase().includes(pathSearch.toLowerCase())) return false
      return true
    })
  }, [entries, methodFilter, pathSearch])

  const columns = useMemo((): Column[] => [
    {
      key: 'timestamp',
      label: 'Time',
      sortable: true,
      render: (v) => <span className="font-mono text-body-sm text-neutral-60">{formatTime(String(v))}</span>,
    },
    {
      key: 'method',
      label: 'Method',
      sortable: true,
      render: (v) => <Badge variant={methodVariant(String(v))}>{String(v)}</Badge>,
    },
    {
      key: 'path',
      label: 'Path',
      sortable: true,
      render: (v) => <span className="max-w-xs truncate font-mono text-body-sm">{String(v)}</span>,
    },
    {
      key: 'statusCode',
      label: 'Status',
      sortable: true,
      render: (v) => <Badge variant={statusVariant(Number(v))}>{String(v)}</Badge>,
    },
    {
      key: 'durationMs',
      label: 'Duration',
      sortable: true,
      render: (v) => <span className="text-body-sm text-neutral-60">{String(v)}ms</span>,
    },
  ], [])

  return (
    <div className="flex h-full flex-col">
      <PageHeader
        title="API Log"
        subtitle={
          connected
            ? `Live -- ${entries.length} requests captured`
            : 'Connecting...'
        }
        icon={<Activity className="h-5 w-5" />}
        actions={
          <div className="flex items-center gap-2">
            <span className={`h-2 w-2 rounded-full ${connected ? 'bg-success' : 'bg-neutral-30'}`} />
            <button
              type="button"
              onClick={onClear}
              className="rounded-slds border border-neutral-20 bg-neutral-00 px-3 py-1.5 text-body-sm font-medium text-neutral-70 hover:bg-neutral-05"
            >
              Clear
            </button>
          </div>
        }
      />

      <div className="border-b border-neutral-20 bg-neutral-00 px-6 py-3">
        <div className="flex flex-wrap items-center gap-3">
          <select
            value={methodFilter}
            onChange={(e) => setMethodFilter(e.target.value as MethodFilter)}
            className="rounded-slds border border-neutral-20 bg-neutral-00 px-3 py-1.5 text-body-sm text-neutral-90 outline-none focus:border-brand"
          >
            <option value="ALL">All Methods</option>
            <option value="GET">GET</option>
            <option value="POST">POST</option>
            <option value="PATCH">PATCH</option>
            <option value="PUT">PUT</option>
            <option value="DELETE">DELETE</option>
          </select>
          <input
            type="text"
            value={pathSearch}
            onChange={(e) => setPathSearch(e.target.value)}
            placeholder="Filter by path..."
            className="min-w-[200px] rounded-slds border border-neutral-20 bg-neutral-00 px-3 py-1.5 text-body-sm text-neutral-90 outline-none focus:border-brand focus:ring-1 focus:ring-brand"
          />
          <span className="text-body-sm text-neutral-50">{filtered.length} shown</span>
        </div>
      </div>

      <div className="flex flex-1 overflow-hidden">
        <div className="flex-1 overflow-auto p-6">
          <DataTable
            columns={columns}
            data={filtered as unknown as Record<string, unknown>[]}
            onRowClick={(row) => { setSelectedEntry(row as unknown as RequestLogEntry); setDetailTab('response') }}
            emptyMessage="No requests captured yet. Use the API to generate traffic."
            pageSize={50}
          />
        </div>

        {selectedEntry && (
          <div className="w-[28rem] flex-shrink-0 overflow-auto border-l border-neutral-20 bg-neutral-00">
            <div className="flex items-center justify-between border-b border-neutral-20 px-4 py-3">
              <div className="flex items-center gap-2">
                <Badge variant={methodVariant(selectedEntry.method)}>{selectedEntry.method}</Badge>
                <Badge variant={statusVariant(selectedEntry.statusCode)}>{String(selectedEntry.statusCode)}</Badge>
              </div>
              <button type="button" onClick={() => setSelectedEntry(null)} className="rounded-slds p-1 text-neutral-60 hover:bg-neutral-05">
                <X className="h-4 w-4" />
              </button>
            </div>

            <div className="border-b border-neutral-20 px-4 py-3">
              <div className="font-mono text-body-sm text-neutral-80 break-all">{selectedEntry.path}</div>
              <div className="mt-1 text-body-sm text-neutral-50">
                {formatTime(selectedEntry.timestamp)} -- {selectedEntry.durationMs}ms
              </div>
            </div>

            <div className="flex border-b border-neutral-20">
              <button
                type="button"
                onClick={() => setDetailTab('request')}
                className={`flex-1 px-4 py-2 text-body-sm font-medium ${detailTab === 'request' ? 'border-b-2 border-brand text-brand' : 'text-neutral-60 hover:text-neutral-80'}`}
              >
                Request
              </button>
              <button
                type="button"
                onClick={() => setDetailTab('response')}
                className={`flex-1 px-4 py-2 text-body-sm font-medium ${detailTab === 'response' ? 'border-b-2 border-brand text-brand' : 'text-neutral-60 hover:text-neutral-80'}`}
              >
                Response
              </button>
            </div>

            <div className="p-4">
              <pre className="max-h-[60vh] overflow-auto rounded-slds bg-neutral-05 p-4 text-body-sm text-neutral-80">
                {detailTab === 'request'
                  ? formatBody(selectedEntry.requestBody) || 'No request body'
                  : formatBody(selectedEntry.responseBody) || 'No response body'}
              </pre>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

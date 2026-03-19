import { useMemo, useState } from 'react'
import type { RequestLogEntry } from '../types'

interface Props {
  entries: RequestLogEntry[]
  connected: boolean
  selectedEntry: RequestLogEntry | null
  onSelect: (entry: RequestLogEntry) => void
  onClear: () => void
}

type SurfaceFilter = 'all' | 'rest' | 'bulk' | 'metadata' | 'tooling'

function formatTime(timestamp: string): string {
  try {
    const d = new Date(timestamp)
    return d.toTimeString().slice(0, 8)
  } catch {
    return timestamp
  }
}

function methodBadge(method: string): string {
  switch (method) {
    case 'GET': return 'bg-blue-700 text-blue-100'
    case 'POST': return 'bg-green-700 text-green-100'
    case 'PATCH': return 'bg-yellow-700 text-yellow-100'
    case 'DELETE': return 'bg-red-700 text-red-100'
    default: return 'bg-gray-700 text-gray-100'
  }
}

function surfaceInfo(path: string): { label: string; filter: SurfaceFilter } {
  if (path.includes('/jobs/ingest')) return { label: 'Bulk', filter: 'bulk' }
  if (path.includes('/Soap/m/')) return { label: 'Metadata SOAP', filter: 'metadata' }
  if (path.includes('/tooling/')) return { label: 'Tooling', filter: 'tooling' }
  if (path.includes('/query') || path.includes('/sobjects/')) return { label: 'REST', filter: 'rest' }
  return { label: 'App', filter: 'all' }
}

function statusColor(code: number): string {
  if (code >= 200 && code < 300) return 'text-green-400'
  if (code >= 400 && code < 500) return 'text-yellow-400'
  if (code >= 500) return 'text-red-400'
  return 'text-gray-400'
}

export function RequestLog({ entries, connected, selectedEntry, onSelect, onClear }: Props) {
  const [query, setQuery] = useState('')
  const [surface, setSurface] = useState<SurfaceFilter>('all')

  const filteredEntries = useMemo(() => {
    return entries.filter((entry) => {
      const info = surfaceInfo(entry.path)
      const matchesSurface = surface === 'all' || info.filter === surface
      const search = query.trim().toLowerCase()
      const matchesSearch = !search || `${entry.method} ${entry.path} ${entry.statusCode}`.toLowerCase().includes(search)
      return matchesSurface && matchesSearch
    })
  }, [entries, query, surface])

  return (
    <div className="flex h-full flex-col">
      <div className="border-b border-slate-800 px-5 py-4">
        <div className="flex items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2">
              <span className={`h-2 w-2 rounded-full ${connected ? 'bg-emerald-400' : 'bg-slate-500'}`} />
              <span className="text-sm text-slate-300">{connected ? 'Live request stream' : 'Connecting stream...'}</span>
            </div>
            <div className="mt-1 text-xs text-slate-500">{entries.length} requests captured, {filteredEntries.length} shown</div>
          </div>
          <button
            onClick={onClear}
            className="rounded-full border border-slate-700 px-3 py-1 text-xs text-slate-300 transition hover:border-slate-500 hover:text-white"
          >
            Clear
          </button>
        </div>

        <div className="mt-4 flex flex-col gap-3 xl:flex-row xl:items-center">
          <input
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Filter by path, method, or status"
            className="w-full rounded-xl border border-slate-800 bg-slate-900 px-3 py-2 text-sm text-white outline-none focus:border-cyan-400 xl:max-w-sm"
          />
          <div className="flex flex-wrap gap-2 text-xs">
            {([
              ['all', 'All'],
              ['rest', 'REST'],
              ['bulk', 'Bulk'],
              ['metadata', 'Metadata'],
              ['tooling', 'Tooling'],
            ] as const).map(([value, label]) => (
              <button
                key={value}
                type="button"
                onClick={() => setSurface(value)}
                className={`rounded-full px-3 py-1 transition ${
                  surface === value
                    ? 'bg-cyan-500/15 text-white ring-1 ring-cyan-400/40'
                    : 'border border-slate-700 text-slate-400 hover:border-slate-500 hover:text-slate-200'
                }`}
              >
                {label}
              </button>
            ))}
          </div>
        </div>
      </div>

      {filteredEntries.length === 0 ? (
        <div className="flex flex-1 items-center justify-center px-8 text-center text-sm text-slate-500">
          {entries.length === 0
            ? 'No requests yet. Use the REST, Bulk, or Metadata explorers to generate traffic and inspect envelopes here.'
            : 'No requests match the current filter. Clear the search or change the surface chips.'}
        </div>
      ) : (
        <div className="flex-1 overflow-auto">
          <table className="w-full text-sm">
            <thead className="border-b border-slate-800 text-xs uppercase tracking-wider text-slate-500">
              <tr>
                <th className="px-4 py-2 text-left">Time</th>
                <th className="px-4 py-2 text-left">Surface</th>
                <th className="px-4 py-2 text-left">Method</th>
                <th className="px-4 py-2 text-left">Path</th>
                <th className="px-4 py-2 text-left">Status</th>
                <th className="px-4 py-2 text-left">Duration</th>
              </tr>
            </thead>
            <tbody>
              {filteredEntries.map((entry) => {
                const info = surfaceInfo(entry.path)
                return (
                  <tr
                    key={entry.id}
                    onClick={() => onSelect(entry)}
                    className={`cursor-pointer border-b border-slate-800 transition-colors hover:bg-slate-900 ${
                      selectedEntry?.id === entry.id ? 'bg-slate-900' : ''
                    }`}
                  >
                    <td className="px-4 py-2 font-mono text-xs text-slate-400">{formatTime(entry.timestamp)}</td>
                    <td className="px-4 py-2">
                      <span className="rounded-full border border-slate-700 px-2 py-0.5 text-[10px] font-medium uppercase tracking-[0.16em] text-slate-300">
                        {info.label}
                      </span>
                    </td>
                    <td className="px-4 py-2">
                      <span className={`rounded px-1.5 py-0.5 text-xs font-bold ${methodBadge(entry.method)}`}>{entry.method}</span>
                    </td>
                    <td className="max-w-xs truncate px-4 py-2 font-mono text-xs text-slate-300">{entry.path}</td>
                    <td className={`px-4 py-2 font-mono text-xs ${statusColor(entry.statusCode)}`}>{entry.statusCode}</td>
                    <td className="px-4 py-2 text-xs text-slate-400">{entry.durationMs}ms</td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

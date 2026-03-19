import type { RequestLogEntry } from '../types'

interface Props {
  entries: RequestLogEntry[]
  connected: boolean
  selectedEntry: RequestLogEntry | null
  onSelect: (entry: RequestLogEntry) => void
  onClear: () => void
}

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

function surfaceLabel(path: string): string {
  if (path.includes('/jobs/ingest')) return 'Bulk'
  if (path.includes('/Soap/m/')) return 'Metadata SOAP'
  if (path.includes('/tooling/')) return 'Tooling'
  if (path.includes('/query')) return 'REST Query'
  if (path.includes('/sobjects/')) return 'REST Data'
  if (path.includes('/reset')) return 'Reset'
  return 'App'
}

function statusColor(code: number): string {
  if (code >= 200 && code < 300) return 'text-green-400'
  if (code >= 400 && code < 500) return 'text-yellow-400'
  if (code >= 500) return 'text-red-400'
  return 'text-gray-400'
}

export function RequestLog({ entries, connected, selectedEntry, onSelect, onClear }: Props) {
  return (
    <div className="flex flex-col h-full">
      <div className="flex items-center justify-between border-b border-slate-800 px-5 py-4">
        <div>
          <div className="flex items-center gap-2">
            <span className={`h-2 w-2 rounded-full ${connected ? 'bg-emerald-400' : 'bg-slate-500'}`} />
            <span className="text-sm text-slate-300">{connected ? 'Live request stream' : 'Connecting stream...'}</span>
          </div>
          <div className="mt-1 text-xs text-slate-500">{entries.length} requests captured in this session</div>
        </div>
        <button
          onClick={onClear}
          className="rounded-full border border-slate-700 px-3 py-1 text-xs text-slate-300 transition hover:border-slate-500 hover:text-white"
        >
          Clear
        </button>
      </div>

      {entries.length === 0 ? (
        <div className="flex flex-1 items-center justify-center px-8 text-center text-sm text-slate-500">
          No requests yet. Use the REST, Bulk, or Metadata explorers to generate traffic and inspect the envelopes here.
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
              {entries.map((entry) => (
                <tr
                  key={entry.id}
                  onClick={() => onSelect(entry)}
                  className={`cursor-pointer border-b border-slate-800 transition-colors hover:bg-slate-900
                    ${selectedEntry?.id === entry.id ? 'bg-slate-900' : ''}`}
                >
                  <td className="px-4 py-2 font-mono text-xs text-slate-400">
                    {formatTime(entry.timestamp)}
                  </td>
                  <td className="px-4 py-2">
                    <span className="rounded-full border border-slate-700 px-2 py-0.5 text-[10px] font-medium uppercase tracking-[0.16em] text-slate-300">
                      {surfaceLabel(entry.path)}
                    </span>
                  </td>
                  <td className="px-4 py-2">
                    <span className={`px-1.5 py-0.5 rounded text-xs font-bold ${methodBadge(entry.method)}`}>
                      {entry.method}
                    </span>
                  </td>
                  <td className="max-w-xs truncate px-4 py-2 font-mono text-xs text-slate-300">
                    {entry.path}
                  </td>
                  <td className={`px-4 py-2 font-mono text-xs ${statusColor(entry.statusCode)}`}>
                    {entry.statusCode}
                  </td>
                  <td className="px-4 py-2 text-xs text-slate-400">
                    {entry.durationMs}ms
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

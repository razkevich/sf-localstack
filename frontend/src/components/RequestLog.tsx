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

function statusColor(code: number): string {
  if (code >= 200 && code < 300) return 'text-green-400'
  if (code >= 400 && code < 500) return 'text-yellow-400'
  if (code >= 500) return 'text-red-400'
  return 'text-gray-400'
}

export function RequestLog({ entries, connected, selectedEntry, onSelect, onClear }: Props) {
  return (
    <div className="flex flex-col h-full">
      <div className="flex items-center justify-between px-4 py-3 border-b border-gray-800">
        <div className="flex items-center gap-2">
          <span className={`w-2 h-2 rounded-full ${connected ? 'bg-green-500' : 'bg-gray-500'}`} />
          <span className="text-sm text-gray-400">{connected ? 'Live' : 'Connecting...'}</span>
        </div>
        <button
          onClick={onClear}
          className="text-xs text-gray-400 hover:text-gray-200 px-2 py-1 rounded hover:bg-gray-800"
        >
          Clear
        </button>
      </div>

      {entries.length === 0 ? (
        <div className="flex-1 flex items-center justify-center text-gray-600 text-sm">
          No requests yet. Make a call to the API to see logs here.
        </div>
      ) : (
        <div className="flex-1 overflow-auto">
          <table className="w-full text-sm">
            <thead className="text-xs text-gray-500 uppercase tracking-wider border-b border-gray-800">
              <tr>
                <th className="px-4 py-2 text-left">Time</th>
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
                  className={`cursor-pointer border-b border-gray-800 hover:bg-gray-800 transition-colors
                    ${selectedEntry?.id === entry.id ? 'bg-gray-800' : ''}`}
                >
                  <td className="px-4 py-2 text-gray-400 font-mono text-xs">
                    {formatTime(entry.timestamp)}
                  </td>
                  <td className="px-4 py-2">
                    <span className={`px-1.5 py-0.5 rounded text-xs font-bold ${methodBadge(entry.method)}`}>
                      {entry.method}
                    </span>
                  </td>
                  <td className="px-4 py-2 text-gray-300 font-mono text-xs max-w-xs truncate">
                    {entry.path}
                  </td>
                  <td className={`px-4 py-2 font-mono text-xs ${statusColor(entry.statusCode)}`}>
                    {entry.statusCode}
                  </td>
                  <td className="px-4 py-2 text-gray-400 text-xs">
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

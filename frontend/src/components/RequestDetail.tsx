import type { RequestLogEntry } from '../types'

interface Props {
  entry: RequestLogEntry | null
}

function formatBody(body: string): string {
  if (!body) return ''
  try {
    return JSON.stringify(JSON.parse(body), null, 2)
  } catch {
    return body
  }
}

function statusColor(code: number): string {
  if (code >= 200 && code < 300) return 'bg-green-700 text-green-100'
  if (code >= 400 && code < 500) return 'bg-yellow-700 text-yellow-100'
  if (code >= 500) return 'bg-red-700 text-red-100'
  return 'bg-gray-700 text-gray-100'
}

export function RequestDetail({ entry }: Props) {
  if (!entry) {
    return (
      <div className="flex items-center justify-center h-full text-gray-500 text-sm">
        Click a request to see details
      </div>
    )
  }

  return (
    <div className="p-4 space-y-4 overflow-auto h-full">
      <div className="flex items-center gap-3">
        <span className="font-mono font-bold text-white">{entry.method}</span>
        <span className="text-gray-300 font-mono text-sm">{entry.path}</span>
        <span className={`ml-auto px-2 py-0.5 rounded text-xs font-bold ${statusColor(entry.statusCode)}`}>
          {entry.statusCode}
        </span>
        <span className="text-gray-400 text-xs">{entry.durationMs}ms</span>
      </div>

      <div>
        <h3 className="text-gray-400 text-xs font-semibold uppercase tracking-wider mb-2">
          Request Body
        </h3>
        <pre className="bg-gray-900 rounded p-3 text-xs text-gray-300 font-mono overflow-auto max-h-48">
          {formatBody(entry.requestBody) || <span className="text-gray-600">empty</span>}
        </pre>
      </div>

      <div>
        <h3 className="text-gray-400 text-xs font-semibold uppercase tracking-wider mb-2">
          Response Body
        </h3>
        <pre className="bg-gray-900 rounded p-3 text-xs text-gray-300 font-mono overflow-auto max-h-48">
          {formatBody(entry.responseBody) || <span className="text-gray-600">empty</span>}
        </pre>
      </div>
    </div>
  )
}

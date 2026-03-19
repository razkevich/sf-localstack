import { useMemo, useState } from 'react'
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
  if (code >= 200 && code < 300) return 'bg-emerald-500/15 text-emerald-200 ring-1 ring-emerald-400/30'
  if (code >= 400 && code < 500) return 'bg-amber-500/15 text-amber-100 ring-1 ring-amber-400/30'
  if (code >= 500) return 'bg-rose-500/15 text-rose-100 ring-1 ring-rose-400/30'
  return 'bg-slate-700 text-slate-100'
}

function inferSurface(path: string): string {
  if (path.includes('/jobs/ingest')) return 'Bulk API v2'
  if (path.includes('/Soap/m/')) return 'Metadata SOAP'
  if (path.includes('/tooling/')) return 'Metadata helper query'
  if (path.includes('/query')) return 'REST query'
  if (path.includes('/sobjects/')) return 'REST data'
  if (path.includes('/reset')) return 'Reset'
  return 'App'
}

export function RequestDetail({ entry }: Props) {
  const [tab, setTab] = useState<'request' | 'response'>('request')

  const requestText = useMemo(() => formatBody(entry?.requestBody ?? ''), [entry?.requestBody])
  const responseText = useMemo(() => formatBody(entry?.responseBody ?? ''), [entry?.responseBody])

  async function copyCurrent() {
    const text = tab === 'request' ? requestText : responseText
    if (!text) return
    await navigator.clipboard.writeText(text)
  }

  if (!entry) {
    return (
      <div className="flex h-full items-center justify-center px-8 text-center text-sm text-slate-500">
        Pick a request from the log to inspect method, path, status, and formatted payloads.
      </div>
    )
  }

  return (
    <div className="flex h-full flex-col overflow-hidden bg-slate-950">
      <div className="border-b border-slate-800 px-5 py-4">
        <div className="flex items-start gap-3">
          <span className="rounded-full bg-slate-900 px-2.5 py-1 font-mono text-xs font-semibold text-white ring-1 ring-slate-700">
            {entry.method}
          </span>
          <div className="min-w-0 flex-1">
            <div className="truncate font-mono text-xs text-slate-300">{entry.path}</div>
            <div className="mt-2 flex flex-wrap items-center gap-2 text-xs text-slate-500">
              <span>{inferSurface(entry.path)}</span>
              <span>•</span>
              <span>{new Date(entry.timestamp).toLocaleString()}</span>
              <span>•</span>
              <span>{entry.durationMs}ms</span>
            </div>
          </div>
          <span className={`rounded-full px-2.5 py-1 text-xs font-semibold ${statusColor(entry.statusCode)}`}>
            {entry.statusCode}
          </span>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-3 border-b border-slate-800 px-5 py-4 text-sm">
        <Metric label="Request Id" value={entry.id} mono />
        <Metric label="Surface" value={inferSurface(entry.path)} />
      </div>

      <div className="flex items-center justify-between border-b border-slate-800 px-3 py-2">
        <div className="flex gap-2">
          <TabButton active={tab === 'request'} onClick={() => setTab('request')}>
            Request body
          </TabButton>
          <TabButton active={tab === 'response'} onClick={() => setTab('response')}>
            Response body
          </TabButton>
        </div>
        <button
          type="button"
          onClick={() => void copyCurrent()}
          className="rounded-full border border-slate-700 px-3 py-1 text-xs text-slate-300 transition hover:border-slate-500 hover:text-white"
        >
          Copy {tab}
        </button>
      </div>

      <div className="flex-1 overflow-auto p-4">
        <pre className="min-h-full rounded-2xl border border-slate-800 bg-slate-900/70 p-4 text-xs text-slate-200">
          {(tab === 'request' ? requestText : responseText) || 'empty'}
        </pre>
      </div>
    </div>
  )
}

function Metric({ label, value, mono = false }: { label: string; value: string; mono?: boolean }) {
  return (
    <div className="rounded-xl border border-slate-800 bg-slate-900/70 px-3 py-3">
      <div className="text-[10px] uppercase tracking-[0.18em] text-slate-500">{label}</div>
      <div className={`mt-2 text-sm text-white ${mono ? 'font-mono break-all' : ''}`}>{value}</div>
    </div>
  )
}

function TabButton({ active, onClick, children }: { active: boolean; onClick: () => void; children: string }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`rounded-full px-3 py-1 text-xs font-medium transition ${
        active ? 'bg-cyan-500/15 text-white ring-1 ring-cyan-400/40' : 'text-slate-400 hover:bg-slate-900 hover:text-slate-200'
      }`}
    >
      {children}
    </button>
  )
}

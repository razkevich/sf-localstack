import { useState } from 'react'
import { BulkExplorer } from './components/BulkExplorer'
import { DataManager } from './components/DataManager'
import { MetadataManager } from './components/MetadataManager'
import { OverviewPanel } from './components/OverviewPanel'
import { RestExplorer } from './components/RestExplorer'
import { Sidebar } from './components/Sidebar'
import { RequestLog } from './components/RequestLog'
import { RequestDetail } from './components/RequestDetail'
import { useDashboardOverview } from './hooks/useDashboardOverview'
import { useSse } from './hooks/useSse'
import type { RequestLogEntry } from './types'

export default function App() {
  const [selectedView, setSelectedView] = useState<'overview' | 'requests' | 'data' | 'metadata' | 'bulk' | 'rest'>('overview')
  const [refreshToken, setRefreshToken] = useState(0)
  const { entries, connected, clear } = useSse(refreshToken)
  const { overview, loading, error } = useDashboardOverview(refreshToken)
  const [selectedEntry, setSelectedEntry] = useState<RequestLogEntry | null>(null)

  const detailTitle = selectedEntry ? `${selectedEntry.method} ${selectedEntry.statusCode} — ${selectedEntry.path}` : 'Request detail'
  const showDetailPane = selectedView === 'requests' || selectedView === 'rest' || selectedView === 'bulk'

  function handleResetComplete() {
    setRefreshToken((value) => value + 1)
  }

  return (
    <div className="flex h-screen overflow-hidden bg-slate-950 text-slate-100">
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_top_left,_rgba(34,211,238,0.14),_transparent_28%),radial-gradient(circle_at_bottom_right,_rgba(16,185,129,0.12),_transparent_22%)]" />
      <Sidebar
        selectedView={selectedView}
        onSelect={setSelectedView}
        overview={overview}
        onReset={handleResetComplete}
      />
      <div className="relative z-10 flex flex-1 flex-col overflow-hidden xl:flex-row">
        <div className="min-h-0 flex-1 overflow-hidden border-b border-slate-800 xl:border-b-0 xl:border-r">
          {selectedView === 'overview' ? (
            <OverviewPanel overview={overview} loading={loading} error={error} />
          ) : selectedView === 'data' ? (
            <DataManager overview={overview} />
          ) : selectedView === 'metadata' ? (
            <MetadataManager />
          ) : selectedView === 'bulk' ? (
            <BulkExplorer />
          ) : selectedView === 'rest' ? (
            <RestExplorer overview={overview} />
          ) : (
            <RequestLog
              entries={entries}
              connected={connected}
              selectedEntry={selectedEntry}
              onSelect={setSelectedEntry}
              onClear={clear}
            />
          )}
        </div>
        {showDetailPane ? (
          <div className="flex h-[20rem] flex-col overflow-hidden border-t border-slate-800 bg-slate-950/95 backdrop-blur xl:h-auto xl:w-[28rem] xl:border-l xl:border-t-0">
            <div className="border-b border-slate-800 px-4 py-3 text-xs uppercase tracking-[0.18em] text-slate-500">
              {detailTitle}
            </div>
            <RequestDetail entry={selectedEntry} />
          </div>
        ) : null}
      </div>
    </div>
  )
}

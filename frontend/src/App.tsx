import { useState } from 'react'
import { OverviewPanel } from './components/OverviewPanel'
import { BulkExplorer } from './components/BulkExplorer'
import { RestExplorer } from './components/RestExplorer'
import { Sidebar } from './components/Sidebar'
import { RequestLog } from './components/RequestLog'
import { RequestDetail } from './components/RequestDetail'
import { useDashboardOverview } from './hooks/useDashboardOverview'
import { useSse } from './hooks/useSse'
import type { RequestLogEntry } from './types'

export default function App() {
  const [selectedView, setSelectedView] = useState<'overview' | 'requests' | 'rest' | 'bulk'>('overview')
  const [refreshToken, setRefreshToken] = useState(0)
  const { entries, connected, clear } = useSse(refreshToken)
  const { overview, loading, error } = useDashboardOverview(refreshToken)
  const [selectedEntry, setSelectedEntry] = useState<RequestLogEntry | null>(null)

  function handleResetComplete() {
    setRefreshToken((value) => value + 1)
  }

  return (
    <div className="flex h-screen overflow-hidden bg-slate-950 text-slate-100">
      <Sidebar
        selectedView={selectedView}
        onSelect={setSelectedView}
        overview={overview}
        onReset={handleResetComplete}
      />
      <div className="flex flex-1 overflow-hidden">
        <div className="flex-1 overflow-hidden border-r border-slate-800">
          {selectedView === 'overview' ? (
            <OverviewPanel overview={overview} loading={loading} error={error} />
          ) : selectedView === 'rest' ? (
            <RestExplorer overview={overview} />
          ) : selectedView === 'bulk' ? (
            <BulkExplorer />
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
        <div className="flex w-[28rem] flex-col overflow-hidden bg-slate-950">
          <div className="border-b border-slate-800 px-4 py-3 text-xs uppercase tracking-[0.18em] text-slate-500">
            Request detail
          </div>
          <RequestDetail entry={selectedEntry} />
        </div>
      </div>
    </div>
  )
}

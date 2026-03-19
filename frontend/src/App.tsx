import { useState } from 'react'
import { Sidebar } from './components/Sidebar'
import { RequestLog } from './components/RequestLog'
import { RequestDetail } from './components/RequestDetail'
import { useSse } from './hooks/useSse'
import type { RequestLogEntry } from './types'

export default function App() {
  const { entries, connected, clear } = useSse()
  const [selectedEntry, setSelectedEntry] = useState<RequestLogEntry | null>(null)

  return (
    <div className="flex h-screen bg-gray-950 text-gray-100 overflow-hidden">
      <Sidebar />
      <div className="flex flex-1 overflow-hidden">
        <div className="flex-1 border-r border-gray-800 overflow-hidden flex flex-col">
          <RequestLog
            entries={entries}
            connected={connected}
            selectedEntry={selectedEntry}
            onSelect={setSelectedEntry}
            onClear={clear}
          />
        </div>
        <div className="w-96 overflow-hidden flex flex-col">
          <RequestDetail entry={selectedEntry} />
        </div>
      </div>
    </div>
  )
}

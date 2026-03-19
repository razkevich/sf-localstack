import { ResetButton } from './ResetButton'
import type { DashboardOverview } from '../types'

interface Props {
  selectedView: 'overview' | 'requests' | 'data' | 'metadata'
  onSelect: (view: 'overview' | 'requests' | 'data' | 'metadata') => void
  overview: DashboardOverview | null
  onReset: () => void
}

export function Sidebar({ selectedView, onSelect, overview, onReset }: Props) {
  const navItems = [
    { id: 'overview', label: 'Overview', hint: 'State + quick summary' },
    { id: 'data', label: 'Data', hint: 'Manage records directly' },
    { id: 'metadata', label: 'Metadata', hint: 'Manage metadata directly' },
    { id: 'requests', label: 'Request Log', hint: 'Live traffic inspector' },
  ]

  return (
    <div className="relative z-10 flex h-full w-72 flex-col border-r border-slate-800 bg-slate-950/95 backdrop-blur">
      <div className="border-b border-slate-800 p-5">
        <div className="text-xs uppercase tracking-[0.2em] text-cyan-300">sf-localstack</div>
        <h1 className="mt-2 text-lg font-semibold text-white">Salesforce API Emulator</h1>
        <p className="mt-1 text-sm text-slate-400">
          Manage emulator data and metadata without thinking about transport details.
        </p>
      </div>

      <div className="border-b border-slate-800 px-5 py-4">
        <div className="text-xs uppercase tracking-[0.18em] text-slate-500">Current baseline</div>
        <div className="mt-3 grid grid-cols-2 gap-3 text-sm">
          <div className="rounded-xl border border-slate-800 bg-slate-900/70 p-3">
            <div className="text-slate-500">Records</div>
            <div className="mt-1 text-xl font-semibold text-white">{overview?.totalRecords ?? '--'}</div>
          </div>
          <div className="rounded-xl border border-slate-800 bg-slate-900/70 p-3">
            <div className="text-slate-500">Objects</div>
            <div className="mt-1 text-xl font-semibold text-white">{overview?.objectCounts.length ?? '--'}</div>
          </div>
        </div>
        <div className="mt-3 rounded-xl border border-slate-800 bg-slate-900/70 px-3 py-3">
          <div className="text-[10px] uppercase tracking-[0.18em] text-slate-500">API target</div>
          <div className="mt-1 text-sm font-medium text-white">Salesforce {overview?.apiVersion ?? 'v60.0'}</div>
        </div>
      </div>

      <nav className="flex-1 space-y-1 p-3">
        {navItems.map((item) => (
          <button
            key={item.id}
            type="button"
            onClick={() => onSelect(item.id as 'overview' | 'requests' | 'data' | 'metadata')}
            className={`w-full rounded-xl px-3 py-3 text-left transition-colors ${
              selectedView === item.id
                ? 'bg-cyan-500/15 text-white ring-1 ring-cyan-400/40'
                : 'text-slate-400 hover:bg-slate-900 hover:text-slate-200'
            }`}
          >
            <div className="text-sm font-medium">{item.label}</div>
            <div className="mt-1 text-xs text-slate-500">{item.hint}</div>
          </button>
        ))}
      </nav>

      <div className="border-t border-slate-800 p-4">
        <ResetButton onReset={onReset} />
      </div>
    </div>
  )
}

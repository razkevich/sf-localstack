import { ResetButton } from './ResetButton'
import type { DashboardOverview } from '../types'

interface Props {
  selectedView: 'overview' | 'requests' | 'rest' | 'bulk' | 'metadata'
  onSelect: (view: 'overview' | 'requests' | 'rest' | 'bulk' | 'metadata') => void
  overview: DashboardOverview | null
  onReset: () => void
}

export function Sidebar({ selectedView, onSelect, overview, onReset }: Props) {
  const navItems = [
    { id: 'overview', label: 'Overview', hint: 'Seed + app status' },
    { id: 'rest', label: 'REST Explorer', hint: 'SOQL + object browser' },
    { id: 'bulk', label: 'Bulk Explorer', hint: 'CSV ingest console' },
    { id: 'metadata', label: 'Metadata', hint: 'SOAP + tooling helpers' },
    { id: 'requests', label: 'Request Log', hint: 'Live traffic inspector' },
  ]

  return (
    <div className="relative z-10 flex h-full w-72 flex-col border-r border-slate-800 bg-slate-950/95 backdrop-blur">
      <div className="border-b border-slate-800 p-5">
        <div className="text-xs uppercase tracking-[0.2em] text-cyan-300">sf-localstack</div>
        <h1 className="mt-2 text-lg font-semibold text-white">Salesforce API Emulator</h1>
        <p className="mt-1 text-sm text-slate-400">
          Build REST, Bulk, and Metadata support inside a working app shell.
        </p>
        <div className="mt-4 flex flex-wrap gap-2 text-[10px] uppercase tracking-[0.16em] text-slate-400">
          <span className="rounded-full border border-cyan-400/30 bg-cyan-400/10 px-2 py-1 text-cyan-200">REST</span>
          <span className="rounded-full border border-emerald-400/30 bg-emerald-400/10 px-2 py-1 text-emerald-200">Bulk</span>
          <span className="rounded-full border border-fuchsia-400/30 bg-fuchsia-400/10 px-2 py-1 text-fuchsia-200">Metadata</span>
        </div>
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
      </div>

      <nav className="flex-1 space-y-1 p-3">
        {navItems.map((item) => (
          <button
            key={item.id}
            type="button"
            onClick={() => onSelect(item.id as 'overview' | 'requests' | 'rest' | 'bulk' | 'metadata')}
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

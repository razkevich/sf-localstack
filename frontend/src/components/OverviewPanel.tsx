import type { DashboardOverview } from '../types'

interface Props {
  overview: DashboardOverview | null
  loading: boolean
  error: string | null
}

export function OverviewPanel({ overview, loading, error }: Props) {
  if (loading) {
    return <div className="p-6 text-sm text-slate-400">Loading emulator overview...</div>
  }

  if (error) {
    return <div className="p-6 text-sm text-rose-300">{error}</div>
  }

  if (!overview) {
    return <div className="p-6 text-sm text-slate-400">Overview unavailable.</div>
  }

  return (
    <div className="h-full overflow-auto bg-slate-950 text-slate-100">
      <div className="border-b border-slate-800 px-6 py-5">
        <div className="flex items-center gap-2 text-xs uppercase tracking-[0.2em] text-emerald-300">
          <span className="h-2 w-2 rounded-full bg-emerald-400" />
          {overview.status}
        </div>
        <h2 className="mt-3 text-2xl font-semibold text-white">{overview.service}</h2>
        <p className="mt-1 text-sm text-slate-400">
          Working against Salesforce API {overview.apiVersion} from a resettable local app shell.
        </p>
      </div>

      <div className="grid gap-4 p-6 md:grid-cols-2 xl:grid-cols-3">
        <MetricCard label="Seeded Records" value={overview.totalRecords} />
        <MetricCard label="Captured Requests" value={overview.recentRequestCount} />
        <MetricCard label="Object Types" value={overview.objectCounts.length} />
      </div>

      <div className="px-6 pb-6">
        <div className="rounded-2xl border border-slate-800 bg-slate-900/70 p-5">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-semibold uppercase tracking-[0.18em] text-slate-400">
              Org Snapshot
            </h3>
            <span className="text-xs text-slate-500">Refreshed on reset</span>
          </div>

          <div className="mt-4 space-y-3">
            {overview.objectCounts.map((item) => (
              <div
                key={item.objectType}
                className="flex items-center justify-between rounded-xl border border-slate-800 bg-slate-950/70 px-4 py-3"
              >
                <div>
                  <div className="text-sm font-medium text-white">{item.objectType}</div>
                  <div className="text-xs text-slate-500">Available in the seeded org baseline</div>
                </div>
                <div className="text-lg font-semibold text-cyan-300">{item.count}</div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}

function MetricCard({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-2xl border border-slate-800 bg-slate-900/70 p-5">
      <div className="text-xs uppercase tracking-[0.18em] text-slate-500">{label}</div>
      <div className="mt-3 text-3xl font-semibold text-white">{value}</div>
    </div>
  )
}

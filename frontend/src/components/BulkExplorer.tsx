import { useState } from 'react'
import {
  closeBulkJob,
  createBulkJob,
  fetchBulkCsvResult,
  fetchBulkJob,
  uploadBulkCsv,
} from '../services/api'
import type { BulkJob } from '../types'

const DEFAULT_CSV = 'Name,Industry\nBulk Corp,Technology\n'
const BULK_PRESETS = {
  insert: 'Name,Industry\nBulk Corp,Technology\n',
  update: 'Id,Name\n001000000000001AAA,Updated Bulk Corp\n',
  delete: 'Id\n001000000000001AAA\n',
  upsert: 'External_Id__c,Name,Industry\nEXT-BULK-001,Bulk Upsert Corp,Technology\n',
} as const

export function BulkExplorer() {
  const [objectType, setObjectType] = useState('Account')
  const [operation, setOperation] = useState('insert')
  const [externalIdFieldName, setExternalIdFieldName] = useState('External_Id__c')
  const [csv, setCsv] = useState(DEFAULT_CSV)
  const [job, setJob] = useState<BulkJob | null>(null)
  const [successCsv, setSuccessCsv] = useState('')
  const [failedCsv, setFailedCsv] = useState('')
  const [unprocessedCsv, setUnprocessedCsv] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleCreate() {
    setLoading(true)
    setError(null)
    try {
      const nextJob = await createBulkJob({
        object: objectType,
        operation,
        externalIdFieldName: operation === 'upsert' ? externalIdFieldName : undefined,
      })
      setJob(nextJob)
      setSuccessCsv('')
      setFailedCsv('')
      setUnprocessedCsv('')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create bulk job')
    } finally {
      setLoading(false)
    }
  }

  async function handleUploadAndClose() {
    if (!job) return
    setLoading(true)
    setError(null)
    try {
      await uploadBulkCsv(job.id, csv)
      const completed = await closeBulkJob(job.id)
      setJob(completed)
      const [successfulResults, failedResults, unprocessedResults] = await Promise.all([
        fetchBulkCsvResult(job.id, 'successfulResults'),
        fetchBulkCsvResult(job.id, 'failedResults'),
        fetchBulkCsvResult(job.id, 'unprocessedrecords'),
      ])
      setSuccessCsv(successfulResults)
      setFailedCsv(failedResults)
      setUnprocessedCsv(unprocessedResults)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to process bulk job')
    } finally {
      setLoading(false)
    }
  }

  async function handleRefresh() {
    if (!job) return
    setLoading(true)
    setError(null)
    try {
      setJob(await fetchBulkJob(job.id))
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to refresh bulk job')
    } finally {
      setLoading(false)
    }
  }

  async function copyText(value: string) {
    if (!value) return
    await navigator.clipboard.writeText(value)
  }

  function applyPreset(nextOperation: 'insert' | 'update' | 'delete' | 'upsert') {
    setOperation(nextOperation)
    setCsv(BULK_PRESETS[nextOperation])
  }

  return (
    <div className="grid h-full grid-cols-1 overflow-hidden xl:grid-cols-[0.95fr,1.05fr]">
      <div className="overflow-auto border-r border-slate-800 bg-slate-950 p-6">
        <div className="text-xs uppercase tracking-[0.2em] text-cyan-300">Feature 3</div>
        <h2 className="mt-2 text-2xl font-semibold text-white">Bulk API v2</h2>
        <p className="mt-1 text-sm text-slate-400">
          Create an ingest job, upload CSV rows, close it synchronously, and inspect result files.
        </p>
        <div className="mt-4 grid gap-3 md:grid-cols-3">
          <StatusCard label="Mode" value="Synchronous close" tone="cyan" />
          <StatusCard label="Result files" value="CSV mirrors" tone="emerald" />
          <StatusCard label="Best use" value="Fast ingest parity" tone="amber" />
        </div>

        <div className="mt-6 grid gap-4 md:grid-cols-2">
          <Field label="Object">
            <input value={objectType} onChange={(e) => setObjectType(e.target.value)} className={inputClass} />
          </Field>
          <Field label="Operation">
            <select value={operation} onChange={(e) => setOperation(e.target.value)} className={inputClass}>
              <option value="insert">insert</option>
              <option value="update">update</option>
              <option value="delete">delete</option>
              <option value="upsert">upsert</option>
            </select>
          </Field>
        </div>

        {operation === 'upsert' ? (
          <div className="mt-4">
            <Field label="External Id Field">
              <input value={externalIdFieldName} onChange={(e) => setExternalIdFieldName(e.target.value)} className={inputClass} />
            </Field>
          </div>
        ) : null}

        <div className="mt-4">
          <div className="mb-3 flex flex-wrap gap-2">
            {(['insert', 'update', 'delete', 'upsert'] as const).map((preset) => (
              <button
                key={preset}
                type="button"
                onClick={() => applyPreset(preset)}
                className="rounded-full border border-slate-700 px-3 py-1 text-xs text-slate-300 transition hover:border-slate-500 hover:text-white"
              >
                {preset} preset
              </button>
            ))}
          </div>
          <Field label="CSV payload">
            <textarea value={csv} onChange={(e) => setCsv(e.target.value)} className={`${inputClass} h-56 font-mono`} />
          </Field>
        </div>

        <div className="mt-4 flex gap-3">
          <button onClick={() => void handleCreate()} disabled={loading} className={primaryButton}>
            {loading ? 'Working...' : 'Create job'}
          </button>
          <button onClick={() => void handleUploadAndClose()} disabled={loading || !job} className={secondaryButton}>
            Upload + close
          </button>
          <button onClick={() => void handleRefresh()} disabled={loading || !job} className={secondaryButton}>
            Refresh
          </button>
          <button onClick={() => void copyText(csv)} className={secondaryButton}>
            Copy CSV
          </button>
        </div>

        {error ? <div className="mt-4 rounded-xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-200">{error}</div> : null}
      </div>

      <div className="overflow-auto border-t border-slate-800 bg-slate-950 p-6 xl:border-t-0">
        <section className="rounded-2xl border border-slate-800 bg-slate-900/70 p-4">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-semibold uppercase tracking-[0.18em] text-slate-400">Job summary</h3>
            <div className="flex items-center gap-3">
              <span className="text-xs text-slate-500">{job?.state ?? 'No job yet'}</span>
              <button
                type="button"
                onClick={() => void copyText(job ? JSON.stringify(job, null, 2) : '')}
                className="rounded-full border border-slate-700 px-3 py-1 text-[11px] text-slate-300 transition hover:border-slate-500 hover:text-white"
              >
                Copy job
              </button>
            </div>
          </div>
          {job ? (
            <div className="mt-4 space-y-3">
              <div className="grid gap-3 md:grid-cols-4">
                <Metric label="State" value={job.state} />
                <Metric label="Processed" value={String(job.numberRecordsProcessed ?? 0)} />
                <Metric label="Failed" value={String(job.numberRecordsFailed ?? 0)} />
                <Metric label="Operation" value={job.operation} />
              </div>
              <pre className="max-h-64 overflow-auto rounded-xl bg-slate-950 p-4 text-xs text-slate-200">
                {JSON.stringify(job, null, 2)}
              </pre>
            </div>
          ) : (
            <pre className="mt-4 max-h-64 overflow-auto rounded-xl bg-slate-950 p-4 text-xs text-slate-200">
              Create a Bulk ingest job to inspect the response envelope.
            </pre>
          )}
        </section>

        <div className="mt-6 grid gap-6 xl:grid-cols-3">
          <CsvCard title="Successful results" csv={successCsv || 'sf__Id,sf__Created\n'} />
          <CsvCard title="Failed results" csv={failedCsv || 'sf__Id,sf__Error\n'} />
          <CsvCard title="Unprocessed records" csv={unprocessedCsv || 'sf__Id,sf__Error\n'} />
        </div>
      </div>
    </div>
  )
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="block text-sm text-slate-300">
      <span className="mb-2 block text-xs uppercase tracking-[0.16em] text-slate-500">{label}</span>
      {children}
    </label>
  )
}

function CsvCard({ title, csv }: { title: string; csv: string }) {
  return (
    <section className="rounded-2xl border border-slate-800 bg-slate-900/70 p-4">
      <h3 className="text-sm font-semibold uppercase tracking-[0.18em] text-slate-400">{title}</h3>
      <pre className="mt-4 max-h-80 overflow-auto rounded-xl bg-slate-950 p-4 text-xs text-slate-200">{csv}</pre>
    </section>
  )
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-xl border border-slate-800 bg-slate-950/70 px-4 py-3">
      <div className="text-[10px] uppercase tracking-[0.18em] text-slate-500">{label}</div>
      <div className="mt-2 text-sm font-medium text-white">{value}</div>
    </div>
  )
}

const inputClass = 'w-full rounded-xl border border-slate-800 bg-slate-950 px-3 py-2 text-sm text-white outline-none focus:border-cyan-400'
const primaryButton = 'rounded-xl bg-cyan-400 px-4 py-2 text-sm font-medium text-slate-950 transition hover:bg-cyan-300 disabled:cursor-not-allowed disabled:opacity-60'
const secondaryButton = 'rounded-xl border border-slate-700 px-4 py-2 text-sm font-medium text-slate-200 transition hover:border-slate-500 hover:text-white disabled:cursor-not-allowed disabled:opacity-60'

function StatusCard({ label, value, tone }: { label: string; value: string; tone: 'cyan' | 'emerald' | 'amber' }) {
  const styles = {
    cyan: 'border-cyan-400/20 bg-cyan-400/10 text-cyan-100',
    emerald: 'border-emerald-400/20 bg-emerald-400/10 text-emerald-100',
    amber: 'border-amber-400/20 bg-amber-400/10 text-amber-100',
  }

  return (
    <div className={`rounded-2xl border px-4 py-4 ${styles[tone]}`}>
      <div className="text-[10px] uppercase tracking-[0.18em] opacity-70">{label}</div>
      <div className="mt-2 text-sm font-medium text-white">{value}</div>
    </div>
  )
}

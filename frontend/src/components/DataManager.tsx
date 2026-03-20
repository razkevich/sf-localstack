import { useEffect, useMemo, useState } from 'react'
import { createRecord, deleteRecord, fetchDescribe, fetchObjectRecords, replaceRecord } from '../services/api'
import type { DashboardOverview, DescribeResult, SObjectListResult, SalesforceRecord } from '../types'

interface Props {
  overview: DashboardOverview | null
}

const EMPTY_RECORD = '{\n  "Name": ""\n}'

export function DataManager({ overview }: Props) {
  const objectOptions = useMemo(() => overview?.objectCounts.map((item) => item.objectType) ?? ['Account', 'Contact'], [overview])
  const [selectedObject, setSelectedObject] = useState(objectOptions[0] ?? 'Account')
  const [recordsResult, setRecordsResult] = useState<SObjectListResult | null>(null)
  const [describeResult, setDescribeResult] = useState<DescribeResult | null>(null)
  const [selectedRecordId, setSelectedRecordId] = useState<string | null>(null)
  const [editor, setEditor] = useState(EMPTY_RECORD)
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [status, setStatus] = useState<string | null>(null)

  useEffect(() => {
    if (objectOptions.length > 0 && !objectOptions.includes(selectedObject)) {
      setSelectedObject(objectOptions[0])
    }
  }, [objectOptions, selectedObject])

  useEffect(() => {
    void refreshObject()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedObject])

  const filteredRecords = useMemo(() => {
    const records = recordsResult?.recentItems ?? []
    const normalized = search.trim().toLowerCase()
    if (!normalized) return records
    return records.filter((record) => JSON.stringify(record).toLowerCase().includes(normalized))
  }, [recordsResult, search])

  async function refreshObject() {
    setLoading(true)
    setError(null)
    try {
      const [records, describe] = await Promise.all([
        fetchObjectRecords(selectedObject),
        fetchDescribe(selectedObject),
      ])
      setRecordsResult(records)
      setDescribeResult(describe)
      setSelectedRecordId(null)
      setEditor(EMPTY_RECORD)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load records')
    } finally {
      setLoading(false)
    }
  }

  function selectRecord(record: SalesforceRecord) {
    const normalized = normalizeRecordForEditing(record)
    setSelectedRecordId(String(normalized.Id ?? ''))
    setEditor(JSON.stringify(normalized, null, 2))
    setStatus(null)
    setError(null)
  }

  function startNewRecord() {
    setSelectedRecordId(null)
    setEditor(EMPTY_RECORD)
    setStatus(null)
    setError(null)
  }

  async function saveRecord() {
    setSaving(true)
    setError(null)
    setStatus(null)
    try {
      const parsed = JSON.parse(editor) as Record<string, unknown>
      if (selectedRecordId) {
        await replaceRecord(selectedObject, selectedRecordId, parsed)
        setStatus('Record updated')
      } else {
        const result = await createRecord(selectedObject, parsed)
        setSelectedRecordId(result.id ?? null)
        setStatus('Record created')
      }
      await refreshObject()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save record')
    } finally {
      setSaving(false)
    }
  }

  async function removeRecord() {
    if (!selectedRecordId) return
    setSaving(true)
    setError(null)
    setStatus(null)
    try {
      await deleteRecord(selectedObject, selectedRecordId)
      setStatus('Record deleted')
      await refreshObject()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete record')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="grid h-full grid-cols-1 xl:grid-cols-[0.85fr,1.15fr]">
      <div className="border-b border-slate-800 bg-slate-950 xl:border-b-0 xl:border-r">
        <div className="border-b border-slate-800 px-6 py-5">
          <div className="text-xs uppercase tracking-[0.18em] text-cyan-300">Data</div>
          <h2 className="mt-2 text-2xl font-semibold text-white">Record Manager</h2>
          <p className="mt-1 text-sm text-slate-400">Create, edit, and delete records directly without thinking about which API method to use.</p>
          <div className="mt-4 flex flex-wrap gap-3">
            <select value={selectedObject} onChange={(e) => setSelectedObject(e.target.value)} className={inputClass}>
              {objectOptions.map((option) => <option key={option} value={option}>{option}</option>)}
            </select>
            <input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Filter records" className={`${inputClass} min-w-48`} />
            <button type="button" onClick={startNewRecord} className={secondaryButton}>New record</button>
            <button type="button" onClick={() => void refreshObject()} className={secondaryButton}>Refresh</button>
          </div>
        </div>

        <div className="h-[calc(100%-9rem)] overflow-auto p-4">
          {loading ? <div className="text-sm text-slate-400">Loading records...</div> : null}
          {error ? <div className="rounded-xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-200">{error}</div> : null}
          <div className="space-y-3">
            {filteredRecords.map((record, index) => {
              const id = String(record.Id ?? `row-${index}`)
              const selected = selectedRecordId === id
              return (
                <button
                  key={id}
                  type="button"
                  onClick={() => selectRecord(record)}
                  className={`w-full rounded-2xl border px-4 py-4 text-left transition ${selected ? 'border-cyan-400/50 bg-cyan-400/10' : 'border-slate-800 bg-slate-900/70 hover:border-slate-700'}`}
                >
                  <div className="flex items-center justify-between gap-3">
                    <div>
                      <div className="text-sm font-medium text-white">{String(record.Name ?? record.attributes?.type ?? 'Record')}</div>
                      <div className="mt-1 font-mono text-xs text-slate-500">{id}</div>
                    </div>
                    <div className="text-xs text-slate-400">{String(record.LastModifiedDate ?? '')}</div>
                  </div>
                </button>
              )
            })}
            {!loading && filteredRecords.length === 0 ? <div className="text-sm text-slate-500">No matching records.</div> : null}
          </div>
        </div>
      </div>

      <div className="overflow-auto bg-slate-950 p-6">
        <div className="rounded-2xl border border-slate-800 bg-slate-900/70 p-5">
          <div className="flex items-center justify-between gap-3">
            <div>
              <div className="text-xs uppercase tracking-[0.18em] text-slate-500">Editor</div>
              <div className="mt-1 text-lg font-semibold text-white">{selectedRecordId ? 'Edit record' : 'Create record'}</div>
            </div>
            <div className="flex gap-2">
              <button type="button" onClick={() => navigator.clipboard.writeText(editor)} className={secondaryButton}>Copy JSON</button>
              {selectedRecordId ? <button type="button" onClick={() => void removeRecord()} className={dangerButton}>Delete</button> : null}
              <button type="button" onClick={() => void saveRecord()} disabled={saving} className={primaryButton}>{saving ? 'Saving...' : 'Save'}</button>
            </div>
          </div>

          {status ? <div className="mt-4 rounded-xl border border-emerald-500/30 bg-emerald-500/10 px-4 py-3 text-sm text-emerald-200">{status}</div> : null}
          {error ? <div className="mt-4 rounded-xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-200">{error}</div> : null}

          <textarea value={editor} onChange={(e) => setEditor(e.target.value)} className="mt-4 h-80 w-full rounded-2xl border border-slate-800 bg-slate-950 px-4 py-3 font-mono text-sm text-slate-100 outline-none focus:border-cyan-400" />

          <div className="mt-6 grid gap-4 xl:grid-cols-[0.8fr,1.2fr]">
            <div className="rounded-2xl border border-slate-800 bg-slate-950/70 p-4">
              <div className="text-xs uppercase tracking-[0.18em] text-slate-500">Current object</div>
              <div className="mt-2 text-xl font-semibold text-white">{selectedObject}</div>
              <div className="mt-4 grid gap-3 md:grid-cols-2">
                <Info label="Records" value={String(recordsResult?.recentItems.length ?? 0)} />
                <Info label="Fields" value={String(describeResult?.fields.length ?? 0)} />
              </div>
            </div>
            <div className="rounded-2xl border border-slate-800 bg-slate-950/70 p-4">
              <div className="text-xs uppercase tracking-[0.18em] text-slate-500">Field reference</div>
              <div className="mt-3 grid gap-2 md:grid-cols-2">
                {(describeResult?.fields ?? []).map((field) => (
                  <div key={field.name} className="rounded-xl border border-slate-800 bg-slate-900/70 px-3 py-3">
                    <div className="text-sm font-medium text-white">{field.name}</div>
                    <div className="mt-1 text-xs text-slate-500">{field.type}</div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

function normalizeRecordForEditing(record: SalesforceRecord): Record<string, unknown> {
  const clone = JSON.parse(JSON.stringify(record)) as Record<string, unknown>
  delete clone.attributes
  return clone
}

function Info({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-xl border border-slate-800 bg-slate-900/70 px-3 py-3">
      <div className="text-[10px] uppercase tracking-[0.18em] text-slate-500">{label}</div>
      <div className="mt-2 text-sm font-medium text-white">{value}</div>
    </div>
  )
}

const inputClass = 'rounded-xl border border-slate-800 bg-slate-900 px-3 py-2 text-sm text-white outline-none focus:border-cyan-400'
const primaryButton = 'rounded-xl bg-cyan-400 px-4 py-2 text-sm font-medium text-slate-950 transition hover:bg-cyan-300 disabled:cursor-not-allowed disabled:opacity-60'
const secondaryButton = 'rounded-xl border border-slate-700 px-4 py-2 text-sm font-medium text-slate-200 transition hover:border-slate-500 hover:text-white'
const dangerButton = 'rounded-xl border border-rose-500/40 px-4 py-2 text-sm font-medium text-rose-200 transition hover:border-rose-400 hover:text-white'

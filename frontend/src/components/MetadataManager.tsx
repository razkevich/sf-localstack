import { useEffect, useMemo, useState } from 'react'
import {
  createMetadataResource,
  deleteMetadataResource,
  fetchMetadataResources,
  updateMetadataResource,
} from '../services/api'
import type { MetadataResource } from '../types'

const EMPTY_RESOURCE: MetadataResource = {
  type: 'CustomField',
  fullName: '',
  fileName: '',
  directoryName: 'objects',
  inFolder: false,
  metaFile: true,
  lastModifiedDate: new Date().toISOString(),
  label: '',
  attributes: {},
}

export function MetadataManager() {
  const [resources, setResources] = useState<MetadataResource[]>([])
  const [selectedKey, setSelectedKey] = useState<string | null>(null)
  const [draft, setDraft] = useState<MetadataResource>(EMPTY_RESOURCE)
  const [attributeEditor, setAttributeEditor] = useState('{}')
  const [filter, setFilter] = useState('')
  const [typeFilter, setTypeFilter] = useState('all')
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [status, setStatus] = useState<string | null>(null)

  const resourceTypes = useMemo(() => ['all', ...new Set(resources.map((resource) => resource.type))], [resources])

  const filteredResources = useMemo(() => {
    const query = filter.trim().toLowerCase()
    return resources.filter((resource) => {
      const matchesType = typeFilter === 'all' || resource.type === typeFilter
      const matchesQuery = !query || JSON.stringify(resource).toLowerCase().includes(query)
      return matchesType && matchesQuery
    })
  }, [filter, resources, typeFilter])

  useEffect(() => {
    void refreshResources()
  }, [])

  async function refreshResources() {
    setLoading(true)
    setError(null)
    try {
      const next = await fetchMetadataResources()
      setResources(next)
      if (selectedKey) {
        const selected = next.find((resource) => keyFor(resource) === selectedKey)
        if (selected) {
          applyDraft(selected)
        }
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load metadata resources')
    } finally {
      setLoading(false)
    }
  }

  function applyDraft(resource: MetadataResource) {
    setSelectedKey(keyFor(resource))
    setDraft(resource)
    setAttributeEditor(JSON.stringify(resource.attributes ?? {}, null, 2))
    setStatus(null)
    setError(null)
  }

  function startNew() {
    setSelectedKey(null)
    setDraft({ ...EMPTY_RESOURCE, lastModifiedDate: new Date().toISOString() })
    setAttributeEditor('{}')
    setStatus(null)
    setError(null)
  }

  async function save() {
    setSaving(true)
    setError(null)
    setStatus(null)
    try {
      const attributes = JSON.parse(attributeEditor) as Record<string, unknown>
      const payload = { ...draft, attributes }
      if (selectedKey) {
        const [originalType, originalFullName] = selectedKey.split('::')
        const updated = await updateMetadataResource(originalType, originalFullName, payload)
        setStatus('Metadata resource updated')
        setSelectedKey(keyFor(updated))
      } else {
        const created = await createMetadataResource(payload)
        setStatus('Metadata resource created')
        setSelectedKey(keyFor(created))
      }
      await refreshResources()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save metadata resource')
    } finally {
      setSaving(false)
    }
  }

  async function remove() {
    if (!selectedKey) return
    setSaving(true)
    setError(null)
    setStatus(null)
    try {
      const [type, fullName] = selectedKey.split('::')
      await deleteMetadataResource(type, fullName)
      setStatus('Metadata resource deleted')
      startNew()
      await refreshResources()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete metadata resource')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="grid h-full grid-cols-1 xl:grid-cols-[0.8fr,1.2fr]">
      <div className="border-b border-slate-800 bg-slate-950 xl:border-b-0 xl:border-r">
        <div className="border-b border-slate-800 px-6 py-5">
          <div className="text-xs uppercase tracking-[0.18em] text-fuchsia-300">Metadata</div>
          <h2 className="mt-2 text-2xl font-semibold text-white">Metadata Manager</h2>
          <p className="mt-1 text-sm text-slate-400">Manage the local metadata catalog directly instead of thinking in SOAP operations.</p>
          <div className="mt-4 flex flex-wrap gap-3">
            <select value={typeFilter} onChange={(e) => setTypeFilter(e.target.value)} className={inputClass}>
              {resourceTypes.map((type) => <option key={type} value={type}>{type}</option>)}
            </select>
            <input value={filter} onChange={(e) => setFilter(e.target.value)} placeholder="Filter metadata" className={`${inputClass} min-w-48`} />
            <button type="button" onClick={startNew} className={secondaryButton}>New metadata</button>
            <button type="button" onClick={() => void refreshResources()} className={secondaryButton}>Refresh</button>
          </div>
        </div>

        <div className="h-[calc(100%-9rem)] overflow-auto p-4">
          {loading ? <div className="text-sm text-slate-400">Loading metadata...</div> : null}
          {error ? <div className="rounded-xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-200">{error}</div> : null}
          <div className="space-y-3">
            {filteredResources.map((resource) => {
              const selected = selectedKey === keyFor(resource)
              return (
                <button
                  key={keyFor(resource)}
                  type="button"
                  onClick={() => applyDraft(resource)}
                  className={`w-full rounded-2xl border px-4 py-4 text-left transition ${selected ? 'border-fuchsia-400/50 bg-fuchsia-400/10' : 'border-slate-800 bg-slate-900/70 hover:border-slate-700'}`}
                >
                  <div className="flex items-center justify-between gap-3">
                    <div>
                      <div className="text-sm font-medium text-white">{resource.fullName}</div>
                      <div className="mt-1 text-xs text-slate-500">{resource.type}</div>
                    </div>
                    <div className="text-xs text-slate-400">{resource.directoryName}</div>
                  </div>
                </button>
              )
            })}
            {!loading && filteredResources.length === 0 ? <div className="text-sm text-slate-500">No matching metadata resources.</div> : null}
          </div>
        </div>
      </div>

      <div className="overflow-auto bg-slate-950 p-6">
        <div className="rounded-2xl border border-slate-800 bg-slate-900/70 p-5">
          <div className="flex items-center justify-between gap-3">
            <div>
              <div className="text-xs uppercase tracking-[0.18em] text-slate-500">Editor</div>
              <div className="mt-1 text-lg font-semibold text-white">{selectedKey ? 'Edit metadata resource' : 'Create metadata resource'}</div>
            </div>
            <div className="flex gap-2">
              {selectedKey ? <button type="button" onClick={() => void remove()} className={dangerButton}>Delete</button> : null}
              <button type="button" onClick={() => void save()} disabled={saving} className={primaryButton}>{saving ? 'Saving...' : 'Save'}</button>
            </div>
          </div>

          {status ? <div className="mt-4 rounded-xl border border-emerald-500/30 bg-emerald-500/10 px-4 py-3 text-sm text-emerald-200">{status}</div> : null}
          {error ? <div className="mt-4 rounded-xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-200">{error}</div> : null}

          <div className="mt-4 grid gap-4 md:grid-cols-2">
            <Field label="Type"><input value={draft.type} onChange={(e) => setDraft({ ...draft, type: e.target.value })} className={inputClass} /></Field>
            <Field label="Full name"><input value={draft.fullName} onChange={(e) => setDraft({ ...draft, fullName: e.target.value })} className={inputClass} /></Field>
            <Field label="Label"><input value={draft.label} onChange={(e) => setDraft({ ...draft, label: e.target.value })} className={inputClass} /></Field>
            <Field label="Directory"><input value={draft.directoryName} onChange={(e) => setDraft({ ...draft, directoryName: e.target.value })} className={inputClass} /></Field>
            <Field label="File name"><input value={draft.fileName} onChange={(e) => setDraft({ ...draft, fileName: e.target.value })} className={inputClass} /></Field>
            <Field label="Last modified"><input value={draft.lastModifiedDate} onChange={(e) => setDraft({ ...draft, lastModifiedDate: e.target.value })} className={inputClass} /></Field>
          </div>

          <div className="mt-4 grid gap-4 md:grid-cols-2">
            <label className="flex items-center gap-3 rounded-xl border border-slate-800 bg-slate-950/70 px-4 py-3 text-sm text-slate-200">
              <input type="checkbox" checked={draft.inFolder} onChange={(e) => setDraft({ ...draft, inFolder: e.target.checked })} />
              In folder
            </label>
            <label className="flex items-center gap-3 rounded-xl border border-slate-800 bg-slate-950/70 px-4 py-3 text-sm text-slate-200">
              <input type="checkbox" checked={draft.metaFile} onChange={(e) => setDraft({ ...draft, metaFile: e.target.checked })} />
              Meta file
            </label>
          </div>

          <div className="mt-4">
            <Field label="Attributes JSON">
              <textarea value={attributeEditor} onChange={(e) => setAttributeEditor(e.target.value)} className="h-64 w-full rounded-2xl border border-slate-800 bg-slate-950 px-4 py-3 font-mono text-sm text-slate-100 outline-none focus:border-fuchsia-400" />
            </Field>
          </div>
        </div>
      </div>
    </div>
  )
}

function keyFor(resource: MetadataResource): string {
  return `${resource.type}::${resource.fullName}`
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="block text-sm text-slate-300">
      <span className="mb-2 block text-xs uppercase tracking-[0.16em] text-slate-500">{label}</span>
      {children}
    </label>
  )
}

const inputClass = 'rounded-xl border border-slate-800 bg-slate-900 px-3 py-2 text-sm text-white outline-none focus:border-fuchsia-400'
const primaryButton = 'rounded-xl bg-fuchsia-400 px-4 py-2 text-sm font-medium text-slate-950 transition hover:bg-fuchsia-300 disabled:cursor-not-allowed disabled:opacity-60'
const secondaryButton = 'rounded-xl border border-slate-700 px-4 py-2 text-sm font-medium text-slate-200 transition hover:border-slate-500 hover:text-white'
const dangerButton = 'rounded-xl border border-rose-500/40 px-4 py-2 text-sm font-medium text-rose-200 transition hover:border-rose-400 hover:text-white'

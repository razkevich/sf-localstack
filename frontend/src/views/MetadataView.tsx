import { useEffect, useMemo, useState } from 'react'
import { FileCode, Plus, RefreshCw, Trash2 } from 'lucide-react'
import {
  createMetadataResource,
  deleteMetadataResource,
  fetchMetadataResources,
  updateMetadataResource,
} from '../services/api'
import type { MetadataResource } from '../types'
import { DataTable } from '../components/ui/DataTable'
import type { Column } from '../components/ui/DataTable'
import { PageHeader } from '../components/ui/PageHeader'
import { Modal } from '../components/ui/Modal'
import { Badge } from '../components/ui/Badge'
import { useToast } from '../components/ui/Toast'

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

export function MetadataView() {
  const [resources, setResources] = useState<MetadataResource[]>([])
  const [loading, setLoading] = useState(true)
  const [showCreate, setShowCreate] = useState(false)
  const [showDetail, setShowDetail] = useState<MetadataResource | null>(null)
  const [editing, setEditing] = useState(false)
  const [draft, setDraft] = useState<MetadataResource>(EMPTY_RESOURCE)
  const [attrJson, setAttrJson] = useState('{}')
  const [saving, setSaving] = useState(false)
  const { showSuccess, showError } = useToast()

  async function loadResources() {
    setLoading(true)
    try {
      setResources(await fetchMetadataResources())
    } catch (err) {
      showError(err instanceof Error ? err.message : 'Failed to load metadata')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { void loadResources() }, []) // eslint-disable-line react-hooks/exhaustive-deps

  const columns = useMemo((): Column[] => [
    { key: 'type', label: 'Type', sortable: true, render: (v) => <Badge variant="info">{String(v)}</Badge> },
    { key: 'fullName', label: 'Full Name', sortable: true },
    { key: 'label', label: 'Label', sortable: true },
    { key: 'directoryName', label: 'Directory', sortable: true },
    {
      key: 'lastModifiedDate',
      label: 'Last Modified',
      sortable: true,
      render: (v) => {
        try { return new Date(String(v)).toLocaleDateString() } catch { return String(v) }
      },
    },
  ], [])

  function openDetail(resource: MetadataResource) {
    setShowDetail(resource)
    setDraft({ ...resource })
    setAttrJson(JSON.stringify(resource.attributes ?? {}, null, 2))
    setEditing(false)
  }

  function openCreate() {
    setShowCreate(true)
    setDraft({ ...EMPTY_RESOURCE, lastModifiedDate: new Date().toISOString() })
    setAttrJson('{}')
  }

  async function handleSave() {
    setSaving(true)
    try {
      const attributes = JSON.parse(attrJson) as Record<string, unknown>
      const payload = { ...draft, attributes }

      if (showDetail) {
        await updateMetadataResource(showDetail.type, showDetail.fullName, payload)
        showSuccess('Metadata resource updated')
        setShowDetail(null)
      } else {
        await createMetadataResource(payload)
        showSuccess('Metadata resource created')
        setShowCreate(false)
      }
      await loadResources()
    } catch (err) {
      showError(err instanceof Error ? err.message : 'Failed to save')
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(resource: MetadataResource) {
    setSaving(true)
    try {
      await deleteMetadataResource(resource.type, resource.fullName)
      showSuccess('Metadata resource deleted')
      setShowDetail(null)
      await loadResources()
    } catch (err) {
      showError(err instanceof Error ? err.message : 'Failed to delete')
    } finally {
      setSaving(false)
    }
  }

  const formContent = (
    <div className="space-y-4">
      <div className="grid gap-4 sm:grid-cols-2">
        <FormField label="Type" value={draft.type} onChange={(v) => setDraft({ ...draft, type: v })} />
        <FormField label="Full Name" value={draft.fullName} onChange={(v) => setDraft({ ...draft, fullName: v })} />
        <FormField label="Label" value={draft.label} onChange={(v) => setDraft({ ...draft, label: v })} />
        <FormField label="Directory" value={draft.directoryName} onChange={(v) => setDraft({ ...draft, directoryName: v })} />
        <FormField label="File Name" value={draft.fileName} onChange={(v) => setDraft({ ...draft, fileName: v })} />
        <FormField label="Last Modified" value={draft.lastModifiedDate} onChange={(v) => setDraft({ ...draft, lastModifiedDate: v })} />
      </div>
      <div className="flex gap-4">
        <label className="flex items-center gap-2 text-body-md text-neutral-70">
          <input type="checkbox" checked={draft.inFolder} onChange={(e) => setDraft({ ...draft, inFolder: e.target.checked })} className="h-4 w-4 rounded-sm border-neutral-20 text-brand" />
          In Folder
        </label>
        <label className="flex items-center gap-2 text-body-md text-neutral-70">
          <input type="checkbox" checked={draft.metaFile} onChange={(e) => setDraft({ ...draft, metaFile: e.target.checked })} className="h-4 w-4 rounded-sm border-neutral-20 text-brand" />
          Meta File
        </label>
      </div>
      <div>
        <label className="mb-1 block text-body-sm font-medium text-neutral-70">Attributes</label>
        <textarea
          value={attrJson}
          onChange={(e) => setAttrJson(e.target.value)}
          rows={6}
          className="w-full rounded-slds border border-neutral-20 bg-neutral-00 px-3 py-2 font-mono text-body-sm text-neutral-90 outline-none focus:border-brand focus:ring-1 focus:ring-brand"
        />
      </div>
    </div>
  )

  return (
    <div className="flex h-full flex-col">
      <PageHeader
        title="Metadata"
        subtitle={`${resources.length} resource${resources.length !== 1 ? 's' : ''}`}
        icon={<FileCode className="h-5 w-5" />}
        actions={
          <div className="flex items-center gap-2">
            <button type="button" onClick={() => { void loadResources() }} className="flex items-center gap-1.5 rounded-slds border border-neutral-20 bg-neutral-00 px-3 py-1.5 text-body-sm font-medium text-neutral-70 hover:bg-neutral-05">
              <RefreshCw className="h-3.5 w-3.5" />
              Refresh
            </button>
            <button type="button" onClick={openCreate} className="flex items-center gap-1.5 rounded-slds bg-brand px-3 py-1.5 text-body-sm font-semibold text-neutral-00 hover:bg-brand-dark">
              <Plus className="h-3.5 w-3.5" />
              New
            </button>
          </div>
        }
      />

      <div className="flex-1 overflow-auto p-6">
        <DataTable
          columns={columns}
          data={resources as unknown as Record<string, unknown>[]}
          loading={loading}
          onRowClick={(row) => openDetail(row as unknown as MetadataResource)}
          emptyMessage="No metadata resources to display"
        />
      </div>

      <Modal
        isOpen={showCreate}
        onClose={() => setShowCreate(false)}
        title="New Metadata Resource"
        actions={
          <>
            <button type="button" onClick={() => setShowCreate(false)} className="rounded-slds border border-neutral-20 bg-neutral-00 px-4 py-2 text-body-md font-medium text-neutral-70 hover:bg-neutral-05">Cancel</button>
            <button type="button" onClick={() => { void handleSave() }} disabled={saving} className="rounded-slds bg-brand px-4 py-2 text-body-md font-semibold text-neutral-00 hover:bg-brand-dark disabled:opacity-60">{saving ? 'Saving...' : 'Save'}</button>
          </>
        }
      >
        {formContent}
      </Modal>

      <Modal
        isOpen={showDetail !== null}
        onClose={() => setShowDetail(null)}
        title={showDetail ? `${showDetail.type}: ${showDetail.fullName}` : ''}
        actions={
          <>
            {!editing ? (
              <>
                <button type="button" onClick={() => { if (showDetail) void handleDelete(showDetail) }} className="flex items-center gap-1.5 rounded-slds border border-error/20 bg-neutral-00 px-4 py-2 text-body-md font-medium text-error hover:bg-error/5">
                  <Trash2 className="h-3.5 w-3.5" />
                  Delete
                </button>
                <button type="button" onClick={() => setEditing(true)} className="rounded-slds border border-neutral-20 bg-neutral-00 px-4 py-2 text-body-md font-medium text-neutral-70 hover:bg-neutral-05">Edit</button>
                <button type="button" onClick={() => setShowDetail(null)} className="rounded-slds bg-brand px-4 py-2 text-body-md font-semibold text-neutral-00 hover:bg-brand-dark">Close</button>
              </>
            ) : (
              <>
                <button type="button" onClick={() => setEditing(false)} className="rounded-slds border border-neutral-20 bg-neutral-00 px-4 py-2 text-body-md font-medium text-neutral-70 hover:bg-neutral-05">Cancel</button>
                <button type="button" onClick={() => { void handleSave() }} disabled={saving} className="rounded-slds bg-brand px-4 py-2 text-body-md font-semibold text-neutral-00 hover:bg-brand-dark disabled:opacity-60">{saving ? 'Saving...' : 'Save'}</button>
              </>
            )}
          </>
        }
      >
        {editing ? formContent : showDetail ? (
          <div className="space-y-3">
            <div className="grid gap-x-8 gap-y-3 sm:grid-cols-2">
              <DetailField label="Type" value={showDetail.type} />
              <DetailField label="Full Name" value={showDetail.fullName} />
              <DetailField label="Label" value={showDetail.label} />
              <DetailField label="Directory" value={showDetail.directoryName} />
              <DetailField label="File Name" value={showDetail.fileName} />
              <DetailField label="Last Modified" value={showDetail.lastModifiedDate} />
              <DetailField label="In Folder" value={showDetail.inFolder ? 'Yes' : 'No'} />
              <DetailField label="Meta File" value={showDetail.metaFile ? 'Yes' : 'No'} />
            </div>
            {Object.keys(showDetail.attributes).length > 0 && (
              <div>
                <div className="mb-2 text-body-sm font-semibold uppercase text-neutral-50">Attributes</div>
                <div className="grid gap-x-8 gap-y-3 sm:grid-cols-2">
                  {Object.entries(showDetail.attributes).map(([k, v]) => (
                    <DetailField key={k} label={k} value={String(v)} />
                  ))}
                </div>
              </div>
            )}
          </div>
        ) : null}
      </Modal>
    </div>
  )
}

function FormField({ label, value, onChange }: { label: string; value: string; onChange: (v: string) => void }) {
  return (
    <div>
      <label className="mb-1 block text-body-sm font-medium text-neutral-70">{label}</label>
      <input
        type="text"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="w-full rounded-slds border border-neutral-20 bg-neutral-00 px-3 py-2 text-body-md text-neutral-90 outline-none transition focus:border-brand focus:ring-1 focus:ring-brand"
      />
    </div>
  )
}

function DetailField({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="text-body-sm text-neutral-50">{label}</dt>
      <dd className="mt-0.5 text-body-md text-neutral-80">{value || '--'}</dd>
    </div>
  )
}

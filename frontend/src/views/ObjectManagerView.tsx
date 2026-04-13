import { useState, useMemo } from 'react'
import { Database, Search, Briefcase, Contact, Target, ChevronRight, FileCheck, User, CheckSquare, Calendar, Plus } from 'lucide-react'
import { PageHeader } from '../components/ui/PageHeader'
import { Modal } from '../components/ui/Modal'
import { createCustomObject } from '../services/api'
import { useToast } from '../components/ui/Toast'

interface ObjectCount {
  objectType: string
  count: number
}

interface Props {
  objectCounts: ObjectCount[]
  onSelectObject: (objectType: string) => void
  onRefresh?: () => void
}

const STANDARD_ICONS: Record<string, React.ReactNode> = {
  Account: <Briefcase className="h-4 w-4 text-brand" />,
  Contact: <Contact className="h-4 w-4 text-brand" />,
  Lead: <Target className="h-4 w-4 text-brand" />,
  Opportunity: <Database className="h-4 w-4 text-brand" />,
  Case: <FileCheck className="h-4 w-4 text-brand" />,
  User: <User className="h-4 w-4 text-brand" />,
  Task: <CheckSquare className="h-4 w-4 text-brand" />,
  Event: <Calendar className="h-4 w-4 text-brand" />,
}

const STANDARD_OBJECTS = ['Account', 'Contact', 'Lead', 'Opportunity', 'Case', 'User', 'Task', 'Event']

export function ObjectManagerView({ objectCounts, onSelectObject, onRefresh }: Props) {
  const [search, setSearch] = useState('')
  const [showCreate, setShowCreate] = useState(false)
  const [newLabel, setNewLabel] = useState('')
  const [newApiName, setNewApiName] = useState('')
  const [saving, setSaving] = useState(false)
  const { showSuccess, showError } = useToast()

  function handleLabelChange(value: string) {
    setNewLabel(value)
    setNewApiName(value.replace(/\s+/g, '_') + '__c')
  }

  async function handleCreateObject() {
    if (!newLabel.trim() || !newApiName.trim()) return
    setSaving(true)
    try {
      await createCustomObject(newLabel.trim(), newApiName.trim())
      showSuccess(`Custom object "${newLabel}" created`)
      setShowCreate(false)
      setNewLabel('')
      setNewApiName('')
      onRefresh?.()
    } catch (err) {
      showError(err instanceof Error ? err.message : 'Failed to create custom object')
    } finally {
      setSaving(false)
    }
  }

  const allObjects = useMemo(() => {
    const countMap = new Map(objectCounts.map((o) => [o.objectType, o.count]))
    // Ensure standard objects always appear, even with 0 records
    const types = new Set([...STANDARD_OBJECTS, ...objectCounts.map((o) => o.objectType)])
    return Array.from(types).map((name) => ({
      name,
      count: countMap.get(name) ?? 0,
      isCustom: name.endsWith('__c'),
      isStandard: STANDARD_OBJECTS.includes(name),
    }))
  }, [objectCounts])

  const filtered = useMemo(() => {
    if (!search.trim()) return allObjects
    const q = search.toLowerCase()
    return allObjects.filter((o) => o.name.toLowerCase().includes(q))
  }, [allObjects, search])

  const standard = filtered.filter((o) => o.isStandard)
  const custom = filtered.filter((o) => o.isCustom)
  const other = filtered.filter((o) => !o.isStandard && !o.isCustom)

  return (
    <div className="flex h-full flex-col">
      <PageHeader
        title="Object Manager"
        subtitle={`${allObjects.length} object${allObjects.length !== 1 ? 's' : ''}`}
        icon={<Database className="h-5 w-5" />}
        actions={
          <button
            type="button"
            onClick={() => setShowCreate(true)}
            className="flex items-center gap-1.5 rounded-slds bg-brand px-3 py-1.5 text-body-sm font-semibold text-neutral-00 hover:bg-brand-dark"
          >
            <Plus className="h-3.5 w-3.5" />
            New Custom Object
          </button>
        }
      />

      <div className="px-6 pt-4 pb-2">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-neutral-40" />
          <input
            type="text"
            placeholder="Search objects..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full rounded-slds border border-neutral-20 bg-neutral-00 py-2 pl-9 pr-3 text-body-md text-neutral-90 placeholder:text-neutral-40 focus:border-brand focus:outline-none focus:ring-1 focus:ring-brand"
          />
        </div>
      </div>

      <div className="flex-1 overflow-auto px-6 pb-6">
        {standard.length > 0 && (
          <ObjectSection title="Standard Objects" objects={standard} onSelect={onSelectObject} />
        )}
        {custom.length > 0 && (
          <ObjectSection title="Custom Objects" objects={custom} onSelect={onSelectObject} />
        )}
        {other.length > 0 && (
          <ObjectSection title="Other Objects" objects={other} onSelect={onSelectObject} />
        )}
        {filtered.length === 0 && (
          <div className="py-12 text-center text-body-md text-neutral-50">
            No objects matching "{search}"
          </div>
        )}
      </div>

      <Modal
        isOpen={showCreate}
        onClose={() => setShowCreate(false)}
        title="New Custom Object"
      >
        <div className="space-y-4">
          <div>
            <label className="mb-1 block text-body-sm font-medium text-neutral-70">Label</label>
            <input
              type="text"
              value={newLabel}
              onChange={(e) => handleLabelChange(e.target.value)}
              placeholder="e.g. Invoice"
              className="w-full rounded-slds border border-neutral-20 bg-neutral-00 px-3 py-2 text-body-md text-neutral-90 placeholder:text-neutral-40 focus:border-brand focus:outline-none focus:ring-1 focus:ring-brand"
            />
          </div>
          <div>
            <label className="mb-1 block text-body-sm font-medium text-neutral-70">API Name</label>
            <input
              type="text"
              value={newApiName}
              onChange={(e) => setNewApiName(e.target.value)}
              placeholder="e.g. Invoice__c"
              className="w-full rounded-slds border border-neutral-20 bg-neutral-00 px-3 py-2 text-body-md text-neutral-90 placeholder:text-neutral-40 focus:border-brand focus:outline-none focus:ring-1 focus:ring-brand"
            />
          </div>
          <div className="flex items-center justify-end gap-2 pt-2">
            <button
              type="button"
              onClick={() => setShowCreate(false)}
              className="rounded-slds border border-neutral-20 bg-neutral-00 px-4 py-2 text-body-sm font-medium text-neutral-70 hover:bg-neutral-05"
            >
              Cancel
            </button>
            <button
              type="button"
              onClick={() => { void handleCreateObject() }}
              disabled={saving || !newLabel.trim() || !newApiName.trim()}
              className="rounded-slds bg-brand px-4 py-2 text-body-sm font-semibold text-neutral-00 hover:bg-brand-dark disabled:opacity-50"
            >
              {saving ? 'Saving...' : 'Save'}
            </button>
          </div>
        </div>
      </Modal>
    </div>
  )
}

function ObjectSection({
  title,
  objects,
  onSelect,
}: {
  title: string
  objects: { name: string; count: number; isCustom: boolean }[]
  onSelect: (name: string) => void
}) {
  return (
    <div className="mt-4">
      <h3 className="mb-2 text-body-sm font-semibold uppercase tracking-wider text-neutral-50">
        {title}
      </h3>
      <div className="overflow-hidden rounded-slds border border-neutral-20 bg-neutral-00">
        {objects.map((obj, i) => (
          <button
            key={obj.name}
            type="button"
            onClick={() => onSelect(obj.name)}
            className={`flex w-full items-center gap-3 px-4 py-3 text-left transition-colors hover:bg-neutral-05 ${
              i > 0 ? 'border-t border-neutral-10' : ''
            }`}
          >
            {STANDARD_ICONS[obj.name] ?? <Database className="h-4 w-4 text-neutral-50" />}
            <span className="flex-1 text-body-md font-medium text-neutral-90">{obj.name}</span>
            <span className="text-body-sm text-neutral-50">{obj.count} record{obj.count !== 1 ? 's' : ''}</span>
            <ChevronRight className="h-4 w-4 text-neutral-30" />
          </button>
        ))}
      </div>
    </div>
  )
}

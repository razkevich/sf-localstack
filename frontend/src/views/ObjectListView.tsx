import { useEffect, useState, useMemo } from 'react'
import { Database, Plus, RefreshCw } from 'lucide-react'
import { fetchDescribe, fetchObjectRecords, createRecord, replaceRecord, deleteRecord } from '../services/api'
import type { DescribeResult, SObjectListResult, SalesforceRecord } from '../types'
import { DataTable } from '../components/ui/DataTable'
import type { Column } from '../components/ui/DataTable'
import { PageHeader } from '../components/ui/PageHeader'
import { Modal } from '../components/ui/Modal'
import { RecordForm } from './RecordForm'
import { RecordDetail } from './RecordDetail'
import { useToast } from '../components/ui/Toast'

interface Props {
  objectType: string
}

const DEFAULT_COLUMNS = ['Name', 'Id', 'CreatedDate', 'LastModifiedDate']

type ViewTab = 'records' | 'fields'

export function ObjectListView({ objectType }: Props) {
  const [recordsResult, setRecordsResult] = useState<SObjectListResult | null>(null)
  const [describeResult, setDescribeResult] = useState<DescribeResult | null>(null)
  const [loading, setLoading] = useState(true)
  const [showCreate, setShowCreate] = useState(false)
  const [selectedRecord, setSelectedRecord] = useState<SalesforceRecord | null>(null)
  const [saving, setSaving] = useState(false)
  const [viewTab, setViewTab] = useState<ViewTab>('records')
  const { showSuccess, showError } = useToast()

  async function loadData() {
    setLoading(true)
    try {
      const [records, describe] = await Promise.all([
        fetchObjectRecords(objectType),
        fetchDescribe(objectType),
      ])
      setRecordsResult(records)
      setDescribeResult(describe)
    } catch (err) {
      showError(err instanceof Error ? err.message : 'Failed to load records')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    setSelectedRecord(null)
    setShowCreate(false)
    void loadData()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [objectType])

  const columns = useMemo((): Column[] => {
    if (!describeResult) return []
    const fieldMap = new Map(describeResult.fields.map((f) => [f.name, f]))

    const colNames = DEFAULT_COLUMNS.filter((name) => fieldMap.has(name))
    const extraFields = describeResult.fields
      .filter((f) => !DEFAULT_COLUMNS.includes(f.name) && f.custom && !f.deprecatedAndHidden)
      .slice(0, 3)
      .map((f) => f.name)

    const allCols = [...colNames, ...extraFields]

    return allCols.map((name): Column => {
      const field = fieldMap.get(name)
      return {
        key: name,
        label: field?.label ?? name,
        sortable: field?.sortable ?? true,
      }
    })
  }, [describeResult])

  const records = useMemo(() => {
    const items = recordsResult?.recentItems ?? []
    return items.map((r) => {
      const clone = { ...r }
      delete clone.attributes
      return clone
    })
  }, [recordsResult])

  const fieldColumns = useMemo((): Column[] => [
    { key: 'name', label: 'API Name', sortable: true },
    { key: 'label', label: 'Label', sortable: true },
    { key: 'type', label: 'Type', sortable: true },
    {
      key: 'kind',
      label: 'Kind',
      sortable: true,
      render: (value) => {
        const isCustom = value === 'Custom'
        return (
          <span className={`inline-block rounded-full px-2 py-0.5 text-body-sm font-medium ${isCustom ? 'bg-brand-lighter text-brand-dark' : 'bg-neutral-10 text-neutral-60'}`}>
            {String(value)}
          </span>
        )
      },
    },
  ], [])

  const fieldsData = useMemo(() => {
    if (!describeResult) return []
    return describeResult.fields.map((f) => ({
      name: f.name,
      label: f.label,
      type: f.type,
      kind: f.custom ? 'Custom' : 'Standard',
    }))
  }, [describeResult])

  async function handleCreate(values: Record<string, unknown>) {
    setSaving(true)
    try {
      await createRecord(objectType, values)
      showSuccess('Record created successfully')
      setShowCreate(false)
      await loadData()
    } catch (err) {
      showError(err instanceof Error ? err.message : 'Failed to create record')
    } finally {
      setSaving(false)
    }
  }

  async function handleUpdate(values: Record<string, unknown>) {
    if (!selectedRecord) return
    const id = String(selectedRecord.Id ?? '')
    if (!id) return
    setSaving(true)
    try {
      await replaceRecord(objectType, id, values)
      showSuccess('Record updated successfully')
      await loadData()
      setSelectedRecord(null)
    } catch (err) {
      showError(err instanceof Error ? err.message : 'Failed to update record')
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete() {
    if (!selectedRecord) return
    const id = String(selectedRecord.Id ?? '')
    if (!id) return
    setSaving(true)
    try {
      await deleteRecord(objectType, id)
      showSuccess('Record deleted successfully')
      setSelectedRecord(null)
      await loadData()
    } catch (err) {
      showError(err instanceof Error ? err.message : 'Failed to delete record')
    } finally {
      setSaving(false)
    }
  }

  if (selectedRecord) {
    return (
      <div className="p-6">
        <RecordDetail
          record={selectedRecord}
          fields={describeResult?.fields ?? []}
          objectType={objectType}
          onSave={handleUpdate}
          onDelete={handleDelete}
          onClose={() => setSelectedRecord(null)}
          saving={saving}
        />
      </div>
    )
  }

  return (
    <div className="flex h-full flex-col">
      <PageHeader
        title={describeResult?.labelPlural ?? objectType}
        subtitle={viewTab === 'records'
          ? `${records.length} record${records.length !== 1 ? 's' : ''}`
          : `${fieldsData.length} field${fieldsData.length !== 1 ? 's' : ''}`}
        icon={<Database className="h-5 w-5" />}
        actions={
          <div className="flex items-center gap-2">
            <div className="flex overflow-hidden rounded-slds border border-neutral-20">
              <button
                type="button"
                onClick={() => setViewTab('records')}
                className={`px-3 py-1.5 text-body-sm font-medium ${viewTab === 'records' ? 'bg-brand text-neutral-00' : 'bg-neutral-00 text-neutral-70 hover:bg-neutral-05'}`}
              >
                Records
              </button>
              <button
                type="button"
                onClick={() => setViewTab('fields')}
                className={`border-l border-neutral-20 px-3 py-1.5 text-body-sm font-medium ${viewTab === 'fields' ? 'bg-brand text-neutral-00' : 'bg-neutral-00 text-neutral-70 hover:bg-neutral-05'}`}
              >
                Fields
              </button>
            </div>
            <button
              type="button"
              onClick={() => { void loadData() }}
              className="flex items-center gap-1.5 rounded-slds border border-neutral-20 bg-neutral-00 px-3 py-1.5 text-body-sm font-medium text-neutral-70 hover:bg-neutral-05"
            >
              <RefreshCw className="h-3.5 w-3.5" />
              Refresh
            </button>
            {viewTab === 'records' && (
              <button
                type="button"
                onClick={() => setShowCreate(true)}
                className="flex items-center gap-1.5 rounded-slds bg-brand px-3 py-1.5 text-body-sm font-semibold text-neutral-00 hover:bg-brand-dark"
              >
                <Plus className="h-3.5 w-3.5" />
                New
              </button>
            )}
          </div>
        }
      />

      <div className="flex-1 overflow-auto p-6">
        {viewTab === 'records' ? (
          <DataTable
            columns={columns}
            data={records as Record<string, unknown>[]}
            loading={loading}
            onRowClick={(row) => setSelectedRecord(row as SalesforceRecord)}
            emptyMessage={`No ${objectType} records to display`}
          />
        ) : (
          <DataTable
            columns={fieldColumns}
            data={fieldsData as Record<string, unknown>[]}
            loading={loading}
            emptyMessage={`No fields to display for ${objectType}`}
          />
        )}
      </div>

      <Modal
        isOpen={showCreate}
        onClose={() => setShowCreate(false)}
        title={`New ${objectType}`}
      >
        <RecordForm
          fields={describeResult?.fields ?? []}
          onSave={handleCreate}
          onCancel={() => setShowCreate(false)}
          saving={saving}
        />
      </Modal>
    </div>
  )
}

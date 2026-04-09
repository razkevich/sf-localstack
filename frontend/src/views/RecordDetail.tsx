import { useState } from 'react'
import { Edit2, Trash2 } from 'lucide-react'
import { RecordForm } from './RecordForm'
import { Modal } from '../components/ui/Modal'
import type { DescribeField, SalesforceRecord } from '../types'

interface Props {
  record: SalesforceRecord
  fields: DescribeField[]
  objectType: string
  onSave: (values: Record<string, unknown>) => Promise<void>
  onDelete: () => Promise<void>
  onClose: () => void
  saving?: boolean
}

const SYSTEM_FIELDS = new Set(['Id', 'CreatedDate', 'LastModifiedDate', 'SystemModstamp', 'CreatedById', 'LastModifiedById', 'IsDeleted'])

export function RecordDetail({ record, fields, objectType, onSave, onDelete, onClose, saving = false }: Props) {
  const [editing, setEditing] = useState(false)
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)

  const recordData = normalizeRecord(record)
  const detailFields = Object.entries(recordData).filter(([key]) => !SYSTEM_FIELDS.has(key))
  const systemFields = Object.entries(recordData).filter(([key]) => SYSTEM_FIELDS.has(key))

  async function handleSave(values: Record<string, unknown>) {
    await onSave(values)
    setEditing(false)
  }

  async function handleDelete() {
    await onDelete()
    setShowDeleteConfirm(false)
  }

  if (editing) {
    return (
      <div className="rounded-slds border border-neutral-20 bg-neutral-00 p-6">
        <h3 className="mb-4 text-heading-sm font-bold text-neutral-90">Edit {objectType}</h3>
        <RecordForm
          fields={fields}
          initialValues={recordData}
          onSave={handleSave}
          onCancel={() => setEditing(false)}
          saving={saving}
        />
      </div>
    )
  }

  return (
    <>
      <div className="rounded-slds border border-neutral-20 bg-neutral-00">
        <div className="flex items-center justify-between border-b border-neutral-20 px-6 py-4">
          <div>
            <div className="text-body-sm text-neutral-50">{objectType}</div>
            <h3 className="text-heading-sm font-bold text-neutral-90">
              {String(recordData.Name ?? recordData.Id ?? 'Record')}
            </h3>
          </div>
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => setEditing(true)}
              className="flex items-center gap-1.5 rounded-slds border border-neutral-20 bg-neutral-00 px-3 py-1.5 text-body-sm font-medium text-neutral-70 hover:bg-neutral-05"
            >
              <Edit2 className="h-3.5 w-3.5" />
              Edit
            </button>
            <button
              type="button"
              onClick={() => setShowDeleteConfirm(true)}
              className="flex items-center gap-1.5 rounded-slds border border-error/20 bg-neutral-00 px-3 py-1.5 text-body-sm font-medium text-error hover:bg-error/5"
            >
              <Trash2 className="h-3.5 w-3.5" />
              Delete
            </button>
            <button
              type="button"
              onClick={onClose}
              className="rounded-slds border border-neutral-20 bg-neutral-00 px-3 py-1.5 text-body-sm font-medium text-neutral-70 hover:bg-neutral-05"
            >
              Close
            </button>
          </div>
        </div>

        <div className="p-6">
          <h4 className="mb-3 text-body-sm font-semibold uppercase text-neutral-50">Details</h4>
          <div className="grid gap-x-8 gap-y-3 sm:grid-cols-2">
            {detailFields.map(([key, value]) => (
              <FieldRow key={key} label={fieldLabel(key, fields)} value={value} />
            ))}
          </div>

          {systemFields.length > 0 && (
            <>
              <h4 className="mb-3 mt-6 text-body-sm font-semibold uppercase text-neutral-50">System Information</h4>
              <div className="grid gap-x-8 gap-y-3 sm:grid-cols-2">
                {systemFields.map(([key, value]) => (
                  <FieldRow key={key} label={fieldLabel(key, fields)} value={value} />
                ))}
              </div>
            </>
          )}
        </div>
      </div>

      <Modal
        isOpen={showDeleteConfirm}
        onClose={() => setShowDeleteConfirm(false)}
        title="Confirm Delete"
        actions={
          <>
            <button
              type="button"
              onClick={() => setShowDeleteConfirm(false)}
              className="rounded-slds border border-neutral-20 bg-neutral-00 px-4 py-2 text-body-md font-medium text-neutral-70 hover:bg-neutral-05"
            >
              Cancel
            </button>
            <button
              type="button"
              onClick={() => { void handleDelete() }}
              className="rounded-slds bg-destructive px-4 py-2 text-body-md font-semibold text-neutral-00 hover:bg-error"
            >
              Delete
            </button>
          </>
        }
      >
        <p className="text-body-md text-neutral-70">
          Are you sure you want to delete this {objectType} record? This action cannot be undone.
        </p>
      </Modal>
    </>
  )
}

function FieldRow({ label, value }: { label: string; value: unknown }) {
  return (
    <div>
      <dt className="text-body-sm text-neutral-50">{label}</dt>
      <dd className="mt-0.5 text-body-md text-neutral-80">
        {value === null || value === undefined ? '--' : typeof value === 'object' ? JSON.stringify(value) : String(value)}
      </dd>
    </div>
  )
}

function fieldLabel(key: string, fields: DescribeField[]): string {
  const found = fields.find((f) => f.name === key)
  return found?.label ?? key
}

function normalizeRecord(record: SalesforceRecord): Record<string, unknown> {
  const clone = JSON.parse(JSON.stringify(record)) as Record<string, unknown>
  delete clone.attributes
  return clone
}

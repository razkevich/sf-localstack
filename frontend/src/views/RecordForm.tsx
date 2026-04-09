import { useState, useEffect } from 'react'
import type { DescribeField } from '../types'

interface Props {
  fields: DescribeField[]
  initialValues?: Record<string, unknown>
  onSave: (values: Record<string, unknown>) => Promise<void>
  onCancel: () => void
  saving?: boolean
}

export function RecordForm({ fields, initialValues, onSave, onCancel, saving = false }: Props) {
  const editableFields = fields.filter((f) =>
    !f.deprecatedAndHidden &&
    f.name !== 'Id' &&
    f.name !== 'CreatedDate' &&
    f.name !== 'LastModifiedDate' &&
    f.name !== 'SystemModstamp' &&
    f.name !== 'IsDeleted' &&
    f.name !== 'CreatedById' &&
    f.name !== 'LastModifiedById' &&
    (initialValues ? f.updateable : f.createable)
  )

  const [values, setValues] = useState<Record<string, unknown>>({})

  useEffect(() => {
    const init: Record<string, unknown> = {}
    for (const field of editableFields) {
      init[field.name] = initialValues?.[field.name] ?? ''
    }
    setValues(init)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [initialValues])

  function handleChange(fieldName: string, value: unknown) {
    setValues((prev) => ({ ...prev, [fieldName]: value }))
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    const payload: Record<string, unknown> = {}
    for (const field of editableFields) {
      const v = values[field.name]
      if (v !== '' && v !== undefined && v !== null) {
        if (field.type === 'double' || field.type === 'currency' || field.type === 'percent' || field.type === 'int') {
          payload[field.name] = Number(v)
        } else if (field.type === 'boolean') {
          payload[field.name] = Boolean(v)
        } else {
          payload[field.name] = v
        }
      }
    }
    await onSave(payload)
  }

  return (
    <form onSubmit={(e) => { void handleSubmit(e) }} className="space-y-4">
      <div className="grid gap-4 sm:grid-cols-2">
        {editableFields.map((field) => (
          <div key={field.name}>
            <label className="mb-1 block text-body-sm font-medium text-neutral-70">
              {field.label}
              {!field.nillable && field.name !== 'Id' && (
                <span className="ml-0.5 text-error">*</span>
              )}
            </label>
            {renderField(field, values[field.name], (v) => handleChange(field.name, v))}
          </div>
        ))}
      </div>

      <div className="flex items-center justify-end gap-2 border-t border-neutral-20 pt-4">
        <button
          type="button"
          onClick={onCancel}
          className="rounded-slds border border-neutral-20 bg-neutral-00 px-4 py-2 text-body-md font-medium text-neutral-70 hover:bg-neutral-05"
        >
          Cancel
        </button>
        <button
          type="submit"
          disabled={saving}
          className="rounded-slds bg-brand px-4 py-2 text-body-md font-semibold text-neutral-00 hover:bg-brand-dark disabled:opacity-60"
        >
          {saving ? 'Saving...' : 'Save'}
        </button>
      </div>
    </form>
  )
}

function renderField(
  field: DescribeField,
  value: unknown,
  onChange: (v: unknown) => void,
) {
  const baseClass = 'w-full rounded-slds border border-neutral-20 bg-neutral-00 px-3 py-2 text-body-md text-neutral-90 outline-none transition focus:border-brand focus:ring-1 focus:ring-brand'

  switch (field.type) {
    case 'boolean':
      return (
        <label className="flex items-center gap-2">
          <input
            type="checkbox"
            checked={Boolean(value)}
            onChange={(e) => onChange(e.target.checked)}
            className="h-4 w-4 rounded-sm border-neutral-20 text-brand focus:ring-brand"
          />
          <span className="text-body-md text-neutral-70">{value ? 'Yes' : 'No'}</span>
        </label>
      )
    case 'textarea':
      return (
        <textarea
          value={String(value ?? '')}
          onChange={(e) => onChange(e.target.value)}
          rows={3}
          className={baseClass}
        />
      )
    case 'date':
      return (
        <input
          type="date"
          value={String(value ?? '')}
          onChange={(e) => onChange(e.target.value)}
          className={baseClass}
        />
      )
    case 'datetime':
      return (
        <input
          type="datetime-local"
          value={String(value ?? '')}
          onChange={(e) => onChange(e.target.value)}
          className={baseClass}
        />
      )
    case 'double':
    case 'currency':
    case 'percent':
    case 'int':
      return (
        <input
          type="number"
          value={String(value ?? '')}
          onChange={(e) => onChange(e.target.value)}
          step={field.type === 'int' ? '1' : 'any'}
          className={baseClass}
        />
      )
    default:
      return (
        <input
          type="text"
          value={String(value ?? '')}
          onChange={(e) => onChange(e.target.value)}
          className={baseClass}
        />
      )
  }
}

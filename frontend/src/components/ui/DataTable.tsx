import { useState, useMemo } from 'react'
import { ChevronUp, ChevronDown, ChevronsUpDown } from 'lucide-react'

export interface Column<T = Record<string, unknown>> {
  key: string
  label: string
  sortable?: boolean
  render?: (value: unknown, row: T) => React.ReactNode
}

interface Props<T = Record<string, unknown>> {
  columns: Column<T>[]
  data: T[]
  onRowClick?: (row: T) => void
  pageSize?: number
  loading?: boolean
  emptyMessage?: string
}

type SortDir = 'asc' | 'desc'

function getNestedValue(obj: Record<string, unknown>, path: string): unknown {
  return path.split('.').reduce<unknown>((acc, key) => {
    if (acc && typeof acc === 'object') return (acc as Record<string, unknown>)[key]
    return undefined
  }, obj)
}

export function DataTable<T extends Record<string, unknown>>({
  columns,
  data,
  onRowClick,
  pageSize = 25,
  loading = false,
  emptyMessage = 'No records to display',
}: Props<T>) {
  const [sortKey, setSortKey] = useState<string | null>(null)
  const [sortDir, setSortDir] = useState<SortDir>('asc')
  const [page, setPage] = useState(0)

  const sorted = useMemo(() => {
    if (!sortKey) return data
    const slice = [...data]
    slice.sort((a, b) => {
      const va = getNestedValue(a, sortKey)
      const vb = getNestedValue(b, sortKey)
      const sa = va == null ? '' : String(va)
      const sb = vb == null ? '' : String(vb)
      const cmp = sa.localeCompare(sb, undefined, { numeric: true })
      return sortDir === 'asc' ? cmp : -cmp
    })
    return slice
  }, [data, sortKey, sortDir])

  const totalPages = Math.max(1, Math.ceil(sorted.length / pageSize))
  const safePage = Math.min(page, totalPages - 1)
  const paged = sorted.slice(safePage * pageSize, (safePage + 1) * pageSize)

  function handleSort(key: string) {
    if (sortKey === key) {
      setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'))
    } else {
      setSortKey(key)
      setSortDir('asc')
    }
    setPage(0)
  }

  if (loading) {
    return (
      <div className="overflow-x-auto rounded-slds border border-neutral-20 bg-neutral-00">
        <table className="w-full text-body-md">
          <thead>
            <tr className="border-b border-neutral-20 bg-neutral-05">
              {columns.map((col) => (
                <th key={col.key} className="px-4 py-2 text-left text-body-sm font-semibold text-neutral-70 uppercase">
                  {col.label}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {Array.from({ length: 5 }).map((_, i) => (
              <tr key={i} className="border-b border-neutral-10">
                {columns.map((col) => (
                  <td key={col.key} className="px-4 py-3">
                    <div className="h-4 w-3/4 animate-pulse rounded bg-neutral-10" />
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    )
  }

  if (data.length === 0) {
    return (
      <div className="rounded-slds border border-neutral-20 bg-neutral-00 px-6 py-12 text-center text-body-md text-neutral-50">
        {emptyMessage}
      </div>
    )
  }

  return (
    <div className="overflow-x-auto rounded-slds border border-neutral-20 bg-neutral-00">
      <table className="w-full text-body-md">
        <thead>
          <tr className="border-b border-neutral-20 bg-neutral-05">
            {columns.map((col) => (
              <th
                key={col.key}
                className={`px-4 py-2 text-left text-body-sm font-semibold uppercase text-neutral-70 ${col.sortable ? 'cursor-pointer select-none hover:bg-neutral-10' : ''}`}
                onClick={col.sortable ? () => handleSort(col.key) : undefined}
              >
                <span className="inline-flex items-center gap-1">
                  {col.label}
                  {col.sortable && sortKey === col.key ? (
                    sortDir === 'asc' ? <ChevronUp className="h-3.5 w-3.5" /> : <ChevronDown className="h-3.5 w-3.5" />
                  ) : col.sortable ? (
                    <ChevronsUpDown className="h-3.5 w-3.5 text-neutral-30" />
                  ) : null}
                </span>
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {paged.map((row, i) => (
            <tr
              key={String(row.Id ?? row.id ?? i)}
              onClick={onRowClick ? () => onRowClick(row) : undefined}
              className={`border-b border-neutral-10 transition-colors ${onRowClick ? 'cursor-pointer hover:bg-brand-lighter/40' : ''} ${i % 2 === 1 ? 'bg-neutral-05/50' : ''}`}
            >
              {columns.map((col) => (
                <td key={col.key} className="px-4 py-2.5 text-neutral-80">
                  {col.render
                    ? col.render(getNestedValue(row, col.key), row)
                    : formatCellValue(getNestedValue(row, col.key))}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>

      {totalPages > 1 && (
        <div className="flex items-center justify-between border-t border-neutral-20 bg-neutral-05 px-4 py-2 text-body-sm text-neutral-60">
          <span>
            {safePage * pageSize + 1}--{Math.min((safePage + 1) * pageSize, sorted.length)} of {sorted.length}
          </span>
          <div className="flex items-center gap-1">
            <button
              type="button"
              disabled={safePage === 0}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              className="rounded-slds px-3 py-1 hover:bg-neutral-10 disabled:opacity-40"
            >
              Previous
            </button>
            <button
              type="button"
              disabled={safePage >= totalPages - 1}
              onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
              className="rounded-slds px-3 py-1 hover:bg-neutral-10 disabled:opacity-40"
            >
              Next
            </button>
          </div>
        </div>
      )}
    </div>
  )
}

function formatCellValue(value: unknown): string {
  if (value === null || value === undefined) return '--'
  if (typeof value === 'boolean') return value ? 'Yes' : 'No'
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}

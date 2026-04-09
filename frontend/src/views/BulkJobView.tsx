import { useState, useMemo } from 'react'
import { UploadCloud, Plus, RefreshCw } from 'lucide-react'
import {
  createBulkJob,
  uploadBulkCsv,
  closeBulkJob,
  fetchBulkJob,
  fetchBulkCsvResult,
} from '../services/api'
import type { BulkJob } from '../types'
import { DataTable } from '../components/ui/DataTable'
import type { Column } from '../components/ui/DataTable'
import { PageHeader } from '../components/ui/PageHeader'
import { Modal } from '../components/ui/Modal'
import { Badge } from '../components/ui/Badge'
import { useToast } from '../components/ui/Toast'

function stateVariant(state: string): 'success' | 'warning' | 'error' | 'info' | 'neutral' {
  switch (state) {
    case 'JobComplete': return 'success'
    case 'Open': case 'UploadComplete': return 'info'
    case 'Failed': case 'Aborted': return 'error'
    default: return 'neutral'
  }
}

const CSV_PRESETS: Record<string, string> = {
  insert: 'Name,Industry\nBulk Corp,Technology\n',
  update: 'Id,Name\n001000000000001AAA,Updated Bulk Corp\n',
  delete: 'Id\n001000000000001AAA\n',
  upsert: 'External_Id__c,Name,Industry\nEXT-BULK-001,Bulk Upsert Corp,Technology\n',
}

export function BulkJobView() {
  const [jobs, setJobs] = useState<BulkJob[]>([])
  const [showCreate, setShowCreate] = useState(false)
  const [selectedJob, setSelectedJob] = useState<BulkJob | null>(null)
  const [loading, setLoading] = useState(false)
  const { showSuccess, showError } = useToast()

  // Create form state
  const [objectType, setObjectType] = useState('Account')
  const [operation, setOperation] = useState('insert')
  const [externalIdField, setExternalIdField] = useState('External_Id__c')
  const [csv, setCsv] = useState(CSV_PRESETS.insert)

  // Detail state
  const [successCsv, setSuccessCsv] = useState('')
  const [failedCsv, setFailedCsv] = useState('')
  const [unprocessedCsv, setUnprocessedCsv] = useState('')

  const columns = useMemo((): Column[] => [
    { key: 'id', label: 'Job ID', sortable: true },
    { key: 'object', label: 'Object', sortable: true },
    { key: 'operation', label: 'Operation', sortable: true },
    {
      key: 'state',
      label: 'State',
      sortable: true,
      render: (v) => <Badge variant={stateVariant(String(v))}>{String(v)}</Badge>,
    },
    { key: 'numberRecordsProcessed', label: 'Records', sortable: true },
    { key: 'contentType', label: 'Type', sortable: false },
  ], [])

  async function handleCreate() {
    setLoading(true)
    try {
      const job = await createBulkJob({
        object: objectType,
        operation,
        externalIdFieldName: operation === 'upsert' ? externalIdField : undefined,
      })
      setJobs((prev) => [job, ...prev])
      showSuccess(`Bulk job ${job.id} created`)
      setShowCreate(false)
      setSelectedJob(job)
    } catch (err) {
      showError(err instanceof Error ? err.message : 'Failed to create bulk job')
    } finally {
      setLoading(false)
    }
  }

  async function handleUploadAndClose() {
    if (!selectedJob) return
    setLoading(true)
    try {
      await uploadBulkCsv(selectedJob.id, csv)
      const completed = await closeBulkJob(selectedJob.id)
      setSelectedJob(completed)
      setJobs((prev) => prev.map((j) => (j.id === completed.id ? completed : j)))

      const [s, f, u] = await Promise.all([
        fetchBulkCsvResult(selectedJob.id, 'successfulResults'),
        fetchBulkCsvResult(selectedJob.id, 'failedResults'),
        fetchBulkCsvResult(selectedJob.id, 'unprocessedrecords'),
      ])
      setSuccessCsv(s)
      setFailedCsv(f)
      setUnprocessedCsv(u)
      showSuccess('CSV uploaded and job closed')
    } catch (err) {
      showError(err instanceof Error ? err.message : 'Failed to process bulk job')
    } finally {
      setLoading(false)
    }
  }

  async function handleRefreshJob() {
    if (!selectedJob) return
    setLoading(true)
    try {
      const refreshed = await fetchBulkJob(selectedJob.id)
      setSelectedJob(refreshed)
      setJobs((prev) => prev.map((j) => (j.id === refreshed.id ? refreshed : j)))
    } catch (err) {
      showError(err instanceof Error ? err.message : 'Failed to refresh job')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex h-full flex-col">
      <PageHeader
        title="Bulk Jobs"
        subtitle={`${jobs.length} job${jobs.length !== 1 ? 's' : ''}`}
        icon={<UploadCloud className="h-5 w-5" />}
        actions={
          <button type="button" onClick={() => setShowCreate(true)} className="flex items-center gap-1.5 rounded-slds bg-brand px-3 py-1.5 text-body-sm font-semibold text-neutral-00 hover:bg-brand-dark">
            <Plus className="h-3.5 w-3.5" />
            New Job
          </button>
        }
      />

      <div className="flex flex-1 flex-col overflow-hidden lg:flex-row">
        <div className="flex-1 overflow-auto border-b border-neutral-20 p-6 lg:border-b-0 lg:border-r">
          <DataTable
            columns={columns}
            data={jobs as unknown as Record<string, unknown>[]}
            loading={false}
            onRowClick={(row) => {
              const job = row as unknown as BulkJob
              setSelectedJob(job)
              setSuccessCsv('')
              setFailedCsv('')
              setUnprocessedCsv('')
            }}
            emptyMessage="No bulk jobs. Click New Job to create one."
          />
        </div>

        {selectedJob && (
          <div className="w-full overflow-auto bg-neutral-00 lg:w-[28rem]">
            <div className="border-b border-neutral-20 px-6 py-4">
              <div className="flex items-center justify-between">
                <div>
                  <div className="text-body-sm text-neutral-50">Job {selectedJob.id}</div>
                  <Badge variant={stateVariant(selectedJob.state)}>{selectedJob.state}</Badge>
                </div>
                <button type="button" onClick={() => { void handleRefreshJob() }} className="flex items-center gap-1.5 rounded-slds border border-neutral-20 bg-neutral-00 px-3 py-1.5 text-body-sm font-medium text-neutral-70 hover:bg-neutral-05">
                  <RefreshCw className="h-3.5 w-3.5" />
                  Refresh
                </button>
              </div>
            </div>

            <div className="space-y-4 p-6">
              <div className="grid grid-cols-2 gap-3">
                <InfoCell label="Object" value={selectedJob.object} />
                <InfoCell label="Operation" value={selectedJob.operation} />
                <InfoCell label="Processed" value={String(selectedJob.numberRecordsProcessed)} />
                <InfoCell label="Failed" value={String(selectedJob.numberRecordsFailed)} />
              </div>

              <div>
                <label className="mb-1 block text-body-sm font-medium text-neutral-70">CSV Data</label>
                <textarea
                  value={csv}
                  onChange={(e) => setCsv(e.target.value)}
                  rows={6}
                  className="w-full rounded-slds border border-neutral-20 bg-neutral-00 px-3 py-2 font-mono text-body-sm text-neutral-90 outline-none focus:border-brand focus:ring-1 focus:ring-brand"
                />
              </div>

              <button
                type="button"
                onClick={() => { void handleUploadAndClose() }}
                disabled={loading}
                className="w-full rounded-slds bg-brand px-4 py-2 text-body-md font-semibold text-neutral-00 hover:bg-brand-dark disabled:opacity-60"
              >
                {loading ? 'Processing...' : 'Upload CSV & Close Job'}
              </button>

              {successCsv && <CsvResultPanel title="Successful Results" csv={successCsv} />}
              {failedCsv && <CsvResultPanel title="Failed Results" csv={failedCsv} />}
              {unprocessedCsv && <CsvResultPanel title="Unprocessed Records" csv={unprocessedCsv} />}
            </div>
          </div>
        )}
      </div>

      <Modal
        isOpen={showCreate}
        onClose={() => setShowCreate(false)}
        title="New Bulk Job"
        actions={
          <>
            <button type="button" onClick={() => setShowCreate(false)} className="rounded-slds border border-neutral-20 bg-neutral-00 px-4 py-2 text-body-md font-medium text-neutral-70 hover:bg-neutral-05">Cancel</button>
            <button type="button" onClick={() => { void handleCreate() }} disabled={loading} className="rounded-slds bg-brand px-4 py-2 text-body-md font-semibold text-neutral-00 hover:bg-brand-dark disabled:opacity-60">{loading ? 'Creating...' : 'Create Job'}</button>
          </>
        }
      >
        <div className="space-y-4">
          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className="mb-1 block text-body-sm font-medium text-neutral-70">Object</label>
              <input type="text" value={objectType} onChange={(e) => setObjectType(e.target.value)} className="w-full rounded-slds border border-neutral-20 bg-neutral-00 px-3 py-2 text-body-md text-neutral-90 outline-none focus:border-brand focus:ring-1 focus:ring-brand" />
            </div>
            <div>
              <label className="mb-1 block text-body-sm font-medium text-neutral-70">Operation</label>
              <select value={operation} onChange={(e) => { setOperation(e.target.value); setCsv(CSV_PRESETS[e.target.value] ?? CSV_PRESETS.insert) }} className="w-full rounded-slds border border-neutral-20 bg-neutral-00 px-3 py-2 text-body-md text-neutral-90 outline-none focus:border-brand focus:ring-1 focus:ring-brand">
                <option value="insert">Insert</option>
                <option value="update">Update</option>
                <option value="delete">Delete</option>
                <option value="upsert">Upsert</option>
              </select>
            </div>
          </div>
          {operation === 'upsert' && (
            <div>
              <label className="mb-1 block text-body-sm font-medium text-neutral-70">External ID Field</label>
              <input type="text" value={externalIdField} onChange={(e) => setExternalIdField(e.target.value)} className="w-full rounded-slds border border-neutral-20 bg-neutral-00 px-3 py-2 text-body-md text-neutral-90 outline-none focus:border-brand focus:ring-1 focus:ring-brand" />
            </div>
          )}
        </div>
      </Modal>
    </div>
  )
}

function InfoCell({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-slds border border-neutral-20 bg-neutral-05 px-3 py-2">
      <div className="text-body-sm text-neutral-50">{label}</div>
      <div className="mt-0.5 text-body-md font-medium text-neutral-80">{value}</div>
    </div>
  )
}

function CsvResultPanel({ title, csv }: { title: string; csv: string }) {
  return (
    <div className="rounded-slds border border-neutral-20 bg-neutral-05 p-3">
      <div className="mb-2 text-body-sm font-semibold text-neutral-60">{title}</div>
      <pre className="max-h-40 overflow-auto whitespace-pre-wrap text-body-sm text-neutral-80">{csv}</pre>
    </div>
  )
}

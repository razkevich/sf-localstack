import { useState } from 'react'
import { callMetadataSoap, runToolingQuery } from '../services/api'
import type { ToolingQueryResult } from '../types'

const SOAP_PRESETS = {
  describeMetadata: '<met:describeMetadata/>',
  listMetadata: '<met:listMetadata><met:queries><met:type>CustomField</met:type></met:queries><met:asOfVersion>60.0</met:asOfVersion></met:listMetadata>',
  readMetadata: '<met:readMetadata><met:type>GlobalValueSet</met:type><met:fullNames>CustomerPriority</met:fullNames></met:readMetadata>',
  deployStatus: '<met:checkDeployStatus><met:asyncProcessId>0AfFAKEDEPLOY01</met:asyncProcessId><met:includeDetails>true</met:includeDetails></met:checkDeployStatus>',
  cancelDeploy: '<met:cancelDeploy><met:asyncProcessId>0AfFAKEDEPLOY01</met:asyncProcessId></met:cancelDeploy>',
} as const

const TOOLING_PRESETS = {
  tabs: 'SELECT Name FROM TabDefinition',
  applications: 'SELECT DeveloperName, NamespacePrefix FROM CustomApplication',
  customSettings: 'SELECT QualifiedApiName FROM EntityDefinition WHERE IsCustomSetting = true',
  loginFlows: 'SELECT DeveloperName, NamespacePrefix FROM FlowDefinition',
  flowDefinitionView: "SELECT DurableId FROM FlowDefinitionView WHERE ApiName = 'LoginFlow'",
  flowVersions: "SELECT VersionNumber FROM Flow WHERE DefinitionId = 'FlowDefinition/LoginFlow'",
} as const

export function MetadataExplorer() {
  const [soapBody, setSoapBody] = useState<string>(SOAP_PRESETS.describeMetadata)
  const [soapResponse, setSoapResponse] = useState('')
  const [soapError, setSoapError] = useState<string | null>(null)
  const [toolingQuery, setToolingQuery] = useState<string>(TOOLING_PRESETS.tabs)
  const [toolingResponse, setToolingResponse] = useState<ToolingQueryResult | null>(null)
  const [toolingError, setToolingError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function handleSoapRun() {
    setLoading(true)
    setSoapError(null)
    try {
      setSoapResponse(await callMetadataSoap(soapBody))
    } catch (err) {
      setSoapResponse('')
      setSoapError(err instanceof Error ? err.message : 'Failed to run metadata SOAP call')
    } finally {
      setLoading(false)
    }
  }

  async function handleToolingRun() {
    setLoading(true)
    setToolingError(null)
    try {
      setToolingResponse(await runToolingQuery(toolingQuery))
    } catch (err) {
      setToolingResponse(null)
      setToolingError(err instanceof Error ? err.message : 'Failed to run tooling query')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="grid h-full grid-cols-[1fr,1fr] overflow-hidden">
      <div className="overflow-auto border-r border-slate-800 bg-slate-950 p-6">
        <div className="text-xs uppercase tracking-[0.2em] text-cyan-300">Feature 4</div>
        <h2 className="mt-2 text-2xl font-semibold text-white">Metadata Explorer</h2>
        <p className="mt-1 text-sm text-slate-400">
          Inspect Metadata SOAP responses and the tooling-backed helper queries used by `metadata-service`.
        </p>

        <div className="mt-6 rounded-2xl border border-slate-800 bg-slate-900/70 p-4">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-semibold uppercase tracking-[0.18em] text-slate-400">SOAP operations</h3>
            <div className="flex flex-wrap gap-2">
              {Object.entries(SOAP_PRESETS).map(([key, value]) => (
                <button key={key} type="button" onClick={() => setSoapBody(value)} className={presetButton}>{key}</button>
              ))}
            </div>
          </div>
          <textarea value={soapBody} onChange={(e) => setSoapBody(e.target.value)} className={`${editorClass} mt-4 h-48`} />
          <div className="mt-4 flex items-center gap-3">
            <button type="button" onClick={() => void handleSoapRun()} disabled={loading} className={primaryButton}>Run SOAP</button>
            {soapError ? <span className="text-sm text-rose-300">{soapError}</span> : null}
          </div>
          <pre className="mt-4 max-h-[28rem] overflow-auto rounded-xl bg-slate-950 p-4 text-xs text-slate-200">{soapResponse || 'Run a metadata SOAP operation to inspect the XML response.'}</pre>
        </div>
      </div>

      <div className="overflow-auto bg-slate-950 p-6">
        <div className="rounded-2xl border border-slate-800 bg-slate-900/70 p-4">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-semibold uppercase tracking-[0.18em] text-slate-400">Tooling helper queries</h3>
            <div className="flex flex-wrap gap-2">
              {Object.entries(TOOLING_PRESETS).map(([key, value]) => (
                <button key={key} type="button" onClick={() => setToolingQuery(value)} className={presetButton}>{key}</button>
              ))}
            </div>
          </div>
          <textarea value={toolingQuery} onChange={(e) => setToolingQuery(e.target.value)} className={`${editorClass} mt-4 h-40`} />
          <div className="mt-4 flex items-center gap-3">
            <button type="button" onClick={() => void handleToolingRun()} disabled={loading} className={primaryButton}>Run tooling query</button>
            {toolingError ? <span className="text-sm text-rose-300">{toolingError}</span> : null}
          </div>
          <pre className="mt-4 max-h-[28rem] overflow-auto rounded-xl bg-slate-950 p-4 text-xs text-slate-200">{toolingResponse ? JSON.stringify(toolingResponse, null, 2) : 'Run a tooling query to inspect helper data.'}</pre>
        </div>
      </div>
    </div>
  )
}

const editorClass = 'w-full rounded-2xl border border-slate-800 bg-slate-950 px-4 py-3 font-mono text-sm text-slate-100 outline-none focus:border-cyan-400'
const primaryButton = 'rounded-xl bg-cyan-400 px-4 py-2 text-sm font-medium text-slate-950 transition hover:bg-cyan-300 disabled:cursor-not-allowed disabled:opacity-60'
const presetButton = 'rounded-full border border-slate-700 px-3 py-1 text-xs font-medium text-slate-300 transition hover:border-cyan-400 hover:text-white'

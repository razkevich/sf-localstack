import { useState } from 'react'
import { resetOrg } from '../services/api'

type Status = 'idle' | 'loading' | 'success' | 'error'

interface Props {
  onReset?: () => void
}

export function ResetButton({ onReset }: Props) {
  const [status, setStatus] = useState<Status>('idle')

  async function handleReset() {
    setStatus('loading')
    try {
      await resetOrg()
      setStatus('success')
      onReset?.()
      setTimeout(() => setStatus('idle'), 2000)
    } catch {
      setStatus('error')
      setTimeout(() => setStatus('idle'), 2000)
    }
  }

  const label =
    status === 'loading' ? 'Resetting...' :
    status === 'success' ? 'Reset!' :
    status === 'error' ? 'Error' :
    'Reset Org'

  return (
    <button
      onClick={handleReset}
      disabled={status === 'loading'}
      className={`w-full rounded-xl px-3 py-3 text-sm font-medium transition-colors
        ${status === 'success' ? 'bg-green-600 text-white' :
          status === 'error' ? 'bg-red-600 text-white' :
          'bg-slate-100 text-slate-900 hover:bg-white'}`}
    >
      {label}
    </button>
  )
}

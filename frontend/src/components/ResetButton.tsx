import { useState } from 'react'

type Status = 'idle' | 'loading' | 'success' | 'error'

export function ResetButton() {
  const [status, setStatus] = useState<Status>('idle')

  async function handleReset() {
    setStatus('loading')
    try {
      const res = await fetch('/reset', { method: 'POST' })
      if (!res.ok) throw new Error('Failed')
      setStatus('success')
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
      className={`w-full px-3 py-2 rounded text-sm font-medium transition-colors
        ${status === 'success' ? 'bg-green-600 text-white' :
          status === 'error' ? 'bg-red-600 text-white' :
          'bg-gray-700 hover:bg-gray-600 text-gray-200'}`}
    >
      {label}
    </button>
  )
}

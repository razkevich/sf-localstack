import { useState } from 'react'
import { Cloud } from 'lucide-react'
import { login, register } from '../services/api'

interface Props {
  onLoginSuccess: (token: string, user: { username: string; role: string }) => void
}

export function LoginPage({ onLoginSuccess }: Props) {
  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setLoading(true)
    setError(null)
    try {
      if (mode === 'register') {
        const result = await register(username, email, password)
        onLoginSuccess(result.accessToken, { username: result.username ?? username, role: result.role ?? 'user' })
      } else {
        const result = await login(username, password)
        onLoginSuccess(result.accessToken, { username: result.username ?? username, role: result.role ?? 'user' })
      }
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Authentication failed'
      if (mode === 'register' && msg.includes('Admin JWT required')) {
        setError('An account already exists. Please log in instead.')
        setMode('login')
      } else {
        setError(msg)
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-neutral-05 px-4">
      <div className="w-full max-w-md">
        <div className="mb-8 text-center">
          <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-brand">
            <Cloud className="h-8 w-8 text-neutral-00" />
          </div>
          <h1 className="mt-4 text-heading-lg font-bold text-neutral-90">SF LocalStack</h1>
          <p className="mt-1 text-body-md text-neutral-60">Salesforce API Emulator</p>
        </div>

        <div className="rounded-slds border border-neutral-20 bg-neutral-00 p-6 shadow-slds">
          <h2 className="text-heading-sm font-bold text-neutral-90">
            {mode === 'login' ? 'Log In' : 'Register'}
          </h2>

          {error && (
            <div className="mt-3 rounded-slds border border-error/20 bg-error/5 px-4 py-2.5 text-body-md text-error">
              {error}
            </div>
          )}

          <form onSubmit={(e) => { void handleSubmit(e) }} className="mt-4 space-y-4">
            <div>
              <label className="block text-body-sm font-medium text-neutral-70">Username</label>
              <input
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
                className="mt-1 w-full rounded-slds border border-neutral-20 bg-neutral-00 px-3 py-2 text-body-md text-neutral-90 outline-none transition focus:border-brand focus:ring-1 focus:ring-brand"
              />
            </div>

            {mode === 'register' && (
              <div>
                <label className="block text-body-sm font-medium text-neutral-70">Email</label>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                  className="mt-1 w-full rounded-slds border border-neutral-20 bg-neutral-00 px-3 py-2 text-body-md text-neutral-90 outline-none transition focus:border-brand focus:ring-1 focus:ring-brand"
                />
              </div>
            )}

            <div>
              <label className="block text-body-sm font-medium text-neutral-70">Password</label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                className="mt-1 w-full rounded-slds border border-neutral-20 bg-neutral-00 px-3 py-2 text-body-md text-neutral-90 outline-none transition focus:border-brand focus:ring-1 focus:ring-brand"
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full rounded-slds bg-brand px-4 py-2.5 text-body-md font-semibold text-neutral-00 transition hover:bg-brand-dark disabled:opacity-60"
            >
              {loading ? 'Please wait...' : mode === 'login' ? 'Log In' : 'Register'}
            </button>
          </form>

          <div className="mt-4 text-center text-body-sm text-neutral-60">
            {mode === 'login' ? (
              <>
                No account?{' '}
                <button type="button" onClick={() => { setMode('register'); setError(null) }} className="font-medium text-brand hover:underline">
                  Register
                </button>
              </>
            ) : (
              <>
                Already have an account?{' '}
                <button type="button" onClick={() => { setMode('login'); setError(null) }} className="font-medium text-brand hover:underline">
                  Log In
                </button>
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

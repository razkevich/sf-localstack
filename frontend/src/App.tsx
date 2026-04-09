import { useState, useEffect, useMemo } from 'react'
import { AppShell } from './components/layout/AppShell'
import type { ViewId } from './components/layout/Sidebar'
import { ToastProvider } from './components/ui/Toast'
import { LoginPage } from './views/LoginPage'
import { ObjectListView } from './views/ObjectListView'
import { MetadataView } from './views/MetadataView'
import { BulkJobView } from './views/BulkJobView'
import { RequestLogView } from './views/RequestLogView'
import { SetupView } from './views/SetupView'
import { useDashboardOverview } from './hooks/useDashboardOverview'
import { useSse } from './hooks/useSse'

const TOKEN_KEY = 'sf_localstack_access_token'
const USER_KEY = 'sf_localstack_user'

export default function App() {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem(TOKEN_KEY))
  const [user, setUser] = useState<{ username: string; role: string }>(() => {
    try {
      const stored = localStorage.getItem(USER_KEY)
      return stored ? JSON.parse(stored) as { username: string; role: string } : { username: 'admin', role: 'admin' }
    } catch {
      return { username: 'admin', role: 'admin' }
    }
  })
  const [activeView, setActiveView] = useState<ViewId>('object:Account')
  const [refreshToken, setRefreshToken] = useState(0)

  const { entries, connected, clear } = useSse(refreshToken)
  const { overview } = useDashboardOverview(refreshToken)

  const customObjects = useMemo(() => {
    return overview?.objectCounts.map((o) => o.objectType) ?? []
  }, [overview])

  useEffect(() => {
    // If token exists but no user info, try to verify
    if (token && user.username === 'admin') {
      // Try loading user from stored data
      try {
        const stored = localStorage.getItem(USER_KEY)
        if (stored) {
          setUser(JSON.parse(stored) as { username: string; role: string })
        }
      } catch {
        // Keep default
      }
    }
  }, [token, user.username])

  function handleLoginSuccess(newToken: string, newUser: { username: string; role: string }) {
    localStorage.setItem(TOKEN_KEY, newToken)
    localStorage.setItem(USER_KEY, JSON.stringify(newUser))
    setToken(newToken)
    setUser(newUser)
    setRefreshToken((v) => v + 1)
  }

  function handleLogout() {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
    setToken(null)
    setUser({ username: 'admin', role: 'admin' })
  }

  if (!token) {
    return (
      <ToastProvider>
        <LoginPage onLoginSuccess={handleLoginSuccess} />
      </ToastProvider>
    )
  }

  function renderView() {
    if (activeView.startsWith('object:')) {
      const objectType = activeView.replace('object:', '')
      return <ObjectListView key={objectType} objectType={objectType} />
    }

    switch (activeView) {
      case 'metadata':
        return <MetadataView />
      case 'bulk':
        return <BulkJobView />
      case 'requests':
        return <RequestLogView entries={entries} connected={connected} onClear={clear} />
      case 'setup':
        return <SetupView currentUser={user} apiVersion={overview?.apiVersion ?? 'v60.0'} />
      default:
        return <ObjectListView objectType="Account" />
    }
  }

  return (
    <ToastProvider>
      <AppShell
        username={user.username}
        role={user.role}
        activeView={activeView}
        onNavigate={setActiveView}
        onLogout={handleLogout}
        customObjects={customObjects}
      >
        {renderView()}
      </AppShell>
    </ToastProvider>
  )
}

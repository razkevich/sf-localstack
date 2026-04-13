import { GlobalHeader } from './GlobalHeader'
import { LayoutSidebar } from './Sidebar'
import type { ViewId } from './Sidebar'

interface Props {
  username: string
  role: string
  activeView: string
  onNavigate: (view: string) => void
  onLogout: () => void
  children: React.ReactNode
}

export function AppShell({ username, role, activeView, onNavigate, onLogout, children }: Props) {
  return (
    <div className="flex h-screen flex-col bg-neutral-05">
      <GlobalHeader username={username} role={role} onLogout={onLogout} />
      <div className="flex flex-1 overflow-hidden">
        <LayoutSidebar activeView={activeView} onNavigate={onNavigate as (view: ViewId) => void} />
        <main className="flex-1 overflow-auto">{children}</main>
      </div>
    </div>
  )
}

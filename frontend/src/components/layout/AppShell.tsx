import { GlobalHeader } from './GlobalHeader'
import { LayoutSidebar } from './Sidebar'
import type { ViewId } from './Sidebar'

interface Props {
  username: string
  role: string
  activeView: ViewId
  onNavigate: (view: ViewId) => void
  onLogout: () => void
  customObjects?: string[]
  children: React.ReactNode
}

export function AppShell({ username, role, activeView, onNavigate, onLogout, customObjects, children }: Props) {
  return (
    <div className="flex h-screen flex-col bg-neutral-05">
      <GlobalHeader username={username} role={role} onLogout={onLogout} />
      <div className="flex flex-1 overflow-hidden">
        <LayoutSidebar activeView={activeView} onNavigate={onNavigate} customObjects={customObjects} />
        <main className="flex-1 overflow-auto">{children}</main>
      </div>
    </div>
  )
}

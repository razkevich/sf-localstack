import {
  Database,
  Users,
  Contact,
  Target,
  Briefcase,
  FileCode,
  UploadCloud,
  Activity,
  Settings,
  Menu,
  X,
} from 'lucide-react'
import { useState } from 'react'

export type ViewId =
  | 'object:Account'
  | 'object:Contact'
  | 'object:Lead'
  | 'object:Opportunity'
  | 'metadata'
  | 'bulk'
  | 'requests'
  | 'setup'

interface NavItem {
  id: ViewId
  label: string
  icon: React.ReactNode
}

interface NavSection {
  title: string
  items: NavItem[]
}

interface Props {
  activeView: ViewId
  onNavigate: (view: ViewId) => void
  customObjects?: string[]
}

export function LayoutSidebar({ activeView, onNavigate, customObjects = [] }: Props) {
  const [collapsed, setCollapsed] = useState(false)

  const sections: NavSection[] = [
    {
      title: 'Objects',
      items: [
        { id: 'object:Account', label: 'Accounts', icon: <Briefcase className="h-4 w-4" /> },
        { id: 'object:Contact', label: 'Contacts', icon: <Contact className="h-4 w-4" /> },
        { id: 'object:Lead', label: 'Leads', icon: <Target className="h-4 w-4" /> },
        { id: 'object:Opportunity', label: 'Opportunities', icon: <Database className="h-4 w-4" /> },
        ...customObjects
          .filter((name) => !['Account', 'Contact', 'Lead', 'Opportunity'].includes(name))
          .map((name): NavItem => ({
            id: `object:${name}` as ViewId,
            label: name,
            icon: <Users className="h-4 w-4" />,
          })),
      ],
    },
    {
      title: 'Tools',
      items: [
        { id: 'metadata', label: 'Metadata', icon: <FileCode className="h-4 w-4" /> },
        { id: 'bulk', label: 'Bulk Jobs', icon: <UploadCloud className="h-4 w-4" /> },
        { id: 'requests', label: 'API Log', icon: <Activity className="h-4 w-4" /> },
      ],
    },
    {
      title: 'Admin',
      items: [
        { id: 'setup', label: 'Setup', icon: <Settings className="h-4 w-4" /> },
      ],
    },
  ]

  return (
    <>
      <button
        type="button"
        onClick={() => setCollapsed((v) => !v)}
        className="fixed left-3 top-14 z-40 rounded-slds bg-neutral-00 p-1.5 shadow-slds lg:hidden"
      >
        {collapsed ? <X className="h-5 w-5 text-neutral-70" /> : <Menu className="h-5 w-5 text-neutral-70" />}
      </button>

      <aside
        className={`fixed inset-y-12 left-0 z-30 w-60 transform border-r border-neutral-20 bg-neutral-00 transition-transform lg:static lg:translate-x-0 ${
          collapsed ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'
        }`}
      >
        <nav className="flex h-full flex-col overflow-y-auto py-3">
          {sections.map((section, si) => (
            <div key={section.title}>
              {si > 0 && <div className="mx-4 my-2 border-t border-neutral-10" />}
              <div className="px-4 py-1.5 text-body-sm font-semibold uppercase tracking-wider text-neutral-50">
                {section.title}
              </div>
              {section.items.map((item) => {
                const isActive = activeView === item.id
                return (
                  <button
                    key={item.id}
                    type="button"
                    onClick={() => { onNavigate(item.id); if (collapsed) setCollapsed(false) }}
                    className={`flex w-full items-center gap-3 px-4 py-2 text-body-md transition-colors ${
                      isActive
                        ? 'border-l-[3px] border-brand bg-brand-lighter/30 font-semibold text-brand'
                        : 'border-l-[3px] border-transparent text-neutral-70 hover:bg-neutral-05'
                    }`}
                  >
                    {item.icon}
                    <span>{item.label}</span>
                  </button>
                )
              })}
            </div>
          ))}
        </nav>
      </aside>
    </>
  )
}

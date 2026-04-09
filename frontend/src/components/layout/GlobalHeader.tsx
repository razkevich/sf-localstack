import { useState, useRef, useEffect } from 'react'
import { Cloud, ChevronDown, LogOut, User } from 'lucide-react'
import { Badge } from '../ui/Badge'

interface Props {
  username: string
  role: string
  onLogout: () => void
}

export function GlobalHeader({ username, role, onLogout }: Props) {
  const [menuOpen, setMenuOpen] = useState(false)
  const menuRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    function handleClick(e: MouseEvent) {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        setMenuOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClick)
    return () => document.removeEventListener('mousedown', handleClick)
  }, [])

  return (
    <header className="flex h-12 items-center justify-between border-b border-brand-dark bg-brand-dark px-4">
      <div className="flex items-center gap-2">
        <Cloud className="h-6 w-6 text-neutral-00" />
        <span className="text-body-md font-bold text-neutral-00">SF LocalStack</span>
      </div>

      <div ref={menuRef} className="relative">
        <button
          type="button"
          onClick={() => setMenuOpen((v) => !v)}
          className="flex items-center gap-2 rounded-slds px-3 py-1.5 text-body-sm text-neutral-00/90 hover:bg-brand/80"
        >
          <User className="h-4 w-4" />
          <span>{username}</span>
          <ChevronDown className="h-3.5 w-3.5" />
        </button>

        {menuOpen && (
          <div className="absolute right-0 top-full mt-1 w-56 rounded-slds border border-neutral-20 bg-neutral-00 shadow-slds-lg">
            <div className="border-b border-neutral-10 px-4 py-3">
              <div className="text-body-md font-semibold text-neutral-80">{username}</div>
              <div className="mt-1">
                <Badge variant="info">{role}</Badge>
              </div>
            </div>
            <button
              type="button"
              onClick={() => { setMenuOpen(false); onLogout() }}
              className="flex w-full items-center gap-2 px-4 py-2.5 text-body-md text-neutral-70 hover:bg-neutral-05"
            >
              <LogOut className="h-4 w-4" />
              Log Out
            </button>
          </div>
        )}
      </div>
    </header>
  )
}

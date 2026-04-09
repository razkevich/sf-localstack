import { useEffect, useRef } from 'react'
import { X } from 'lucide-react'

interface Props {
  isOpen: boolean
  onClose: () => void
  title: string
  children: React.ReactNode
  actions?: React.ReactNode
}

export function Modal({ isOpen, onClose, title, children, actions }: Props) {
  const overlayRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!isOpen) return
    function handleKeyDown(e: KeyboardEvent) {
      if (e.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [isOpen, onClose])

  if (!isOpen) return null

  return (
    <div
      ref={overlayRef}
      className="fixed inset-0 z-50 flex items-center justify-center bg-neutral-90/60"
      onClick={(e) => { if (e.target === overlayRef.current) onClose() }}
    >
      <div className="mx-4 w-full max-w-2xl rounded-slds bg-neutral-00 shadow-slds-lg">
        <div className="flex items-center justify-between border-b border-neutral-20 px-6 py-4">
          <h2 className="text-heading-sm font-bold text-neutral-90">{title}</h2>
          <button type="button" onClick={onClose} className="rounded-slds p-1 text-neutral-60 hover:bg-neutral-05 hover:text-neutral-80">
            <X className="h-5 w-5" />
          </button>
        </div>
        <div className="max-h-[70vh] overflow-y-auto px-6 py-4">
          {children}
        </div>
        {actions && (
          <div className="flex items-center justify-end gap-2 border-t border-neutral-20 px-6 py-3">
            {actions}
          </div>
        )}
      </div>
    </div>
  )
}

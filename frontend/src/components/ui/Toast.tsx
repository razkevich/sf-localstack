import { createContext, useContext, useState, useCallback } from 'react'
import { X, CheckCircle, AlertTriangle, XCircle } from 'lucide-react'

interface ToastItem {
  id: number
  type: 'success' | 'error' | 'warning'
  message: string
}

interface ToastContextValue {
  showSuccess: (message: string) => void
  showError: (message: string) => void
  showWarning: (message: string) => void
}

const ToastContext = createContext<ToastContextValue | null>(null)

let nextId = 0

export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([])

  const addToast = useCallback((type: ToastItem['type'], message: string) => {
    const id = nextId++
    setToasts((prev) => [...prev, { id, type, message }])
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id))
    }, 5000)
  }, [])

  const showSuccess = useCallback((message: string) => addToast('success', message), [addToast])
  const showError = useCallback((message: string) => addToast('error', message), [addToast])
  const showWarning = useCallback((message: string) => addToast('warning', message), [addToast])

  const removeToast = useCallback((id: number) => {
    setToasts((prev) => prev.filter((t) => t.id !== id))
  }, [])

  return (
    <ToastContext.Provider value={{ showSuccess, showError, showWarning }}>
      {children}
      <div className="fixed right-4 top-16 z-50 flex flex-col gap-2">
        {toasts.map((toast) => (
          <ToastMessage key={toast.id} toast={toast} onClose={() => removeToast(toast.id)} />
        ))}
      </div>
    </ToastContext.Provider>
  )
}

export function useToast(): ToastContextValue {
  const ctx = useContext(ToastContext)
  if (!ctx) throw new Error('useToast must be used within ToastProvider')
  return ctx
}

const toastStyles: Record<ToastItem['type'], { bg: string; icon: typeof CheckCircle }> = {
  success: { bg: 'bg-success text-neutral-00', icon: CheckCircle },
  error: { bg: 'bg-error text-neutral-00', icon: XCircle },
  warning: { bg: 'bg-warning text-neutral-00', icon: AlertTriangle },
}

function ToastMessage({ toast, onClose }: { toast: ToastItem; onClose: () => void }) {
  const style = toastStyles[toast.type]
  const Icon = style.icon

  return (
    <div className={`flex items-center gap-3 rounded-slds px-4 py-3 shadow-slds-lg animate-in slide-in-from-right ${style.bg}`}>
      <Icon className="h-5 w-5 flex-shrink-0" />
      <span className="text-body-md font-medium">{toast.message}</span>
      <button type="button" onClick={onClose} className="ml-2 flex-shrink-0 opacity-80 hover:opacity-100">
        <X className="h-4 w-4" />
      </button>
    </div>
  )
}

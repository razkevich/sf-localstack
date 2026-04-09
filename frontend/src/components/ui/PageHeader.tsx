import type { ReactNode } from 'react'

interface Props {
  title: string
  subtitle?: string
  icon?: ReactNode
  actions?: ReactNode
}

export function PageHeader({ title, subtitle, icon, actions }: Props) {
  return (
    <div className="border-b border-neutral-20 bg-neutral-00 px-6 py-4">
      <div className="flex items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          {icon && (
            <div className="flex h-10 w-10 items-center justify-center rounded-slds bg-brand text-neutral-00">
              {icon}
            </div>
          )}
          <div>
            <h1 className="text-heading-md font-bold text-neutral-90">{title}</h1>
            {subtitle && <p className="mt-0.5 text-body-sm text-neutral-60">{subtitle}</p>}
          </div>
        </div>
        {actions && <div className="flex items-center gap-2">{actions}</div>}
      </div>
    </div>
  )
}

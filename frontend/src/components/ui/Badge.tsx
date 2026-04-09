interface Props {
  variant: 'success' | 'warning' | 'error' | 'info' | 'neutral'
  children: React.ReactNode
}

const variantStyles: Record<Props['variant'], string> = {
  success: 'bg-success/10 text-success border-success/20',
  warning: 'bg-warning/10 text-warning border-warning/20',
  error: 'bg-error/10 text-error border-error/20',
  info: 'bg-brand/10 text-brand border-brand/20',
  neutral: 'bg-neutral-10 text-neutral-70 border-neutral-20',
}

export function Badge({ variant, children }: Props) {
  return (
    <span className={`inline-flex items-center rounded-full border px-2 py-0.5 text-body-sm font-medium ${variantStyles[variant]}`}>
      {children}
    </span>
  )
}

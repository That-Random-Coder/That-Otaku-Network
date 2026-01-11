const palettes = {
  success: {
    bg: 'bg-emerald-900/60',
    border: 'border-emerald-400/50',
    text: 'text-emerald-100',
    glow: 'shadow-[0_0_30px_rgba(16,185,129,0.25)]',
    icon: '✓',
  },
  error: {
    bg: 'bg-rose-900/50',
    border: 'border-rose-400/60',
    text: 'text-rose-50',
    glow: 'shadow-[0_0_30px_rgba(244,63,94,0.25)]',
    icon: '⚠',
  },
  info: {
    bg: 'bg-indigo-900/50',
    border: 'border-indigo-400/50',
    text: 'text-indigo-50',
    glow: 'shadow-[0_0_30px_rgba(99,102,241,0.25)]',
    icon: 'ℹ',
  },
}

function AlertBanner({ status, message }) {
  if (!message) return null
  const theme = palettes[status] || palettes.info
  return (
    <div className={`flex items-center gap-3 rounded-2xl border px-4 py-3 ${theme.bg} ${theme.border} ${theme.text} ${theme.glow}`}>
      <span className="text-lg font-bold" aria-hidden>{theme.icon}</span>
      <p className="text-sm leading-snug">{message}</p>
    </div>
  )
}

export default AlertBanner

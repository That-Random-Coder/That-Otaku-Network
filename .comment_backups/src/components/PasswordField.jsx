import FormField from './FormField.jsx'

function PasswordField({ id, label, value, onChange, autoComplete, visible, onToggle, ariaLabel, required = true }) {
  return (
    <FormField
      id={id}
      label={label}
      type={visible ? 'text' : 'password'}
      value={value}
      autoComplete={autoComplete}
      onChange={onChange}
      required={required}
      rightSlot={(
        <button
          type="button"
          onClick={onToggle}
          className="flex h-8 w-8 items-center justify-center rounded-full bg-white/10 text-xs font-semibold text-white/80 hover:bg-white/20 focus:outline-none focus:ring-2 focus:ring-white/30"
          aria-label={ariaLabel}
        >
          {visible ? '🛡' : '👁'}
        </button>
      )}
    />
  )
}

export default PasswordField

const FormField = ({ id, label, type = 'text', value, onChange, autoComplete, rightSlot, required = false }) => (
  <label className="block text-sm font-medium text-slate-200" htmlFor={id}>
    <div className="relative mt-4">
      <input
        id={id}
        name={id}
        type={type}
        value={value}
        onChange={onChange}
        autoComplete={autoComplete}
        placeholder=" "
        required={required}
        aria-required={required}
        className={`peer block w-full rounded-2xl border border-white/10 bg-white/5 px-4 pb-3 pt-6 text-base text-slate-50 shadow-inner shadow-black/10 outline-none transition focus:border-violet-300/60 focus:ring-2 focus:ring-violet-300/60 ${rightSlot ? 'pr-12' : ''}`}
      />
      <span className="pointer-events-none absolute left-4 top-2 text-xs uppercase tracking-wide text-slate-300/80 transition-all duration-200 peer-placeholder-shown:top-3.5 peer-placeholder-shown:text-sm peer-placeholder-shown:text-slate-400 peer-focus:top-2 peer-focus:text-xs peer-focus:text-white">
        {label} {required && <span className="text-rose-400">*</span>}
      </span>
      {rightSlot && (
        <div className="absolute inset-y-0 right-3 flex items-center" aria-hidden>
          {rightSlot}
        </div>
      )}
    </div>
  </label>
)

export default FormField

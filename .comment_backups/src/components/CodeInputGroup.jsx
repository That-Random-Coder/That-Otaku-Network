function CodeInputGroup({ codeDigits, onChange, onKeyDown, codeRefs }) {
  return (
    <div className="flex justify-center gap-3">
      {codeDigits.map((digit, idx) => (
        <input
          key={idx}
          type="text"
          inputMode="numeric"
          maxLength={1}
          value={digit}
          onChange={(e) => onChange(idx, e.target.value)}
          onKeyDown={(e) => onKeyDown(idx, e)}
          ref={(el) => { codeRefs.current[idx] = el }}
          className="h-12 w-10 rounded-xl border border-white/15 bg-white/5 text-center text-xl font-semibold text-white shadow-inner shadow-black/20 focus:border-white/40 focus:outline-none focus:ring-2 focus:ring-white/30"
          style={{ caretColor: 'transparent' }}
        />
      ))}
    </div>
  )
}

export default CodeInputGroup

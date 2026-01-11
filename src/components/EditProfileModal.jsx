import { useEffect, useState, useMemo } from 'react'
import { AnimatePresence, motion } from 'framer-motion'
import { X, MapPin } from 'lucide-react'
import Select from 'react-select'
import { Country, State, City } from 'country-state-city'


const selectStyles = (accent) => ({
  control: (base, state) => ({
    ...base,
    backgroundColor: 'rgba(0,0,0,0.3)',
    borderColor: state.isFocused ? accent.mid : 'rgba(255,255,255,0.15)',
    borderRadius: '0.75rem',
    padding: '0.25rem',
    boxShadow: state.isFocused ? `0 0 0 2px ${accent.glow}` : 'none',
    '&:hover': { borderColor: accent.mid },
  }),
  menu: (base) => ({
    ...base,
    backgroundColor: 'rgba(15,15,25,0.98)',
    borderRadius: '0.75rem',
    border: '1px solid rgba(255,255,255,0.1)',
    backdropFilter: 'blur(20px)',
    zIndex: 9999,
  }),
  option: (base, state) => ({
    ...base,
    backgroundColor: state.isSelected ? accent.mid : state.isFocused ? 'rgba(255,255,255,0.1)' : 'transparent',
    color: '#f1f5f9',
    cursor: 'pointer',
    '&:active': { backgroundColor: accent.strong },
  }),
  singleValue: (base) => ({ ...base, color: '#f1f5f9' }),
  input: (base) => ({ ...base, color: '#f1f5f9' }),
  placeholder: (base) => ({ ...base, color: 'rgba(255,255,255,0.5)' }),
  indicatorSeparator: () => ({ display: 'none' }),
  dropdownIndicator: (base) => ({ ...base, color: 'rgba(255,255,255,0.5)' }),
})

const LocationPickerModal = ({ isOpen, onClose, onSelect, accent, motionSafe }) => {
  const [selectedCountry, setSelectedCountry] = useState(null)
  const [selectedState, setSelectedState] = useState(null)
  const [selectedCity, setSelectedCity] = useState(null)

  const countries = useMemo(() => Country.getAllCountries().map(c => ({ value: c.isoCode, label: c.name, ...c })), [])
  const states = useMemo(() => selectedCountry ? State.getStatesOfCountry(selectedCountry.value).map(s => ({ value: s.isoCode, label: s.name, ...s })) : [], [selectedCountry])
  const cities = useMemo(() => selectedCountry && selectedState ? City.getCitiesOfState(selectedCountry.value, selectedState.value).map(c => ({ value: c.name, label: c.name, ...c })) : [], [selectedCountry, selectedState])

  const handleConfirm = () => {
    const parts = []
    if (selectedCity) parts.push(selectedCity.label)
    if (selectedState) parts.push(selectedState.label)
    if (selectedCountry) parts.push(selectedCountry.label)
    onSelect(parts.join(', '))
    onClose()
  }

  if (!isOpen) return null

  return (
    <AnimatePresence>
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        className="fixed inset-0 z-[100] flex items-center justify-center bg-black/70 backdrop-blur-sm p-4"
        onClick={onClose}
      >
        <motion.div
          initial={motionSafe ? { opacity: 0, scale: 0.95, y: 20 } : false}
          animate={motionSafe ? { opacity: 1, scale: 1, y: 0 } : { opacity: 1 }}
          exit={motionSafe ? { opacity: 0, scale: 0.95, y: 20 } : { opacity: 0 }}
          transition={{ type: 'spring', stiffness: 300, damping: 25 }}
          className="relative w-full max-w-lg overflow-hidden rounded-3xl border backdrop-blur-3xl"
          style={{
            borderColor: 'rgba(255,255,255,0.08)',
            backgroundColor: accent.surface,
            boxShadow: `0 30px 90px rgba(0,0,0,0.5), 0 0 40px ${accent.glow}`,
          }}
          onClick={(e) => e.stopPropagation()}
        >
          <div className="pointer-events-none absolute -left-16 -top-16 h-48 w-48 rounded-full bg-purple-500/20 blur-3xl" />
          <div className="pointer-events-none absolute -bottom-16 -right-16 h-56 w-56 rounded-full bg-indigo-500/25 blur-3xl" />

          <div className="relative p-6 sm:p-8">
            <div className="flex items-center justify-between mb-6">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-xl" style={{ background: `linear-gradient(135deg, ${accent.mid}, ${accent.strong})` }}>
                  <MapPin className="h-5 w-5 text-white" />
                </div>
                <div>
                  <h2 className="text-xl font-semibold text-white">Select Location</h2>
                  <p className="text-xs text-slate-300/80">Choose your country, state, and city</p>
                </div>
              </div>
              <button onClick={onClose} className="rounded-full p-2 text-slate-300 hover:bg-white/10 hover:text-white transition">
                <X className="h-5 w-5" />
              </button>
            </div>

            <div className="space-y-4">
              <div>
                <label className="text-sm font-medium text-slate-200 mb-2 block">Country</label>
                <Select
                  options={countries}
                  value={selectedCountry}
                  onChange={(val) => { setSelectedCountry(val); setSelectedState(null); setSelectedCity(null) }}
                  placeholder="Select a country..."
                  styles={selectStyles(accent)}
                  isSearchable
                />
              </div>

              <div>
                <label className="text-sm font-medium text-slate-200 mb-2 block">State / Region</label>
                <Select
                  options={states}
                  value={selectedState}
                  onChange={(val) => { setSelectedState(val); setSelectedCity(null) }}
                  placeholder={selectedCountry ? 'Select a state...' : 'Select country first'}
                  styles={selectStyles(accent)}
                  isSearchable
                  isDisabled={!selectedCountry}
                />
              </div>

              <div>
                <label className="text-sm font-medium text-slate-200 mb-2 block">City</label>
                <Select
                  options={cities}
                  value={selectedCity}
                  onChange={setSelectedCity}
                  placeholder={selectedState ? 'Select a city...' : 'Select state first'}
                  styles={selectStyles(accent)}
                  isSearchable
                  isDisabled={!selectedState}
                />
              </div>
            </div>

            <div className="mt-6 flex gap-3">
              <button
                onClick={onClose}
                className="flex-1 rounded-xl border border-white/10 bg-white/5 px-4 py-3 text-sm font-semibold text-white transition hover:bg-white/10"
              >
                Cancel
              </button>
              <motion.button
                onClick={handleConfirm}
                disabled={!selectedCountry}
                className="flex-1 rounded-xl px-4 py-3 text-sm font-semibold text-white transition disabled:opacity-50"
                style={{ backgroundImage: `linear-gradient(90deg, ${accent.mid}, ${accent.strong})`, boxShadow: `0 8px 24px ${accent.glow}` }}
                whileHover={motionSafe ? { scale: 1.02 } : undefined}
                whileTap={motionSafe ? { scale: 0.98 } : undefined}
              >
                Confirm Location
              </motion.button>
            </div>
          </div>
        </motion.div>
      </motion.div>
    </AnimatePresence>
  )
}

const EditProfileModal = ({ isOpen, onClose, onSave, initial = {}, accent, motionSafe }) => {
  const [displayName, setDisplayName] = useState(initial.displayName || '')
  const [bio, setBio] = useState(initial.bio || '')
  const [location, setLocation] = useState(initial.location || '')
  const [dateOfBirth, setDateOfBirth] = useState(initial.dateOfBirth || '')
  const todayIso = new Date().toISOString().split('T')[0]

  const [dobY, setDobY] = useState(initial.dateOfBirth ? String(initial.dateOfBirth).split('-')[0] : '')
  const [dobM, setDobM] = useState(initial.dateOfBirth ? String(initial.dateOfBirth).split('-')[1] : '')
  const [dobD, setDobD] = useState(initial.dateOfBirth ? String(initial.dateOfBirth).split('-')[2] : '')
  const [dobFocus, setDobFocus] = useState({ day: false, month: false, year: false })
  const [dateError, setDateError] = useState('')
  const [showLocationPicker, setShowLocationPicker] = useState(false)

  useEffect(() => {
    if (!isOpen) return
    setDisplayName(initial.displayName || '')
    setBio(initial.bio || '')
    setLocation(initial.location || '')
    setDateOfBirth(initial.dateOfBirth || '')
    setDobY(initial.dateOfBirth ? String(initial.dateOfBirth).split('-')[0] : '')
    setDobM(initial.dateOfBirth ? String(initial.dateOfBirth).split('-')[1] : '')
    setDobD(initial.dateOfBirth ? String(initial.dateOfBirth).split('-')[2] : '')
  }, [isOpen, initial])


  const setDobParts = (y, m, d) => {
    setDobY(y || '')
    setDobM(m || '')
    setDobD(d || '')
    if (!y || !m || !d) {
      setDateOfBirth('')
      setDateError('')
      return
    }
    const yy = parseInt(y, 10)
    const mm = parseInt(m, 10)
    let dd = parseInt(d, 10)
    const daysInMonth = new Date(yy, mm, 0).getDate()
    if (dd > daysInMonth) dd = daysInMonth
    const mmStr = String(mm).padStart(2, '0')
    const ddStr = String(dd).padStart(2, '0')
    const iso = `${String(yy)}-${mmStr}-${ddStr}`
    if (iso > todayIso) {
      setDateError('Date of birth cannot be in the future')
      setDateOfBirth(todayIso)
      const [ty, tm, td] = todayIso.split('-')
      setDobY(ty); setDobM(tm); setDobD(td)
    } else {
      setDateError('')
      setDateOfBirth(iso)
    }
  }

  const handleSave = () => {
    onSave({ displayName: displayName?.trim() || '', bio: bio?.trim() || '', location, dateOfBirth })
  }

  if (!isOpen) return null

  return (
    <AnimatePresence>
      <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="fixed inset-0 z-[120] flex items-center justify-center bg-black/80 backdrop-blur-md p-4" onClick={onClose}>
        <motion.div initial={motionSafe ? { opacity: 0, scale: 0.95, y: 18 } : false} animate={motionSafe ? { opacity: 1, scale: 1, y: 0 } : { opacity: 1 }} exit={motionSafe ? { opacity: 0, scale: 0.95, y: 18 } : { opacity: 0 }} transition={{ type: 'spring', stiffness: 280, damping: 24 }} className="relative w-full max-w-3xl h-auto overflow-hidden rounded-3xl border backdrop-blur-3xl flex flex-col" style={{ borderColor: 'rgba(255,255,255,0.08)', backgroundColor: accent.surface, boxShadow: `0 40px 100px rgba(0,0,0,0.6), 0 0 50px ${accent.glow}` }} onClick={(e) => e.stopPropagation()}>
          <div className="pointer-events-none absolute -left-24 -top-24 h-72 w-72 rounded-full bg-purple-500/20 blur-3xl" />
          <div className="pointer-events-none absolute -bottom-24 -right-24 h-80 w-80 rounded-full bg-indigo-500/25 blur-3xl" />

          <div className="relative p-6 border-b border-white/10">
            <div className="flex items-center justify-between mb-4">
              <div>
                <h2 className="text-2xl font-semibold text-white">Edit Profile</h2>
                <p className="text-sm text-slate-300/80 mt-1">Update your public profile details</p>
              </div>
              <button onClick={onClose} className="rounded-full p-2 text-slate-300 hover:bg-white/10 hover:text-white transition"><X className="h-6 w-6" /></button>
            </div>

            <div className="space-y-4">
              <div>
                <label className="text-sm font-medium text-slate-200 mb-2 block">Display Name</label>
                <input value={displayName} onChange={(e) => setDisplayName(e.target.value)} className="w-full rounded-xl border border-white/15 bg-black/30 px-4 py-3 text-base text-slate-200 placeholder:text-slate-400 focus:border-white/30 focus:outline-none focus:ring-2 focus:ring-white/20" placeholder="Display name" />
              </div>

              <div>
                <label className="text-sm font-medium text-slate-200 mb-2 block">Bio</label>
                <textarea value={bio} onChange={(e) => setBio(e.target.value)} className="w-full rounded-xl border border-white/15 bg-black/30 px-4 py-3 text-base text-slate-200 placeholder:text-slate-400 focus:border-white/30 focus:outline-none focus:ring-2 focus:ring-white/20" rows={3} placeholder="Short bio..." />
              </div>

              <div>
                <label className="text-sm font-medium text-slate-200 mb-2 block">Location</label>
                <div className="flex items-center gap-3">
                  <input value={location} readOnly className="flex-1 rounded-xl border border-white/15 bg-black/30 px-4 py-3 text-base text-slate-200" placeholder="Select location" />
                  <button type="button" onClick={() => setShowLocationPicker(true)} className="rounded-xl border border-white/10 bg-white/5 px-3 py-2 text-sm text-white">Select</button>
                </div>
              </div>

              <div>
                <label className="text-sm font-medium text-slate-200 mb-2 block">Date of Birth</label>
                <div className="mt-2 grid grid-cols-3 gap-3" role="group" aria-label="Date of birth selector">

                  <div>
                    <label className="sr-only">Day</label>
                    <select
                      aria-label="Day"
                      value={dobD}
                      onFocus={() => setDobFocus((s) => ({ ...s, day: true }))}
                      onBlur={() => setDobFocus((s) => ({ ...s, day: false }))}
                      onChange={(e) => { const day = e.target.value; setDobParts(dobY, dobM, day); }}
                      className="w-full rounded-2xl px-3 py-2 text-sm outline-none"
                      style={{ backgroundColor: accent.surface, color: '#f8fafc', borderWidth: '1px', borderStyle: 'solid', borderColor: dobFocus.day ? accent.mid : 'rgba(255,255,255,0.08)', boxShadow: dobFocus.day ? `0 0 0 6px ${accent.glow}` : undefined }}
                    >
                      <option value="">Day</option>
                      {(() => {
                        const [y,m,] = String(dateOfBirth || '').split('-')
                        const yy = parseInt(y, 10) || new Date().getFullYear()
                        const mm = parseInt(m, 10) || 1
                        const days = new Date(yy, mm, 0).getDate()
                        return Array.from({ length: days }, (_, i) => i + 1).map((d) => (
                          <option key={d} value={String(d).padStart(2, '0')}>{d}</option>
                        ))
                      })()}
                    </select>
                  </div>


                  <div>
                    <label className="sr-only">Month</label>
                    <select
                      aria-label="Month"
                      value={dobM}
                      onFocus={() => setDobFocus((s) => ({ ...s, month: true }))}
                      onBlur={() => setDobFocus((s) => ({ ...s, month: false }))}
                      onChange={(e) => { const month = e.target.value; setDobParts(dobY, month, dobD); }}
                      className="w-full rounded-2xl px-3 py-2 text-sm outline-none"
                      style={{ backgroundColor: accent.surface, color: '#f8fafc', borderWidth: '1px', borderStyle: 'solid', borderColor: dobFocus.month ? accent.mid : 'rgba(255,255,255,0.08)', boxShadow: dobFocus.month ? `0 0 0 6px ${accent.glow}` : undefined }}
                    >
                      <option value="">Month</option>
                      {['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'].map((m, idx) => (
                        <option key={m} value={String(idx + 1).padStart(2, '0')}>{m}</option>
                      ))}
                    </select>
                  </div>


                  <div>
                    <label className="sr-only">Year</label>
                    <select
                      aria-label="Year"
                      value={dobY}
                      onFocus={() => setDobFocus((s) => ({ ...s, year: true }))}
                      onBlur={() => setDobFocus((s) => ({ ...s, year: false }))}
                      onChange={(e) => { const year = e.target.value; setDobParts(year, dobM, dobD); }}
                      className="w-full rounded-2xl px-3 py-2 text-sm outline-none"
                      style={{ backgroundColor: accent.surface, color: '#f8fafc', borderWidth: '1px', borderStyle: 'solid', borderColor: dobFocus.year ? accent.mid : 'rgba(255,255,255,0.08)', boxShadow: dobFocus.year ? `0 0 0 6px ${accent.glow}` : undefined }}
                    >
                      <option value="">Year</option>
                      {Array.from({ length: 100 }, (_, i) => new Date().getFullYear() - i).map((yr) => (
                        <option key={yr} value={String(yr)}>{yr}</option>
                      ))}
                    </select>
                  </div>
                </div>
                {dateError && <p className="mt-2 text-sm text-rose-200">{dateError}</p>}
              </div>
            </div>
          </div>

          <div className="relative flex-1 p-6">
            <LocationPickerModal isOpen={showLocationPicker} onClose={() => setShowLocationPicker(false)} onSelect={(v) => { setLocation(v); setShowLocationPicker(false) }} accent={accent} motionSafe={motionSafe} />
          </div>

          <div className="relative p-6 border-t border-white/10 flex items-center justify-between gap-4">
            <button onClick={onClose} className="rounded-xl border border-white/10 bg-white/5 px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-white/10">Cancel</button>
            <button onClick={handleSave} className="rounded-xl px-5 py-2.5 text-sm font-semibold text-white transition" style={{ backgroundImage: `linear-gradient(90deg, ${accent.mid}, ${accent.strong})`, boxShadow: `0 8px 24px ${accent.glow}` }}>Save</button>
          </div>
        </motion.div>
      </motion.div>
    </AnimatePresence>
  )
}

export default EditProfileModal

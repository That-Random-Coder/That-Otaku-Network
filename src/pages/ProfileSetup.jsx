import { Suspense, lazy, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AnimatePresence, motion } from 'framer-motion'
import Select from 'react-select'
import { Country, State, City } from 'country-state-city'
import { MapPin, X, Search, Check } from 'lucide-react'
import accentOptions from '../theme/accentOptions.js'
import { getSavedAccentKey, persistAccentKey } from '../theme/accentStorage.js'
import WallSkeleton from '../components/WallSkeleton.jsx'
import axios from 'axios'
import FormField from '../components/FormField.jsx'
import AlertBanner from '../components/AlertBanner.jsx'

const AnimeWall = lazy(() => import('../components/AnimeWall.jsx'))

const usePrefersReducedMotion = () => {
  const [prefersReducedMotion, setPrefersReducedMotion] = useState(false)
  useEffect(() => {
    const mediaQuery = window.matchMedia('(prefers-reduced-motion: reduce)')
    const updatePreference = () => setPrefersReducedMotion(mediaQuery.matches)
    updatePreference()
    mediaQuery.addEventListener('change', updatePreference)
    return () => mediaQuery.removeEventListener('change', updatePreference)
  }, [])
  return prefersReducedMotion
}

const useDeviceProfile = () => {
  const [isLowPower, setIsLowPower] = useState(false)
  useEffect(() => {
    const cores = navigator.hardwareConcurrency || 4
    const memory = navigator.deviceMemory || 4
    setIsLowPower(cores <= 4 || memory <= 4)
  }, [])
  return isLowPower
}

const getCookie = (name) => {
  if (typeof document === 'undefined') return ''
  const match = document.cookie.match(new RegExp(`(?:^|; )${name}=([^;]*)`))
  return match ? decodeURIComponent(match[1]) : ''
}

const getBearerToken = () => {
  const raw =
    getCookie('AccessToken') ||
    getCookie('accessToken') ||
    (typeof localStorage !== 'undefined' ? localStorage.getItem('AccessToken') || localStorage.getItem('accessToken') : '') ||
    ''
  const trimmed = raw.trim()
  if (!trimmed) return ''
  return trimmed.toLowerCase().startsWith('bearer ') ? trimmed : `Bearer ${trimmed}`
}

// Custom styles for react-select to match the dark theme
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

// Location Picker Modal
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

// Character Picker Modal (80% of screen)
const CharacterPickerModal = ({ isOpen, onClose, onSelect, accent, motionSafe }) => {
  const [query, setQuery] = useState('')
  const [results, setResults] = useState([])
  const [featured, setFeatured] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [selected, setSelected] = useState(null)

  // Fetch featured characters on mount
  useEffect(() => {
    if (!isOpen) return
    const fetchFeatured = async () => {
      try {
        const res = await fetch('https://api.jikan.moe/v4/top/characters?limit=12')
        if (!res.ok) {
          const body = await res.json().catch(() => ({}))
          const parsed = (await import('../lib/parseApiError.js')).parseApiErrorBody(body)
          throw new Error(parsed.message || `Failed to fetch featured characters: ${res.status}`)
        }
        const data = await res.json()
        setFeatured(data.data?.map(c => ({ id: c.mal_id, name: c.name, imageUrl: c.images?.jpg?.image_url })) || [])
      } catch (err) {
        setFeatured([])
        // set a small error message for the modal context
        setError(err?.message || 'Failed to fetch featured characters')
      }
    }
    fetchFeatured()
  }, [isOpen])

  // Search characters
  useEffect(() => {
    if (!query.trim()) { setResults([]); return }
    const controller = new AbortController()
    const search = async () => {
      setLoading(true)
      setError('')
      try {
        const res = await fetch(`https://api.jikan.moe/v4/characters?q=${encodeURIComponent(query)}&limit=20`, { signal: controller.signal })
        if (!res.ok) {
          const body = await res.json().catch(() => ({}))
          const parsed = (await import('../lib/parseApiError.js')).parseApiErrorBody(body)
          throw new Error(parsed.message || `Search failed: ${res.status}`)
        }
        const data = await res.json()
        setResults(data.data?.map(c => ({ id: c.mal_id, name: c.name, imageUrl: c.images?.jpg?.image_url })) || [])
      } catch (e) {
        if (e.name !== 'AbortError') setError(e.message || 'Failed to search characters')
      } finally {
        setLoading(false)
      }
    }
    const timeout = setTimeout(search, 400)
    return () => { clearTimeout(timeout); controller.abort() }
  }, [query])

  const handleConfirm = () => {
    if (selected) {
      onSelect(selected)
      onClose()
    }
  }

  if (!isOpen) return null

  const displayChars = query.trim() ? results : featured

  return (
    <AnimatePresence>
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        className="fixed inset-0 z-[100] flex items-center justify-center bg-black/80 backdrop-blur-md p-4"
        onClick={onClose}
      >
        <motion.div
          initial={motionSafe ? { opacity: 0, scale: 0.9, y: 30 } : false}
          animate={motionSafe ? { opacity: 1, scale: 1, y: 0 } : { opacity: 1 }}
          exit={motionSafe ? { opacity: 0, scale: 0.9, y: 30 } : { opacity: 0 }}
          transition={{ type: 'spring', stiffness: 280, damping: 24 }}
          className="relative w-full max-w-5xl h-[80vh] overflow-hidden rounded-3xl border backdrop-blur-3xl flex flex-col"
          style={{
            borderColor: 'rgba(255,255,255,0.08)',
            backgroundColor: accent.surface,
            boxShadow: `0 40px 100px rgba(0,0,0,0.6), 0 0 50px ${accent.glow}`,
          }}
          onClick={(e) => e.stopPropagation()}
        >
          <div className="pointer-events-none absolute -left-24 -top-24 h-72 w-72 rounded-full bg-purple-500/20 blur-3xl" />
          <div className="pointer-events-none absolute -bottom-24 -right-24 h-80 w-80 rounded-full bg-indigo-500/25 blur-3xl" />

          {/* Header */}
          <div className="relative p-6 border-b border-white/10">
            <div className="flex items-center justify-between mb-4">
              <div>
                <h2 className="text-2xl font-semibold text-white">Choose Your Avatar</h2>
                <p className="text-sm text-slate-300/80 mt-1">Search for your favorite anime character</p>
              </div>
              <button onClick={onClose} className="rounded-full p-2 text-slate-300 hover:bg-white/10 hover:text-white transition">
                <X className="h-6 w-6" />
              </button>
            </div>

            <div className="relative">
              <Search className="absolute left-4 top-1/2 -translate-y-1/2 h-5 w-5 text-slate-400" />
              <input
                type="search"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="Search for a character..."
                className="w-full rounded-xl border border-white/15 bg-black/30 pl-12 pr-4 py-3 text-base text-slate-200 placeholder:text-slate-400 focus:border-white/30 focus:outline-none focus:ring-2 focus:ring-white/20"
              />
            </div>
          </div>

          {/* Character Grid */}
          <div className="relative flex-1 overflow-y-auto p-6">
            {loading && <div className="text-center text-slate-300 py-8">Searching characters...</div>}
            {error && (
              <div className="mb-4 px-4">
                <AlertBanner status="error" message={error} />
              </div>
            )}
            {!loading && !error && (
              <div className="grid gap-4 grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6">
                {displayChars.map((char) => (
                  <motion.button
                    key={char.id}
                    type="button"
                    onClick={() => setSelected(char)}
                    className={`group flex flex-col items-center gap-3 rounded-2xl border p-3 text-center transition ${
                      selected?.id === char.id ? 'border-white/60 bg-white/15 ring-2 ring-white/40' : 'border-white/10 bg-black/20 hover:bg-white/5 hover:border-white/20'
                    }`}
                    whileHover={motionSafe ? { scale: 1.03 } : undefined}
                    whileTap={motionSafe ? { scale: 0.98 } : undefined}
                  >
                    <div className="relative aspect-[3/4] w-full overflow-hidden rounded-xl border border-white/10 bg-black/40">
                      <img src={char.imageUrl} alt={char.name} className="h-full w-full object-cover" loading="lazy" />
                      {selected?.id === char.id && (
                        <div className="absolute inset-0 flex items-center justify-center bg-black/40">
                          <div className="rounded-full p-2" style={{ background: `linear-gradient(135deg, ${accent.mid}, ${accent.strong})` }}>
                            <Check className="h-6 w-6 text-white" />
                          </div>
                        </div>
                      )}
                    </div>
                    <span className="text-sm text-slate-100 line-clamp-2">{char.name}</span>
                  </motion.button>
                ))}
              </div>
            )}
            {!loading && !error && !displayChars.length && !query.trim() && (
              <div className="text-center text-slate-400 py-8">Loading featured characters...</div>
            )}
            {!loading && !error && !displayChars.length && query.trim() && (
              <div className="text-center text-slate-400 py-8">No characters found for "{query}"</div>
            )}
          </div>

          {/* Footer */}
          <div className="relative p-6 border-t border-white/10 flex items-center justify-between gap-4">
            {selected ? (
              <div className="flex items-center gap-3">
                <img src={selected.imageUrl} alt={selected.name} className="h-12 w-12 rounded-lg object-cover border border-white/20" />
                <div>
                  <p className="text-sm text-slate-300">Selected:</p>
                  <p className="text-white font-medium">{selected.name}</p>
                </div>
              </div>
            ) : (
              <p className="text-sm text-slate-400">Select a character to use as your avatar</p>
            )}
            <div className="flex gap-3">
              <button
                onClick={onClose}
                className="rounded-xl border border-white/10 bg-white/5 px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-white/10"
              >
                Cancel
              </button>
              <motion.button
                onClick={handleConfirm}
                disabled={!selected}
                className="rounded-xl px-5 py-2.5 text-sm font-semibold text-white transition disabled:opacity-50"
                style={{ backgroundImage: `linear-gradient(90deg, ${accent.mid}, ${accent.strong})`, boxShadow: `0 8px 24px ${accent.glow}` }}
                whileHover={motionSafe && selected ? { scale: 1.02 } : undefined}
                whileTap={motionSafe && selected ? { scale: 0.98 } : undefined}
              >
                Use This Character
              </motion.button>
            </div>
          </div>
        </motion.div>
      </motion.div>
    </AnimatePresence>
  )
}

function ProfileSetup() {
  const usernameFromCookie = getCookie('username') || ''
  const todayIso = new Date().toISOString().split('T')[0]
  const navigate = useNavigate()
  const reduceMotion = usePrefersReducedMotion()
  const isLowPower = useDeviceProfile()
  const motionSafe = !reduceMotion

  const [accentKey, setAccentKey] = useState(() => getSavedAccentKey('crimson-night'))
  const accent = useMemo(() => accentOptions.find((opt) => opt.key === accentKey)?.colors || accentOptions[0].colors, [accentKey])
  const [wallLoading, setWallLoading] = useState(true)

  const [form, setForm] = useState(() => ({ username: usernameFromCookie, displayName: '', bio: '', location: '', dateOfBirth: '' }))
  const [avatarMode, setAvatarMode] = useState('upload')
  const [imageFile, setImageFile] = useState(null)
  const [imagePreview, setImagePreview] = useState(null)
  const [selectedCharacter, setSelectedCharacter] = useState(null)
  const [fileError, setFileError] = useState('')
  const [apiMessage, setApiMessage] = useState('')
  const [apiStatus, setApiStatus] = useState('info')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [locationModalOpen, setLocationModalOpen] = useState(false)
  const [characterModalOpen, setCharacterModalOpen] = useState(false)
  const [fieldErrors, setFieldErrors] = useState({})

  const userId = getCookie('userId')
  const maxFileSize = 5 * 1024 * 1024

  useEffect(() => {
    if (!userId) {
      setApiStatus('error')
      setApiMessage('User ID missing. Please register again.')
    }
  }, [userId])

  useEffect(() => {
    if (usernameFromCookie) {
      setForm((prev) => ({ ...prev, username: usernameFromCookie }))
    }
  }, [usernameFromCookie])

  useEffect(() => {
    persistAccentKey(accentKey)
  }, [accentKey])

  useEffect(() => {
    const hasProfile = localStorage.getItem('profileCompleted') === 'true'
    const token = getCookie('AccessToken') || localStorage.getItem('AccessToken')
    if (hasProfile && token) {
      navigate('/', { replace: true })
    }
  }, [navigate])

  const handleChange = (e) => {
    const { name, value } = e.target
    setForm((prev) => ({ ...prev, [name]: value }))
  }

  // Local DOB parts for flexible selection order
  const [dobY, setDobY] = useState('')
  const [dobM, setDobM] = useState('')
  const [dobD, setDobD] = useState('')
  const [dobFocus, setDobFocus] = useState({ day: false, month: false, year: false })

  useEffect(() => {
    // If form.dateOfBirth is populated externally, sync to local parts
    if (form.dateOfBirth) {
      const [y, m, d] = String(form.dateOfBirth).split('-')
      setDobY(y || '')
      setDobM(m || '')
      setDobD(d || '')
    }
  }, [form.dateOfBirth])

  // Helper to set date parts and validate against today
  const setDobParts = (y, m, d) => {
    if (!y || !m || !d) {
      // partial selection: store parts but clear final value
      setForm((prev) => ({ ...prev, dateOfBirth: '' }))
      setFieldErrors((prev) => ({ ...prev, dateOfBirth: '' }))
      setDobY(y || '')
      setDobM(m || '')
      setDobD(d || '')
      return
    }
    // normalize ints
    const yy = parseInt(String(y), 10)
    const mm = parseInt(String(m), 10)
    let dd = parseInt(String(d), 10)
    // clamp day to month length
    const daysInMonth = new Date(yy, mm, 0).getDate()
    if (dd > daysInMonth) dd = daysInMonth
    const ddStr = String(dd).padStart(2, '0')
    const mmStr = String(mm).padStart(2, '0')
    const iso = `${String(yy)}-${mmStr}-${ddStr}`
    if (iso > todayIso) {
      setFieldErrors((prev) => ({ ...prev, dateOfBirth: 'Date of birth cannot be in the future' }))
      // clamp to today
      setForm((prev) => ({ ...prev, dateOfBirth: todayIso }))
      // sync parts to today
      const [ty, tm, td] = todayIso.split('-')
      setDobY(ty); setDobM(tm); setDobD(td)
    } else {
      setFieldErrors((prev) => ({ ...prev, dateOfBirth: '' }))
      setForm((prev) => ({ ...prev, dateOfBirth: iso }))
      setDobY(String(yy)); setDobM(mmStr); setDobD(ddStr)
    }
  }

  const handleFileChange = (file) => {
    setFileError('')
    if (!file) { setImageFile(null); setImagePreview(null); return }
    if (file.size > maxFileSize) {
      setFileError('File exceeds 5 MB limit.')
      return
    }
    setImageFile(file)
    const reader = new FileReader()
    reader.onload = () => setImagePreview(reader.result)
    reader.readAsDataURL(file)
  }

  const handleCharacterSelect = (char) => {
    setSelectedCharacter(char)
    setAvatarMode('character')
    setImageFile(null)
    setImagePreview(null)
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setFieldErrors({})

    if (!userId) {
      setApiStatus('error')
      setApiMessage('User ID missing. Please register again.')
      return
    }

    // Client-side required validation
    const errs = {}
    if (!String(form.displayName || '').trim()) errs.displayName = 'Display name is required.'
    else if (String(form.displayName || '').trim().length < 6 || String(form.displayName || '').trim().length > 50) errs.displayName = 'Display name length must be between 6 and 50 characters'
    if (!String(form.bio || '').trim()) errs.bio = 'Bio is required.'
    if (!String(form.location || '').trim()) errs.location = 'Location is required.'
    if (!String(form.dateOfBirth || '').trim()) errs.dateOfBirth = 'Date of Birth is required.'
    if (fieldErrors.dateOfBirth) errs.dateOfBirth = fieldErrors.dateOfBirth
    if (avatarMode === 'upload' && !imageFile) errs.avatar = 'Please upload a profile image or pick a character.'
    if (avatarMode === 'character' && !selectedCharacter) errs.avatar = 'Please select a character for your avatar.'

    if (Object.keys(errs).length > 0) {
      setFieldErrors(errs)
      setApiStatus('error')
      setApiMessage(Object.values(errs).join(' '))
      return
    }

    setIsSubmitting(true)
    setApiStatus('info')
    setApiMessage('Creating your profile...')

    try {
      const token = getBearerToken()
      const dto = {
        id: String(userId || ''),
        username: String(form.username || ''),
        displayName: String(form.displayName?.trim() || ''),
        bio: String(form.bio?.trim() || ''),
        location: String(form.location?.trim() || ''),
        dateOfBirth: form.dateOfBirth ? String(form.dateOfBirth) : '',
      }

      const formData = new FormData()
      formData.append('requestDto', new Blob([JSON.stringify(dto)], { type: 'application/json' }))

      let fileToSend = null
      if (avatarMode === 'upload' && imageFile) {
        fileToSend = imageFile
      } else if (avatarMode === 'character' && selectedCharacter?.imageUrl) {
        const response = await fetch(selectedCharacter.imageUrl)
        const blob = await response.blob()
        const ext = (blob.type?.split('/')?.[1] || 'jpg').split(';')[0]
        fileToSend = new File([blob], `avatar.${ext}`, { type: blob.type || 'image/jpeg' })
      }

      if (!fileToSend) {
        throw new Error('Profile image is required by the API')
      }

      formData.append('file', fileToSend)

      const url = `${import.meta.env.VITE_API_BASE_URL}profile/user/profile/create`
      const headers = token ? { Authorization: token } : undefined
      await axios.post(url, formData, { headers })

      setApiStatus('success')
      setApiMessage('Profile created successfully! Proceeding to preferences...')
      // Set displayName cookie so it's available across the app
      try { const d = String(form.displayName?.trim() || ''); if (d) document.cookie = `displayName=${encodeURIComponent(d)}; path=/; max-age=604800; SameSite=Lax` } catch(e) {}
      // Redirect to preferences step instead of marking profile completed & going home
      setTimeout(() => navigate('/preferences', { replace: true }), 900)
    } catch (err) {
      const body = err?.response?.data
      try {
        const parsed = (await import('../lib/parseApiError.js')).parseApiErrorBody(body)
        setFieldErrors((prev) => ({ ...prev, ...parsed.fields }))
        setApiStatus('error')
        setApiMessage(parsed.message || err.message || 'Something went wrong. Please try again.')
      } catch (e) {
        setApiStatus('error')
        setApiMessage(body?.message || err.message || 'Something went wrong. Please try again.')
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div
      className="relative min-h-screen overflow-visible text-slate-50"
      style={{ backgroundImage: `linear-gradient(135deg, ${accent.start}, ${accent.mid}, ${accent.end})` }}
    >
      <style>{`
        ::selection { background: ${accent.strong}; color: #f8fafc; }
        input::selection, textarea::selection { background: ${accent.strong}; color: #f8fafc; }
      `}</style>
      <div
        className="pointer-events-none absolute inset-0"
        style={{
          backgroundImage: `radial-gradient(circle at 45% 20%, ${accent.glow}, transparent 38%), radial-gradient(circle at 78% 30%, rgba(255,255,255,0.06), transparent 30%), radial-gradient(circle at 55% 78%, rgba(255,255,255,0.04), transparent 33%)`,
          mixBlendMode: 'screen',
        }}
      />

      <div className="relative flex min-h-screen w-full max-w-none flex-col items-center justify-center gap-8 px-0 sm:px-0 lg:flex-row lg:items-stretch lg:justify-between lg:gap-0 lg:px-0">
        {/* Left: Profile Card */}
        <div className="flex w-full max-w-xl flex-col items-center justify-center px-4 py-10 sm:px-8 lg:w-2/5 lg:items-start lg:justify-center lg:px-10 xl:px-14">
          <motion.div
            initial={motionSafe ? { opacity: 0, y: 20, scale: 0.98 } : false}
            animate={motionSafe ? { opacity: 1, y: 0, scale: 1 } : { opacity: 1 }}
            transition={{ type: 'spring', stiffness: 140, damping: 18 }}
            className="relative w-full max-w-2xl overflow-hidden rounded-3xl border backdrop-blur-3xl"
            style={{
              borderColor: 'rgba(255,255,255,0.08)',
              backgroundColor: accent.surface,
              boxShadow: `0 30px 90px rgba(0,0,0,0.45), 0 0 35px ${accent.glow}`,
            }}
          >
            <div className="pointer-events-none absolute -left-20 -top-24 h-64 w-64 rounded-full bg-purple-500/20 blur-3xl" />
            <div className="pointer-events-none absolute -bottom-24 -right-20 h-72 w-72 rounded-full bg-indigo-500/25 blur-3xl" />

            <div className="relative p-8 sm:p-10">
              {/* Header */}
              <div className="flex flex-col items-center gap-4 text-center sm:flex-row sm:items-start sm:justify-between sm:text-left">
                <div className="flex flex-col items-center sm:items-start">
                  <p className="text-xs uppercase tracking-[0.25em] text-slate-300/80">ThatOtakuNetwork</p>
                  <h1 className="mt-2 text-3xl font-semibold text-white">Complete Your Profile</h1>
                  <p className="mt-1 text-sm text-slate-300 max-w-xl">Add a few details to finish setting up your account.</p>
                </div>
                <div className="flex flex-col items-center gap-3 sm:items-end">
                  <div className="flex flex-wrap items-center justify-center gap-2 lg:flex-nowrap">
                    {accentOptions.map((opt) => (
                      <div key={opt.key} className="relative group">
                        <button
                          type="button"
                          aria-label={`${opt.label} accent`}
                          onClick={() => setAccentKey(opt.key)}
                          className={`h-7 w-7 rounded-full border border-white/20 transition ${accentKey === opt.key ? 'ring-2 ring-white/70 scale-105' : 'opacity-75 hover:opacity-100 hover:scale-[1.08]'}`}
                          style={{ background: `radial-gradient(circle at 30% 30%, ${opt.colors.strong}, ${opt.colors.mid})` }}
                        />
                        <span className="pointer-events-none absolute left-1/2 top-9 -translate-x-1/2 whitespace-nowrap rounded-full border border-white/10 bg-black/70 px-3 py-1 text-xs font-semibold text-white opacity-0 backdrop-blur-lg shadow-glow transition duration-200 ease-out group-hover:opacity-100 group-hover:-translate-y-1 group-hover:scale-100 scale-95" style={{ boxShadow: `0 14px 36px ${opt.colors.glow}` }}>{opt.label}</span>
                      </div>
                    ))}
                  </div>
                </div>
              </div>

              {/* Form */}
              <div className="mt-8">
                <AlertBanner status={apiStatus} message={apiMessage} />
                <form onSubmit={handleSubmit} className="space-y-5">
                  {/* Username (text display, not input) */}
                  <div>
                    <label className="block text-sm font-medium text-slate-200 mb-2">Username</label>
                    <div className="flex items-center gap-3 rounded-2xl border border-white/10 bg-white/5 px-4 py-3">
                      <span className="text-base text-slate-100 font-medium">@{form.username || '—'}</span>
                      <span className="ml-auto text-xs text-slate-400 bg-white/5 px-2 py-1 rounded-full">Cannot be changed</span>
                    </div>
                  </div>

                  {/* Display Name */}
                  <label className="block text-sm font-medium text-slate-200">Display Name <span className="text-rose-400">*</span></label>
                  <input
                    id="displayName"
                    name="displayName"
                    value={form.displayName}
                    onChange={handleChange}
                    autoComplete="name"
                    required
                    aria-required
                    className="block w-full rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-base text-slate-50 shadow-inner shadow-black/10 outline-none transition focus:border-violet-300/60 focus:ring-2 focus:ring-violet-300/60"
                  />
                  {fieldErrors.displayName && <p className="mt-2 text-sm text-rose-200">{fieldErrors.displayName}</p>}

                  {/* Bio */}
                  <div>
                    <label htmlFor="bio" className="block text-sm font-medium text-slate-200">Bio <span className="text-rose-400">*</span></label>
                    <div className="relative mt-2">
                      <textarea
                        id="bio"
                        name="bio"
                        value={form.bio}
                        onChange={handleChange}
                        rows={3}
                        placeholder="Tell us about yourself..."
                        required
                        aria-required
                        className="block w-full rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-base text-slate-50 shadow-inner shadow-black/10 outline-none transition focus:border-violet-300/60 focus:ring-2 focus:ring-violet-300/60 resize-none"
                      />
                    </div>
                    {fieldErrors.bio && <p className="mt-2 text-sm text-rose-200">{fieldErrors.bio}</p>}
                  </div>

                  {/* Location (with modal trigger) */}
                  <div>
                    <label className="block text-sm font-medium text-slate-200 mb-2">Location <span className="text-rose-400">*</span></label>
                    <button
                      type="button"
                      onClick={() => setLocationModalOpen(true)}
                      className="w-full flex items-center gap-3 rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-left transition hover:bg-white/10 hover:border-white/20"
                      aria-required
                    >
                      <MapPin className="h-5 w-5 text-slate-400" />
                      <span className={form.location ? 'text-slate-100' : 'text-slate-400'}>
                        {form.location || 'Click to select your location'}
                      </span>
                    </button>
                    {fieldErrors.location && <p className="mt-2 text-sm text-rose-200">{fieldErrors.location}</p>}
                  </div>

                  {/* Date of Birth (Discord-style selector) */}
                  <div>
                    <label className="block text-sm font-medium text-slate-200">Date of Birth <span className="text-rose-400">*</span></label>
                    <div className="mt-2 grid grid-cols-3 gap-3" role="group" aria-label="Date of birth selector">
                      {/* Day */}
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
                            const [y,m,] = String(form.dateOfBirth || '').split('-')
                            const yy = parseInt(y, 10) || new Date().getFullYear()
                            const mm = parseInt(m, 10) || 1
                            const days = new Date(yy, mm, 0).getDate()
                            return Array.from({ length: days }, (_, i) => i + 1).map((d) => (
                              <option key={d} value={String(d).padStart(2, '0')}>{d}</option>
                            ))
                          })()}
                        </select>
                      </div>

                      {/* Month */}
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

                      {/* Year */}
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
                    <p className="mt-2 text-xs text-slate-400">Select day, month, and year.</p>
                    {fieldErrors.dateOfBirth && <p className="mt-2 text-sm text-rose-200">{fieldErrors.dateOfBirth}</p>}
                  </div>

                  {/* Profile Image */}
                  <div className="rounded-2xl border border-white/10 bg-black/20 p-5">
                    <div className="flex items-center justify-between gap-3 mb-4">
                      <span className="text-sm font-semibold text-white">Profile Image</span>
                      <div className="flex gap-2">
                        <button
                          type="button"
                          onClick={() => { setAvatarMode('upload'); setSelectedCharacter(null) }}
                          className={`rounded-full px-4 py-1.5 text-xs font-medium border transition ${avatarMode === 'upload' ? 'border-white/50 bg-white/15 text-white' : 'border-white/15 bg-white/5 text-slate-300 hover:bg-white/10'}`}
                        >
                          Upload
                        </button>
                        <button
                          type="button"
                          onClick={() => setCharacterModalOpen(true)}
                          className={`rounded-full px-4 py-1.5 text-xs font-medium border transition ${avatarMode === 'character' ? 'border-white/50 bg-white/15 text-white' : 'border-white/15 bg-white/5 text-slate-300 hover:bg-white/10'}`}
                        >
                          Pick Character
                        </button>
                      </div>
                    </div>

                    {avatarMode === 'upload' && (
                      <div className="space-y-3">
                        <input
                          type="file"
                          accept="image/*"
                          onChange={(e) => handleFileChange(e.target.files?.[0] || null)}
                          className="block w-full text-sm text-slate-300 file:mr-4 file:py-2 file:px-4 file:rounded-full file:border-0 file:text-sm file:font-semibold file:bg-white/10 file:text-white hover:file:bg-white/20"
                        />
                        {imagePreview && (
                          <div className="flex items-center gap-3">
                            <img src={imagePreview} alt="Preview" className="h-16 w-16 rounded-xl object-cover border border-white/20" />
                            <span className="text-sm text-slate-300">Image selected</span>
                          </div>
                        )}
                        {fileError && (
                          <div className="mt-2">
                            <AlertBanner status="error" message={fileError} />
                          </div>
                        )}
                        <p className="text-xs text-slate-400">Max 5 MB. JPG, PNG, or WebP recommended.</p>
                      </div>
                    )}

                    {avatarMode === 'character' && selectedCharacter && (
                      <div className="flex items-center gap-4 rounded-xl border border-emerald-400/30 bg-emerald-400/10 p-3">
                        <img src={selectedCharacter.imageUrl} alt={selectedCharacter.name} className="h-14 w-14 rounded-xl object-cover border border-white/20" />
                        <div className="flex-1">
                          <p className="text-sm text-emerald-200">Selected Character</p>
                          <p className="text-white font-medium">{selectedCharacter.name}</p>
                        </div>
                        <button type="button" onClick={() => setCharacterModalOpen(true)} className="text-xs text-slate-300 hover:text-white underline">Change</button>
                      </div>
                    )}
                  </div>

                  {/* Submit Button */}
                  <motion.button
                    type="submit"
                    disabled={isSubmitting}
                    className="w-full rounded-2xl border border-white/10 px-5 py-3.5 text-base font-semibold text-white shadow-lg focus:outline-none focus:ring-2 disabled:opacity-60"
                    style={{
                      backgroundImage: `linear-gradient(90deg, ${accent.mid}, ${accent.strong})`,
                      boxShadow: `0 10px 30px ${accent.glow}`,
                    }}
                    whileHover={motionSafe && !isSubmitting ? { scale: 1.02, boxShadow: `0 14px 40px ${accent.glow}` } : undefined}
                    whileTap={motionSafe && !isSubmitting ? { scale: 0.98 } : undefined}
                  >
                    {isSubmitting ? 'Creating Profile...' : 'Create Profile'}
                  </motion.button>
                </form>
              </div>
            </div>
          </motion.div>
        </div>

        {/* Right: AnimeWall */}
        <div className="hidden w-full items-stretch lg:flex lg:w-[62%] relative">
          <Suspense fallback={<WallSkeleton />}>
            <AnimeWall reduceMotion={reduceMotion} isLowPower={isLowPower} onLoadingChange={setWallLoading} />
          </Suspense>
        </div>
      </div>

      {/* Modals */}
      <LocationPickerModal
        isOpen={locationModalOpen}
        onClose={() => setLocationModalOpen(false)}
        onSelect={(loc) => setForm((prev) => ({ ...prev, location: loc }))}
        accent={accent}
        motionSafe={motionSafe}
      />
      <CharacterPickerModal
        isOpen={characterModalOpen}
        onClose={() => setCharacterModalOpen(false)}
        onSelect={handleCharacterSelect}
        accent={accent}
        motionSafe={motionSafe}
      />
    </div>
  )
}

export default ProfileSetup

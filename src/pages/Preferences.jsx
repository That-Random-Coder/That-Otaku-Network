import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AnimatePresence, motion } from 'framer-motion'
import { Check } from 'lucide-react'
import accentOptions from '../theme/accentOptions.js'
import { getSavedAccentKey } from '../theme/accentStorage.js'
import AlertBanner from '../components/AlertBanner.jsx'

const getCookie = (name) => {
  if (typeof document === 'undefined') return ''
  const match = document.cookie.match(new RegExp(`(?:^|; )${name}=([^;]*)`))
  return match ? decodeURIComponent(match[1]) : ''
}


const humanize = (s) => String(s || '').toLowerCase().split('_').map(p => p.charAt(0).toUpperCase() + p.slice(1)).join(' ')

const GENRES = [
  'ACTION','ADVENTURE','COMEDY','DRAMA','FANTASY','SCI_FI','SLICE_OF_LIFE','ROMANCE','HORROR','MYSTERY','THRILLER','SUPERNATURAL','PSYCHOLOGICAL','SPORTS','MUSIC','MECHA','HISTORICAL','MILITARY','ECCHI','HAREM','ISEKAI','MAGIC','SCHOOL','DEMON'
]

const CATEGORIES = [
  'MEME','FUNNY','DISCUSSION','QUESTION','OPINION','REVIEW','ANALYSIS','NEWS','CLIP','FAN_ART','EDIT','COSPLAY','SLICE_OF_LIFE','STORY','ROMANCE','ECCHI','META'
]

export default function Preferences() {
  const [accentKey] = useState(() => getSavedAccentKey('crimson-night'))
  const accent = useMemo(() => accentOptions.find((opt) => opt.key === accentKey)?.colors || accentOptions[0].colors, [accentKey])
  const navigate = useNavigate()

  const [selectedGenres, setSelectedGenres] = useState([])
  const [selectedCategories, setSelectedCategories] = useState([])
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const toggle = (arr, setArr, val) => {
    const idx = arr.indexOf(val)
    if (idx === -1) setArr([ ...arr, val ])
    else setArr(arr.filter((v) => v !== val))
  }

  const MIN_SELECT = 2
  const MAX_SELECT = 5
  const canSave = selectedGenres.length >= MIN_SELECT && selectedGenres.length <= MAX_SELECT && selectedCategories.length >= MIN_SELECT && selectedCategories.length <= MAX_SELECT

  const handleSkip = async () => {
    setError('')
    setLoading(true)
    try {
      const uid = getCookie('id') || getCookie('userId') || ''
      const token = getCookie('AccessToken') || (typeof localStorage !== 'undefined' ? localStorage.getItem('AccessToken') || '' : '')
      const auth = token ? (token.toLowerCase().startsWith('bearer ') ? token : `Bearer ${token}`) : ''
      const url = `${import.meta.env.VITE_API_BASE_URL}recommendation/feed/get?userId=${encodeURIComponent(uid)}`
      try { console.log('[Preferences] fetching recommendation feed (skip) URL:', url, 'authPresent:', !!auth) } catch (e) {}
      const res = await fetch(url, { headers: { ...(auth ? { Authorization: auth, AccessToken: token } : {}) } })
      const data = await res.json().catch(() => ([]))
      if (!res.ok) {
        const parsed = (await import('../lib/parseApiError.js')).parseApiErrorBody(data)
        throw new Error(parsed.message || 'Failed to fetch recommendations')
      }

      
      try { localStorage.setItem('recommendationFeed', JSON.stringify(Array.isArray(data) ? data : (data?.data || []))) } catch (e) {}

      try { localStorage.setItem('profileCompleted', 'true'); document.cookie = 'profileCompleted=true; path=/; max-age=604800; SameSite=Lax' } catch (e) {}
      navigate('/', { replace: true })
    } catch (err) {
      console.error('skip preferences error', err)
      setError(err?.message || 'Failed to fetch recommendations')
      setLoading(false)
    }
  }

  const handleSave = async () => {
    if (!canSave) return
    setError('')
    setLoading(true)
    try {
      const token = getCookie('AccessToken') || (typeof localStorage !== 'undefined' ? localStorage.getItem('AccessToken') || '' : '')
      const auth = token ? (token.toLowerCase().startsWith('bearer ') ? token : `Bearer ${token}`) : ''

      
      const payload = { category: selectedCategories.map(humanize), genre: selectedGenres.map(humanize) }
      const url = import.meta.env.VITE_API_BASE_URL + 'recommendation/feed/first/get'
      try { console.log('[Preferences] sending preferences to:', url, 'payload:', payload, 'authPresent:', !!auth) } catch (e) {}
      const res = await fetch(url, { method: 'POST', headers: { 'Content-Type': 'application/json', ...(auth ? { Authorization: auth, AccessToken: token } : {}) }, body: JSON.stringify(payload) })
      const data = await res.json().catch(() => ([]))
      if (!res.ok) {
        const parsed = (await import('../lib/parseApiError.js')).parseApiErrorBody(data)
        throw new Error(parsed.message || 'Failed to save preferences')
      }

      
      try { localStorage.setItem('recommendationFeed', JSON.stringify(Array.isArray(data) ? data : (data?.data || []))) } catch (e) {}

      
      try { localStorage.setItem('profileCompleted', 'true'); document.cookie = 'profileCompleted=true; path=/; max-age=604800; SameSite=Lax' } catch (e) {}
      navigate('/', { replace: true })
    } catch (err) {
      console.error('save preferences error', err)
      setError(err?.message || 'Failed to save preferences')
      setLoading(false)
    }
  }

  return (
    <div className="relative min-h-screen overflow-visible text-slate-50" style={{ backgroundImage: `linear-gradient(135deg, ${accent.start}, ${accent.mid}, ${accent.end})` }}>
      <div className="pointer-events-none absolute inset-0" style={{ backgroundImage: `radial-gradient(circle at 45% 20%, ${accent.glow}, transparent 38%), radial-gradient(circle at 78% 30%, rgba(255,255,255,0.06), transparent 30%), radial-gradient(circle at 55% 78%, rgba(255,255,255,0.04), transparent 33%)`, mixBlendMode: 'screen' }} />

      <div className="relative z-10 px-4 pb-20 pt-14 sm:px-8 lg:px-12 xl:px-16">
        <div className="md:grid md:grid-cols-[20rem_1fr] md:gap-8">
          <div className="hidden md:block">
          </div>
          <div className="min-w-0">
            <header className="flex flex-wrap items-center justify-between gap-4">
              <div>
                <p className="text-xs uppercase tracking-[0.25em] text-slate-300/80">Preferences</p>
                <h1 className="mt-2 text-3xl font-semibold text-white">Pick your favourites</h1>
                <p className="mt-2 text-sm text-slate-200/85 max-w-2xl">Choose at least 2 genres and 2 categories to get better recommendations.</p>
              </div>
            </header>

            <div className="mt-8 w-full max-w-4xl">
              {error && <div className="mb-4"><AlertBanner status="error" message={error} /></div>}

              <div className="grid grid-cols-1 gap-8 sm:grid-cols-2">
                <div className="rounded-2xl border border-white/10 bg-white/5 p-6">
                  <div className="flex items-baseline justify-between mb-3">
                    <h3 className="text-lg font-semibold text-white">Preferable Genres</h3>
                    <div className="text-xs text-slate-400">Selected: <strong className="text-white">{selectedGenres.length}</strong> / {MAX_SELECT}</div>
                  </div>
                  <div className="flex flex-wrap gap-3">
                    {GENRES.map((g) => {
                      const label = humanize(g)
                      const selected = selectedGenres.includes(g)
                      const disabled = !selected && selectedGenres.length >= MAX_SELECT
                      return (
                        <button key={g} type="button" onClick={() => toggle(selectedGenres, setSelectedGenres, g)} disabled={disabled} className={`flex items-center gap-2 rounded-full px-3 py-1.5 text-sm font-semibold transition ${selected ? 'bg-gradient-to-r from-[var(--accent-mid)] to-[var(--accent-strong)] text-white shadow-glow' : 'bg-white/5 text-slate-200 hover:bg-white/10'} ${disabled ? 'opacity-60 cursor-not-allowed hover:bg-white/5' : ''}`} style={selected ? { backgroundImage: `linear-gradient(90deg, ${accent.mid}, ${accent.strong})`, boxShadow: `0 8px 24px ${accent.glow}` } : undefined}>
                          {selected && <Check className="h-4 w-4" />}
                          <span>{label}</span>
                        </button>
                      )
                    })}
                  </div>
                </div>

                <div className="rounded-2xl border border-white/10 bg-white/5 p-6">
                  <div className="flex items-baseline justify-between mb-3">
                    <h3 className="text-lg font-semibold text-white">Preferable Categories</h3>
                    <div className="text-xs text-slate-400">Selected: <strong className="text-white">{selectedCategories.length}</strong> / {MAX_SELECT}</div>
                  </div>
                  <div className="flex flex-wrap gap-3">
                    {CATEGORIES.map((c) => {
                      const label = humanize(c)
                      const selected = selectedCategories.includes(c)
                      const disabled = !selected && selectedCategories.length >= MAX_SELECT
                      return (
                        <button key={c} type="button" onClick={() => toggle(selectedCategories, setSelectedCategories, c)} disabled={disabled} className={`flex items-center gap-2 rounded-full px-3 py-1.5 text-sm font-semibold transition ${selected ? 'bg-gradient-to-r from-[var(--accent-mid)] to-[var(--accent-strong)] text-white shadow-glow' : 'bg-white/5 text-slate-200 hover:bg-white/10'} ${disabled ? 'opacity-60 cursor-not-allowed hover:bg-white/5' : ''}`} style={selected ? { backgroundImage: `linear-gradient(90deg, ${accent.mid}, ${accent.strong})`, boxShadow: `0 8px 24px ${accent.glow}` } : undefined}>
                          {selected && <Check className="h-4 w-4" />}
                          <span>{label}</span>
                        </button>
                      )
                    })}
                  </div>
                </div>
              </div>

              <div className="mt-6 flex items-center justify-between">
                <button onClick={handleSkip} disabled={loading} className="rounded-xl border border-white/10 bg-white/5 px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-white/10 disabled:opacity-50">{loading ? 'Loading...' : 'Skip'}</button>
                <button onClick={handleSave} disabled={!canSave || loading} className="rounded-xl px-5 py-2.5 text-sm font-semibold text-white transition disabled:opacity-50" style={{ backgroundImage: `linear-gradient(90deg, ${accent.mid}, ${accent.strong})`, boxShadow: `0 8px 24px ${accent.glow}` }}>{loading ? 'Saving...' : `Save`}</button>
              </div>

            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

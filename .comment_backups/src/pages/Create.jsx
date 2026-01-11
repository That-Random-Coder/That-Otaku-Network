import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import accentOptions from '../theme/accentOptions.js'
import { getSavedAccentKey } from '../theme/accentStorage.js'
import NavigationBar from '../components/NavigationBar.jsx'
import AlertBanner from '../components/AlertBanner.jsx'
import { Check, ImageIcon, X } from 'lucide-react' 

const GENRES = [
  'ACTION','ADVENTURE','COMEDY','DRAMA','FANTASY','SCI_FI','SLICE_OF_LIFE','ROMANCE','HORROR','MYSTERY','THRILLER','SUPERNATURAL','PSYCHOLOGICAL','SPORTS','MUSIC','MECHA','HISTORICAL','MILITARY','ECCHI','HAREM','ISEKAI','MAGIC','SCHOOL','DEMON'
]

const CATEGORIES = [
  'MEME','FUNNY','DISCUSSION','QUESTION','OPINION','REVIEW','ANALYSIS','NEWS','CLIP','FAN_ART','EDIT','COSPLAY','SLICE_OF_LIFE','STORY','ROMANCE','ECCHI','META'
]

// Maximum selections for genres and categories
const MAX_SELECT = 5

const getCookie = (name) => {
  if (typeof document === 'undefined') return ''
  const match = document.cookie.match(new RegExp(`(?:^|; )${name}=([^;]*)`))
  return match ? decodeURIComponent(match[1]) : ''
}

const humanize = (s) => String(s || '').toLowerCase().split('_').map(p => p.charAt(0).toUpperCase() + p.slice(1)).join(' ')

function Create() {
  const navigate = useNavigate()
  const accentKey = getSavedAccentKey('crimson-night')
  const accent = useMemo(() => accentOptions.find((opt) => opt.key === accentKey)?.colors || accentOptions[0].colors, [accentKey])

  const [title, setTitle] = useState('')
  const [bio, setBio] = useState('')
  const [selectedGenres, setSelectedGenres] = useState([])
  const [selectedCategories, setSelectedCategories] = useState([])
  const [tags, setTags] = useState([])
  const [tagInput, setTagInput] = useState('')
  const [file, setFile] = useState(null)
  const [filePreview, setFilePreview] = useState(null)
  const [mediaString, setMediaString] = useState('')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [loading, setLoading] = useState(false)
  const [isDragging, setIsDragging] = useState(false)

  const fileRef = useRef(null)

  useEffect(() => {
    // cleanup preview URL on unmount
    return () => {
      if (filePreview && filePreview.startsWith('blob:')) URL.revokeObjectURL(filePreview)
    }
  }, [filePreview])

  const toggle = (arr, setArr, val) => {
    const idx = arr.indexOf(val)
    if (idx === -1) setArr([ ...arr, val ])
    else setArr(arr.filter((v) => v !== val))
  }

  // Toggle but prevent adding more than MAX_SELECT items
  const toggleLimited = (arr, setArr, val) => {
    if (!arr.includes(val) && arr.length >= MAX_SELECT) return
    toggle(arr, setArr, val)
  }

  const canSubmit = title.trim().length > 0 && selectedGenres.length >= 1 && selectedGenres.length <= MAX_SELECT && selectedCategories.length >= 1 && selectedCategories.length <= MAX_SELECT && (file || mediaString)

  const sanitizeTagForSend = (t) => {
    if (!t) return ''
    // remove leading '#', trim, replace spaces with underscores
    const cleaned = String(t).replace(/^#+/, '').trim().replace(/\s+/g, '_')
    return cleaned
  }

  const addTag = (raw) => {
    const cleaned = sanitizeTagForSend(raw)
    if (!cleaned) return
    if (tags.includes(cleaned)) return
    setTags((prev) => [...prev, cleaned])
    setTagInput('')
  }

  const handleTagKey = (e) => {
    if (e.key === 'Enter' || e.key === ',') {
      e.preventDefault()
      if (tagInput.trim()) addTag(tagInput)
    }
  }

  const handleFile = async (f) => {
    setError('')
    if (!f) { setFile(null); setFilePreview(null); setMediaString(''); return }
    const allowedTypes = ['image/', 'video/']
    if (!allowedTypes.some(t => f.type.startsWith(t))) { setError('Unsupported media type.'); return }
    setFile(f)
    const url = URL.createObjectURL(f)
    setFilePreview(url)

    // convert to base64 string for 'media' field
    try {
      const reader = new FileReader()
      reader.onload = () => {
        const result = reader.result
        // result is Data URL; send as-is
        setMediaString(result)
      }
      reader.readAsDataURL(f)
    } catch (e) {
      console.error('Failed to read file', e)
    }
  }

  const removeTag = (t) => setTags((prev) => prev.filter((x) => x !== t))
  const removeFile = () => { setFile(null); setFilePreview(null); setMediaString(''); if (fileRef.current) fileRef.current.value = '' }

  const handleSubmit = async () => {
    setError('')
    setSuccess('')

    // Build payload immediately and show it regardless of validation outcome
    const userID = getCookie('userId') || getCookie('id') || ''
    const userName = getCookie('username') || (typeof localStorage !== 'undefined' ? localStorage.getItem('username') || '' : '') || ''
    const displayName = getCookie('displayName') || (typeof localStorage !== 'undefined' ? localStorage.getItem('displayName') || localStorage.getItem('displayname') : '') || getCookie('displayname') || userName || getCookie('name') || ''

    const payload = {
      request: {
        title: String(title).trim(),
        animeCategories: selectedCategories.slice(0, MAX_SELECT),
        genres: selectedGenres.slice(0, MAX_SELECT),
        tags: tags.slice(0, 20),
        bio: String(bio).trim(),
        userID,
        userName,
        displayName,
      },
      media: mediaString || ''
    }

    try { console.log('[Create] click payload', JSON.parse(JSON.stringify(payload))) } catch (e) { console.log('[Create] click payload (string)', JSON.stringify(payload)) }

    if (!canSubmit) { setError('Please provide a title and select at least 1 genre and 1 category.'); return }
    setLoading(true)
    try {
      const tokenRaw = getCookie('AccessToken') || (typeof localStorage !== 'undefined' ? localStorage.getItem('AccessToken') || '' : '')
      const auth = tokenRaw ? (tokenRaw.toLowerCase().startsWith('bearer ') ? tokenRaw : `Bearer ${tokenRaw}`) : ''

      const formData = new FormData()
      // include request payload as JSON under both keys for backend compatibility
      const requestBlob = new Blob([JSON.stringify(payload.request)], { type: 'application/json' })
      formData.append('requestDto', requestBlob)
      formData.append('request', requestBlob)

      // If a file is present, append it as media/file
      if (file) {
        formData.append('media', file)
        formData.append('file', file)
      } else if (mediaString) {
        // if only base64 string exists, send as media string field
        formData.append('media', mediaString)
      }

      // Debug: list FormData keys and file names
      try {
        const keys = []
        for (const k of formData.keys()) keys.push(k)
        console.debug('[Create] formData keys', keys)
        for (const v of formData.values()) {
          if (v instanceof File) console.debug('[Create] formData file', v.name, v.type, v.size)
        }
      } catch (e) { console.debug('[Create] formData logging failed', e) }

      const url = import.meta.env.VITE_API_BASE_URL + 'content/content/create'
      const res = await fetch(url, { method: 'POST', headers: { ...(auth ? { Authorization: auth } : {}) }, body: formData })
      const body = await res.json().catch(() => ({}))
      if (!res.ok) {
        const parsed = (await import('../lib/parseApiError.js')).parseApiErrorBody(body)
        throw new Error(parsed.message || 'Failed to create post')
      }

      setSuccess('Post created successfully! Redirecting...')
      setTimeout(() => navigate('/', { replace: true }), 900)
    } catch (err) {
      console.error('create post error', err)
      setError(err?.message || 'Failed to create post')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="relative min-h-screen overflow-hidden text-slate-50" style={{ backgroundImage: `linear-gradient(135deg, ${accent.start}, ${accent.mid}, ${accent.end})` }}>
      <div className="pointer-events-none absolute inset-0" style={{ backgroundImage: `radial-gradient(circle at 45% 20%, ${accent.glow}, transparent 38%), radial-gradient(circle at 78% 30%, rgba(255,255,255,0.06), transparent 30%), radial-gradient(circle at 55% 78%, rgba(255,255,255,0.04), transparent 33%)`, mixBlendMode: 'screen' }} />

      <div className="relative z-10 px-4 pb-20 pt-14 sm:px-8 lg:px-12 xl:px-16">
        <div className="md:grid md:grid-cols-[20rem_1fr] md:gap-8">
          <aside className="hidden md:block">
            <NavigationBar accent={accent} variant="inline" />
          </aside>

          <main className="min-w-0">
            <header className="flex flex-wrap items-center justify-between gap-4">
              <div>
                <p className="text-xs uppercase tracking-[0.25em] text-slate-300/80">Create</p>
                <h1 className="mt-2 text-3xl font-semibold text-white">Create a Post</h1>
                <p className="mt-2 text-sm text-slate-200/85 max-w-2xl">Reddit-inspired post composer with category, genre & tag controls.</p>
              </div>
            </header>

            <div className="mt-8 grid gap-6 md:grid-cols-[1fr_360px]">
              <div className="rounded-3xl border border-white/10 bg-white/5 p-6 backdrop-blur-2xl shadow-[0_20px_60px_rgba(0,0,0,0.35)]">
                {error && <div className="mb-4"><AlertBanner status="error" message={error} /></div>}
                {success && <div className="mb-4"><AlertBanner status="success" message={success} /></div>}

                <div className="space-y-4">
                  <div>
                    <label className="text-sm font-medium text-slate-200 mb-2 block">Media <span className="text-xs text-slate-400">(required)</span></label>

                    {!filePreview ? (
                      <div
                        onClick={() => fileRef.current?.click()}
                        onDragOver={(e) => { e.preventDefault(); setIsDragging(true) }}
                        onDragLeave={(e) => { e.preventDefault(); setIsDragging(false) }}
                        onDrop={(e) => {
                          e.preventDefault()
                          setIsDragging(false)
                          const f = e.dataTransfer?.files?.[0]
                          if (f) handleFile(f)
                        }}
                        role="button"
                        tabIndex={0}
                        className={`group relative flex flex-col items-center justify-center gap-6 rounded-xl border-2 ${isDragging ? 'border-white/40' : 'border-dashed border-white/10'} bg-black/20 h-64 cursor-pointer text-center p-6 transition`}
                      >
                        <ImageIcon className="h-16 w-16 text-slate-300" />
                        <div className="text-lg font-semibold text-slate-200">Drag photos and videos here</div>
                        <button
                          type="button"
                          onClick={(e) => { e.stopPropagation(); fileRef.current?.click() }}
                          onKeyDown={(e) => { e.stopPropagation(); if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); fileRef.current?.click() } }}
                          aria-label="Select media from computer"
                          className="mt-2 rounded-md px-4 py-2 text-sm font-semibold text-white hover:brightness-105 focus:outline-none focus:ring-2 focus:ring-white/20"
                          style={{ backgroundImage: `linear-gradient(90deg, ${accent.mid}, ${accent.strong})`, boxShadow: `0 8px 24px ${accent.glow}` }}
                        >
                          Select from computer
                        </button>
                        <input ref={fileRef} type="file" accept="image/*,video/*" onChange={(e) => handleFile(e.target.files?.[0] || null)} className="hidden" />
                      </div>
                    ) : (
                      <div className="mt-3 rounded-xl overflow-hidden border border-white/10 bg-black/20 relative">
                        {file && file.type.startsWith('image/') ? (
                          <img src={filePreview} alt="preview" className="w-full h-64 object-cover" />
                        ) : (
                          <video src={filePreview} className="w-full h-64 object-cover" controls />
                        )}
                        <button
                          onClick={removeFile}
                          aria-label="Remove media"
                          title="Remove"
                          className="absolute top-3 right-3 rounded-full w-8 h-8 flex items-center justify-center text-sm text-white focus:outline-none focus:ring-2 focus:ring-white/20"
                          style={{ backgroundImage: `linear-gradient(90deg, ${accent.mid}, ${accent.strong})`, boxShadow: `0 8px 24px ${accent.glow}` }}
                        >
                          <X className="h-5 w-5" />
                        </button>
                      </div>
                    )}
                  </div>

                  <div>
                    <label className="text-sm font-medium text-slate-200 mb-2 block">Title</label>
                    <input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="Title" className="w-full rounded-xl border border-white/15 bg-black/30 px-4 py-3 text-base text-slate-200 placeholder:text-slate-400 focus:border-white/30 focus:outline-none" />
                  </div>

                  <div>
                    <label className="text-sm font-medium text-slate-200 mb-2 block">Content</label>
                    <textarea value={bio} onChange={(e) => setBio(e.target.value)} rows={6} placeholder="Add context or a short description..." className="w-full rounded-xl border border-white/15 bg-black/30 px-4 py-3 text-base text-slate-200 placeholder:text-slate-400 focus:border-white/30 focus:outline-none" />
                  </div>

                  <div>
                    <label className="text-sm font-medium text-slate-200 mb-2 block">Anime Categories (choose at least 1)</label>
                    <div className="flex items-center justify-between mb-2">
                      <div className="text-xs text-slate-400">Selected: <strong className="text-white">{selectedCategories.length}</strong> / {MAX_SELECT}</div>
                      <div className="text-xs text-slate-400">Pick at least <strong>1</strong> and up to <strong>{MAX_SELECT}</strong> categories</div>
                    </div>
                    <div className="flex flex-wrap gap-3">
                      {CATEGORIES.map((c) => {
                        const sel = selectedCategories.includes(c)
                        const disabled = !sel && selectedCategories.length >= MAX_SELECT
                        return (
                          <button
                            key={c}
                            type="button"
                            onClick={() => toggleLimited(selectedCategories, setSelectedCategories, c)}
                            disabled={disabled}
                            aria-disabled={disabled}
                            className={`flex items-center gap-2 rounded-full px-3 py-1.5 text-sm font-semibold transition ${sel ? 'bg-gradient-to-r from-[var(--accent-mid)] to-[var(--accent-strong)] text-white shadow-glow' : 'bg-white/5 text-slate-200 hover:bg-white/10'} ${disabled ? 'opacity-50 cursor-not-allowed' : ''}`}
                            style={sel ? { backgroundImage: `linear-gradient(90deg, ${accent.mid}, ${accent.strong})`, boxShadow: `0 8px 24px ${accent.glow}` } : undefined}
                          >
                            {sel && <Check className="h-4 w-4" />}
                            <span>{humanize(c)}</span>
                          </button>
                        )
                      })}
                    </div>
                  </div>


                  <div>
                    <label className="text-sm font-medium text-slate-200 mb-2 block">Genres (choose at least 1)</label>
                    <div className="flex items-center justify-between mb-2">
                      <div className="text-xs text-slate-400">Selected: <strong className="text-white">{selectedGenres.length}</strong> / {MAX_SELECT}</div>
                      <div className="text-xs text-slate-400">Pick at least <strong>1</strong> and up to <strong>{MAX_SELECT}</strong> genres</div>
                    </div>
                    <div className="flex flex-wrap gap-3">
                      {GENRES.map((g) => {
                        const sel = selectedGenres.includes(g)
                        const disabled = !sel && selectedGenres.length >= MAX_SELECT
                        return (
                          <button
                            key={g}
                            type="button"
                            onClick={() => toggleLimited(selectedGenres, setSelectedGenres, g)}
                            disabled={disabled}
                            aria-disabled={disabled}
                            className={`flex items-center gap-2 rounded-full px-3 py-1.5 text-sm font-semibold transition ${sel ? 'bg-gradient-to-r from-[var(--accent-mid)] to-[var(--accent-strong)] text-white shadow-glow' : 'bg-white/5 text-slate-200 hover:bg-white/10'} ${disabled ? 'opacity-50 cursor-not-allowed' : ''}`}
                            style={sel ? { backgroundImage: `linear-gradient(90deg, ${accent.mid}, ${accent.strong})`, boxShadow: `0 8px 24px ${accent.glow}` } : undefined}
                          >
                            {sel && <Check className="h-4 w-4" />}
                            <span>{humanize(g)}</span>
                          </button>
                        )
                      })}
                    </div>
                  </div>
                  <div>
                    <label className="text-sm font-medium text-slate-200 mb-2 block">Tags</label>
                    <div className="flex items-center gap-2">
                      <input value={tagInput} onChange={(e) => setTagInput(e.target.value)} onKeyDown={handleTagKey} placeholder="Add a tag and press Enter (use underscores, not spaces)" className="flex-1 rounded-xl border border-white/15 bg-black/30 px-4 py-3 text-base text-slate-200 placeholder:text-slate-400 focus:border-white/30 focus:outline-none" />
                      <button onClick={() => addTag(tagInput)} type="button" className="rounded-xl border border-white/10 bg-white/5 px-4 py-2 text-sm font-semibold text-white">Add</button>
                    </div>
                    <p className="mt-2 text-xs text-slate-400">Tags will be sent without the leading '#', and spaces will be converted to underscores.</p>
                    <div className="mt-2 flex flex-wrap gap-2">
                      {tags.map((t) => <div key={t} className="flex items-center gap-2 rounded-full bg-white/5 px-3 py-1 text-sm text-slate-200"><span>#{t}</span><button onClick={() => removeTag(t)} className="text-slate-400 hover:text-white">×</button></div>)}
                    </div>
                  </div>
                  <div className="mt-6 flex items-center justify-between">
                    <button onClick={() => { setTitle(''); setBio(''); setSelectedGenres([]); setSelectedCategories([]); setTags([]); removeFile(); }} className="rounded-xl border border-white/10 bg-white/5 px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-white/10">Reset</button>
                    <button onClick={handleSubmit} disabled={!canSubmit || loading} className="rounded-xl px-5 py-2.5 text-sm font-semibold text-white transition disabled:opacity-50" style={{ backgroundImage: `linear-gradient(90deg, ${accent.mid}, ${accent.strong})`, boxShadow: `0 8px 24px ${accent.glow}` }}>{loading ? 'Posting...' : 'Post'}</button>
                  </div>
                </div>
              </div>

              <aside className="hidden md:block rounded-3xl border border-white/10 bg-white/5 p-6 backdrop-blur-2xl shadow-[0_20px_60px_rgba(0,0,0,0.35)]">
                <h3 className="text-lg font-semibold text-white mb-4">Preview</h3>
                <div className="space-y-3">
                  <div className="text-slate-400 text-sm">Title</div>
                  <div className="text-white font-semibold text-lg">{title || 'Post title'}</div>

                  <div className="mt-3 text-slate-400 text-sm">Content</div>
                  <div className="text-slate-200 text-sm">{bio || 'Post content preview...'}</div>

                  <div className="mt-3 text-slate-400 text-sm">Categories / Genres</div>
                  <div className="flex flex-wrap gap-2 mt-2">
                    {selectedCategories.map((c) => <div key={c} className="rounded-full bg-white/5 px-3 py-1 text-sm text-slate-200">{humanize(c)}</div>)}
                    {selectedGenres.map((g) => <div key={g} className="rounded-full bg-white/5 px-3 py-1 text-sm text-slate-200">{humanize(g)}</div>)}
                  </div>

                  <div className="mt-3 text-slate-400 text-sm">Tags</div>
                  <div className="flex flex-wrap gap-2 mt-2">
                    {tags.map((t) => <div key={t} className="rounded-full bg-white/5 px-3 py-1 text-sm text-slate-200">#{t}</div>)}
                  </div>

                  <div className="mt-3 text-slate-400 text-sm">Media Preview</div>
                  <div className="mt-2">
                    {filePreview ? (
                      file && file.type.startsWith('image/') ? <img src={filePreview} alt="preview small" className="w-full h-36 object-cover rounded-lg" /> : <video src={filePreview} className="w-full h-36 object-cover rounded-lg" controls />
                    ) : (
                      <div className="flex items-center gap-3 rounded-xl border border-white/10 bg-black/20 p-4 text-slate-300"> <ImageIcon className="h-5 w-5" /> <span>No media selected</span></div>
                    )}
                  </div>
                </div>
              </aside>
            </div>

          </main>
        </div>
      </div>

      <NavigationBar accent={accent} variant="mobile" />
    </div>
  )
}

export default Create

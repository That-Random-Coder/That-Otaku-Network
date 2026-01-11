import { useEffect, useMemo, useState } from 'react'
import { Search } from 'lucide-react'
import accentOptions from '../theme/accentOptions.js'
import { getSavedAccentKey } from '../theme/accentStorage.js'
import NavigationBar from '../components/NavigationBar.jsx'
import AlertBanner from '../components/AlertBanner.jsx' 

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

function Friends() {
  const [accentKey] = useState(() => getSavedAccentKey('crimson-night'))
  const accent = useMemo(() => accentOptions.find((opt) => opt.key === accentKey)?.colors || accentOptions[0].colors, [accentKey])
  const [searchTerm, setSearchTerm] = useState('')
  const [results, setResults] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [searchMode, setSearchMode] = useState('users')
  const [isWaiting, setIsWaiting] = useState(false)
  const pageNumber = 0

  useEffect(() => {
    if (!searchTerm.trim()) {
      setResults([])
      setError('')
      setIsWaiting(false)
      return
    }

    const controller = new AbortController()
    const token = getBearerToken()
    if (!token) {
      setError('Missing access token. Please log in again.')
      setResults([])
      setIsWaiting(false)
      return () => controller.abort()
    }

    // Debounce: wait 1 second after the last change before firing the fetch
    setIsWaiting(true)
    const timer = setTimeout(() => {
      setIsWaiting(false)
      setLoading(true)
      setError('')
      const url = searchMode === 'users'
        ? `${import.meta.env.VITE_API_BASE_URL}search/user/get?page=${pageNumber}&keyword=${encodeURIComponent(searchTerm.trim())}`
        : `${import.meta.env.VITE_API_BASE_URL}search/group/get?page=${pageNumber}&keyword=${encodeURIComponent(searchTerm.trim())}`

      fetch(url, {
        method: 'GET',
        headers: { Authorization: token },
        signal: controller.signal,
      })
        .then(async (res) => {
          if (!res.ok) {
            const maybe = await res.json().catch(() => ({}))
            const parsed = (await import('../lib/parseApiError.js')).parseApiErrorBody(maybe)
            throw new Error(parsed.message || `Unable to fetch ${searchMode === 'users' ? 'users' : 'groups'} right now.`)
          }
          return res.json()
        })
        .then((data) => {
          const arr = Array.isArray(data?.content) ? data.content : (Array.isArray(data?.data) ? data.data : (Array.isArray(data) ? data : []))
          if (searchMode === 'users') {
            const mapped = arr.map((item, idx) => ({
              id: item.id || item.userId || idx,
              displayName: item.displayName || 'Unknown Otaku',
              username: item.username || 'unknown',
              bio: item.bio || '',
              avatar: item.avatar || item.imageUrl || '',
            }))
            setResults(mapped)
          } else {
            const mapped = arr.map((item, idx) => ({
              id: item.id || item.groupId || idx,
              name: item.name || item.groupName || 'Group',
              description: item.description || item.bio || item.groupBio || '',
              members: item.members || item.memberCount || item.totalMembers || 1,
              activity: item.activity || item.activityFrequency || '',
              image: item.profileImage || item.icon || item.image || item.bgImage || item.imageUrl || '',
            }))
            setResults(mapped)
          }
        })
        .catch((err) => {
          if (err.name === 'AbortError') return
          setError(err.message || `Unable to fetch ${searchMode === 'users' ? 'users' : 'groups'} right now.`)
          setResults([])
        })
        .finally(() => setLoading(false))
    }, 1000)

    return () => {
      clearTimeout(timer)
      controller.abort()
    }
  }, [searchTerm, pageNumber, searchMode])

  return (
    <div
      className="relative min-h-screen overflow-visible text-slate-50"
      style={{ backgroundImage: `linear-gradient(135deg, ${accent.start}, ${accent.mid}, ${accent.end})` }}
    >
      <div
        className="pointer-events-none absolute inset-0"
        style={{
          backgroundImage: `radial-gradient(circle at 45% 20%, ${accent.glow}, transparent 38%), radial-gradient(circle at 78% 30%, rgba(255,255,255,0.06), transparent 30%), radial-gradient(circle at 55% 78%, rgba(255,255,255,0.04), transparent 33%)`,
          mixBlendMode: 'screen',
        }}
      />
      <div className="relative z-10 px-4 pb-20 pt-14 sm:px-8 lg:px-12 xl:px-16">
        <div className="md:grid md:grid-cols-[20rem_1fr] md:gap-8">
          <div className="hidden md:block">
            <NavigationBar accent={accent} variant="inline" />
          </div>
          <div className="min-w-0">
            <header className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <p className="text-xs uppercase tracking-[0.25em] text-slate-300/80">ThatOtakuNetwork</p>
            <h1 className="mt-2 text-3xl font-semibold text-white">Search</h1>
            <p className="mt-2 text-sm text-slate-200/85 max-w-2xl">Coordinate watch parties, chats, and squads. Fast access to your circles.</p>
          </div>
        </header>

        <div className="mt-8 space-y-6">
          <div className="flex flex-col gap-3 rounded-3xl border border-white/10 bg-white/5 p-5 shadow-[0_18px_60px_rgba(0,0,0,0.35)] backdrop-blur-2xl">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="rounded-full bg-white/5 p-1 flex">
                  <button type="button" onClick={() => setSearchMode('users')} className={`px-3 py-1 rounded-full text-sm font-semibold ${searchMode === 'users' ? 'bg-white/10 text-white' : 'text-slate-300'}`}>Users</button>
                  <button type="button" onClick={() => setSearchMode('groups')} className={`px-3 py-1 rounded-full text-sm font-semibold ${searchMode === 'groups' ? 'bg-white/10 text-white' : 'text-slate-300'}`}>Groups</button>
                </div>
                <span className="text-sm font-semibold text-white/90">{searchMode === 'users' ? 'Find friends' : 'Find groups'}</span>
              </div>
              {/* {isWaiting ? <span className="text-xs text-slate-200/75">Searching {searchMode === 'users' ? 'friends' : 'groups'}...</span> : loading && <span className="text-xs text-slate-200/75">Searching...</span>} */}
            </div>

            <div className="flex items-center gap-3 rounded-2xl border border-white/15 bg-black/30 px-3 py-2">
              <Search className="h-5 w-5 text-slate-200/70" />
              <input
                type="search"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                placeholder={searchMode === 'users' ? 'Connect to an otaku' : 'Search groups'}
                className="w-full bg-transparent text-sm text-slate-100 placeholder:text-slate-400 focus:outline-none"
              />
            </div>

          </div>

          <div className="rounded-3xl border border-white/10 bg-white/5 p-5 shadow-[0_18px_60px_rgba(0,0,0,0.35)] backdrop-blur-2xl">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-semibold text-white">{searchMode === 'users' ? 'Suggested otaku' : 'Groups'}</h3>
              {isWaiting ? <span className="text-xs text-slate-200/75">Searching {searchMode === 'users' ? 'friends' : 'groups'}...</span> : loading && <span className="text-xs text-slate-200/75">Searching...</span>}
            </div>
            {error && (
              <div className="mt-4">
                <AlertBanner status="error" message={error} />
              </div>
            )}
            {!error && !loading && searchTerm.trim() && results.length === 0 && <p className="mt-2 text-sm text-slate-200/85">{searchMode === 'users' ? 'No users found yet.' : 'No groups found yet.'}</p>} 
            {!searchTerm.trim() && <p className="mt-2 text-sm text-slate-200/80">{searchMode === 'users' ? 'Start typing to discover users.' : 'Start typing to find groups.'}</p>}

            <div className="mt-4 grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
              {searchMode === 'users' ? (
                results.map((user) => (
                  <div
                    key={user.id}
                    className="group relative overflow-hidden rounded-2xl border border-white/10 bg-white/5 p-4 transition hover:border-white/20 hover:bg-white/8 cursor-pointer"
                    style={{ boxShadow: `0 10px 28px rgba(0,0,0,0.35), 0 0 16px ${accent.glow}` }}
                    onClick={() => {
                      window.location.href = `/friend-profile/${encodeURIComponent(user.id)}`
                    }}
                  >
                    <div className="flex items-center gap-3">
                      <div className="h-12 w-12 overflow-hidden rounded-full border border-white/15 bg-black/40 flex items-center justify-center text-white/80">
                        {user.avatar ? (
                          <img src={user.avatar} alt={user.displayName} className="h-full w-full object-cover" />
                        ) : (
                          <span className="text-sm font-semibold">{user.username?.slice(0, 2)?.toUpperCase() || 'OT'}</span>
                        )}
                      </div>
                      <div className="flex flex-col">
                        <span className="text-sm font-semibold text-white">{user.displayName}</span>
                        <span className="text-xs text-slate-300/80">@{user.username}</span>
                      </div>
                    </div>
                    {user.bio && <p className="mt-3 line-clamp-2 text-sm text-slate-200/85">{user.bio}</p>}
                    <div className="mt-4 flex items-center justify-between text-xs text-slate-200/80">
                      <span className="rounded-full bg-white/10 px-3 py-1">Otaku</span>
                    </div>
                  </div>
                ))
              ) : (
                results.map((group) => (
                  <div
                    key={group.id}
                    className="group relative overflow-hidden rounded-2xl border border-white/10 bg-white/5 p-4 transition hover:border-white/20 hover:bg-white/8 cursor-pointer"
                    style={{ boxShadow: `0 10px 28px rgba(0,0,0,0.35), 0 0 16px ${accent.glow}` }}
                    onClick={() => { window.location.href = `/group/${encodeURIComponent(group.id)}` }}
                  >
                    <div className="flex items-center gap-3">
                      <div className="h-12 w-12 overflow-hidden rounded-xl border border-white/15 bg-black/40 flex items-center justify-center text-white/80">
                        {group.image ? (
                          <img src={group.image} alt={group.name} className="h-full w-full object-cover" />
                        ) : (
                          <span className="text-sm font-semibold">{(group.name || '').slice(0,2).toUpperCase() || 'GR'}</span>
                        )}
                      </div>
                      <div className="flex flex-col">
                        <span className="text-sm font-semibold text-white">{group.name}</span>
                      </div>
                    </div>
                    {group.description && <p className="mt-3 line-clamp-2 text-sm text-slate-200/85">{group.description}</p>}
                    <div className="mt-4 flex items-center justify-between text-xs text-slate-200/80">
                      <span className="rounded-full bg-white/10 px-3 py-1">Group</span>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
          </div>
        </div>
      </div>
      <NavigationBar accent={accent} variant="mobile" />
    </div>
  )
}

export default Friends

import { useEffect, useState, useRef } from 'react'
import { AnimatePresence, motion } from 'framer-motion'
import { X } from 'lucide-react'
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

const LOCAL_KEYS = {
  followers: (uid) => `followers_meta_${uid}`,
  following: (uid) => `following_meta_${uid}`,
}

const FollowListModal = ({ isOpen, onClose, type = 'followers', accent, motionSafe }) => {
  const [query, setQuery] = useState('')
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const abortRef = useRef(null)
  const sentinelRef = useRef(null)
  const [infiniteEnabled, setInfiniteEnabled] = useState(false)


  useEffect(() => {
    if (!isOpen) return
    if (!infiniteEnabled) return
    const observer = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (!entry.isIntersecting) return
        if (loading) return

        if (totalPages === 0 || page < totalPages - 1) {
          fetchPage(page + 1)
        }
      })
    }, { root: null, rootMargin: '0px', threshold: 0.4 })

    const el = sentinelRef.current
    if (el) observer.observe(el)
    return () => observer.disconnect()

  }, [isOpen, page, totalPages, loading, infiniteEnabled])

  useEffect(() => {
    if (!isOpen) return
    setItems([])
    setPage(0)
    setTotalPages(0)
    setError('')

    fetchPage(0)
    return () => {
      if (abortRef.current) abortRef.current.abort()
    }

  }, [isOpen, type])

  const saveMetaToLocal = (uid, meta) => {
    try {
      const key = LOCAL_KEYS[type](uid)
      localStorage.setItem(key, JSON.stringify(meta))
    } catch (e) {

    }
  }

  const fetchPage = async (p) => {
    setLoading(true)
    setError('')
    if (abortRef.current) abortRef.current.abort()
    const controller = new AbortController()
    abortRef.current = controller

    const cookieUserId = getCookie('userId') || getCookie('id')
    if (!cookieUserId) {
      setError('User ID not found in cookies.')
      setLoading(false)
      return
    }

    const endpoint = type === 'following'
      ? `${import.meta.env.VITE_API_BASE_URL}profile/user/following/list?id=${encodeURIComponent(cookieUserId)}&page=${p}`
      : `${import.meta.env.VITE_API_BASE_URL}profile/user/follower/list?id=${encodeURIComponent(cookieUserId)}&page=${p}`

    const token = getBearerToken()
    try {
      const res = await fetch(endpoint, { method: 'GET', headers: { Authorization: token }, signal: controller.signal })
      if (!res.ok) {
        const maybe = await res.json().catch(() => ({}))
        const parsed = (await import('../lib/parseApiError.js')).parseApiErrorBody(maybe)
        throw new Error(parsed.message || 'Failed to fetch list.')
      }
      const data = await res.json()
      const arr = Array.isArray(data?.content) ? data.content : (Array.isArray(data?.data) ? data.data : (Array.isArray(data) ? data : []))
      const mapped = arr.map((it, idx) => ({
        id: it.id || it.userId || it.userId || idx,
        displayName: it.displayName || it.DisplayName || it.displayname || 'Unknown Otaku',
        username: it.username || it.userName || it.login || 'unknown',
        avatar: it.avatar || it.profileImg || it.imageUrl || '',
      }))


      const meta = {
        numberOfElements: data?.numberOfElements ?? data?.content?.length ?? mapped.length,
        pageSize: data?.pageSize ?? data?.size ?? mapped.length,
        size: data?.size ?? data?.pageSize ?? mapped.length,
        totalPages: data?.totalPages ?? data?.totalPages ?? (typeof data?.totalPages === 'number' ? data.totalPages : (data?.totalPages ?? 0)),
      }
      setTotalPages(meta.totalPages || 0)
      saveMetaToLocal(cookieUserId, meta)
      setPage(p)


      setItems((prev) => {
        const newArr = p === 0 ? mapped : [...prev, ...mapped]
        const knownTotal = data?.numberOfElements ?? data?.totalElements ?? ((meta.pageSize && meta.totalPages) ? (meta.pageSize * meta.totalPages) : undefined)
        const shouldEnable = (knownTotal !== undefined && knownTotal > 15) || newArr.length > 15
        setInfiniteEnabled(Boolean(shouldEnable))
        return newArr
      })
    } catch (err) {
      if (err.name === 'AbortError') return
      setError(err.message || 'Failed to fetch list.')
    } finally {
      setLoading(false)
    }
  }

  if (!isOpen) return null

  const title = type === 'following' ? 'Following' : 'Followers'

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
            backgroundColor: accent?.surface || 'rgba(0,0,0,0.5)',
            boxShadow: `0 40px 100px rgba(0,0,0,0.6), 0 0 50px ${accent?.glow || 'transparent'}`,
          }}
          onClick={(e) => e.stopPropagation()}
        >
          <div className="pointer-events-none absolute -left-24 -top-24 h-72 w-72 rounded-full bg-purple-500/20 blur-3xl" />
          <div className="pointer-events-none absolute -bottom-24 -right-24 h-80 w-80 rounded-full bg-indigo-500/25 blur-3xl" />


          <div className="relative p-6 border-b border-white/10">
            <div className="flex items-center justify-between mb-4">
              <div>
                <h2 className="text-2xl font-semibold text-white">{title}</h2>
                <p className="text-sm text-slate-300/80 mt-1">{title} for this account</p>
              </div>
              <button onClick={onClose} className="rounded-full p-2 text-slate-300 hover:bg-white/10 hover:text-white transition">
                <X className="h-6 w-6" />
              </button>
            </div>

            <div className="relative">
              <input
                type="search"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder={`Filter ${title.toLowerCase()}...`}
                className="w-full rounded-xl border border-white/15 bg-black/30 pl-4 pr-4 py-3 text-base text-slate-200 placeholder:text-slate-400 focus:border-white/30 focus:outline-none focus:ring-2 focus:ring-white/20"
              />
            </div>
          </div>


          <div className="relative flex-1 overflow-y-auto p-6">
            {loading && <div className="text-center text-slate-300 py-8">Loading...</div>}
            {error && (
              <div className="mb-4 px-4">
                <AlertBanner status="error" message={error} />
              </div>
            )}
            {!loading && !error && (
              <div className="grid gap-4 grid-cols-1 sm:grid-cols-2">
                {items.filter(it => (query.trim() ? (`${it.displayName} ${it.username}`.toLowerCase().includes(query.trim().toLowerCase())) : true)).map((user) => (
                  <div key={user.id} className="group relative overflow-hidden rounded-2xl border border-white/10 bg-white/5 p-4 transition hover:border-white/20 hover:bg-white/8">
                    <div className="flex items-center gap-3">
                      <div className="h-12 w-12 overflow-hidden rounded-full border border-white/15 bg-black/40 flex items-center justify-center text-white/80">
                        {user.avatar ? (
                          <img src={user.avatar} alt={user.displayName} className="h-full w-full object-cover" />
                        ) : (
                          <span className="text-sm font-semibold">{user.username?.slice(0,2)?.toUpperCase() || 'OT'}</span>
                        )}
                      </div>
                      <div className="flex flex-col">
                        <span className="text-sm font-semibold text-white">{user.displayName}</span>
                        <span className="text-xs text-slate-300/80">@{user.username}</span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}

            {!loading && !error && items.length === 0 && <div className="text-center text-slate-400 py-8">No {title.toLowerCase()} found.</div>}


            <div ref={sentinelRef} className="h-6" />
            {loading && page > 0 && <div className="text-center text-slate-300 py-4">Loading more…</div>}

          </div>


          <div className="relative p-6 border-t border-white/10 flex items-center justify-between gap-4">
            <p className="text-sm text-slate-300/80" style={{letterSpacing: '.2rem'}}>THATOTAKUNETWORK</p>
            <div className="flex gap-3">
              <button onClick={onClose} className="rounded-xl border border-white/10 bg-white/5 px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-white/10">Close</button>
            </div>
          </div>
        </motion.div>
      </motion.div>
    </AnimatePresence>
  )
}

export default FollowListModal

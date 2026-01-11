import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ThumbsUp, MessageSquare, Share2 } from 'lucide-react'
import AlertBanner from './AlertBanner.jsx'

const getCookie = (name) => {
  if (typeof document === 'undefined') return ''
  const match = document.cookie.match(new RegExp(`(?:^|; )${name}=([^;]*)`))
  return match ? decodeURIComponent(match[1]) : ''
}


const Spinner = ({ size = 40, accent = {} }) => (
  <div style={{ width: size, height: size }} className="flex items-center justify-center">
    <svg viewBox="0 0 50 50" className="animate-spin" style={{ width: size, height: size }}>
      <defs>
        <linearGradient id="g" x1="0%" x2="100%">
          <stop offset="0%" stopColor={accent.start || '#8b5cf6'} />
          <stop offset="100%" stopColor={accent.end || '#06b6d4'} />
        </linearGradient>
      </defs>
      <circle cx="25" cy="25" r="20" stroke="rgba(255,255,255,0.12)" strokeWidth="6" fill="none" />
      <path d="M45 25a20 20 0 0 0-34.64-13" stroke="url(#g)" strokeWidth="6" strokeLinecap="round" fill="none" />
    </svg>
  </div>
)

const BATCH_SIZE = 12

export default function UserPostsGrid({ profileUserId, accent = {} }) {
  const currentUserId = getCookie('id') || getCookie('userId') || ''
  const [posts, setPosts] = useState([])
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(false)
  const [hasMore, setHasMore] = useState(true)
  const [error, setError] = useState('')
  const sentinelRef = useRef(null)
  const navigate = useNavigate()

  useEffect(() => {

    setPosts([])
    setPage(0)
    setHasMore(true)
    setError('')
  }, [profileUserId])

  useEffect(() => {
    const controller = new AbortController()
    const fetchPage = async (p) => {
      setLoading(true)
      setError('')
      try {
        const url = `${import.meta.env.VITE_API_BASE_URL}content/content/user?userId=${encodeURIComponent(profileUserId || '')}&currentUserId=${encodeURIComponent(currentUserId || '')}&page=${p}&includeMedia=true&size=${BATCH_SIZE}`

        const tokenRaw = getCookie('AccessToken') || ''
        const auth = tokenRaw ? (tokenRaw.toLowerCase().startsWith('bearer ') ? tokenRaw : `Bearer ${tokenRaw}`) : ''
        const headers = auth ? { Authorization: auth, AccessToken: tokenRaw } : {}

        try {
          console.log('[UserPostsGrid] fetching URL:', url)
          console.log('[UserPostsGrid] authPresent:', !!auth, 'headers:', headers)
        } catch (e) {}
        const res = await fetch(url, { signal: controller.signal, headers })
        const body = await res.json().catch(() => ({}))
        console.log('[UserPostsGrid] fetch result status:', res.status)
        if (!res.ok) {
          const parsed = (await import('../lib/parseApiError.js')).parseApiErrorBody(body)
          throw new Error(parsed.message || 'Failed to fetch posts')
        }
        const arr = Array.isArray(body?.content) ? body.content : []

        setPosts((prev) => ([ ...prev, ...arr ]))
        const totalPages = Number(body?.totalPages || body?.pageable?.totalPages || 0)
        const number = Number(body?.number ?? body?.pageable?.pageNumber ?? p)
        console.log('[UserPostsGrid] fetched items:', arr.length, 'page:', number, 'totalPages:', totalPages)

        const more = totalPages > 0 ? (number + 1 < Math.max(1, totalPages)) : (arr.length >= BATCH_SIZE)
        setHasMore(more)
      } catch (e) {
        if (e.name !== 'AbortError') {
          console.error('fetch user posts error', e)
          setError(e.message || 'Failed to load posts')
        }
      } finally {
        setLoading(false)
      }
    }

    fetchPage(page)
    return () => controller.abort()
  }, [page, profileUserId, currentUserId])

  useEffect(() => {
    if (!sentinelRef.current) return
    if (!hasMore) return
    const obs = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting && !loading && hasMore) {

          if (posts.length >= (page + 1) * BATCH_SIZE) {
            setPage((p) => p + 1)
          }
        }
      })
    }, { rootMargin: '600px' })
    obs.observe(sentinelRef.current)
    return () => obs.disconnect()
  }, [sentinelRef.current, loading, hasMore, posts, page])


  const RemoteMedia = ({ src, mediaType = '' }) => {
    const [objUrl, setObjUrl] = useState(null)
    const [loaded, setLoaded] = useState(false)

    useEffect(() => {
      if (!src) return
      let canceled = false
      let createdObjectUrl = null
      const controller = new AbortController()

      const isLikelyBase64 = (s) => {
        if (typeof s !== 'string') return false
        const trimmed = s.replace(/\s+/g, '')
        if (trimmed.length < 200) return false
        return /^[A-Za-z0-9+/=]+$/.test(trimmed)
      }

      const base64ToObjectUrl = (b64, mime = 'image/jpeg') => {
        try {
          const binary = atob(b64)
          const len = binary.length
          const bytes = new Uint8Array(len)
          for (let i = 0; i < len; i++) bytes[i] = binary.charCodeAt(i)
          const blob = new Blob([bytes], { type: mime })
          return URL.createObjectURL(blob)
        } catch (e) { console.warn('[RemoteMedia] base64->blob failed', e); return null }
      }

      const dataUriToObjectUrl = async (dataUri) => {
        try {
          const res = await fetch(dataUri)
          if (!res.ok) throw new Error('Failed to convert data URI')
          const blob = await res.blob()
          return URL.createObjectURL(blob)
        } catch (e) { console.warn('[RemoteMedia] data URI->blob failed', e); return null }
      }

      (async () => {
        try {
          if (src.startsWith('blob:')) { if (!canceled) setObjUrl(src); return }

          if (src.startsWith('data:')) {
            const url = await dataUriToObjectUrl(src)
            if (url && !canceled) { createdObjectUrl = url; setObjUrl(url) }
            return
          }

          if (src.startsWith('http://') || src.startsWith('https://')) {
            const tokenRaw = getCookie('AccessToken') || ''
            const auth = tokenRaw ? (tokenRaw.toLowerCase().startsWith('bearer ') ? tokenRaw : `Bearer ${tokenRaw}`) : ''
            const headers = auth ? { Authorization: auth } : {}
            const res = await fetch(src, { signal: controller.signal, headers })
            if (!res.ok) throw new Error('Failed to fetch media')
            const blob = await res.blob()
            const url = URL.createObjectURL(blob)
            if (!canceled) { createdObjectUrl = url; setObjUrl(url) }
            return
          }

          if (isLikelyBase64(src)) {
            const mime = (mediaType && mediaType.includes('/')) ? mediaType : 'image/jpeg'
            const url = base64ToObjectUrl(src, mime)
            if (url && !canceled) { createdObjectUrl = url; setObjUrl(url) }
            return
          }

          if (!canceled) setObjUrl(src)
        } catch (e) { console.warn('[RemoteMedia] failed to resolve src', src, e) }
      })()

      return () => { canceled = true; controller.abort(); if (createdObjectUrl) URL.revokeObjectURL(createdObjectUrl) }
    }, [src, mediaType])

    if (!objUrl) return <div className="w-full h-full bg-white/5 flex items-center justify-center"><div className="animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-white/70" /></div>

    return mediaType.includes('video') ? (
      <video src={objUrl} className="w-full h-full object-cover" muted playsInline preload="metadata" onLoadedData={() => setLoaded(true)} onError={() => setLoaded(false)} />
    ) : (
      <img src={objUrl} className="w-full h-full object-cover" alt="Post media" loading="lazy" onLoad={() => setLoaded(true)} onError={() => setLoaded(false)} />
    )
  }

  const renderMedia = (item) => {
    const media = item.media || ''
    const mediaType = String(item.mediaType || '').toLowerCase()
    if (!media) return null
    const src = media.startsWith('data:') ? media : (mediaType.includes('image') ? `data:${mediaType || 'image/jpeg'};base64,${media}` : (mediaType.includes('video') ? `data:${mediaType};base64,${media}` : media))
    return <RemoteMedia src={src} mediaType={mediaType} />
  }

  return (
    <div className="mt-8">
      <h3 className="text-lg font-semibold text-white mb-3">Posts</h3>
      {error && <div className="mb-4"><AlertBanner status="error" message={error} /></div>}

      {!loading && posts.length === 0 && (
        <div className="w-full max-w-2xl mx-auto rounded-3xl border border-white/10 bg-white/5 p-6 text-center">
          <p className="text-lg text-slate-200">No posts to show</p>
        </div>
      )}

      <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-3 gap-2">
        {posts.map((item) => (
          <div
            key={item.id}
            role="button"
            tabIndex={0}
            onClick={() => navigate(`/post/${item.id}`)}
            onKeyDown={(e) => { if (e.key === 'Enter') navigate(`/post/${item.id}`) }}
            className="group relative overflow-hidden rounded-lg bg-black/10 cursor-pointer focus:outline-none focus:ring-2 focus:ring-white/20"
            style={{ aspectRatio: '1/1' }}
          >
            {renderMedia(item)}
            <div className="absolute inset-0 pointer-events-none">

              <div className="absolute inset-0 transition group-hover:bg-black/30" />


              <div className="absolute inset-0 flex items-center justify-center pointer-events-none z-20">
                <div className="flex items-center gap-6 opacity-0 transform scale-95 transition-all duration-150 group-hover:opacity-100 group-hover:scale-100 pointer-events-auto">
                  <div className="flex flex-col items-center gap-2">
                    <button
                      type="button"
                      onClick={(e) => { e.stopPropagation(); navigate(`/post/${item.id}`) }}
                      aria-label="View post and like"
                      className="rounded-full bg-white/10 hover:bg-white/20 p-3 flex items-center justify-center shadow-lg focus:outline-none focus:ring-2 focus:ring-white/20"
                    >
                      <ThumbsUp className="h-6 w-6 text-white" />
                    </button>
                    <span className="text-base font-semibold text-white pointer-events-none">{item.likeCount || 0}</span>
                  </div>

                  <div className="flex flex-col items-center gap-2">
                    <button
                      type="button"
                      onClick={(e) => { e.stopPropagation(); navigate(`/post/${item.id}`) }}
                      aria-label="View comments"
                      className="rounded-full bg-white/10 hover:bg-white/20 p-3 flex items-center justify-center shadow-lg focus:outline-none focus:ring-2 focus:ring-white/20"
                    >
                      <MessageSquare className="h-6 w-6 text-white" />
                    </button>
                    <span className="text-base font-semibold text-white pointer-events-none">{item.commentCount || 0}</span>
                  </div>
                </div>
              </div>


              <div className="absolute bottom-2 right-2 opacity-0 transition group-hover:opacity-100 pointer-events-auto z-20 flex items-center gap-2">
                <span className="text-base font-semibold text-white pointer-events-none">{item.shareCount || 0}</span>
                <button type="button" onClick={(e) => { e.stopPropagation();  try { navigator.clipboard?.writeText(window.location.origin + `/post/${item.id}`) } catch (err) {} }} className="rounded-md bg-white/8 p-2 hover:bg-white/12 focus:outline-none focus:ring-2 focus:ring-white/20">
                  <Share2 className="h-4 w-4 text-white" />
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>

      <div ref={sentinelRef} className="mt-6 flex items-center justify-center">
        {loading && <Spinner size={44} accent={accent} />}
      </div>
    </div>
  )
}
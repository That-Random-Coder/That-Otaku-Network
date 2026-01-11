import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { MessageCircle, ThumbsDown, ThumbsUp, Bookmark, Sparkles, Share2 } from 'lucide-react'
import AlertBanner from './AlertBanner.jsx'

const getCookie = (name) => {
  if (typeof document === 'undefined') return ''
  const match = document.cookie.match(new RegExp(`(?:^|; )${name}=([^;]*)`))
  return match ? decodeURIComponent(match[1]) : ''
}

// Simple accent-aware spinner
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

export default function AllGroupsPostsGrid({ userIdProp, accent = {} }) {
  const userId = userIdProp || getCookie('userId') || getCookie('id') || ''
  const [posts, setPosts] = useState([])
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(false)
  const [hasMore, setHasMore] = useState(true)
  const [error, setError] = useState('')
  const sentinelRef = useRef(null)
  const navigate = useNavigate()

  useEffect(() => {
    // reset when user changes
    setPosts([])
    setPage(0)
    setHasMore(true)
    setError('')
  }, [userId])

  useEffect(() => {
    const controller = new AbortController()
    const fetchPage = async (p) => {
      setLoading(true)
      setError('')
      try {
        const url = `${import.meta.env.VITE_API_BASE_URL}content/content/recommendation/all-groups-members?userId=${encodeURIComponent(userId || '')}&page=${p}&size=${BATCH_SIZE}`
        // Use AccessToken cookie explicitly as Bearer token
        const tokenRaw = getCookie('AccessToken') || ''
        const auth = tokenRaw ? (tokenRaw.toLowerCase().startsWith('bearer ') ? tokenRaw : `Bearer ${tokenRaw}`) : ''
        const headers = auth ? { Authorization: auth, AccessToken: tokenRaw } : {}
        try { console.log('[AllGroupsPostsGrid] fetching URL:', url); console.log('[AllGroupsPostsGrid] authPresent:', !!auth, 'headers:', headers) } catch (e) {}
        const res = await fetch(url, { signal: controller.signal, headers })
        const body = await res.json().catch(() => ({}))
        console.log('[AllGroupsPostsGrid] fetch result status:', res.status)
        if (!res.ok) {
          const parsed = (await import('../lib/parseApiError.js')).parseApiErrorBody(body)
          throw new Error(parsed.message || 'Failed to fetch posts')
        }
        const arr = Array.isArray(body?.content) ? body.content : []
        // map to the same post shape used by Home
        const mapFeedItem = (it) => ({
          id: it.contentId || it.id || String(Math.random()).slice(2),
          authorId: it.userId || it.userID || it.ownerId || it.creatorId || (it.user && it.user.id) || '',
          author: it.displayName || it.username || it.userName || 'Unknown',
          handle: it.username ? `@${it.username}` : (it.userName ? `@${it.userName}` : ''),
          anime: '',
          title: it.contentTitle || it.title || '',
          description: it.description || it.bio || '',
          image: it.image || it.media || it.imageUrl || '',
          media: it.media ?? it.image ?? it.mediaUrl ?? it.imageUrl,
          mediaType: it.mediaType || it.type || '',
          likes: Number(it.likeCount || 0),
          dislikes: Number(it.dislikeCount || 0),
          comments: Number(it.commentCount || it.comments || 0),
          isFaved: Boolean(it.isFaved || it.faved || false),
          time: it.timeOfCreation || it.time || ''
        })
        const mapped = arr.map(mapFeedItem)
        setPosts((prev) => ([ ...prev, ...mapped ]))
        const totalPages = Number(body?.totalPages || body?.pageable?.totalPages || 0)
        const number = Number(body?.number ?? body?.pageable?.pageNumber ?? p)
        console.log('[AllGroupsPostsGrid] fetched items:', arr.length, 'page:', number, 'totalPages:', totalPages)
        const more = totalPages > 0 ? (number + 1 < Math.max(1, totalPages)) : (arr.length >= BATCH_SIZE)
        setHasMore(more)
      } catch (e) {
        if (e.name !== 'AbortError') {
          console.error('fetch all groups posts error', e)
          setError(e.message || 'Failed to load posts')
        }
      } finally {
        setLoading(false)
      }
    }

    if (userId) fetchPage(page)
    return () => controller.abort()
  }, [page, userId])

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

  // Helpers copied from Home to match post shape and reactions UI
  const getBearerToken = () => {
    const raw = getCookie('AccessToken') || getCookie('accessToken') || (typeof localStorage !== 'undefined' ? localStorage.getItem('AccessToken') || localStorage.getItem('accessToken') : '') || ''
    const trimmed = raw.trim()
    if (!trimmed) return ''
    const token = trimmed.replace(/^\s*bearer\s+/i, '').trim()
    return token ? `Bearer ${token}` : ''
  }

  const parseCountsFromResponse = (data) => {
    if (!data) return {}
    const item = Array.isArray(data) ? null : (data.data || data || {})
    const prefer = (k1, k2) => (item[k1] ?? item[k2] ?? data[k1] ?? data[k2])
    const likes = prefer('likeCount', 'likes')
    const dislikes = prefer('dislikeCount', 'dislikes')
    const commentCount = prefer('commentCount', 'comments')
    const shareCount = prefer('shareCount', 'shares')
    const isLiked = item.isLiked ?? item.liked ?? data.isLiked ?? data.liked
    const isDisliked = item.isDisliked ?? item.disliked ?? data.isDisliked ?? data.disliked
    const isFaved = item.isFaved ?? item.faved ?? data.isFaved ?? data.faved
    const result = {}
    if (typeof likes !== 'undefined') result.likes = Number(likes || 0)
    if (typeof dislikes !== 'undefined') result.dislikes = Number(dislikes || 0)
    if (typeof commentCount !== 'undefined') result.comments = Number(commentCount || 0)
    if (typeof shareCount !== 'undefined') result.shareCount = Number(shareCount || 0)
    if (typeof isLiked !== 'undefined') result.isLiked = Boolean(isLiked)
    if (typeof isDisliked !== 'undefined') result.isDisliked = Boolean(isDisliked)
    if (typeof isFaved !== 'undefined') result.isFaved = Boolean(isFaved)
    return result
  }

  const buildMediaSrc = (media, mediaType) => {
    if (!media) return ''
    if (typeof media === 'string') {
      if (media.startsWith('data:')) return media
      const t = String(mediaType || '').toLowerCase()
      if (t.includes('image')) return `data:${mediaType || 'image/jpeg'};base64,${media}`
      if (t.includes('video')) return `data:${mediaType};base64,${media}`
      if (media.startsWith('http://') || media.startsWith('https://') || media.startsWith('/')) return media
      return `data:image/jpeg;base64,${media}`
    }
    if (typeof media === 'object') {
      const mstr = media.url || media.media || media.mediaUrl || media.uri || media.path || media.fileUrl || media.src || ''
      const mtype = media.mediaType || media.type || media.mimeType || ''
      return buildMediaSrc(mstr, mtype)
    }
    return ''
  }

  // Optimistic reaction handler (likes, dislikes, isFaved)
  const pendingReactions = new Set()
  const toggleReaction = async (id, key) => {
    if (pendingReactions.has(`${id}:${key}`)) return
    pendingReactions.add(`${id}:${key}`)

    setPosts((prev) => prev.map((p) => {
      if (p.id !== id) return p
      if (key === 'isFaved') return { ...p, isFaved: !p.isFaved }
      const delta = key === 'likes' ? 1 : (key === 'dislikes' ? 1 : 0)
      return { ...p, [key]: (Number(p[key] || 0) + delta) }
    }))

    try {
      const tokenRaw = getCookie('AccessToken') || localStorage.getItem('AccessToken') || ''
      const auth = tokenRaw.trim() ? (tokenRaw.toLowerCase().startsWith('bearer ') ? tokenRaw : `Bearer ${tokenRaw}`) : ''
      const uid = getCookie('id') || getCookie('userId') || ''
      let url = ''
      let body = null
      if (key === 'likes') {
        url = import.meta.env.VITE_API_BASE_URL + 'content/content/like'
        body = { contentId: id, userId: uid }
      } else if (key === 'dislikes') {
        url = import.meta.env.VITE_API_BASE_URL + 'content/content/dislike'
        body = { contentId: id, userId: uid }
      } else {
        pendingReactions.delete(`${id}:${key}`)
        const p = posts.find((pp) => pp.id === id)
        window.dispatchEvent(new CustomEvent('post:updated', { detail: { contentId: id, update: { isFaved: p?.isFaved } } }))
        return
      }

      try { console.log('[AllGroupsPostsGrid] reaction URL:', url, 'body:', body, 'authPresent:', !!auth) } catch (e) {}
      const res = await fetch(url, { method: 'POST', headers: { 'Content-Type': 'application/json', ...(auth ? { Authorization: auth, AccessToken: tokenRaw } : {}) }, body: JSON.stringify(body) })
      const data = await res.json().catch(() => ({}))
      if (!res.ok) {
        const parsed = (await import('../lib/parseApiError.js')).parseApiErrorBody(data)
        throw new Error(parsed.message || 'Failed to update reaction')
      }

      const serverUpdate = parseCountsFromResponse(data)
      if (Object.keys(serverUpdate).length > 0) {
        setPosts((prev) => prev.map((p) => (p.id === id ? ({ ...p, ...serverUpdate }) : p)))
        window.dispatchEvent(new CustomEvent('post:updated', { detail: { contentId: id, update: serverUpdate } }))
      } else {
        const updated = (function () { const p = (posts || []).find((pp) => pp.id === id) || null; return p ? { likes: p.likes, dislikes: p.dislikes, isFaved: p.isFaved } : {} })()
        window.dispatchEvent(new CustomEvent('post:updated', { detail: { contentId: id, update: updated } }))
      }
    } catch (err) {
      console.error('reaction error', err)
      setPosts((prev) => prev.map((p) => {
        if (p.id !== id) return p
        if (key === 'isFaved') return { ...p, isFaved: !p.isFaved }
        const delta = key === 'likes' ? -1 : (key === 'dislikes' ? -1 : 0)
        return { ...p, [key]: Math.max(0, Number(p[key] || 0) + delta) }
      }))
    } finally {
      pendingReactions.delete(`${id}:${key}`)
    }
  }

  // Share handler
  const handleShare = async (id) => {
    const url = `${window.location.origin}/post/${id}`
    try {
      if (navigator.share) {
        await navigator.share({ title: 'Check this post', url })
      } else if (navigator.clipboard) {
        await navigator.clipboard.writeText(url)
        try { console.log('[AllGroupsPostsGrid] copied post URL to clipboard:', url) } catch (e) {}
      } else {
        try { console.log('[AllGroupsPostsGrid] share URL (fallback):', url) } catch (e) {}
      }

      setPosts((prev) => prev.map((p) => (p.id === id ? ({ ...p, shareCount: Number(p.shareCount || 0) + 1 }) : p)))

      try {
        const tokenRaw = getCookie('AccessToken') || localStorage.getItem('AccessToken') || ''
        const auth = tokenRaw.trim() ? (tokenRaw.toLowerCase().startsWith('bearer ') ? tokenRaw : `Bearer ${tokenRaw}`) : ''
        const uid = getCookie('id') || getCookie('userId') || ''
        const shareUrl = import.meta.env.VITE_API_BASE_URL + 'content/content/share'
        const res = await fetch(shareUrl, { method: 'POST', headers: { 'Content-Type': 'application/json', ...(auth ? { Authorization: auth, AccessToken: tokenRaw } : {}) }, body: JSON.stringify({ contentId: id, userId: uid }) })
        const data = await res.json().catch(() => ({}))
        if (res.ok) {
          const serverUpdate = parseCountsFromResponse(data)
          if (Object.keys(serverUpdate).length > 0) {
            setPosts((prev) => prev.map((p) => (p.id === id ? ({ ...p, ...serverUpdate }) : p)))
            window.dispatchEvent(new CustomEvent('post:updated', { detail: { contentId: id, update: serverUpdate } }))
          } else {
            window.dispatchEvent(new CustomEvent('post:updated', { detail: { contentId: id, update: { shareCount: (posts.find((pp) => pp.id === id)?.shareCount || 0) + 1 } } }))
          }
        }
      } catch (e) {
        console.warn('share API failed', e)
      }
    } catch (e) {
      console.error('share failed', e)
    }
  }

  // RemoteMedia: fetch remote http(s) media, convert data: and base64 payloads to Blobs and use object URLs to avoid embedding huge data URIs in the DOM
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
        } catch (e) {
          console.warn('[RemoteMedia] base64->blob failed', e)
          return null
        }
      }

      const dataUriToObjectUrl = async (dataUri) => {
        try {
          const res = await fetch(dataUri)
          if (!res.ok) throw new Error('Failed to convert data URI')
          const blob = await res.blob()
          return URL.createObjectURL(blob)
        } catch (e) {
          console.warn('[RemoteMedia] data URI->blob failed', e)
          return null
        }
      }

      (async () => {
        try {
          // blob: URLs can be used directly
          if (src.startsWith('blob:')) { if (!canceled) setObjUrl(src); return }

          // data: URIs -> convert to object URL to avoid embedding huge strings in DOM
          if (src.startsWith('data:')) {
            const url = await dataUriToObjectUrl(src)
            if (url && !canceled) { createdObjectUrl = url; setObjUrl(url) }
            return
          }

          // http(s) -> fetch with Authorization-only header
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

          // Likely raw base64 payload (no data: prefix)
          if (isLikelyBase64(src)) {
            const mime = (mediaType && mediaType.includes('/')) ? mediaType : 'image/jpeg'
            const url = base64ToObjectUrl(src, mime)
            if (url && !canceled) { createdObjectUrl = url; setObjUrl(url) }
            return
          }

          // Fallback: treat as direct URL/path
          if (!canceled) setObjUrl(src)
        } catch (e) {
          console.warn('[RemoteMedia] failed to resolve src', src, e)
        }
      })()

      return () => {
        canceled = true
        controller.abort()
        if (createdObjectUrl) { URL.revokeObjectURL(createdObjectUrl) }
      }
    }, [src, mediaType])

    if (!objUrl) return (
      <div className="h-[360px] w-full bg-white/5 flex items-center justify-center"><div className="animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-white/70" /></div>
    )

    return mediaType.toLowerCase().includes('video') ? (
      <video src={objUrl} className="h-[360px] w-full object-cover" controls onLoadedData={() => setLoaded(true)} onError={() => setLoaded(false)} />
    ) : (
      <img src={objUrl} alt="" onLoad={() => setLoaded(true)} onError={() => setLoaded(false)} className="h-[360px] w-full object-cover" loading="lazy" />
    )
  }

  // Render like Home: article cards
  const PostMedia = ({ item }) => {
    const media = item.media || item.image || ''
    let chosen = ''
    let mtype = ''
    if (Array.isArray(media) && media.length > 0) {
      const m = media[0]
      if (typeof m === 'string') { chosen = buildMediaSrc(m, ''); mtype = '' } else { chosen = buildMediaSrc(m.url || m.media || m.mediaUrl || m.src || '', m.mediaType || m.type || ''); mtype = (m.mediaType || m.type || '') }
    } else if (typeof media === 'string' && media) {
      chosen = buildMediaSrc(media, '')
    }

    return (
      <div className="relative overflow-hidden">
        <RemoteMedia src={chosen} mediaType={mtype} />

        <div className="pointer-events-none absolute inset-0 bg-gradient-to-t from-black/45 via-black/15 to-transparent" />
      </div>
    )
  }

  return (
    <div className="mt-8">
      <h3 className="text-lg font-semibold text-white mb-3">Posts</h3>
      {/* {error && <div className="mb-4"><AlertBanner status="error" message={error} /></div>} */}

      {!loading && posts.length === 0 && (
        <div className="w-full max-w-2xl mx-auto rounded-3xl border border-white/10 bg-white/5 p-6 text-center">
          <p className="text-lg text-slate-200">No posts to show, join groups to see posts here.</p>
        </div>
      )}

      <div className="space-y-5">
        {posts.map((post) => (
          <article
            key={post.id}
            role="button"
            tabIndex={0}
            onClick={() => navigate(`/post/${post.id}`, { state: { post } })}
            onKeyDown={(e) => { if (e.key === 'Enter') navigate(`/post/${post.id}`, { state: { post } }) }}
            className="group relative overflow-hidden rounded-3xl border border-white/10 bg-white/5 shadow-[0_20px_70px_rgba(0,0,0,0.4)] backdrop-blur-2xl cursor-pointer focus:outline-none focus:ring-2 focus:ring-white/20 hover:shadow-lg hover:scale-[1.01] transition-transform"
            style={{ boxShadow: `0 20px 70px rgba(0,0,0,0.45), 0 0 32px ${accent.glow}` }}
          >
            <div className="flex items-center gap-3 px-5 py-4" onClick={(e) => { e.stopPropagation(); if (post.authorId) navigate(`/friend-profile/${encodeURIComponent(post.authorId)}`) }}>
              <div className="h-11 w-11 rounded-full border border-white/15 bg-white/10 flex items-center justify-center text-white/90" style={{ boxShadow: `0 10px 25px ${accent.glow}` }}>
                <Sparkles className="h-5 w-5" />
              </div>
              <div>
                <p className="text-sm font-semibold text-white">{post.author}</p>
                <p className="text-sm text-slate-200/85 leading-relaxed ">{post.handle}</p>
                {post.time && <div className="text-xs text-slate-400">{new Date(post.time).toLocaleString()}</div>}
              </div>
            </div>

            <PostMedia item={post} />

            <div className="space-y-3 px-5 py-4">
              <div className="flex items-start justify-between gap-3">
                <div>
                  <p className="text-sm uppercase tracking-[0.2em] text-slate-300/80">{post.anime || ''}</p>
                  <h3 className="text-lg font-semibold text-white">{post.title}</h3>
                </div>
                <button
                  type="button"
                  onClick={(e) => { e.stopPropagation(); toggleReaction(post.id, 'isFaved') }}
                  className={`rounded-full border px-3 py-2 text-sm font-semibold transition ${post.isFaved ? 'border-white/30 bg-white/15 text-white' : 'border-white/10 bg-white/5 text-slate-200/80 hover:text-white'}`}
                  style={post.isFaved ? { boxShadow: `0 10px 30px ${accent.glow}` } : undefined}
                >
                  <Bookmark className="mb-0.5 inline h-4 w-4" />
                  <span className="ml-2">{post.isFaved ? 'Saved' : 'Save'}</span>
                </button>
              </div>
              <p className="text-sm text-slate-200/85 leading-relaxed">{post.description}</p>
              <div className="flex items-center gap-3 text-sm text-slate-200/85">
                <button
                  type="button"
                  onClick={(e) => { e.stopPropagation(); toggleReaction(post.id, 'likes') }}
                  className="group inline-flex items-center gap-1 rounded-full border border-white/10 bg-white/5 px-3 py-1.5 font-semibold text-white/90 transition hover:border-white/20"
                >
                  <ThumbsUp className="h-4 w-4" />
                  <span>{post.likes}</span>
                </button>
              <button
                type="button"
                onClick={(e) => { e.stopPropagation(); toggleReaction(post.id, 'dislikes') }}
                className="group inline-flex items-center gap-1 rounded-full border border-white/10 bg-white/5 px-3 py-1.5 font-semibold text-white/90 transition hover:border-white/20"
              >
                <ThumbsDown className="h-4 w-4" />
                <span>{post.dislikes}</span>
              </button>
              <button
                type="button"
                onClick={(e) => { e.stopPropagation(); navigate(`/post/${post.id}`, { state: { post } }) }}
                className="group inline-flex items-center gap-1 rounded-full border border-white/10 bg-white/5 px-3 py-1.5 font-semibold text-white/90 transition hover:border-white/20"
              >
                <MessageCircle className="h-4 w-4" />
                <span>{post.comments}</span>
              </button>
              <button
                type="button"
                onClick={(e) => { e.stopPropagation(); handleShare(post.id) }}
                className="group inline-flex items-center gap-1 rounded-full border border-white/10 bg-white/5 px-3 py-1.5 font-semibold text-white/90 transition hover:border-white/20"
                title="Share"
              >
                <Share2 className="h-4 w-4" />
                <span>{post.shareCount ?? 0}</span>
              </button>
            </div>
          </div>
        </article>
      ))}
      </div>

      <div ref={sentinelRef} className="py-6 flex items-center justify-center">
        {loading ? (
          <div className="animate-spin rounded-full h-10 w-10 border-t-2 border-b-2 border-white/70" />
        ) : hasMore ? (
          <div className="text-sm text-slate-400">Scroll to load more</div>
        ) : null}
      </div>
    </div>
  )
}

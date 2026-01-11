import { useEffect, useMemo, useState, useRef } from 'react'
import { useNavigate } from 'react-router-dom'

const getCookie = (name) => {
  if (typeof document === 'undefined') return ''
  const match = document.cookie.match(new RegExp(`(?:^|; )${name}=([^;]*)`))
  return match ? decodeURIComponent(match[1]) : ''
}
import { AnimatePresence } from 'framer-motion'
import * as Tooltip from '@radix-ui/react-tooltip'
import { MessageCircle, ThumbsDown, ThumbsUp, Bookmark, Sparkles, Share2 } from 'lucide-react' 
import accentOptions from '../theme/accentOptions.js'
import { getSavedAccentKey } from '../theme/accentStorage.js'
import NavigationBar from '../components/NavigationBar.jsx'
import LoadingScreen from '../components/LoadingScreen.jsx'
import AlertBanner from '../components/AlertBanner.jsx'
import { refreshAccessToken } from '../lib/apiClient.js'

const seedPosts = []

function Home() {
  const accentKey = getSavedAccentKey('crimson-night')
  const accent = useMemo(() => accentOptions.find((opt) => opt.key === accentKey)?.colors || accentOptions[0].colors, [accentKey])
  const navigate = useNavigate()
  const [showAccentLoader, setShowAccentLoader] = useState(false)
  const [posts, setPosts] = useState(seedPosts)
  const [loadingFeed, setLoadingFeed] = useState(false)
  const [alertBanner, setAlertBanner] = useState(null)

  // Feed scope: 'all' or 'following'
  const [feedScope, setFeedScope] = useState('all')
  // Pagination state for the Following feed
  const [feedPage, setFeedPage] = useState(0)
  const [feedLoadingMore, setFeedLoadingMore] = useState(false)
  const [feedHasMore, setFeedHasMore] = useState(true)

  // Fetch list of following user ids (first page) - used to build a 'following' feed client-side when backend doesn't provide one
  const fetchFollowingIds = async (uid) => {
    const token = getBearerToken()
    const endpoint = `${import.meta.env.VITE_API_BASE_URL}profile/user/following/list?id=${encodeURIComponent(uid)}&page=0`
    try {
      const res = await fetch(endpoint, { headers: { Authorization: token } })
      if (!res.ok) {
        const maybe = await res.json().catch(() => ({}))
        const parsed = (await import('../lib/parseApiError.js')).parseApiErrorBody(maybe)
        throw new Error(parsed.message || 'Failed to fetch following list')
      }
      const data = await res.json()
      const arr = Array.isArray(data?.content) ? data.content : (Array.isArray(data?.data) ? data.data : (Array.isArray(data) ? data : []))
      const ids = arr.map((it) => it.id || it.userId || it.userId || '').filter(Boolean)
      return ids
    } catch (err) {
      console.error('fetchFollowingIds error', err)
      throw err
    }
  }

  // Centralized feed fetch so it can be used on mount and by the Refresh / scope switch
  // Supports paged loading for the 'following' scope (page param appended to endpoint)
  const loadFeed = async (scope = 'all', page = 0, append = false) => {
    // page === 0 is a full/initial load; append === true indicates infinite-scroll append
    if (append) setFeedLoadingMore(true)
    else setLoadingFeed(true)
    setAlertBanner(null)
    try {
      const uid = getCookie('id') || getCookie('userId') || ''
      const tokenRaw = getCookie('AccessToken') || localStorage.getItem('AccessToken') || ''
      const auth = tokenRaw ? (tokenRaw.toLowerCase().startsWith('bearer ') ? tokenRaw : `Bearer ${tokenRaw}`) : ''
      const url = `${import.meta.env.VITE_API_BASE_URL}recommendation/feed/get?userId=${encodeURIComponent(uid)}`
      try { console.log('[Home] fetching recommendation feed URL (manual):', url, 'authPresent:', !!auth, 'scope:', scope, 'page:', page, 'append:', append) } catch (e) {}

      // Fetch the recommendation feed (used as a fallback or for 'all')
      const res = await fetchWithAuthRetry(url, { headers: { ...(auth ? { Authorization: auth, AccessToken: tokenRaw } : {}) } })
      const data = await res.json().catch(() => ([]))
      if (!res.ok) {
        const parsed = (await import('../lib/parseApiError.js')).parseApiErrorBody(data)
        throw new Error(parsed.message || 'Failed to fetch feed')
      }
      const arr = Array.isArray(data) ? data : (data?.data || [])

      if (scope === 'all') {
        try { localStorage.setItem('recommendationFeed', JSON.stringify(arr)) } catch (e) {}
        setPosts(arr.map(mapFeedItem))
        setFeedHasMore(false)
      } else {
        // Following scope: prefer backend paged following-recommendation endpoint
        try {
          const token = getBearerToken()
          const followingUrl = `${import.meta.env.VITE_API_BASE_URL}content/content/recommendation/following?userId=${encodeURIComponent(uid)}&page=${encodeURIComponent(page)}`
          try { console.log('[Home] fetching following recommendations URL:', followingUrl, 'authPresent:', !!token) } catch (e) {}

          // Use fetchWithAuthRetry which will refresh token once on 401
          let res2 = await fetchWithAuthRetry(followingUrl, { headers: { ...(token ? { Authorization: token } : {}) } })
          let data2 = await res2.json().catch(() => ([]))

          if (!res2.ok) {
            const parsed = (await import('../lib/parseApiError.js')).parseApiErrorBody(data2)
            throw new Error(parsed.message || 'Failed to fetch following feed')
          }

          const arr2 = Array.isArray(data2) ? data2 : (data2?.data || data2?.content || [])

          // Determine pagination availability
          const totalPages = (typeof data2?.totalPages === 'number') ? data2.totalPages : (typeof data2?.totalElements === 'number' && typeof data2?.pageSize === 'number' ? Math.ceil(data2.totalElements / data2.pageSize) : undefined)
          const pageSize = data2?.pageSize ?? data2?.size ?? (Array.isArray(arr2) ? arr2.length : 0)

          if (!arr2 || arr2.length === 0) {
            if (!append) setPosts([])
            setFeedHasMore(false)
            if (!append) setLoadingFeed(false)
            return
          }

          if (append) {
            setPosts((prev) => ([...prev, ...arr2.map(mapFeedItem)]))
          } else {
            setPosts(arr2.map(mapFeedItem))
          }

          if (typeof totalPages === 'number') {
            setFeedHasMore(page < (totalPages - 1))
          } else if (pageSize > 0) {
            setFeedHasMore(arr2.length >= pageSize)
          } else {
            setFeedHasMore(true)
          }
        } catch (err) {
          console.error('Failed to fetch following feed endpoint, falling back to local filter', err)
          // fallback: filter recommendation feed client-side and page it locally
          try {
            const followingIds = await fetchFollowingIds(uid)
            if (!followingIds || followingIds.length === 0) {
              if (!append) setPosts([])
              setFeedHasMore(false)
              if (!append) setLoadingFeed(false)
              return
            }

            const filtered = arr.filter((it) => {
              const author = it.userId || it.userID || it.ownerId || it.creatorId || (it.user && it.user.id) || ''
              return followingIds.includes(author)
            })

            // Page locally
            const start = page * BATCH_SIZE
            const slice = filtered.slice(start, start + BATCH_SIZE)
            if (append) setPosts((prev) => ([...prev, ...slice.map(mapFeedItem)]))
            else setPosts(slice.map(mapFeedItem))

            setFeedHasMore(filtered.length > (start + slice.length))
          } catch (err2) {
            console.error('Failed to build following feed', err2)
            setAlertBanner({ status: 'error', message: err2?.message || 'Failed to load following feed' })
            setPosts([])
            setFeedHasMore(false)
          }
        }
      }
    } catch (e) {
      console.error('refresh feed error', e)
      setAlertBanner({ status: 'error', message: e?.message || 'Failed to refresh feed' })
    } finally {
      setLoadingFeed(false)
      setFeedLoadingMore(false)
    }
  }  
  const [mediaMap, setMediaMap] = useState({})
  const [mediaPage, setMediaPage] = useState(0)
  const BATCH_SIZE = 10
  const MAX_RETRIES = 2
  const retryCounts = useRef({})
  const [mediaLoading, setMediaLoading] = useState(false)
  const [mediaHasMore, setMediaHasMore] = useState(true)
  const sentinelRef = useRef(null)

  // Groups (joined by user) shown in Quick Picks
  const [groups, setGroups] = useState([])
  const [loadingGroups, setLoadingGroups] = useState(true)
  const [groupsError, setGroupsError] = useState('')

  const getBearerToken = () => {
    const raw = getCookie('AccessToken') || getCookie('accessToken') || (typeof localStorage !== 'undefined' ? localStorage.getItem('AccessToken') || localStorage.getItem('accessToken') : '') || ''
    const trimmed = raw.trim()
    if (!trimmed) return ''
    const token = trimmed.replace(/^\s*bearer\s+/i, '').trim()
    return token ? `Bearer ${token}` : ''
  }

  // Helper to perform fetch and retry once with a refreshed token when a 401 occurs
  const fetchWithAuthRetry = async (url, opts = {}) => {
    const options = { ...(opts || {}) }
    options.headers = { ...(options.headers || {}) }
    const access = getBearerToken()
    if (access) options.headers.Authorization = access

    let res = await fetch(url, options)
    if (res.status === 401) {
      try {
        console.warn('[Home] fetch returned 401 for', url, '; attempting token refresh')
        await refreshAccessToken()
        const newAccess = getBearerToken()
        if (newAccess) options.headers.Authorization = newAccess
        res = await fetch(url, options)
      } catch (rfErr) {
        console.error('[Home] token refresh attempt failed during fetchWithAuthRetry', rfErr)
        // return original 401 response to be handled by caller
        return res
      }
    }
    return res
  }

  const mapFeedItem = (it) => ({
    id: it.contentId || it.id || String(Math.random()).slice(2),
    authorId: it.userId || it.userID || it.ownerId || it.creatorId || (it.user && it.user.id) || '',
    author: it.displayName || it.username || it.userName || 'Unknown',
    handle: it.username ? `@${it.username}` : (it.userName ? `@${it.userName}` : ''),
    anime: '',
    title: it.contentTitle || it.title || '',
    description: it.description || it.bio || '',
    image: it.image || it.media || it.imageUrl || '',
    likes: Number(it.likeCount || 0),
    dislikes: Number(it.dislikeCount || 0),
    comments: Number(it.commentCount || 0),
    isFaved: false,
    time: it.timeOfCreation || it.time || ''
  })

  // Normalize media payloads into a usable src (matches logic used in Post and UserPostsGrid)
  const buildMediaSrc = (media, mediaType) => {
    if (!media) return ''
    if (typeof media === 'string') {
      if (media.startsWith('data:')) return media
      const t = String(mediaType || '').toLowerCase()
      if (t.includes('image')) return `data:${mediaType || 'image/jpeg'};base64,${media}`
      if (t.includes('video')) return `data:${mediaType};base64,${media}`
      // if it's an absolute/relative URL return as-is, otherwise assume base64 image
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

  // Helper to extract canonical counts from various server response shapes
  const parseCountsFromResponse = (data) => {
    if (!data) return {}
    // Response might be the item itself, or { data: item } or a wrapper
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

  useEffect(() => {
    let timer = null
    const startLoader = () => {
      setShowAccentLoader(true)
      if (timer) clearTimeout(timer)
      timer = setTimeout(() => setShowAccentLoader(false), 2000)
    }

    const onStorage = (e) => {
      if (e.key === 'accentKey') startLoader()
      if (e.key === 'recommendationFeed') {
        try {
          const raw = localStorage.getItem('recommendationFeed')
          const arr = raw ? JSON.parse(raw) : null
          if (Array.isArray(arr)) setPosts(arr.map(mapFeedItem))
        } catch (e) { console.error('failed to parse recommendationFeed', e) }
      }
    }

    const onCustom = (e) => {
      startLoader()
    }

    if (typeof window !== 'undefined' && typeof window.addEventListener === 'function') {
      window.addEventListener('storage', onStorage)
    } else {
      console.warn('[Home] window.addEventListener not available; storage events will not be observed')
    }

    if (typeof window !== 'undefined' && typeof window.addEventListener === 'function') {
      window.addEventListener('accent:changed', onCustom)
    } else {
      console.warn('[Home] window.addEventListener not available; accent:changed events will not be observed')
    }

    // On mount, prefer stored recommendationFeed if present
    (async () => {
      try {
        const raw = localStorage.getItem('recommendationFeed')
        if (raw) {
          const arr = JSON.parse(raw)
          if (Array.isArray(arr) && arr.length > 0) {
            setPosts(arr.map(mapFeedItem))
          }
        }

        // always refresh feed from API
        await loadFeed(feedScope)
      } catch (e) {
        console.error('fetch feed error', e)
      } finally {
        setLoadingFeed(false)
      }
    })()

    return () => {
      if (typeof window !== 'undefined' && typeof window.removeEventListener === 'function') {
        window.removeEventListener('storage', onStorage)
        window.removeEventListener('accent:changed', onCustom)
      }
      if (timer) clearTimeout(timer)
    }
  }, [])

  // Fetch groups joined by the current user (for Quick Picks)
  const fetchGroups = async () => {
    setLoadingGroups(true)
    setGroupsError('')
    try {
      const userId = getCookie('userId') || getCookie('id') || ''
      if (!userId) { setGroups([]); setLoadingGroups(false); return }
      const url = `${import.meta.env.VITE_API_BASE_URL}profile/group/user/${encodeURIComponent(userId)}`
      const accessTokenRaw = getCookie('AccessToken') || getCookie('accessToken') || (typeof localStorage !== 'undefined' ? localStorage.getItem('AccessToken') || localStorage.getItem('accessToken') : '') || ''
      const auth = accessTokenRaw ? (accessTokenRaw.toLowerCase().startsWith('bearer ') ? accessTokenRaw : `Bearer ${accessTokenRaw}`) : ''
      const headers = auth ? { Authorization: auth, ...(accessTokenRaw ? { AccessToken: accessTokenRaw } : {}) } : {}
      const res = await fetch(url, { headers })
      const body = await res.json().catch(() => ({}))
      if (!res.ok) {
        const parsed = (await import('../lib/parseApiError.js')).parseApiErrorBody(body)
        throw new Error(parsed.message || 'Failed to fetch groups')
      }
      const data = body?.data || body || []
      const arr = Array.isArray(data) ? data : (data.groups || [])
      const mapped = arr.map((g) => ({
        id: g.id || g.groupId || g._id || '',
        name: g.name || g.groupName || 'Untitled Group',
        members: g.members || g.memberCount || g.totalMembers || (Array.isArray(g.members) ? g.members.length : 0) || 0,
        activity: g.activity || g.activityFrequency || 'Unknown',
        image: g.profileImage ? `data:image/jpeg;base64,${g.profileImage}` : (g.icon || g.image || g.imageUrl || ''),
      }))
      setGroups(mapped)
    } catch (err) {
      console.error('fetchGroups error', err)
      setGroupsError(err?.message || 'Failed to load groups')
    } finally {
      setLoadingGroups(false)
    }
  }

  useEffect(() => {
    fetchGroups()
  }, [])

  // Optimistic reaction handler that calls backend and syncs across pages
  const pendingReactions = new Set()
  const toggleReaction = async (id, key) => {
    // key: 'likes' or 'dislikes' or 'isFaved'
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
        // Fallback: toggle favorite locally only (no API endpoint assumed)
        pendingReactions.delete(`${id}:${key}`)
        // Broadcast local update
        const p = posts.find((pp) => pp.id === id)
        window.dispatchEvent(new CustomEvent('post:updated', { detail: { contentId: id, update: { isFaved: p?.isFaved } } }))
        return
      }

      try { console.log('[Home] reaction URL:', url, 'body:', body, 'authPresent:', !!auth) } catch (e) {}
      const res = await fetch(url, { method: 'POST', headers: { 'Content-Type': 'application/json', ...(auth ? { Authorization: auth, AccessToken: tokenRaw } : {}) }, body: JSON.stringify(body) })
      const data = await res.json().catch(() => ({}))
      if (!res.ok) {
        const parsed = (await import('../lib/parseApiError.js')).parseApiErrorBody(data)
        throw new Error(parsed.message || 'Failed to update reaction')
      }

      // prefer server-returned counts when available
      const serverUpdate = parseCountsFromResponse(data)
      if (Object.keys(serverUpdate).length > 0) {
        setPosts((prev) => prev.map((p) => (p.id === id ? ({ ...p, ...serverUpdate }) : p)))
        window.dispatchEvent(new CustomEvent('post:updated', { detail: { contentId: id, update: serverUpdate } }))
      } else {
        // fallback to optimistic current values
        const updated = (function () {
          const p = (posts || []).find((pp) => pp.id === id) || null
          return p ? { likes: p.likes, dislikes: p.dislikes, isFaved: p.isFaved } : {}
        })()
        window.dispatchEvent(new CustomEvent('post:updated', { detail: { contentId: id, update: updated } }))
      }
    } catch (err) {
      console.error('reaction error', err)
      // Revert optimistic change on failure
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

  // Fetch media for posts in paginated batches (BATCH_SIZE) and keep a map of contentId -> media array
  useEffect(() => {
    let cancelled = false
    const loadBatchMedia = async () => {
      try {
        const allContentIds = posts.map((p) => p.id).filter(Boolean)
        if (allContentIds.length === 0) return
        const start = mediaPage * BATCH_SIZE
        if (start >= allContentIds.length) {
          setMediaHasMore(false)
          return
        }
        const batchIds = allContentIds.slice(start, start + BATCH_SIZE)
        if (!batchIds.length) {
          setMediaHasMore(false)
          return
        }

        setMediaLoading(true)

        const uid = getCookie('id') || getCookie('userId') || ''
        const token = getCookie('AccessToken') || localStorage.getItem('AccessToken') || ''
        const auth = token ? (token.toLowerCase().startsWith('bearer ') ? token : `Bearer ${token}`) : ''
        // encode each id but keep commas as separators (server expects comma-separated ids)
        const idsParam = batchIds.map((id) => encodeURIComponent(String(id))).join(',')
        // Keep the server page param at 0 (we control batching client-side via contentIds slicing)
        const url = `${import.meta.env.VITE_API_BASE_URL}content/content/get/batch?contentIds=${idsParam}&currentUserId=${encodeURIComponent(uid)}&page=0&includeMedia=true`
        try {
          console.log('[Home] fetching media batch (comma-separated contentIds):', url, 'authPresent:', !!auth)
          console.log('[Home] contentIds batch:', batchIds)
          console.log('[Home] encoded contentIds param:', idsParam)
          console.log('[Home] batch URL (pasteable):', url)
          console.log('[Home] curl (add Authorization header if needed):', `curl -H "Authorization: Bearer <token>" "${url}"`)
        } catch (e) {}

        const res = await fetch(url, { headers: { ...(auth ? { Authorization: auth, AccessToken: token } : {}) } })
        const data = await res.json().catch(() => ([]))
        console.log('[Home] batch response status:', res.status, 'ok:', res.ok)
        console.log('[Home] batch response data:', data)
        if (!res.ok) {
          const parsed = (await import('../lib/parseApiError.js')).parseApiErrorBody(data)
          console.error('[Home] media batch error body:', data)
          throw new Error(parsed.message || 'Failed to fetch media batch')
        }

        // Prefer the `content` array when the backend returns a paginated wrapper
        const pageContents = Array.isArray(data) ? data : (data?.data || [])
        console.log('[Home] pageContents:', pageContents)
        const arr = Array.isArray(data?.content) ? data.content : pageContents
        console.log('[Home] batch returned items:', Array.isArray(arr) ? arr.length : 0)
        console.log('[Home] batch returned items array:', arr)
        const map = {}
        const countsMap = {}
        arr.forEach((it) => {
          const id = it.contentId || it.id
          let mediaArr = []
          if (Array.isArray(it.media)) mediaArr = it.media
          else if (typeof it.media === 'string' && it.media) mediaArr = [{ url: it.media, mediaType: it.mediaType }]
          else if (it.media && typeof it.media === 'object') mediaArr = [it.media]
          else mediaArr = []

          if (id) {
            const key = String(id)
            map[key] = mediaArr
            const counts = parseCountsFromResponse(it)
            if (Object.keys(counts).length > 0) countsMap[key] = counts
            try { console.log('[Home] mapped media for content:', key, mediaArr, 'counts:', counts) } catch (e) {}
          } else {
            try { console.warn('[Home] batch item missing id:', it) } catch (e) {}
          }
        })
        try { console.log('[Home] about to set mediaMap keys:', Object.keys(map)) } catch (e) {}

        if (!cancelled) {
          // If the batch returned nothing for these ids, attempt a retry (maybe server glitch)
          if ((!arr || arr.length === 0) && batchIds.length > 0) {
            const rc = (retryCounts.current[mediaPage] || 0) + 1
            retryCounts.current[mediaPage] = rc
            console.warn('[Home] batch returned 0 items for page', mediaPage, 'retry', rc)
            if (rc <= MAX_RETRIES) {
              // Retry after a short backoff
              setTimeout(() => { loadBatchMedia() }, 500 * rc)
              return
            } else {
              console.warn('[Home] giving up on page', mediaPage, 'after retries; advancing to next page')
              setMediaPage((p) => p + 1)
              return
            }
          }

          setMediaMap((prev) => ({ ...prev, ...map }))
          if (Object.keys(countsMap).length > 0) {
            setPosts((prev) => {
              let changed = false
              const next = prev.map((p) => {
                const c = countsMap[p.id]
                if (!c) return p
                const newVals = {}
                if (typeof c.likes !== 'undefined' || typeof c.likeCount !== 'undefined') newVals.likes = Number(c.likes ?? c.likeCount ?? p.likes)
                if (typeof c.dislikes !== 'undefined' || typeof c.dislikeCount !== 'undefined') newVals.dislikes = Number(c.dislikes ?? c.dislikeCount ?? p.dislikes)
                if (typeof c.comments !== 'undefined' || typeof c.commentCount !== 'undefined') newVals.comments = Number(c.comments ?? c.commentCount ?? p.comments)
                if (typeof c.shareCount !== 'undefined') newVals.shareCount = Number(c.shareCount)
                if (typeof c.isLiked !== 'undefined') newVals.isLiked = Boolean(c.isLiked)
                if (typeof c.isDisliked !== 'undefined') newVals.isDisliked = Boolean(c.isDisliked)
                if (typeof c.isFaved !== 'undefined') newVals.isFaved = Boolean(c.isFaved)

                let different = false
                for (const k of Object.keys(newVals)) {
                  if (p[k] !== newVals[k]) { different = true; break }
                }
                if (!different) return p
                changed = true
                return { ...p, ...newVals }
              })
              return changed ? next : prev
            })
          }

          // Determine if there are more batches available
          const totalFetched = start + (Array.isArray(arr) ? arr.length : 0)
          if (totalFetched >= allContentIds.length) setMediaHasMore(false)
        }
      } catch (e) {
        console.error('fetch media batch error', e)
      } finally {
        if (!cancelled) setMediaLoading(false)
      }
    }
    loadBatchMedia()
    return () => { cancelled = true }
  }, [posts, mediaPage])

  // reset pagination when posts list changes
  useEffect(() => {
    setMediaPage(0)
    setMediaMap({})
    setMediaHasMore(true)
    retryCounts.current = {}
  }, [posts])

  // When feed page increments (due to infinite scroll) load the next page for 'following' scope
  useEffect(() => {
    if (feedPage === 0) return
    // only trigger when scope is following
    if (feedScope !== 'following') return
    // append next page
    loadFeed('following', feedPage, true)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [feedPage])

  // Listen for token refresh events and re-load key data without a full page reload
  useEffect(() => {
    const onTokenRefreshed = (e) => {
      try {
        console.info('[Home] Access token refreshed; reloading feed, groups, and media')
        // reset feed pagination and media pagination
        setFeedPage(0)
        setFeedHasMore(true)
        setFeedLoadingMore(false)

        setPosts([])

        setMediaPage(0)
        setMediaMap({})
        setMediaHasMore(true)
        retryCounts.current = {}

        // reload groups and feed for current scope
        fetchGroups().catch((err) => console.warn('fetchGroups after token refresh failed', err))
        loadFeed(feedScope, 0, false)
      } catch (err) {
        console.error('onTokenRefreshed handler error', err)
      }
    }

    if (typeof window !== 'undefined' && typeof window.addEventListener === 'function') {
      window.addEventListener('accessToken:refreshed', onTokenRefreshed)
    }
    return () => {
      if (typeof window !== 'undefined' && typeof window.removeEventListener === 'function') {
        window.removeEventListener('accessToken:refreshed', onTokenRefreshed)
      }
    }
  }, [feedScope])

  // IntersectionObserver sentinel to load next page when user scrolls near bottom
  useEffect(() => {
    if (!sentinelRef.current) return
    const obs = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (!entry.isIntersecting) return

        // If following scope is active, prefer loading next page of feed posts
        if (feedScope === 'following' && !feedLoadingMore && feedHasMore) {
          setFeedPage((p) => p + 1)
        }

        // Also continue loading media batches as before
        if (!mediaLoading && mediaHasMore) {
          setMediaPage((p) => p + 1)
        }
      })
    }, { root: null, rootMargin: '600px', threshold: 0.1 })
    obs.observe(sentinelRef.current)
    return () => obs.disconnect()
  }, [sentinelRef.current, mediaLoading, mediaHasMore, posts, feedScope, feedLoadingMore, feedHasMore])

  // Component that renders media for a post. Shows a persistent buffer spinner until the media loads; if loading fails, spinner remains visible per requirement.
  function PostMedia({ contentId, fallback }) {
    const [loaded, setLoaded] = useState(false)
    const [src, setSrc] = useState(null)
    const [objSrc, setObjSrc] = useState(null)
    const [isVideo, setIsVideo] = useState(false)

    useEffect(() => {
      const mediaArr = mediaMap[contentId]
      let chosen = null
      let mtype = ''
      if (Array.isArray(mediaArr) && mediaArr.length > 0) {
        const m = mediaArr[0]
        if (typeof m === 'string') {
          chosen = buildMediaSrc(m, '')
          mtype = ''
        } else {
          chosen = buildMediaSrc(m.url || m.media || m.mediaUrl || m.src || '', m.mediaType || m.type || '')
          mtype = (m.mediaType || m.type || '')
        }
      } else if (fallback) {
        chosen = buildMediaSrc(fallback, '')
      }
      setSrc(chosen)
      setIsVideo(String(mtype || '').toLowerCase().includes('video'))
      setLoaded(false)
    }, [contentId, fallback, mediaMap])

    useEffect(() => {
      let canceled = false
      let created = null
      if (!src) { setObjSrc(null); return }

      (async () => {
        try {
          if (src.startsWith('blob:')) { if (!canceled) setObjSrc(src); return }

          if (src.startsWith('data:')) {
            try {
              const res = await fetch(src)
              const blob = await res.blob()
              created = URL.createObjectURL(blob)
              if (!canceled) setObjSrc(created)
            } catch (e) { console.warn('[PostMedia] data URI->blob failed', e); if (!canceled) setObjSrc(src) }
            return
          }

          const trimmed = String(src).replace(/\s+/g, '')
          if (trimmed.length > 200 && /^[A-Za-z0-9+/=]+$/.test(trimmed)) {
            try {
              const binary = atob(trimmed)
              const bytes = new Uint8Array(binary.length)
              for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i)
              const blob = new Blob([bytes], { type: 'image/jpeg' })
              created = URL.createObjectURL(blob)
              if (!canceled) setObjSrc(created)
            } catch (e) { console.warn('[PostMedia] base64->blob failed', e); if (!canceled) setObjSrc(src) }
            return
          }

          if (!canceled) setObjSrc(src)
        } catch (e) { console.warn('[PostMedia] failed to resolve src', src, e); if (!canceled) setObjSrc(src) }
      })()

      return () => { canceled = true; if (created) URL.revokeObjectURL(created) }
    }, [src])

    return (
      <>
        {src ? (
          isVideo ? (
            <video src={objSrc || src} className="h-[360px] w-full object-cover" controls onLoadedData={() => setLoaded(true)} onError={() => setLoaded(false)} />
          ) : (
            <img src={objSrc || src} alt="" onLoad={() => setLoaded(true)} onError={() => setLoaded(false)} className="h-[360px] w-full object-cover" loading="lazy" />
          )
        ) : (
          <div className="h-[360px] w-full bg-white/5" />
        )}

        {!loaded && (
          <div className="absolute inset-0 flex items-center justify-center bg-black/30">
            <div className="animate-spin rounded-full h-10 w-10 border-t-2 border-b-2 border-white/70" />
          </div>
        )}

        <div className="pointer-events-none absolute inset-0 bg-gradient-to-t from-black/45 via-black/15 to-transparent" />
      </>
    )
  }

  // Share handler: open native share or copy URL and increment local share count
  const handleShare = async (id) => {
    const url = `${window.location.origin}/post/${id}`
    try {
      if (navigator.share) {
        await navigator.share({ title: 'Check this post', url })
      } else if (navigator.clipboard) {
        await navigator.clipboard.writeText(url)
        try { console.log('[Home] copied post URL to clipboard:', url) } catch (e) {}
      } else {
        try { console.log('[Home] share URL (fallback):', url) } catch (e) {}
      }

      // optimistic local increment (will be replaced with server value when available)
      setPosts((prev) => prev.map((p) => (p.id === id ? ({ ...p, shareCount: Number(p.shareCount || 0) + 1 }) : p)))

      // Try to call backend share endpoint if present (best-effort)
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
        // ignore server failure; we already updated locally
        console.warn('share API failed', e)
      }
    } catch (e) {
      console.error('share failed', e)
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

      <div className="relative z-10 px-4 pb-16 pt-14 sm:px-8 lg:px-12 xl:px-16">
        <header className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <p className="text-xs uppercase tracking-[0.25em] text-slate-300/80">ThatOtakuNetwork</p>
            <div className="flex items-center gap-3">
              <h1 className="mt-2 text-4xl font-semibold text-white">Feed</h1>

              {/* All / Following switch */}
              <div className="mt-3 ml-4 flex items-center rounded-full bg-white/5 p-1 text-sm font-semibold" role="tablist" aria-label="Feed scope">
                <button
                  type="button"
                  onClick={() => { setFeedScope('all'); setFeedPage(0); setFeedHasMore(true); setPosts([]); loadFeed('all', 0, false) }}
                  role="tab"
                  aria-selected={feedScope === 'all'}
                  className={`rounded-full px-3 py-1 transition ${feedScope === 'all' ? 'bg-white/10 text-white' : 'text-slate-300 hover:bg-white/6'}`}
                >
                  All
                </button>
                <button
                  type="button"
                  onClick={() => { setFeedScope('following'); setFeedPage(0); setFeedHasMore(true); setPosts([]); loadFeed('following', 0, false) }}
                  role="tab"
                  aria-selected={feedScope === 'following'}
                  className={`rounded-full px-3 py-1 transition ${feedScope === 'following' ? 'bg-white/10 text-white' : 'text-slate-300 hover:bg-white/6'}`}
                >
                  Following
                </button>
              </div>

              {/* <button onClick={fetchRecommendationFeed} disabled={loadingFeed} className="mt-2 rounded-md px-3 py-1.5 text-sm font-semibold bg-white/5 hover:bg-white/10 disabled:opacity-50" aria-label="Refresh feed">{loadingFeed ? 'Refreshing...' : 'Refresh Feed'}</button> */}
            </div>
            <p className="mt-1 text-sm text-slate-200/85 max-w-2xl">Fresh drops from your circles. Tap in, react, and favorite the best takes.</p>
            {alertBanner && <div className="mt-3"><AlertBanner status={alertBanner.status} message={alertBanner.message} /></div>}
          </div>
          {/* Accent color picker removed, now only in Profile settings */}
        </header>


        <main className="mt-8 grid gap-6 md:grid-cols-[20rem_1fr] lg:grid-cols-[20rem_1fr_340px] xl:grid-cols-[20rem_1fr_380px]">

        <NavigationBar accent={accent} variant="inline" />
          <section className="space-y-5">

            {/* Empty state messages */}
            {!loadingFeed && posts.length === 0 && (
              <div className="rounded-2xl border border-white/10 bg-white/5 p-6 text-center text-slate-300">
                {feedScope === 'following' ? (
                  <div>
                    <p className="text-lg font-semibold text-white">No posts from accounts you follow yet</p>
                    <p className="mt-2 text-sm">Follow others to see their posts here, or switch to All to view recommendations.</p>
                  </div>
                ) : (
                  <div>
                    <p className="text-lg font-semibold text-white">No posts to show</p>
                    <p className="mt-2 text-sm">Try refreshing or check back later for new posts.</p>
                  </div>
                )}
              </div>
            )}

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
                <div
                  className="flex items-center gap-3 px-5 py-4"
                  {...(post.authorId ? { role: 'button', tabIndex: 0, onClick: () => { window.location.href = `https://thatotakunetwork.netlify.app/friend-profile/${encodeURIComponent(post.authorId)}` }, onKeyDown: (e) => { if (e.key === 'Enter') { window.location.href = `https://thatotakunetwork.netlify.app/friend-profile/${encodeURIComponent(post.authorId)}` } } } : {})}
                >
                  <div className="h-11 w-11 rounded-full border border-white/15 bg-white/10 flex items-center justify-center text-white/90" style={{ boxShadow: `0 10px 25px ${accent.glow}` }}>
                    <Sparkles className="h-5 w-5" />
                  </div>
                  <div>
                  <p className="text-sm font-semibold text-white">{post.author}</p>
                  <p className="text-sm text-slate-200/85 leading-relaxed ">{post.handle}</p>
                  {post.time && <div className="text-xs text-slate-400">{new Date(post.time).toLocaleString()}</div>}
                  </div>
                </div>

                <div className="relative overflow-hidden">
                  <PostMedia contentId={post.id} fallback={post.image} />

                  {/* Hover overlay: center heart and comment icons; stay hidden until hovered (uses parent's .group) */}
                  <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
                    <div className="flex items-center gap-6 opacity-0 transform scale-95 transition-all duration-150 group-hover:opacity-100 group-hover:scale-100 pointer-events-auto">
                      <button
                        type="button"
                        onClick={(e) => { e.stopPropagation(); toggleReaction(post.id, 'likes') }}
                        aria-label="Like post"
                        className="rounded-full bg-white/10 hover:bg-white/20 p-3 flex items-center justify-center shadow-lg focus:outline-none focus:ring-2 focus:ring-white/20"
                      >
                        <ThumbsUp className="h-6 w-6 text-white" />
                      </button>

                      <button
                        type="button"
                        onClick={(e) => { e.stopPropagation(); navigate(`/post/${post.id}`, { state: { post } }) }}
                        aria-label="View comments"
                        className="rounded-full bg-white/10 hover:bg-white/20 p-3 flex items-center justify-center shadow-lg focus:outline-none focus:ring-2 focus:ring-white/20"
                      >
                        <MessageCircle className="h-6 w-6 text-white" />
                      </button>
                    </div>
                  </div>
                </div>

                <div className="space-y-3 px-5 py-4">
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <p className="text-sm uppercase tracking-[0.2em] text-slate-300/80">{post.anime}</p>
                      <h3 className="text-lg font-semibold text-white">{post.title}</h3>
                    </div>
                    <button
                      type="button"
                      onClick={(e) => { e.stopPropagation(); toggleReaction(post.id, 'isFaved') }}
                      className={`rounded-full border px-3 py-2 text-sm font-semibold transition ${
                        post.isFaved ? 'border-white/30 bg-white/15 text-white' : 'border-white/10 bg-white/5 text-slate-200/80 hover:text-white'
                      }`}
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

            {/* Infinite scroll sentinel and buffer spinner */}
            <div ref={sentinelRef} className="py-6 flex items-center justify-center">
              {mediaLoading ? (
                <div className="animate-spin rounded-full h-10 w-10 border-t-2 border-b-2 border-white/70" />
              ) : mediaHasMore ? (
                <div className="text-sm text-slate-400">Scroll to load more</div>
              ) : null}
            </div>
          </section>

          <aside className="hidden lg:block space-y-4 rounded-3xl border border-white/10 bg-white/5 p-6 shadow-[0_20px_70px_rgba(0,0,0,0.4)] backdrop-blur-2xl" style={{ boxShadow: `0 20px 70px rgba(0,0,0,0.4), 0 0 24px ${accent.glow}` }}>
            <p className="text-xs uppercase tracking-[0.25em] text-slate-300/80">Groups</p>
            <div className="space-y-3 text-sm text-white/85">
              {loadingGroups ? (
                <div className="rounded-2xl border border-white/10 bg-white/10 p-4">Loading groups...</div>
              ) : groupsError ? (
                <div className="w-full"><AlertBanner status="error" message={groupsError} /></div>
              ) : groups.length === 0 ? (
                <div className="w-full max-w-2xl rounded-2xl border border-white/10 bg-white/5 p-4 text-center">No Groups to show, Join Groups so they arrive here</div>
              ) : (
                <div className="space-y-3">
                  {groups.map((g) => (
                    <div key={g.id} onClick={() => navigate(`/group/${g.id}`)} className="cursor-pointer rounded-2xl border border-white/10 bg-white/5 p-4 hover:bg-white/8 transition">
                      <div className="flex items-center gap-3">
                        <div className="h-10 w-10 rounded-xl bg-white/8 flex items-center justify-center text-sm font-semibold text-white">{(g.name || '').slice(0,2)}</div>
                        <div>
                          <div className="font-semibold text-white">{g.name}</div>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </aside>
        </main>
      </div>

      <NavigationBar accent={accent} variant="mobile" />

      <AnimatePresence mode="wait">
        {showAccentLoader && <LoadingScreen key="loader-accent" accent={accent} />}
      </AnimatePresence>
    </div>
  )
}

export default Home
